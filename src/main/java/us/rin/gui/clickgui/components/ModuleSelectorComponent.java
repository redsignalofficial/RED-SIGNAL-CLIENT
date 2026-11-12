package us.rin.gui.clickgui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import us.rin.crashers.CrashManager;
import us.rin.crashers.Crasher;
import us.rin.exploits.ExploitManager;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.clickgui.utils.FavoritesManager;
import us.rin.gui.common.RenderUtils;
import us.rin.gui.clickgui.utils.Section;
import us.rin.helpers.ConfigHelper;
import us.rin.helpers.MessageHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static us.rin.gui.common.CommonUi.UiUtils;

public class ModuleSelectorComponent {

    private static final int TITLE_SIZE = 24;
    private static final int SUBTITLE_SIZE = 14;
    private static final int ICON_SIZE = 14;
    private static final int NAME_SIZE = 14;
    private static final int DESC_SIZE = 12;
    private static final int BUTTON_TEXT_SIZE = 12;
    private static final int SEARCH_TEXT_SIZE = 14;

    private static final int ITEM_HEIGHT = 40;
    private static final int ITEM_GAP = 8;
    private static final int SEARCH_HEIGHT = 30;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 28;
    private static final int BORDER_RADIUS = 6;
    private static final int BUTTON_RADIUS = 4;
    private static final int EFFECTIVE_WIDTH_ADJUSTMENT = 35;

    private static final int SUBTITLE_COLOR = new Color(209, 213, 219).getRGB();
    private static final int SEARCH_PLACEHOLDER_COLOR = new Color(156, 163, 175).getRGB();
    private static final int TEXT_COLOR = new Color(255, 255, 255).getRGB();
    private static final int DESC_COLOR = new Color(156, 163, 175).getRGB();
    private static final int ITEM_BACKGROUND = new Color(40, 40, 40, 204).getRGB();
    private static final int SEARCH_BACKGROUND = new Color(50, 50, 50, 204).getRGB();
    private static final int CARET_COLOR = new Color(255, 255, 255).getRGB();

    private static final float SCROLL_SPEED = 20f;
    private static final float SCROLL_SMOOTHING = 0.15f;
    private static final float SCROLL_THRESHOLD = 0.5f;

    private static final long CARET_BLINK_TIME = 500;
    private static final int FRAME_TIME = 16;

    private final MessageHelper msgHelper = new MessageHelper();
    private final CrashManager crashManager;
    private final ExploitManager exploitManager;

    private Section currentSection = Section.CRASHERS;
    private final List<ModuleEntry> entries = new ArrayList<>();
    private final List<ModuleRow> rows = new ArrayList<>();

    private final SearchState searchState = new SearchState();
    private final ScrollManager scrollManager = new ScrollManager();
    private final LayoutManager layoutManager = new LayoutManager();

    private int stopBtnX, stopBtnY, stopBtnW, stopBtnH;
    private int favBtnX, favBtnY, favBtnW, favBtnH;
    private boolean showFavoritesOnly = false;

    private BiConsumer<String, Section> onEdit = null;

    public ModuleSelectorComponent() {
        this.crashManager = CrashManager.getManager();
        this.exploitManager = ExploitManager.getManager();
    }

    public void setSection(Section section) {
        if (this.currentSection == section) return;
        this.currentSection = section;
        rebuildEntries();
        scrollManager.reset();
    }

    public void setOnEdit(BiConsumer<String, Section> callback) {
        this.onEdit = callback;
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, int themeColor, double screenOriginX, double screenOriginY, float guiScale) {
        int effectiveWidth = Math.max(0, width - EFFECTIVE_WIDTH_ADJUSTMENT);

        layoutManager.update(x, y, effectiveWidth, height);
        layoutManager.screenOriginX = screenOriginX;
        layoutManager.screenOriginY = screenOriginY;
        layoutManager.guiScale = guiScale;

        scrollManager.update();

        PoseStack pose = graphics.pose();

        y = renderHeader(pose, x, y, themeColor);
        y = renderSearchBar(graphics, x, y, effectiveWidth, mouseX, mouseY, themeColor);

        renderModuleList(graphics, x, y, effectiveWidth, height - (y - layoutManager.renderY),
                mouseX, mouseY, themeColor);

        searchState.updateCaret();
    }

