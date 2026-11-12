package us.rin.gui.clickgui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import us.rin.gui.common.RenderUtils;
import us.rin.scripts.ScriptRuntime;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static us.rin.gui.common.CommonUi.*;

public class ScriptsComponent {

    private static final String TITLE = "✨ Scripts";
    private static final String SUBTITLE = "Write a simple script to build exploits";

    private static final int TITLE_SIZE = 24;
    private static final int SUBTITLE_SIZE = 14;

    private static final int PANEL_RADIUS = 8;

    private static final int CODE_FONT_SIZE = 13;
    private static final int HEADER_FONT_SIZE = 14;
    private static final int GUTTER_W = 34;

    private static final String DEFAULT_SCRIPT = """
            start
            
            debug.print("Hello RedSignalClient");
            
            stop
            """;

    private static final float SCROLL_SPEED = 18f;
    private static final int AUTOCOMPLETE_MAX_ITEMS = 6;
    private static final int AUTOCOMPLETE_ITEM_H = 16;

    private int lastX, lastY, lastW, lastH;
    private double lastScreenOriginX, lastScreenOriginY;
    private float lastGuiScale;

    private int editorX, editorY, editorW, editorH;

    private int footerX, footerY, footerW;

    private float scrollOffset = 0f;
    private float targetScroll = 0f;

    private float hScrollOffset = 0f;
    private float targetHScroll = 0f;

    private boolean focused = false;
    private String code = DEFAULT_SCRIPT;
    private int caretIndex = 0;

    private int selectionAnchor = 0;
    private int selectionStart = 0;
    private int selectionEnd = 0;
    private boolean selecting = false;

    private boolean autocompleteOpen = false;
    private int autocompleteSelected = 0;
    private final List<String> autocompleteItems = new ArrayList<>();
    private int autocompleteX = 0, autocompleteY = 0, autocompleteW = 0, autocompleteH = 0;
    private int caretScreenX = 0;
    private int caretScreenY = 0;

    private String lexCacheText = null;
    private final List<List<LexToken>> lexTokensByLine = new ArrayList<>();

    private final List<String> outputLog = new ArrayList<>();

    private int outputScrollLines = 0;
    private int outputX = 0, outputY = 0, outputW = 0, outputH = 0;

    private float maxLineWidthPx = 0f;
    private int[] lineStartIndex = null;

    private volatile boolean running = false;
    private volatile Thread runningThread = null;

    public void onOpen() {
    }

    public void onClose() {
        stopRun();
        focused = false;
        autocompleteOpen = false;
    }

    public ScriptsState exportState() {
        ScriptsState st = new ScriptsState();
        st.code = this.code;
        st.caretIndex = this.caretIndex;
        st.scrollOffset = this.scrollOffset;
        st.targetScroll = this.targetScroll;
        st.hScrollOffset = this.hScrollOffset;
        st.targetHScroll = this.targetHScroll;
        st.outputScrollLines = this.outputScrollLines;
        return st;
    }

    public void applyState(ScriptsState st) {
        if (st == null) return;
        stopRun();
        focused = false;

        if (st.code != null) {
            this.code = st.code;
        }
        this.caretIndex = Math.max(0, Math.min(this.code.length(), st.caretIndex));

        this.selectionAnchor = this.caretIndex;
        this.selectionStart = this.caretIndex;
        this.selectionEnd = this.caretIndex;

        this.scrollOffset = Math.max(0f, st.scrollOffset);
        this.targetScroll = Math.max(0f, st.targetScroll);

        this.hScrollOffset = Math.max(0f, st.hScrollOffset);
        this.targetHScroll = Math.max(0f, st.targetHScroll);

        this.outputScrollLines = Math.max(0, st.outputScrollLines);
        synchronized (outputLog) {
            outputLog.clear();
        }
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, int themeColor,
                       double screenOriginX, double screenOriginY, float guiScale) {
        this.lastX = x;
        this.lastY = y;
        this.lastW = width;
        this.lastH = height;
        this.lastScreenOriginX = screenOriginX;
        this.lastScreenOriginY = screenOriginY;
        this.lastGuiScale = guiScale;

        PoseStack pose = graphics.pose();

        int cy = y;

        float titleAscent = RenderUtils.getCustomFontAscent(TITLE_SIZE, true);
        RenderUtils.drawCustomString(pose, TITLE, x, cy + titleAscent, themeColor, true, TITLE_SIZE);
        cy += RenderUtils.getCustomFontHeight(TITLE_SIZE, true) + 6;

        float subtitleAscent = RenderUtils.getCustomFontAscent(SUBTITLE_SIZE, false);
        RenderUtils.drawCustomString(pose, SUBTITLE, x, cy + subtitleAscent, new Color(209, 213, 219).getRGB(), false, SUBTITLE_SIZE);
        cy += RenderUtils.getCustomFontHeight(SUBTITLE_SIZE, false) + 24;

        int contentTop = cy;
        int contentHeight = Math.max(0, height - (contentTop - y));

        int reservedFooter = 175;
        editorX = x;
        editorY = contentTop;
        editorW = Math.max(0, width - 45);
        editorH = Math.max(0, contentHeight - reservedFooter);

        footerX = editorX;
        footerY = editorY + editorH + 10;
        footerW = editorW;

        RenderUtils.drawRoundedRect(pose, editorX, editorY, editorW, editorH, PANEL_RADIUS, new Color(18, 18, 18, 200).getRGB());
        RenderUtils.drawRoundedRectOutline(pose, editorX, editorY, editorW, editorH, PANEL_RADIUS, 1, new Color(55, 65, 81).getRGB());

        renderEditor(pose, themeColor);
        renderFooter(pose, mouseX, mouseY, themeColor);
    }

    private void renderEditor(PoseStack pose, int themeColor) {
        int headerY = editorY + 10;
        RenderUtils.drawCustomString(pose, "Script", editorX + 10, headerY, new Color(255, 255, 255).getRGB(), true, HEADER_FONT_SIZE);

        String status = running ? "Status: Running" : "Status: Idle";
        int statusColor = running ? new Color(34, 197, 94).getRGB() : new Color(156, 163, 175).getRGB();
        float statusW = RenderUtils.getCustomStringWidth(status, false, 12);
        RenderUtils.drawCustomString(pose, status, editorX + editorW - 10 - statusW, headerY + 2, statusColor, false, 12);

        int innerTop = editorY + 30;
        int innerH = Math.max(0, editorH - 36);
        int innerX = editorX + 10;
        int innerW = Math.max(0, editorW - 20);

        int lineH = Math.max(12, RenderUtils.getCustomFontHeight(CODE_FONT_SIZE, false) + 3);
        String[] lines = (code == null ? "" : code).split("\\n", -1);

        ensureLexed();

        float maxScroll = Math.max(0f, lines.length * (float) lineH - (innerH - 10));
        targetScroll = Math.max(0f, Math.min(targetScroll, maxScroll));
        if (Math.abs(targetScroll - scrollOffset) > 0.5f) {
            scrollOffset += (targetScroll - scrollOffset) * 0.15f;
        } else {
            scrollOffset = targetScroll;
        }

        float visibleCodeW = Math.max(0f, (innerW - 10) - GUTTER_W);
        float maxHScroll = Math.max(0f, maxLineWidthPx - visibleCodeW);
        targetHScroll = Math.max(0f, Math.min(targetHScroll, maxHScroll));
        if (Math.abs(targetHScroll - hScrollOffset) > 0.5f) {
            hScrollOffset += (targetHScroll - hScrollOffset) * 0.15f;
        } else {
            hScrollOffset = targetHScroll;
        }

        int scissorX = (int) Math.round(lastScreenOriginX + (innerX - lastX) * lastGuiScale);
        int scissorY = (int) Math.round(lastScreenOriginY + (innerTop - lastY) * lastGuiScale);
        int scissorW = Math.round(innerW * lastGuiScale);
        int scissorH = Math.round(innerH * lastGuiScale);
        RenderUtils.enableScissor(scissorX, scissorY, scissorW, scissorH);

        pose.pushPose();
        pose.translate(0, -scrollOffset, 0);

        int startLine = Math.max(0, (int) Math.floor(scrollOffset / lineH));
        int visible = innerH / lineH + 3;
        int endLine = Math.min(lines.length - 1, startLine + visible);
        int y = innerTop + 6 + startLine * lineH;

        int selA = Math.min(selectionStart, selectionEnd);
        int selB = Math.max(selectionStart, selectionEnd);

        for (int i = startLine; i <= endLine; i++) {
            int drawY = y + (i - startLine) * lineH;
            RenderUtils.drawCustomString(pose, String.valueOf(i + 1), innerX, drawY + 2, new Color(156, 163, 175).getRGB(), false, 11);

            if (focused && selA != selB && lineStartIndex != null && i >= 0 && i < lines.length) {
                int ls = i < lineStartIndex.length ? lineStartIndex[i] : 0;
                String lineText = lines[i] == null ? "" : lines[i];
                int lineLen = lineText.length();

                int a = Math.max(0, Math.min(lineLen, selA - ls));
                int b = Math.max(0, Math.min(lineLen, selB - ls));
                if (a != b) {
                    int leftCol = Math.min(a, b);
                    int rightCol = Math.max(a, b);
                    String pfx = leftCol == 0 ? "" : lineText.substring(0, leftCol);
                    String mid = lineText.substring(0, rightCol);
                    float x1 = innerX + GUTTER_W - hScrollOffset + RenderUtils.getCustomStringWidth(pfx, false, CODE_FONT_SIZE);
                    float x2 = innerX + GUTTER_W - hScrollOffset + RenderUtils.getCustomStringWidth(mid, false, CODE_FONT_SIZE);
                    RenderUtils.fillRect(pose, (int) x1, drawY + 2, Math.max(1, (int) (x2 - x1)), lineH - 4, new Color(59, 130, 246, 110).getRGB());
                }
            }

            float tx = innerX + GUTTER_W - hScrollOffset;
            List<LexToken> toks = i >= 0 && i < lexTokensByLine.size() ? lexTokensByLine.get(i) : null;
            if (toks == null) {
                RenderUtils.drawCustomString(pose, lines[i], tx, drawY + 2, new Color(229, 231, 235).getRGB(), false, CODE_FONT_SIZE);
            } else {
                for (LexToken t : toks) {
                    if (t == null || t.text == null || t.text.isEmpty()) continue;
                    int col = colorFor(t.kind);
                    RenderUtils.drawCustomString(pose, t.text, tx, drawY + 2, col, false, CODE_FONT_SIZE);
                    tx += RenderUtils.getCustomStringWidth(t.text, false, CODE_FONT_SIZE);
                }
            }
        }

        if (focused) {
            int[] lc = caretLineCol(code, caretIndex);
            int caretLine = lc[0];
            int caretCol = lc[1];

            if (caretLine >= startLine && caretLine <= endLine) {
                int caretY = innerTop + 6 + caretLine * lineH;
                String lineText = caretLine < lines.length ? lines[caretLine] : "";
                int safeCol = Math.max(0, Math.min(lineText.length(), caretCol));
                String prefix = safeCol == 0 ? "" : lineText.substring(0, safeCol);
                float px = innerX + GUTTER_W - hScrollOffset + RenderUtils.getCustomStringWidth(prefix, false, CODE_FONT_SIZE);
                caretScreenX = (int) px;
                caretScreenY = (int) (caretY - scrollOffset);
                boolean show = (System.currentTimeMillis() / 500L) % 2L == 0L;
                if (show) {
                    RenderUtils.fillRect(pose, (int) px, caretY + 2, 1, lineH - 4, new Color(255, 255, 255).getRGB());
                }
            }
        }

        pose.popPose();
        RenderUtils.disableScissor();

        if (focused) {
            RenderUtils.drawRoundedRectOutline(pose, editorX, editorY, editorW, editorH, PANEL_RADIUS, 1, themeColor);
        }

        renderAutocomplete(pose);
    }