    private int renderHeader(PoseStack pose, int x, int y, int themeColor) {
        String title = currentSection == Section.EXPLOITS ? "💣 Exploits" : "🔥 Crashers";

        float titleAscent = RenderUtils.getCustomFontAscent(TITLE_SIZE, true);
        RenderUtils.drawCustomString(pose, title, x, y + titleAscent, themeColor, true, TITLE_SIZE);
        y += RenderUtils.getCustomFontHeight(TITLE_SIZE, true) + 6;

        String subtitle = currentSection == Section.EXPLOITS ?
                "Powerful exploits for Minecraft servers" :
                "Powerful crash methods for Minecraft servers";
        float subAscent = RenderUtils.getCustomFontAscent(SUBTITLE_SIZE, false);
        RenderUtils.drawCustomString(pose, subtitle, x, y + subAscent, SUBTITLE_COLOR, false, SUBTITLE_SIZE);
        y += RenderUtils.getCustomFontHeight(SUBTITLE_SIZE, false) + 10;

        return y;
    }

    private int renderSearchBar(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, int themeColor) {
        PoseStack pose = graphics.pose();

        int searchY = y + 15;
        layoutManager.searchY = searchY;

        int searchWidth = width;

        int gap = 10;

        int favW = 80;
        favBtnW = favW;
        favBtnH = SEARCH_HEIGHT;

        int stopW = 80;
        stopBtnW = stopW;
        stopBtnH = SEARCH_HEIGHT;

        if (currentSection == Section.CRASHERS) {
            stopBtnX = x + width - stopBtnW;
            stopBtnY = searchY;

            favBtnX = stopBtnX - gap - favBtnW;
            favBtnY = searchY;

            searchWidth -= (stopBtnW + favBtnW + gap * 2);

            boolean hoverStop = UiUtils.isInside(mouseX, mouseY, stopBtnX, stopBtnY, stopBtnW, stopBtnH);
            int btnColor = hoverStop ? UiUtils.lightenColor(themeColor) : themeColor;

            RenderUtils.drawRoundedRect(pose, stopBtnX, stopBtnY, stopBtnW, stopBtnH, BORDER_RADIUS, btnColor);
            float txtW = RenderUtils.getCustomStringWidth("Stop All", true, 12);
            RenderUtils.drawCustomString(pose, "Stop All", stopBtnX + (stopBtnW - txtW)/2f, stopBtnY + 8, TEXT_COLOR, true, 12);
        } else {
            stopBtnX = -100;
            favBtnX = x + width - favBtnW;
            favBtnY = searchY;
            searchWidth -= (favBtnW + gap);
        }

        boolean favHover = UiUtils.isInside(mouseX, mouseY, favBtnX, favBtnY, favBtnW, favBtnH);
        int favColor = showFavoritesOnly ? themeColor : new Color(120, 120, 120).getRGB();
        int favRenderColor = favHover ? UiUtils.lightenColor(favColor) : favColor;
        RenderUtils.drawRoundedRect(pose, favBtnX, favBtnY, favBtnW, favBtnH, BORDER_RADIUS, favRenderColor);
        float favTxtW = RenderUtils.getCustomStringWidth("★ Favorites", true, 12);
        RenderUtils.drawCustomString(pose, "★ Favorites", favBtnX + (favBtnW - favTxtW)/2f, favBtnY + 8, TEXT_COLOR, true, 12);

        RenderUtils.drawRoundedRect(pose, x, searchY, searchWidth, SEARCH_HEIGHT, BORDER_RADIUS, SEARCH_BACKGROUND);

        float iconY = searchY + (SEARCH_HEIGHT - ICON_SIZE) / 2f;
        RenderUtils.drawCustomString(pose, "🔍", x + 8, iconY, SEARCH_PLACEHOLDER_COLOR, false, ICON_SIZE);

        String display = searchState.isEmpty() ? "Search modules..." : searchState.getText();
        int textColor = searchState.isEmpty() ? SEARCH_PLACEHOLDER_COLOR : TEXT_COLOR;
        float textY = searchY + (SEARCH_HEIGHT - SEARCH_TEXT_SIZE) / 2f;
        RenderUtils.drawCustomString(pose, display, x + 32, textY, textColor, false, SEARCH_TEXT_SIZE);

        if (searchState.isFocused() && searchState.isCaretVisible()) {
            float textWidth = searchState.isEmpty() ? 0 :
                    RenderUtils.getCustomStringWidth(searchState.getText(), false, SEARCH_TEXT_SIZE);
            float caretX = x + 32 + textWidth + 2;
            if (caretX < x + searchWidth - 4) {
                float caretY = searchY + 6;
                RenderUtils.fillRect(pose, (int) caretX, (int) caretY, 1, SEARCH_HEIGHT - 12, CARET_COLOR);
            }
        }

        return searchY + SEARCH_HEIGHT + 25;
    }