    private void renderAutocomplete(PoseStack pose) {
        if (!focused || !autocompleteOpen || autocompleteItems.isEmpty()) return;

        int max = Math.min(AUTOCOMPLETE_MAX_ITEMS, autocompleteItems.size());
        int w = 140;
        for (int i = 0; i < max; i++) {
            String s = autocompleteItems.get(i);
            w = Math.max(w, (int) RenderUtils.getCustomStringWidth(s, false, 12) + 14);
        }
        w = Math.min(260, w);

        int h = 8 + max * AUTOCOMPLETE_ITEM_H;

        int x = caretScreenX;
        int y = caretScreenY + AUTOCOMPLETE_ITEM_H + 6;

        int maxX = editorX + editorW - 10 - w;
        int maxY = editorY + editorH - 10 - h;
        x = clamp(x, editorX + 10, Math.max(editorX + 10, maxX));
        y = clamp(y, editorY + 10, Math.max(editorY + 10, maxY));

        autocompleteX = x;
        autocompleteY = y;
        autocompleteW = w;
        autocompleteH = h;

        RenderUtils.drawRoundedRect(pose, x, y, w, h, 6, new Color(15, 15, 15, 235).getRGB());
        RenderUtils.drawRoundedRectOutline(pose, x, y, w, h, 6, 1, new Color(55, 65, 81).getRGB());

        int yy = y + 4;
        for (int i = 0; i < max; i++) {
            boolean sel = i == autocompleteSelected;
            if (sel) {
                RenderUtils.drawRoundedRect(pose, x + 4, yy - 1, w - 8, AUTOCOMPLETE_ITEM_H, 5, new Color(31, 41, 55, 220).getRGB());
            }
            RenderUtils.drawCustomString(pose, autocompleteItems.get(i), x + 8, yy + 2, new Color(229, 231, 235).getRGB(), false, 12);
            yy += AUTOCOMPLETE_ITEM_H;
        }
    }

    private void renderFooter(PoseStack pose, int mouseX, int mouseY, int themeColor) {
        int btnW = 90;
        int btnH = 28;
        int gap = 10;

        int clearX = footerX + footerW - btnW;
        int stopX = clearX - gap - btnW;
        int execX = stopX - gap - btnW;

        renderButton(pose, "Execute", execX, footerY, btnW, btnH, mouseX, mouseY, themeColor);
        renderButton(pose, "Stop", stopX, footerY, btnW, btnH, mouseX, mouseY, themeColor);
        renderButton(pose, "Clear", clearX, footerY, btnW, btnH, mouseX, mouseY, themeColor);

        int outY = footerY + btnH + 10;
        int outH = Math.max(0, (lastY + lastH) - outY - 6);

        this.outputX = footerX;
        this.outputY = outY;
        this.outputW = footerW;
        this.outputH = outH;

        RenderUtils.drawRoundedRect(pose, footerX, outY, footerW, outH, PANEL_RADIUS, new Color(20, 20, 20, 200).getRGB());
        RenderUtils.drawRoundedRectOutline(pose, footerX, outY, footerW, outH, PANEL_RADIUS, 1, new Color(55, 65, 81).getRGB());

        RenderUtils.drawCustomString(pose, "Preview Output", footerX + 10, outY + 10, new Color(255, 255, 255).getRGB(), true, 14);

        int textY = outY + 28;
        int maxLines = Math.max(1, (outH - 36) / 14);

        List<String> lines;
        synchronized (outputLog) {
            lines = new ArrayList<>(outputLog);
        }

        int maxScroll = Math.max(0, lines.size() - maxLines);
        outputScrollLines = Math.max(0, Math.min(outputScrollLines, maxScroll));
        int start = Math.max(0, lines.size() - maxLines - outputScrollLines);
        int end = Math.min(lines.size(), start + maxLines);
        int ly = textY;

        int clipX = (int) Math.round(lastScreenOriginX + ((footerX + 10) - lastX) * lastGuiScale);
        int clipY = (int) Math.round(lastScreenOriginY + (textY - lastY) * lastGuiScale);
        int clipW = Math.round(Math.max(0, (footerW - 20)) * lastGuiScale);
        int clipH = Math.round(Math.max(0, (outH - 36)) * lastGuiScale);
        RenderUtils.enableScissor(clipX, clipY, clipW, clipH);

        for (int i = start; i < end; i++) {
            String s = lines.get(i);
            s = trimToWidth(s, Math.max(0, footerW - 20), false, 12);
            RenderUtils.drawCustomString(pose, s, footerX + 10, ly, new Color(209, 213, 219).getRGB(), false, 12);
            ly += 14;
        }

        RenderUtils.disableScissor();

        this.execBtnX = execX;
        this.stopBtnX = stopX;
        this.clearBtnX = clearX;
        this.btnY = footerY;
        this.btnW = btnW;
        this.btnH = btnH;
    }