    private void renderModuleList(GuiGraphics graphics, int x, int y, int width, int height,
                                  int mouseX, int mouseY, int themeColor) {
        layoutManager.listY = y;
        layoutManager.listHeight = Math.max(0, height);

        if (entries.isEmpty() && hasManagers()) {
            rebuildEntriesInternal();
        }

        int contentHeight = entries.size() * (ITEM_HEIGHT + ITEM_GAP);
        scrollManager.setMaxScroll(Math.max(0, contentHeight - layoutManager.listHeight));

        int scissorX = (int) Math.round(layoutManager.screenOriginX + (x - layoutManager.renderX) * layoutManager.guiScale);
        int scissorY = (int) Math.round(layoutManager.screenOriginY + (y - layoutManager.renderY) * layoutManager.guiScale);
        int scissorW = Math.round(width * layoutManager.guiScale);
        int scissorH = Math.round(layoutManager.listHeight * layoutManager.guiScale);

        RenderUtils.enableScissor(scissorX, scissorY, scissorW, scissorH);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, -scrollManager.getOffset(), 0);

        rows.clear();
        int currentY = y;

        for (ModuleEntry entry : entries) {
            drawModuleRow(graphics, x, currentY, width, entry, mouseX, mouseY, themeColor);
            rows.add(new ModuleRow(x, currentY, width, ITEM_HEIGHT, entry));
            currentY += ITEM_HEIGHT + ITEM_GAP;
        }