    private int execBtnX, stopBtnX, clearBtnX, btnY, btnW, btnH;

    private void renderButton(PoseStack pose, String text, int x, int y, int w, int h, int mouseX, int mouseY, int themeColor) {
        boolean hover = UiUtils.isInside(mouseX, mouseY, x, y, w, h);
        int color = hover ? UiUtils.lightenColor(themeColor) : themeColor;
        RenderUtils.drawRoundedRect(pose, x, y, w, h, 6, color);
        float tw = RenderUtils.getCustomStringWidth(text, true, 12);
        RenderUtils.drawCustomString(pose, text, x + (w - tw) / 2f, y + 6, new Color(255, 255, 255).getRGB(), true, 12);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!UiUtils.isInside(mouseX, mouseY, lastX, lastY, lastW, lastH)) return false;

        if (focused && autocompleteOpen && UiUtils.isInside(mouseX, mouseY, autocompleteX, autocompleteY, autocompleteW, autocompleteH)) {
            int relY = (int) mouseY - (autocompleteY + 4);
            int idx = relY / AUTOCOMPLETE_ITEM_H;
            if (idx >= 0 && idx < Math.min(AUTOCOMPLETE_MAX_ITEMS, autocompleteItems.size())) {
                autocompleteSelected = idx;
                acceptAutocomplete();
                return true;
            }
        }

        if (UiUtils.isInside(mouseX, mouseY, execBtnX, btnY, btnW, btnH)) {
            startRun();
            return true;
        }

        if (UiUtils.isInside(mouseX, mouseY, stopBtnX, btnY, btnW, btnH)) {
            stopRun();
            return true;
        }

        if (UiUtils.isInside(mouseX, mouseY, clearBtnX, btnY, btnW, btnH)) {
            clear();
            return true;
        }

        if (UiUtils.isInside(mouseX, mouseY, editorX, editorY, editorW, editorH)) {
            focused = true;
            autocompleteOpen = false;

            selecting = true;

            int innerTop = editorY + 30;
            int innerX = editorX + 10;
            int lineH = Math.max(12, RenderUtils.getCustomFontHeight(CODE_FONT_SIZE, false) + 3);
            String[] lines = (code == null ? "" : code).split("\\n", -1);

            double localY = (mouseY - (innerTop + 6)) + scrollOffset;
            int line = (int) Math.floor(localY / lineH);
            line = Math.max(0, Math.min(lines.length - 1, line));

            double localX = (mouseX - (innerX + GUTTER_W)) + hScrollOffset;
            int col = getColumnForX(lines[line], localX);
            int newCaret = caretIndexFromLineCol(lines, line, col);

            if (Screen.hasShiftDown()) {
                if (!hasSelection()) selectionAnchor = caretIndex;
                caretIndex = newCaret;
                updateSelectionFromAnchor();
            } else {
                caretIndex = newCaret;
                selectionAnchor = caretIndex;
                selectionStart = caretIndex;
                selectionEnd = caretIndex;
            }

            updateAutocomplete(false);

            return true;
        }

        focused = false;
        autocompleteOpen = false;
        selecting = false;
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) selecting = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!focused) return;
        if (!selecting) return;
        if (button != 0) return;

        int innerTop = editorY + 30;
        int innerX = editorX + 10;
        int innerH = Math.max(0, editorH - 36);
        int innerW = Math.max(0, editorW - 20);
        if (!UiUtils.isInside(mouseX, mouseY, innerX, innerTop, innerW, innerH)) return;

        int lineH = Math.max(12, RenderUtils.getCustomFontHeight(CODE_FONT_SIZE, false) + 3);
        String t = code == null ? "" : code;
        String[] lines = t.split("\\n", -1);

        double localY = (mouseY - (innerTop + 6)) + scrollOffset;
        int line = (int) Math.floor(localY / lineH);
        line = Math.max(0, Math.min(lines.length - 1, line));

        double localX = (mouseX - (innerX + GUTTER_W)) + hScrollOffset;
        int col = getColumnForX(lines[line], localX);
        caretIndex = caretIndexFromLineCol(lines, line, col);
        updateSelectionFromAnchor();
        updateAutocomplete(false);
        ensureCaretVisible();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0) return false;

        if (UiUtils.isInside(mouseX, mouseY, editorX, editorY, editorW, editorH)) {
            if (Screen.hasShiftDown()) {
                targetHScroll -= (float) (delta * SCROLL_SPEED);
            } else {
                targetScroll -= (float) (delta * SCROLL_SPEED);
            }
            return true;
        }

        if (UiUtils.isInside(mouseX, mouseY, outputX, outputY, outputW, outputH)) {
            int step = (int) Math.round(Math.abs(delta) * 3);
            if (step <= 0) step = 1;
            if (delta > 0) {
                outputScrollLines += step;
            } else {
                outputScrollLines -= step;
            }
            outputScrollLines = Math.max(0, outputScrollLines);
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (autocompleteOpen && !autocompleteItems.isEmpty()) {
            if (keyCode == 256) {
                autocompleteOpen = false;
                return true;
            }
            if (keyCode == 265) {
                autocompleteSelected = Math.max(0, autocompleteSelected - 1);
                return true;
            }
            if (keyCode == 264) {
                int max = Math.min(AUTOCOMPLETE_MAX_ITEMS, autocompleteItems.size());
                autocompleteSelected = Math.min(max - 1, autocompleteSelected + 1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                acceptAutocomplete();
                return true;
            }
            if (keyCode == 258) {
                if ((modifiers & 1) != 0) {
                    autocompleteOpen = false;
                } else {
                    acceptAutocomplete();
                    return true;
                }
            }
        }

        String t = code == null ? "" : code;

        if ((modifiers & 2) != 0 && keyCode == 65) {
            selectionAnchor = 0;
            selectionStart = 0;
            selectionEnd = t.length();
            caretIndex = t.length();
            ensureCaretVisible();
            autocompleteOpen = false;
            return true;
        }

        if ((modifiers & 2) != 0 && keyCode == 67) {
            if (hasSelection()) {
                int a = Math.min(selectionStart, selectionEnd);
                int b = Math.max(selectionStart, selectionEnd);
                Minecraft.getInstance().keyboardHandler.setClipboard(t.substring(a, b));
                return true;
            }
        }

        if ((modifiers & 2) != 0 && keyCode == 88) {
            if (hasSelection()) {
                int a = Math.min(selectionStart, selectionEnd);
                int b = Math.max(selectionStart, selectionEnd);
                Minecraft.getInstance().keyboardHandler.setClipboard(t.substring(a, b));
                deleteSelection();
                autocompleteOpen = false;
                return true;
            }
        }

        if ((modifiers & 2) != 0 && keyCode == 86) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!clip.isEmpty()) {
                String norm = clip.replace("\r\n", "\n").replace("\r", "\n");
                insertAtCaret(norm);
                autocompleteOpen = false;
            }
            return true;
        }

        if (keyCode == 258) {
            if ((modifiers & 1) != 0) {
                unindentLine();
            } else {
                insertAtCaret("    ");
            }
            return true;
        }

        if (keyCode == 259) {
            if (hasSelection()) {
                deleteSelection();
            } else if (caretIndex > 0 && caretIndex <= t.length()) {
                code = t.substring(0, caretIndex - 1) + t.substring(caretIndex);
                caretIndex--;
                selectionAnchor = caretIndex;
                selectionStart = caretIndex;
                selectionEnd = caretIndex;
                updateAutocomplete(false);
                ensureCaretVisible();
            }
            return true;
        }

        if (keyCode == 257 || keyCode == 335) {
            insertAtCaret("\n");
            autocompleteOpen = false;
            return true;
        }

        if (keyCode == 263) {
            int old = caretIndex;
            caretIndex = Math.max(0, caretIndex - 1);
            if ((modifiers & 1) != 0) {
                if (!hasSelection()) selectionAnchor = old;
                updateSelectionFromAnchor();
            } else {
                selectionAnchor = caretIndex;
                selectionStart = caretIndex;
                selectionEnd = caretIndex;
            }
            updateAutocomplete(false);
            ensureCaretVisible();
            return true;
        }

        if (keyCode == 262) {
            int old = caretIndex;
            caretIndex = Math.min(t.length(), caretIndex + 1);
            if ((modifiers & 1) != 0) {
                if (!hasSelection()) selectionAnchor = old;
                updateSelectionFromAnchor();
            } else {
                selectionAnchor = caretIndex;
                selectionStart = caretIndex;
                selectionEnd = caretIndex;
            }
            updateAutocomplete(false);
            ensureCaretVisible();
            return true;
        }

        if (keyCode == 265 || keyCode == 264) {
            String[] lines = t.split("\\n", -1);
            int[] lc = caretLineCol(t, caretIndex);
            int line = lc[0];
            int col = lc[1];

            int old = caretIndex;

            if (keyCode == 265) {
                line = Math.max(0, line - 1);
            } else {
                line = Math.min(lines.length - 1, line + 1);
            }
            caretIndex = caretIndexFromLineCol(lines, line, col);

            if ((modifiers & 1) != 0) {
                if (!hasSelection()) selectionAnchor = old;
                updateSelectionFromAnchor();
            } else {
                selectionAnchor = caretIndex;
                selectionStart = caretIndex;
                selectionEnd = caretIndex;
            }
            updateAutocomplete(false);
            ensureCaretVisible();
            return true;
        }

        if (keyCode == 256) {
            focused = false;
            autocompleteOpen = false;
            selecting = false;
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        if (codePoint < 32 || codePoint == 127) return false;
        insertAtCaret(String.valueOf(codePoint));
        return true;
    }

    private void insertAtCaret(String text) {
        if (text == null || text.isEmpty()) return;
        String t = code == null ? "" : code;
        if (hasSelection()) {
            deleteSelection();
            t = code == null ? "" : code;
        }
        caretIndex = Math.max(0, Math.min(t.length(), caretIndex));
        code = t.substring(0, caretIndex) + text + t.substring(caretIndex);
        caretIndex += text.length();
        selectionAnchor = caretIndex;
        selectionStart = caretIndex;
        selectionEnd = caretIndex;
        updateAutocomplete(false);
        ensureCaretVisible();
    }

    private boolean hasSelection() {
        return selectionStart != selectionEnd;
    }

    private void updateSelectionFromAnchor() {
        selectionStart = selectionAnchor;
        selectionEnd = caretIndex;
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        String t = code == null ? "" : code;
        int a = Math.max(0, Math.min(t.length(), Math.min(selectionStart, selectionEnd)));
        int b = Math.max(0, Math.min(t.length(), Math.max(selectionStart, selectionEnd)));
        code = t.substring(0, a) + t.substring(b);
        caretIndex = a;
        selectionAnchor = caretIndex;
        selectionStart = caretIndex;
        selectionEnd = caretIndex;
        updateAutocomplete(false);
        ensureCaretVisible();
    }

    private void ensureCaretVisible() {
        String t = code == null ? "" : code;
        int[] lc = caretLineCol(t, caretIndex);
        int line = lc[0];

        ensureLexed();

        int innerH = Math.max(0, editorH - 36);
        int innerW = Math.max(0, editorW - 20);
        int lineH = Math.max(12, RenderUtils.getCustomFontHeight(CODE_FONT_SIZE, false) + 3);

        float top = line * (float) lineH;
        float bottom = top + lineH;

        float viewTop = targetScroll;
        float viewBottom = targetScroll + Math.max(0, innerH - 10);

        if (top < viewTop) {
            targetScroll = Math.max(0f, top - 6);
        } else if (bottom > viewBottom) {
            targetScroll = Math.max(0f, bottom - Math.max(0, innerH - 10) + 6);
        }

        float maxScroll;
        {
            String[] lines = t.split("\\n", -1);
            maxScroll = Math.max(0f, lines.length * (float) lineH - (innerH - 10));
        }
        targetScroll = Math.max(0f, Math.min(targetScroll, maxScroll));

        int caretCol = lc[1];
        String[] lines = t.split("\\n", -1);
        String lineText = line >= 0 && line < lines.length ? (lines[line] == null ? "" : lines[line]) : "";
        int safeCol = Math.max(0, Math.min(lineText.length(), caretCol));
        String prefix = safeCol == 0 ? "" : lineText.substring(0, safeCol);
        float caretX = RenderUtils.getCustomStringWidth(prefix, false, CODE_FONT_SIZE);

        float visibleCodeW = Math.max(0f, (innerW - 10) - GUTTER_W);
        float viewLeft = targetHScroll;
        float viewRight = targetHScroll + visibleCodeW;
        if (caretX < viewLeft) {
            targetHScroll = Math.max(0f, caretX - 6);
        } else if (caretX > viewRight - 1) {
            targetHScroll = Math.max(0f, caretX - visibleCodeW + 6);
        }

        float maxHScroll = Math.max(0f, maxLineWidthPx - visibleCodeW);
        targetHScroll = Math.max(0f, Math.min(targetHScroll, maxHScroll));
    }

    private void unindentLine() {
        String t = code == null ? "" : code;
        caretIndex = Math.max(0, Math.min(t.length(), caretIndex));

        int lineStart = t.lastIndexOf('\n', Math.max(0, caretIndex - 1));
        lineStart = lineStart < 0 ? 0 : (lineStart + 1);

        int remove = 0;
        for (int i = 0; i < 4 && lineStart + i < t.length(); i++) {
            if (t.charAt(lineStart + i) != ' ') break;
            remove++;
        }

        if (remove > 0 && caretIndex >= lineStart + remove) {
            code = t.substring(0, lineStart) + t.substring(lineStart + remove);
            caretIndex -= remove;
            selectionAnchor = caretIndex;
            selectionStart = caretIndex;
            selectionEnd = caretIndex;
            updateAutocomplete(false);
            ensureCaretVisible();
        }
    }

    private String trimToWidth(String s, int maxWidth, boolean bold, int fontSize) {
        if (s == null) return "";
        if (maxWidth <= 0) return "";
        if (RenderUtils.getCustomStringWidth(s, bold, fontSize) <= maxWidth) return s;

        String ell = "...";
        float ellW = RenderUtils.getCustomStringWidth(ell, bold, fontSize);
        if (ellW >= maxWidth) return "";

        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String sub = s.substring(0, mid);
            float w = RenderUtils.getCustomStringWidth(sub, bold, fontSize);
            if (w + ellW <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ell;
    }

    private void updateAutocomplete(boolean forceOpen) {
        String t = code == null ? "" : code;
        caretIndex = Math.max(0, Math.min(t.length(), caretIndex));

        int start = caretIndex;
        while (start > 0) {
            char c = t.charAt(start - 1);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) break;
            start--;
        }
        String prefix = t.substring(start, caretIndex);
        if (!forceOpen && prefix.isEmpty()) {
            autocompleteOpen = false;
            return;
        }

        List<String> all = allAutocompleteCandidates();
        autocompleteItems.clear();
        String p = prefix.toLowerCase();
        for (String s : all) {
            if (s == null) continue;
            if (p.isEmpty() || s.toLowerCase().startsWith(p)) {
                autocompleteItems.add(s);
            }
        }

        if (autocompleteItems.isEmpty()) {
            autocompleteOpen = false;
            return;
        }

        autocompleteSelected = Math.max(0, Math.min(autocompleteSelected, Math.min(AUTOCOMPLETE_MAX_ITEMS, autocompleteItems.size()) - 1));
        autocompleteOpen = true;
    }

    private void acceptAutocomplete() {
        if (!autocompleteOpen || autocompleteItems.isEmpty()) return;
        int max = Math.min(AUTOCOMPLETE_MAX_ITEMS, autocompleteItems.size());
        int idx = Math.max(0, Math.min(autocompleteSelected, max - 1));
        String chosen = autocompleteItems.get(idx);
        if (chosen == null) return;

        String t = code == null ? "" : code;
        caretIndex = Math.max(0, Math.min(t.length(), caretIndex));
        int start = caretIndex;
        while (start > 0) {
            char c = t.charAt(start - 1);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) break;
            start--;
        }

        code = t.substring(0, start) + chosen + t.substring(caretIndex);
        caretIndex = start + chosen.length();
        autocompleteOpen = false;
    }

    private List<String> allAutocompleteCandidates() {
        List<String> out = new ArrayList<>(KEYWORDS);
        out.add("debug.print");
        out.add("send");
        out.add("sleep");
        out.add("yield");
        out.add("assert");
        out.add("env");
        out.add("env.player");
        out.add("env.containerId");
        out.add("env.stateId");
        out.add("env.tick");
        out.add("nbt");
        out.add("nbt.fromJson");
        out.add("packet");
        out.add("packet.pickup");
        out.add("packet.quickMove");
        out.add("packet.throw");
        out.add("ClickType");
        out.add("ClickType.values");

        out.add("+");
        out.add("-");
        out.add("*");
        out.add("/");
        out.add("%");

        out.add(".repeat");
        out.add(".toString");
        out.add(".toJson");
        out.add(".has");
        out.add(".get");
        out.add(".remove");
        out.add(".size");
        out.add(".clone");

        out.add("create.item");
        out.add("create.compoundTag");
        out.add("create.listTag");
        out.add("create.stringTag");
        out.add("create.intTag");
        out.add("create.doubleTag");
        out.add("create.byteTag");
        out.add("create.itemMap");
        out.add("create.containerClick");

        out.add("PICKUP");
        out.add("QUICK_MOVE");
        out.add("SWAP");
        out.add("CLONE");
        out.add("THROW");
        out.add("QUICK_CRAFT");
        out.add("PICKUP_ALL");

        out.add("ClickType.PICKUP");
        out.add("ClickType.QUICK_MOVE");
        out.add("ClickType.SWAP");
        out.add("ClickType.CLONE");
        out.add("ClickType.THROW");
        out.add("ClickType.QUICK_CRAFT");
        out.add("ClickType.PICKUP_ALL");
        return out;
    }


    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void clear() {
        stopRun();
        focused = false;
        code = "";
        caretIndex = 0;
        selectionAnchor = 0;
        selectionStart = 0;
        selectionEnd = 0;
        synchronized (outputLog) {
            outputLog.clear();
        }
        outputScrollLines = 0;
        scrollOffset = 0f;
        targetScroll = 0f;
        hScrollOffset = 0f;
        targetHScroll = 0f;
    }

    private void startRun() {
        if (running) return;
        synchronized (outputLog) {
            outputLog.clear();
            outputLog.add("Compiling...");
        }

        outputScrollLines = 0;

        ScriptsState snapshot = exportState();

        running = true;
        Thread t = new Thread(() -> runProgram(snapshot), "Script-Runner");
        t.setDaemon(true);
        runningThread = t;
        t.start();
    }

    private void stopRun() {
        running = false;
        Thread t = runningThread;
        runningThread = null;
        if (t != null) {
            try {
                t.interrupt();
            } catch (Exception ignored) {
            }
        }
    }

    private void runProgram(ScriptsState program) {
        try {
            if (program == null || program.code == null || program.code.trim().isEmpty()) {
                logOnMainThread("Script is empty.");
                return;
            }
            logOnMainThread("Running (preview)...");
            ScriptRuntime.run(program.code, () -> running, () -> running = false, this::logOnMainThread);
            logOnMainThread("Done.");
        } catch (ScriptRuntime.ScriptError e) {
            logOnMainThread("Error at " + e.line + ":" + e.col + " - " + e.getMessage());
        } catch (Exception e) {
            logOnMainThread("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            running = false;
            runningThread = null;
        }
    }

    private String extractCallArg(String line) {
        int l = line.indexOf('(');
        int r = line.lastIndexOf(')');
        if (l < 0 || r <= l) return "";
        String inner = line.substring(l + 1, r).trim();
        if (inner.endsWith(";")) inner = inner.substring(0, inner.length() - 1).trim();
        if ((inner.startsWith("\"") && inner.endsWith("\"")) || (inner.startsWith("'") && inner.endsWith("'"))) {
            inner = inner.substring(1, inner.length() - 1);
        }
        return inner;
    }

    private int[] caretLineCol(String text, int index) {
        if (text == null) text = "";
        int idx = Math.max(0, Math.min(text.length(), index));
        int line = 0;
        int col = 0;
        for (int i = 0; i < idx; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return new int[]{line, col};
    }

    private int caretIndexFromLineCol(String[] lines, int line, int col) {
        if (lines == null || lines.length == 0) return 0;
        int l = Math.max(0, Math.min(lines.length - 1, line));
        int c = Math.max(0, Math.min(lines[l] == null ? 0 : lines[l].length(), col));
        int idx = 0;
        for (int i = 0; i < l; i++) {
            idx += (lines[i] == null ? 0 : lines[i].length()) + 1;
        }
        return idx + c;
    }

    private int getColumnForX(String line, double x) {
        if (line == null || line.isEmpty()) return 0;
        if (x <= 0) return 0;

        int col = 0;
        while (col < line.length()) {
            String prefix = line.substring(0, col + 1);
            float w = RenderUtils.getCustomStringWidth(prefix, false, CODE_FONT_SIZE);
            if (w > x) break;
            col++;
        }
        return col;
    }

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "stop",
            "let", "const",
            "func", "return",
            "if", "else",
            "loop", "break", "continue",
            "try", "catch",
            "true", "false", "null"
    ));

    private static final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
            "create", "debug",
            "send",
            "nbt",
            "env",
            "packet",
            "clicktype",
            "sleep",
            "yield",
            "assert"
    ));

    private void ensureLexed() {
        String t = code == null ? "" : code;
        if (t.equals(lexCacheText)) return;
        lexCacheText = t;
        lexTokensByLine.clear();

        maxLineWidthPx = 0f;

        String[] lines = t.split("\\n", -1);
        lineStartIndex = new int[lines.length];
        int idx = 0;
        for (String line : lines) {
            lexTokensByLine.add(lexLine(line == null ? "" : line));
            String lt = line == null ? "" : line;
            maxLineWidthPx = Math.max(maxLineWidthPx, RenderUtils.getCustomStringWidth(lt, false, CODE_FONT_SIZE));
            lineStartIndex[lexTokensByLine.size() - 1] = idx;
            idx += lt.length() + 1;
        }
    }

    private List<LexToken> lexLine(String line) {
        List<LexToken> out = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);

            if (c == ' ' || c == '\t') {
                int j = i + 1;
                while (j < line.length()) {
                    char cc = line.charAt(j);
                    if (cc != ' ' && cc != '\t') break;
                    j++;
                }
                out.add(new LexToken(line.substring(i, j), LexKind.WHITESPACE));
                i = j;
                continue;
            }

            if (c == '#') {
                out.add(new LexToken(line.substring(i), LexKind.COMMENT));
                break;
            }

            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                out.add(new LexToken(line.substring(i), LexKind.COMMENT));
                break;
            }

            if (c == '"' || c == '\'') {
                int j = i + 1;
                boolean esc = false;
                while (j < line.length()) {
                    char cc = line.charAt(j);
                    if (esc) {
                        esc = false;
                    } else if (cc == '\\') {
                        esc = true;
                    } else if (cc == c) {
                        j++;
                        break;
                    }
                    j++;
                }
                out.add(new LexToken(line.substring(i, j), LexKind.STRING));
                i = j;
                continue;
            }

            if (Character.isDigit(c)) {
                int j = i + 1;
                boolean dot = false;
                while (j < line.length()) {
                    char cc = line.charAt(j);
                    if (cc == '.' && !dot) {
                        dot = true;
                        j++;
                        continue;
                    }
                    if (!Character.isDigit(cc)) break;
                    j++;
                }
                out.add(new LexToken(line.substring(i, j), LexKind.NUMBER));
                i = j;
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < line.length()) {
                    char cc = line.charAt(j);
                    if (!Character.isLetterOrDigit(cc) && cc != '_') break;
                    j++;
                }
                String ident = line.substring(i, j);
                LexKind kind;
                String identLower = ident.toLowerCase();
                if (KEYWORDS.contains(identLower)) {
                    kind = LexKind.KEYWORD;
                } else if (BUILTINS.contains(identLower)) {
                    kind = LexKind.BUILTIN;
                } else {
                    kind = LexKind.IDENT;
                }
                out.add(new LexToken(ident, kind));
                i = j;
                continue;
            }

            out.add(new LexToken(String.valueOf(c), LexKind.SYMBOL));
            i++;
        }

        return out;
    }

    private int colorFor(LexKind kind) {
        if (kind == null) return new Color(229, 231, 235).getRGB();
        return switch (kind) {
            case KEYWORD -> new Color(251, 191, 36).getRGB();
            case BUILTIN -> new Color(34, 211, 238).getRGB();
            case STRING -> new Color(74, 222, 128).getRGB();
            case NUMBER -> new Color(196, 181, 253).getRGB();
            case COMMENT -> new Color(156, 163, 175).getRGB();
            case SYMBOL -> new Color(209, 213, 219).getRGB();
            case WHITESPACE, IDENT -> new Color(229, 231, 235).getRGB();
        };
    }

    private enum LexKind {
        KEYWORD,
        BUILTIN,
        IDENT,
        NUMBER,
        STRING,
        COMMENT,
        SYMBOL,
        WHITESPACE
    }

    private record LexToken(String text, LexKind kind) {
    }

    private void logOnMainThread(String line) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            synchronized (outputLog) {
                outputLog.add(line);
                if (outputScrollLines > 0) {
                    outputScrollLines++;
                }
                if (outputLog.size() > 200) {
                    outputLog.remove(0);
                }
            }
        });
    }

    public static final class ScriptsState implements Serializable {
        public String code = "";
        public int caretIndex = 0;
        public float scrollOffset = 0f;
        public float targetScroll = 0f;
        public float hScrollOffset = 0f;
        public float targetHScroll = 0f;
        public int outputScrollLines = 0;
    }
}