        pose.popPose();
        RenderUtils.disableScissor();
    }

    private void drawModuleRow(GuiGraphics graphics, int x, int y, int width, ModuleEntry entry,
                               int mouseX, int mouseY, int themeColor) {
        PoseStack pose = graphics.pose();
        float screenY = y - scrollManager.getOffset();

        RenderUtils.drawRoundedRect(pose, x, y, width, ITEM_HEIGHT, BORDER_RADIUS, ITEM_BACKGROUND);

        String displayName = entry.name;
        RenderUtils.drawCustomString(pose, displayName, x + 10, y + 6, TEXT_COLOR, true, NAME_SIZE);

        String description = truncateDescription(entry.description, width - 180);
        String displayDesc = ClickGui.IS_VISIBLE ? description : "*".repeat(description.length());
        RenderUtils.drawCustomString(pose, displayDesc, x + 10, y + 22, DESC_COLOR, false, DESC_SIZE);

        renderModuleButton(pose, "Edit", x + width - 160, y + 6, screenY + 6,
                mouseX, mouseY, themeColor);

        renderModuleButton(pose, "Execute", x + width - 80, y + 6, screenY + 6,
                mouseX, mouseY, themeColor);
    }

    private void renderModuleButton(PoseStack pose, String text, int x, int y, float screenY,
                                    int mouseX, int mouseY, int themeColor) {
        boolean hovered = UiUtils.isInside(mouseX, mouseY, x, screenY, BUTTON_WIDTH, BUTTON_HEIGHT);
        int buttonColor = hovered ? UiUtils.lightenColor(themeColor) : themeColor;

        RenderUtils.drawRoundedRect(pose, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, buttonColor);

        float textWidth = RenderUtils.getCustomStringWidth(text, true, BUTTON_TEXT_SIZE);
        float textX = x + (BUTTON_WIDTH - textWidth) / 2;
        float textY = y + (BUTTON_HEIGHT - BUTTON_TEXT_SIZE) / 2f;
        RenderUtils.drawCustomString(pose, text, textX, textY, TEXT_COLOR, true, BUTTON_TEXT_SIZE);
    }

    private String truncateDescription(String description, int maxWidth) {
        if (maxWidth < 40) maxWidth = Math.max(40, layoutManager.renderWidth - 100);

        String result = description;
        if (RenderUtils.getCustomStringWidth(result, false, DESC_SIZE) > maxWidth) {
            while (RenderUtils.getCustomStringWidth(result + "...", false, DESC_SIZE) > maxWidth && !result.isEmpty()) {
                result = result.substring(0, result.length() - 1);
            }
            result = result + "...";
        }
        return result;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (currentSection == Section.CRASHERS && UiUtils.isInside(mouseX, mouseY, stopBtnX, stopBtnY, stopBtnW, stopBtnH)) {
            if (crashManager != null) {
                int count = 0;
                for (Crasher c : crashManager.getMethodsList()) {
                    if (c.getEnabled()) {
                        c.setEnabled(false);
                        count++;
                    }
                }
                if (count > 0) {
                    msgHelper.sendMessage("&7Stopped &c" + count + " &7active crashers!", true);
                } else {
                    msgHelper.sendMessage("&7No active crashers found to stop.", true);
                }
            }
            return true;
        }

        if (UiUtils.isInside(mouseX, mouseY, favBtnX, favBtnY, favBtnW, favBtnH)) {
            showFavoritesOnly = !showFavoritesOnly;
            rebuildEntries();
            return true;
        }

        if (handleSearchClick(mouseX, mouseY)) return true;
        return handleModuleClicks(mouseX, mouseY);
    }

    private boolean handleSearchClick(double mouseX, double mouseY) {
        int searchW = layoutManager.renderWidth;
        if (currentSection == Section.CRASHERS) {
            searchW -= (stopBtnW + favBtnW + 10 * 2);
        } else {
            searchW -= (favBtnW + 10);
        }

        if (UiUtils.isInside(mouseX, mouseY, layoutManager.renderX, layoutManager.searchY,
                searchW, SEARCH_HEIGHT)) {
            searchState.setFocused(true);
            return true;
        }
        searchState.setFocused(false);
        return false;
    }

    private boolean handleModuleClicks(double mouseX, double mouseY) {
        for (ModuleRow row : rows) {
            double rowScreenY = row.y - scrollManager.getOffset();

            if (!isRowVisible(rowScreenY)) continue;

            if (UiUtils.isInside(mouseX, mouseY, row.x, rowScreenY, row.width, row.height)) {
                if (handleRowButtonClick(mouseX, mouseY, row, rowScreenY)) {
                    return true;
                }

                invokeEdit(row.entry);
                return true;
            }
        }
        return false;
    }

    private boolean handleRowButtonClick(double mouseX, double mouseY, ModuleRow row, double rowScreenY) {
        double editX = row.x + row.width - 160;
        double editY = rowScreenY + 6;
        if (UiUtils.isInside(mouseX, mouseY, editX, editY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            invokeEdit(row.entry);
            return true;
        }

        double execX = row.x + row.width - 80;
        double execY = rowScreenY + 6;
        if (UiUtils.isInside(mouseX, mouseY, execX, execY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            invokeExecute(row.entry);
            return true;
        }

        return false;
    }

    private boolean isRowVisible(double rowScreenY) {
        return rowScreenY + ITEM_HEIGHT >= layoutManager.listY &&
                rowScreenY <= layoutManager.listY + layoutManager.listHeight;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!UiUtils.isInside(mouseX, mouseY, layoutManager.renderX, layoutManager.listY,
                layoutManager.renderWidth, layoutManager.listHeight)) {
            return false;
        }

        if (delta != 0) {
            scrollManager.addDelta((float)(-delta * SCROLL_SPEED));
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint) {
        if (!searchState.isFocused()) return false;

        if (codePoint == '\b') return false;

        if (codePoint >= 32 && codePoint < 127) {
            searchState.append(codePoint);
            rebuildEntries();
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!searchState.isFocused()) return false;

        switch (keyCode) {
            case 259 -> {
                if (searchState.removeLastChar()) {
                    rebuildEntries();
                }
                return true;
            }
            case 256, 257, 335 -> {
                searchState.setFocused(false);
                return true;
            }
        }

        return false;
    }

    public void setSearchFocused(boolean focused) {
        searchState.setFocused(focused);
    }

    public void setSearchText(String text) {
        searchState.setText(text);
        rebuildEntries();
    }

    private void rebuildEntries() {
        rebuildEntriesInternal();
        scrollManager.reset();
    }

    private void rebuildEntriesInternal() {
        entries.clear();
        String searchTerm = searchState.getText().toLowerCase().trim();

        if (currentSection == Section.CRASHERS && crashManager != null) {
            crashManager.getMethodsList().forEach(crasher -> {
                if (matchesSearch(crasher.getName(), crasher.getDescription(), searchTerm)) {
                    if (showFavoritesOnly && !FavoritesManager.isFavorite(crasher.getName())) return;
                    entries.add(new ModuleEntry(crasher.getName(), crasher.getDescription(), Section.CRASHERS));
                }
            });
        } else if (currentSection == Section.EXPLOITS && exploitManager != null) {
            exploitManager.getExploitList().forEach(exploit -> {
                if (matchesSearch(exploit.getName(), exploit.getDescription(), searchTerm)) {
                    if (showFavoritesOnly && !FavoritesManager.isFavorite(exploit.getName())) return;
                    entries.add(new ModuleEntry(exploit.getName(), exploit.getDescription(), Section.EXPLOITS));
                }
            });
        }
    }

    private boolean matchesSearch(String name, String description, String searchTerm) {
        if (searchTerm.isEmpty()) return true;
        return name.toLowerCase().contains(searchTerm) ||
                description.toLowerCase().contains(searchTerm);
    }

    private boolean hasManagers() {
        return (currentSection == Section.CRASHERS && crashManager != null) ||
                (currentSection == Section.EXPLOITS && exploitManager != null);
    }

    private void invokeEdit(ModuleEntry entry) {
        if (onEdit != null) {
            onEdit.accept(entry.name, entry.section);
        } else {
            defaultOnEdit(entry);
        }
    }

    private void invokeExecute(ModuleEntry entry) {
        defaultOnExecute(entry);
    }

    private void defaultOnEdit(ModuleEntry entry) {
        try {
            if (onEdit != null) {
                onEdit.accept(entry.name, entry.section);
            }
        } catch (Exception ex) {
            msgHelper.sendMessage("&cNo loaded &7config found for this method! " + entry.name, true);
        }
    }

    private void defaultOnExecute(ModuleEntry entry) {
        String[] args = ConfigHelper.loadedConfigs.get(entry.name);
        if (args != null) {
            StringBuilder builder = new StringBuilder();
            for (String val : args) {
                builder.append(val).append(" ");
            }

            if (currentSection == Section.CRASHERS && crashManager != null) {
                crashManager.handleMethod(String.format("!crash %s %s", entry.name, builder));
            } else if (currentSection == Section.EXPLOITS && exploitManager != null) {
                exploitManager.handleExploit(String.format("!exploit %s %s", entry.name, builder));
            }
        } else {
            msgHelper.sendMessage("&cNo loaded &7config found for this method!", true);
        }
    }

    private record ModuleEntry(String name, String description, Section section) { }
    private record ModuleRow(int x, int y, int width, int height, ModuleEntry entry) { }

    private static class SearchState {
        private final StringBuilder text = new StringBuilder();
        private boolean focused = false;
        private long caretTimer = 0;
        private boolean caretVisible = true;

        void setFocused(boolean focused) {
            this.focused = focused;
            if (focused) {
                caretTimer = 0;
                caretVisible = true;
            }
        }

        boolean isFocused() {
            return focused;
        }

        void append(char c) {
            text.append(c);
            resetCaret();
        }

        boolean removeLastChar() {
            if (!text.isEmpty()) {
                text.deleteCharAt(text.length() - 1);
                resetCaret();
                return true;
            }
            return false;
        }

        void setText(String s) {
            text.setLength(0);
            if (s != null) {
                text.append(s);
            }
        }

        String getText() {
            return text.toString();
        }

        boolean isEmpty() {
            return text.isEmpty();
        }

        void updateCaret() {
            if (focused) {
                caretTimer += FRAME_TIME;
                if (caretTimer >= CARET_BLINK_TIME) {
                    caretTimer = 0;
                    caretVisible = !caretVisible;
                }
            }
        }

        boolean isCaretVisible() {
            return caretVisible;
        }

        private void resetCaret() {
            caretTimer = 0;
            caretVisible = true;
        }
    }

    private static class ScrollManager {
        private float offset = 0f;
        private float target = 0f;
        private float maxScroll = 0f;

        void reset() {
            offset = 0f;
            target = 0f;
        }

        void update() {
            if (Math.abs(target - offset) > SCROLL_THRESHOLD) {
                offset += (target - offset) * SCROLL_SMOOTHING;
            } else {
                offset = target;
            }
        }

        void setMaxScroll(float max) {
            this.maxScroll = max;
            target = Math.max(0, Math.min(target, maxScroll));
            offset = Math.max(0, Math.min(offset, maxScroll));
        }

        void addDelta(float delta) {
            target += delta;
            target = Math.max(0, Math.min(target, maxScroll));
        }

        float getOffset() {
            return offset;
        }
    }

    private static class LayoutManager {
        int renderX, renderY, renderWidth, renderHeight;
        int searchY;
        int listY, listHeight;

        double screenOriginX;
        double screenOriginY;
        float guiScale;

        void update(int x, int y, int width, int height) {
            this.renderX = x;
            this.renderY = y;
            this.renderWidth = width;
            this.renderHeight = height;
        }
    }
}
