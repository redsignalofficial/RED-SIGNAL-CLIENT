package us.whitedev.gui.clickgui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import us.whitedev.crashers.Crasher;
import us.whitedev.crashers.CrashManager;
import us.whitedev.exploits.ExploitManager;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.gui.clickgui.utils.FavoritesManager;
import us.whitedev.gui.common.CommonUi;
import us.whitedev.gui.common.RenderUtils;
import us.whitedev.gui.clickgui.utils.Section;
import us.whitedev.helpers.ClientConfigHelper;
import us.whitedev.helpers.ConfigHelper;
import us.whitedev.helpers.MessageHelper;
import us.whitedev.utils.BypassListUtil;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.awt.Color;
import java.util.*;

public class ModuleEditComponent {
    private final ConfigHelper configHelper = new ConfigHelper();
    private final MessageHelper msgHelper = new MessageHelper();
    private final Map<Integer, Boolean> booleanMap = new HashMap<>();

    private String moduleName = "";
    private String moduleDesc = "";
    private List<OptionUtil> settings = new ArrayList<>();
    private boolean isVisible = true;
    private Section section = Section.CRASHERS;

    private final Map<Integer, TextFieldWidget> textFields = new HashMap<>();
    private final Map<Integer, ChoiceBoxWidget> choiceBoxes = new HashMap<>();
    private final Map<Integer, ToggleButtonWidget> toggleButtons = new HashMap<>();

    private final List<String> configList = new ArrayList<>();

    private ChoiceBoxWidget configChoice = null;
    private final TextFieldWidget configTitleField = new TextFieldWidget(-100);
    private final TextFieldWidget configBypassField = new TextFieldWidget(-101);

    private int lastRenderX = 0;
    private int lastRenderY = 0;
    private int lastRenderWidth = 0;
    private int lastRenderHeight = 0;

    private int lastContentStartY = 0;
    private int lastRowHeight = 80;
    private int lastRowsCount = 1;

    private int lastActionsX = 0;
    private int lastActionsY = 0;
    private int lastCfgBoxX = 0;
    private int lastCfgBoxY = 0;
    private int lastTitleBoxX = 0;
    private int lastTitleBoxY = 0;
    private int lastSaveX = 0;
    private int lastSaveY = 0;
    private int roundedScroll = 0;

    private int lastFavX = 0;
    private int lastFavY = 0;

    private int lastBindX = 0;
    private int lastBindY = 0;
    private int lastBindW = 0;
    private int lastBindH = 0;
    private boolean waitingForBind = false;

    private float scrollOffset = 0f;
    private float targetScroll = 0f;
    private static final float SCROLL_SPEED = 18f;

    private double lastScreenOriginY = 0;
    private float lastGuiScale = 1f;

    public void createEditor(String moduleName, String moduleDesc, List<OptionUtil> settings, boolean isVisible, Section section) {
        this.moduleName = moduleName;
        this.moduleDesc = moduleDesc;
        this.settings = settings == null ? new ArrayList<>() : settings;
        this.isVisible = isVisible;
        this.section = section;
        this.waitingForBind = false;
        buildWidgets();
        reloadConfigList();
        scrollOffset = 0f;
        targetScroll = 0f;
    }

    public record EditorState(
            String moduleName,
            Section section,
            Map<Integer, String> textFieldValues,
            Map<Integer, String> listValues,
            Map<Integer, Boolean> booleanValues,
            String configTitleText,
            String configBypassText,
            int configSelectedIndex,
            float scrollOffset,
            float targetScroll
    ) { }

    public EditorState exportState() {
        Map<Integer, String> textFieldValues = new HashMap<>();
        Map<Integer, String> listValues = new HashMap<>();
        Map<Integer, Boolean> booleanValues = new HashMap<>();

        for (int i = 0; i < settings.size(); i++) {
            OptionUtil option = settings.get(i);
            if (option.type() == OptionType.BOOLEAN) {
                booleanValues.put(i, booleanMap.getOrDefault(i, false));
            } else if (option.type() == OptionType.LIST) {
                ChoiceBoxWidget cb = choiceBoxes.get(i);
                listValues.put(i, cb == null ? "" : cb.getSelected());
            } else {
                TextFieldWidget tf = textFields.get(i);
                textFieldValues.put(i, tf == null ? "" : tf.text);
            }
        }

        int cfgSelected = configChoice == null ? 0 : configChoice.selected;

        return new EditorState(
                this.moduleName,
                this.section,
                textFieldValues,
                listValues,
                booleanValues,
                this.configTitleField.text,
                this.configBypassField.text,
                cfgSelected,
                this.scrollOffset,
                this.targetScroll
        );
    }

    public void applyEditorState(EditorState state) {
        if (state == null) return;
        if (state.moduleName() == null || !state.moduleName().equals(this.moduleName)) return;
        if (state.section() != this.section) return;

        for (Map.Entry<Integer, String> entry : state.textFieldValues().entrySet()) {
            TextFieldWidget tf = textFields.get(entry.getKey());
            if (tf != null) tf.text = entry.getValue() == null ? "" : entry.getValue();
        }

        for (Map.Entry<Integer, String> entry : state.listValues().entrySet()) {
            ChoiceBoxWidget cb = choiceBoxes.get(entry.getKey());
            if (cb != null) cb.selectValue(entry.getValue() == null ? "" : entry.getValue());
        }

        for (Map.Entry<Integer, Boolean> entry : state.booleanValues().entrySet()) {
            boolean v = Boolean.TRUE.equals(entry.getValue());
            ToggleButtonWidget tb = toggleButtons.get(entry.getKey());
            if (tb != null) tb.state = v;
            booleanMap.put(entry.getKey(), v);
        }

        this.configTitleField.text = state.configTitleText() == null ? "" : state.configTitleText();
        this.configBypassField.text = state.configBypassText() == null ? "" : state.configBypassText();

        if (this.configChoice != null && !this.configChoice.items.isEmpty()) {
            this.configChoice.selected = Math.max(0, Math.min(state.configSelectedIndex(), this.configChoice.items.size() - 1));
        }

        this.scrollOffset = state.scrollOffset();
        this.targetScroll = state.targetScroll();
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, int themeColor,
                       double screenOriginX, double screenOriginY, float guiScale) {
        this.lastRenderX = x;
        this.lastRenderY = y;
        this.lastRenderWidth = width;
        this.lastRenderHeight = height;

        this.lastScreenOriginY = screenOriginY;
        this.lastGuiScale = guiScale;

        int padding = 12;
        int innerW = Math.max(0, width - padding * 2);
        int cols = Math.max(1, Math.min(3, innerW / 220));
        int itemGap = 10;
        int colW = Math.max(120, (innerW - (cols - 1) * itemGap) / cols);
        int rowHeight = 80;
        int rowsCount = Math.max(1, (settings.size() + cols - 1) / cols);
        this.lastRowHeight = rowHeight;
        this.lastRowsCount = rowsCount;

        int titleSize = 24;
        float titleAscent = RenderUtils.getCustomFontAscent(titleSize, true);
        RenderUtils.drawCustomString(graphics.pose(), (isVisible ? moduleName : "******") + " - Edit Mode", x, y + titleAscent, themeColor, true, titleSize);
        y += RenderUtils.getCustomFontHeight(titleSize, true) + 6;
        int subtitleSize = 14;
        RenderUtils.drawCustomString(graphics.pose(), "Edit configuration and execute the module", x, y + RenderUtils.getCustomFontAscent(subtitleSize, false), new Color(209,213,219).getRGB(), false, subtitleSize);
        y += RenderUtils.getCustomFontHeight(subtitleSize, false) + 24;
        int labelSize = 13;
        RenderUtils.drawCustomString(graphics.pose(), "Description: ", x, y, new Color(156,163,175).getRGB(), true, labelSize);
        float descX = x + RenderUtils.getCustomStringWidth("Description: ", true, labelSize) + 6;
        String desc = isVisible ? moduleDesc : "********".repeat(Math.max(0, moduleDesc.length() / 8 + 1));
        RenderUtils.drawCustomString(graphics.pose(), desc, descX, y, new Color(255,255,255).getRGB(), false, labelSize);
        y += 24;

        int bindKey = getCurrentBindKey();
        String bindText = waitingForBind ? "Bind: ..." : (bindKey > 0 ? ("Bind: " + getKeyDisplayName(bindKey)) : "Bind: none");
        RenderUtils.drawCustomString(graphics.pose(), bindText, x, y, new Color(156,163,175).getRGB(), false, 12);
        y += 18;

        int startX = x + padding;
        int cy = y + padding;
        this.lastContentStartY = cy;

        int estimatedContentHeight = rowsCount * rowHeight + 360;
        float maxScroll = Math.max(0, estimatedContentHeight - (height - (cy - y)));
        targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
        if (Math.abs(targetScroll - scrollOffset) > 0.5f) scrollOffset += (targetScroll - scrollOffset) * 0.15f; else scrollOffset = targetScroll;

        if (scrollOffset < 0) scrollOffset = 0;

        PoseStack pose = graphics.pose();

        int scissorLocalTop = this.lastContentStartY;
        int scissorLocalHeight = Math.max(0, (this.lastRenderY + height) - scissorLocalTop);

        int scissorX = (int) Math.round(screenOriginX + (startX - x) * guiScale);
        int scissorY = (int) Math.round(screenOriginY + (scissorLocalTop - this.lastRenderY) * guiScale);
        int scissorW = Math.round(innerW * guiScale);
        int scissorH = Math.round(scissorLocalHeight * guiScale);

        RenderUtils.enableScissor(scissorX, scissorY, scissorW, scissorH);

        pose.pushPose();
        pose.translate(0, -scrollOffset, 0);

        List<ChoiceBoxWidget> openDropdowns = new ArrayList<>();

        for (int i = 0; i < settings.size(); i++) {
            OptionUtil option = settings.get(i);
            int col = i % cols;
            int row = i / cols;
            int ox = startX + col * (colW + itemGap);
            int oy = cy + row * rowHeight;
            String label = isVisible ? option.name() : "********";
            RenderUtils.drawCustomString(pose, label, ox, oy, new Color(255,255,255).getRGB(), true, labelSize);
            int widgetY = oy + 18;
            if (option.type() == OptionType.BOOLEAN) {
                ToggleButtonWidget tb = toggleButtons.get(i);
                if (tb != null) tb.setBounds(ox, widgetY, colW, 28);
                if (tb != null) tb.render(pose, mouseX, mouseY + (int) scrollOffset, themeColor);
            } else if (option.type() == OptionType.LIST) {
                ChoiceBoxWidget cb = choiceBoxes.get(i);
                if (cb != null) cb.setBounds(ox, widgetY, colW, 28);
                if (cb != null) cb.render(pose, mouseX, mouseY + (int) scrollOffset);
                if (cb != null && cb.open) openDropdowns.add(cb);
            } else {
                TextFieldWidget tf = textFields.get(i);
                if (tf != null) tf.setBounds(ox, widgetY, colW, 28);
                if (tf != null) tf.render(pose, mouseX, mouseY + (int) scrollOffset);
            }
        }

        if (configChoice != null && configChoice.open) openDropdowns.add(configChoice);
        int actionsY = cy + rowsCount * rowHeight + 10;
        int btnW = 90;
        int hideBtnW = 110;
        int gap = 10;
        int hideX = x + Math.max(0, width - hideBtnW - padding) - 20;
        int stopX = hideX - btnW - gap;
        int executeX = stopX - btnW - gap;
        int favX = executeX - btnW - gap;
        int rScroll = Math.round(scrollOffset);
        this.roundedScroll = rScroll;
        this.lastActionsX = executeX;
        this.lastActionsY = actionsY - rScroll;

        boolean favState = FavoritesManager.isFavorite(moduleName);
        int favColor = favState ? themeColor : new Color(120,120,120).getRGB();
        boolean favHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, favX, actionsY, btnW, 28);
        RenderUtils.drawRoundedRect(pose, favX, actionsY, btnW, 28, 6, favHover ? CommonUi.UiUtils.lightenColor(favColor) : favColor);
        float favTextW = RenderUtils.getCustomStringWidth(favState ? "Favorited ★" : "Favorite ☆", true, 12);
        float ftx = favX + (btnW - favTextW) / 2f;
        float fty = actionsY + (28 - 12) / 2f;
        RenderUtils.drawCustomString(pose, favState ? "Favorited ★" : "Favorite ☆", ftx, fty, new Color(255,255,255).getRGB(), true, 12);

        boolean execHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, executeX, actionsY, btnW, 28);
        RenderUtils.drawRoundedRect(pose, executeX, actionsY, btnW, 28, 6, execHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float execTextW = RenderUtils.getCustomStringWidth("Execute", true, 12);
        float tx = executeX + (btnW - execTextW) / 2f;
        float ty = actionsY + (28 - 12) / 2f;
        RenderUtils.drawCustomString(pose, "Execute", tx, ty, new Color(255,255,255).getRGB(), true, 12);

        boolean stopHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, stopX, actionsY, btnW, 28);
        RenderUtils.drawRoundedRect(pose, stopX, actionsY, btnW, 28, 6, stopHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float stopTextW = RenderUtils.getCustomStringWidth("Stop", true, 12);
        tx = stopX + (btnW - stopTextW) / 2f;
        RenderUtils.drawCustomString(pose, "Stop", tx, ty, new Color(255,255,255).getRGB(), true, 12);
        boolean hideHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, hideX, actionsY, hideBtnW, 28);
        RenderUtils.drawRoundedRect(pose, hideX, actionsY, hideBtnW, 28, 6, hideHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float vtextW = RenderUtils.getCustomStringWidth("Toggle Visibility", true, 12);
        tx = hideX + (hideBtnW - vtextW) / 2f;
        RenderUtils.drawCustomString(pose, "Toggle Visibility", tx, ty, new Color(255,255,255).getRGB(), true, 12);

        int bindW = hideBtnW;
        int bindH = 28;
        int bindX = hideX;
        int bindY = actionsY + 38;
        boolean bindHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, bindX, bindY, bindW, bindH);
        int bindBtnColor = bindHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor;
        RenderUtils.drawRoundedRect(pose, bindX, bindY, bindW, bindH, 6, bindBtnColor);
        String bindBtnText = waitingForBind ? "Press key" : "Bind";
        float bindTextW = RenderUtils.getCustomStringWidth(bindBtnText, true, 12);
        RenderUtils.drawCustomString(pose, bindBtnText, bindX + (bindW - bindTextW) / 2f, bindY + (bindH - 12) / 2f, new Color(255,255,255).getRGB(), true, 12);

        this.lastFavX = favX;
        this.lastFavY = actionsY - rScroll;

        this.lastBindX = bindX;
        this.lastBindY = bindY - rScroll;
        this.lastBindW = bindW;
        this.lastBindH = bindH;

        int cfgY = bindY + 48;
        RenderUtils.drawCustomString(pose, "Configs", x + padding, cfgY, new Color(255,255,255).getRGB(), true, 16);
        cfgY += 18;
        RenderUtils.drawCustomString(pose, "Select config to load:", x + padding, cfgY, new Color(156,163,175).getRGB(), false, 12);
        cfgY += 18;

        int cfgBoxW = 220;
        int cfgBoxH = 28;
        int cfgBoxX = x + padding;
        this.lastCfgBoxX = cfgBoxX;
        this.lastCfgBoxY = cfgY - rScroll;
        if (configChoice == null) configChoice = new ChoiceBoxWidget(-999, configList.toArray(new String[0]));
        configChoice.setBounds(cfgBoxX, cfgY, cfgBoxW, cfgBoxH);
        configChoice.render(pose, mouseX, mouseY + (int) scrollOffset);

        int loadX = cfgBoxX + cfgBoxW + 10;
        boolean loadHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, loadX, cfgY, 100, cfgBoxH);
        RenderUtils.drawRoundedRect(pose, loadX, cfgY, 100, cfgBoxH, 6, loadHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float loadTextW = RenderUtils.getCustomStringWidth("Load Config", true, 12);
        RenderUtils.drawCustomString(pose, "Load Config", loadX + (100 - loadTextW) / 2f, cfgY + 6, new Color(255,255,255).getRGB(), true, 12);

        int removeX = loadX + 110;
        boolean remHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, removeX, cfgY, 110, cfgBoxH);
        RenderUtils.drawRoundedRect(pose, removeX, cfgY, 110, cfgBoxH, 6, remHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float remTextW = RenderUtils.getCustomStringWidth("Remove Config", true, 12);
        RenderUtils.drawCustomString(pose, "Remove Config", removeX + (110 - remTextW) / 2f, cfgY + 6, new Color(255,255,255).getRGB(), true, 12);

        cfgY += cfgBoxH + 12;
        RenderUtils.drawCustomString(pose, "Create new Config", x + padding, cfgY, new Color(255,255,255).getRGB(), true, 14);
        cfgY += 18;
        int titleBoxX = x + padding;
        int bypassBoxX = x + padding + 160;
        configTitleField.setBounds(titleBoxX, cfgY, 150, 28);
        configTitleField.render(pose, mouseX, mouseY + (int) scrollOffset);
        configBypassField.setBounds(bypassBoxX, cfgY, 150, 28);
        configBypassField.render(pose, mouseX, mouseY + (int) scrollOffset);
        int saveX = x + padding + 320;
        int titleY = cfgY;
        this.lastTitleBoxX = titleBoxX;
        this.lastTitleBoxY = titleY - rScroll;
        this.lastSaveX = saveX;
        this.lastSaveY = titleY - rScroll;
        boolean saveHover = RenderUtils.isMouseOver(mouseX, mouseY + (int) scrollOffset, saveX, cfgY, 100, 28);
        RenderUtils.drawRoundedRect(pose, saveX, cfgY, 100, 28, 6, saveHover ? CommonUi.UiUtils.lightenColor(themeColor) : themeColor);
        float saveTextW = RenderUtils.getCustomStringWidth("Save Config", true, 12);
        RenderUtils.drawCustomString(pose, "Save Config", saveX + (100 - saveTextW) / 2f, cfgY + 6, new Color(255,255,255).getRGB(), true, 12);

        pose.popPose();
        RenderUtils.disableScissor();

        for (ChoiceBoxWidget cb : openDropdowns) {
            int drawX = cb.x;
            int drawY = cb.y - rScroll + cb.height;
            int totalH = cb.items.size() * cb.height;

            double screenDrawY = screenOriginY + (drawY - y) * guiScale;
            double screenTop = screenOriginY + (this.lastRenderY - y) * guiScale;
            double screenBottom = screenTop + this.lastRenderHeight * guiScale;

            if (screenDrawY + totalH * guiScale > screenBottom) {
                screenDrawY = screenOriginY + ((cb.y - rScroll) - totalH - y) * guiScale;
            }
            if (screenDrawY < screenTop) screenDrawY = screenTop;

            int finalDrawY = (int) Math.round(y + (screenDrawY - screenOriginY) / guiScale);

            for (int i = 0; i < cb.items.size(); i++) {
                int itemY = finalDrawY + i * cb.height;
                RenderUtils.drawRoundedRect(pose, drawX, itemY, cb.width, cb.height, 0, new Color(40,40,40).getRGB());
                RenderUtils.drawCustomString(pose, cb.items.get(i), drawX + 6, itemY + 6, new Color(255,255,255).getRGB(), false, 12);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (isInside(mouseX, mouseY, lastRenderX, lastRenderY, lastRenderWidth, lastRenderHeight)) return false;

        for (ChoiceBoxWidget cb : choiceBoxes.values()) {
            int sx = cb.x;
            int sy = cb.y - this.roundedScroll;
            if (mouseX >= sx && mouseX <= sx + cb.width && mouseY >= sy && mouseY <= sy + cb.height) {
                cb.toggleOpen();
                return true;
            }
            if (cb.open) {
                int localDrawY = getLocalDrawY(cb);

                for (int i = 0; i < cb.items.size(); i++) {
                    int itemY = localDrawY + i * cb.height;
                    if (mouseX >= cb.x && mouseX <= cb.x + cb.width && mouseY >= itemY && mouseY <= itemY + cb.height) {
                        cb.selected = i;
                        cb.open = false;
                        return true;
                    }
                }
            }
        }

        for (TextFieldWidget tf : textFields.values()) {
            int sx = tf.x;
            int sy = tf.y - this.roundedScroll;
            if (mouseX >= sx && mouseX <= sx + tf.width && mouseY >= sy && mouseY <= sy + tf.height) {
                for (TextFieldWidget other : textFields.values()) other.focused = false;
                tf.focused = true;
                return true;
            }
        }

        for (ToggleButtonWidget tb : toggleButtons.values()) {
            int sx = tb.x;
            int sy = tb.y - this.roundedScroll;
            if (mouseX >= sx && mouseX <= sx + tb.width && mouseY >= sy && mouseY <= sy + tb.height) {
                tb.toggle();
                booleanMap.put(tb.id, tb.state);
                return true;
            }
        }

        int btnW = 90;
        int gap = 10;
        int executeX = this.lastActionsX;
        int actionsY = this.lastActionsY;

        if (mouseX >= lastBindX && mouseX <= lastBindX + lastBindW && mouseY >= lastBindY && mouseY <= lastBindY + lastBindH) {
            waitingForBind = true;
            configTitleField.focused = false;
            configBypassField.focused = false;
            for (TextFieldWidget tf : textFields.values()) tf.focused = false;
            return true;
        }

        if (mouseX >= lastFavX && mouseX <= lastFavX + btnW && mouseY >= lastFavY && mouseY <= lastFavY + 28) {
            if (FavoritesManager.isFavorite(moduleName)) {
                FavoritesManager.removeFavorite(moduleName);
            } else {
                FavoritesManager.addFavorite(moduleName);
            }
            createEditor(this.moduleName, this.moduleDesc, this.settings, this.isVisible, this.section);
            return true;
        }

        if (mouseX >= executeX && mouseX <= executeX + btnW && mouseY >= actionsY && mouseY <= actionsY + 28) {
            executeModule();
            return true;
        }
        int stopX = executeX + btnW + gap;
        if (mouseX >= stopX && mouseX <= stopX + btnW && mouseY >= actionsY && mouseY <= actionsY + 28) {
            stopModule();
            return true;
        }
        int hideX = stopX + btnW + gap;
        int hideBtnW = 110;
        if (mouseX >= hideX && mouseX <= hideX + hideBtnW && mouseY >= actionsY && mouseY <= actionsY + 28) {
            ClickGui.IS_VISIBLE = !isVisible;
            createEditor(this.moduleName, this.moduleDesc, this.settings, ClickGui.IS_VISIBLE, this.section);
            return true;
        }

        int cfgBoxW = 220;
        int cfgBoxH = 28;
        int cfgBoxX = this.lastCfgBoxX;
        int cfgScreenY = this.lastCfgBoxY;

        if (configChoice == null) configChoice = new ChoiceBoxWidget(-999, configList.toArray(new String[0]));
        int sx = configChoice.x;
        int sy = configChoice.y - this.roundedScroll;
        if (mouseX >= sx && mouseX <= sx + configChoice.width && mouseY >= sy && mouseY <= sy + configChoice.height) {
            configChoice.toggleOpen();
            return true;
        }
        if (configChoice.open) {
            int localDrawY = getLocalDrawY();

            for (int i = 0; i < configChoice.items.size(); i++) {
                int itemY = localDrawY + i * configChoice.height;
                if (mouseX >= configChoice.x && mouseX <= configChoice.x + configChoice.width && mouseY >= itemY && mouseY <= itemY + configChoice.height) {
                    configChoice.selected = i;
                    configChoice.open = false;
                    return true;
                }
            }
        }

        int loadX = cfgBoxX + cfgBoxW + 10;
        if (mouseX >= loadX && mouseX <= loadX + 100 && mouseY >= cfgScreenY && mouseY <= cfgScreenY + cfgBoxH) {
            if (!configChoice.items.isEmpty()) {
                configHelper.loadConfig(configChoice.items.get(configChoice.selected).split(" - ")[0]);
                createEditor(this.moduleName, this.moduleDesc, this.settings, this.isVisible, this.section);
            }
            return true;
        }
        int removeX = loadX + 110;
        if (mouseX >= removeX && mouseX <= removeX + 110 && mouseY >= cfgScreenY && mouseY <= cfgScreenY + cfgBoxH) {
            if (!configChoice.items.isEmpty()) {
                configHelper.removeBypassList(configChoice.items.get(configChoice.selected).split(" - ")[0]);
                configHelper.saveBypassList();
                reloadConfigList();
            }
            return true;
        }

        int titleBoxX = this.lastTitleBoxX;
        int bypassBoxX = titleBoxX + 160;
        int titleY = this.lastTitleBoxY;

        if (mouseX >= titleBoxX && mouseX <= titleBoxX + 150 && mouseY >= titleY && mouseY <= titleY + 28) {
            for (TextFieldWidget other : textFields.values()) other.focused = false;
            configTitleField.focused = true;
            return true;
        }

        if (mouseX >= bypassBoxX && mouseX <= bypassBoxX + 150 && mouseY >= titleY && mouseY <= titleY + 28) {
            for (TextFieldWidget other : textFields.values()) other.focused = false;
            configBypassField.focused = true;
            return true;
        }
        int saveX = this.lastSaveX;
        int saveY = this.lastSaveY;
        if (mouseX >= saveX && mouseX <= saveX + 100 && mouseY >= saveY && mouseY <= saveY + 28) {
            List<String> vals = new ArrayList<>();
            for (int i = 0; i < settings.size(); i++) {
                OptionUtil option = settings.get(i);
                if (option.type() == OptionType.BOOLEAN) {
                    vals.add(String.valueOf(booleanMap.getOrDefault(i, false)));
                } else if (option.type() == OptionType.LIST) {
                    ChoiceBoxWidget cb = findChoiceById(i);
                    vals.add(cb == null ? "" : cb.getSelected());
                } else {
                    TextFieldWidget tf = findTextById(i);
                    vals.add(tf == null ? "0" : (tf.text.isEmpty() ? "0" : tf.text.replace(" ", "[-space-]")));
                }
            }
            createConfig(configTitleField.text, configBypassField.text, vals);
            reloadConfigList();
            return true;
        }

        return false;
    }

    private String getBindEntryKey() {
        String prefix = section == Section.CRASHERS ? "CRASH:" : "EXPLOIT:";
        return prefix + moduleName;
    }

    private int getCurrentBindKey() {
        Integer v = ClientConfigHelper.MODULE_BINDS.get(getBindEntryKey());
        return v == null ? 0 : v;
    }

    private void setBindKey(int keyCode) {
        String entryKey = getBindEntryKey();
        if (keyCode <= 0) {
            ClientConfigHelper.MODULE_BINDS.remove(entryKey);
            new ClientConfigHelper().saveClientConfig();
            return;
        }

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Integer> e : ClientConfigHelper.MODULE_BINDS.entrySet()) {
            if (e.getValue() != null && e.getValue() == keyCode) {
                toRemove.add(e.getKey());
            }
        }
        for (String k : toRemove) ClientConfigHelper.MODULE_BINDS.remove(k);

        ClientConfigHelper.MODULE_BINDS.put(entryKey, keyCode);
        new ClientConfigHelper().saveClientConfig();
    }

    private String getKeyDisplayName(int keyCode) {
        try {
            return InputConstants.getKey(keyCode, -1).getDisplayName().getString();
        } catch (Exception e) {
            return String.valueOf(keyCode);
        }
    }

    private int getLocalDrawY() {
        int drawY = configChoice.y + configChoice.height - this.roundedScroll;
        int totalH = configChoice.items.size() * configChoice.height;
        double screenDrawY = lastScreenOriginY + (drawY - lastRenderY) * lastGuiScale;
        double screenTop = lastScreenOriginY;
        double screenBottom = lastScreenOriginY + lastRenderHeight * lastGuiScale;
        if (screenDrawY + totalH * lastGuiScale > screenBottom) {
            screenDrawY = lastScreenOriginY + (configChoice.y - this.roundedScroll - totalH - lastRenderY) * lastGuiScale;
        }
        if (screenDrawY < screenTop) screenDrawY = screenTop;
        return (int) Math.round(lastRenderY + (screenDrawY - lastScreenOriginY) / lastGuiScale);
    }

    private int getLocalDrawY(ChoiceBoxWidget cb) {
        int drawY = cb.y + cb.height - this.roundedScroll;
        int totalH = cb.items.size() * cb.height;

        double screenDrawY = lastScreenOriginY + (drawY - lastRenderY) * lastGuiScale;
        double screenTop = lastScreenOriginY;
        double screenBottom = lastScreenOriginY + lastRenderHeight * lastGuiScale;

        if (screenDrawY + totalH * lastGuiScale > screenBottom) {
            screenDrawY = lastScreenOriginY + ((cb.y - this.roundedScroll) - totalH - lastRenderY) * lastGuiScale;
        }
        if (screenDrawY < screenTop) screenDrawY = screenTop;

        return (int) Math.round(lastRenderY + (screenDrawY - lastScreenOriginY) / lastGuiScale);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInside(mouseX, mouseY, lastRenderX, lastRenderY, lastRenderWidth, lastRenderHeight)) return false;
        if (delta == 0) return false;

        int contentHeight = this.lastRowsCount * this.lastRowHeight + 360;
        float maxScroll = Math.max(0, contentHeight - lastRenderHeight + (lastContentStartY - lastRenderY));
        targetScroll -= (float) (delta * SCROLL_SPEED);
        targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
        return true;
    }

    public boolean charTyped(char codePoint) {
        if (configTitleField.focused) {
            if (codePoint >= 32 && codePoint < 127) {
                configTitleField.text += codePoint;
                return true;
            }
        }
        if (configBypassField.focused) {
            if (codePoint >= 32 && codePoint < 127) {
                configBypassField.text += codePoint;
                return true;
            }
        }
        for (TextFieldWidget tf : textFields.values()) {
            if (tf.focused) {
                if (codePoint >= 32 && codePoint < 127) {
                    tf.text += codePoint;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (waitingForBind) {
            if (keyCode == 256) {
                waitingForBind = false;
                return true;
            }
            if (keyCode == 259) {
                setBindKey(0);
                waitingForBind = false;
                return true;
            }
            setBindKey(keyCode);
            waitingForBind = false;
            return true;
        }

        if (configTitleField.focused) {
            if (keyCode == 259) {
                if (!configTitleField.text.isEmpty()) configTitleField.text = configTitleField.text.substring(0, configTitleField.text.length() - 1);
                return true;
            }
            if (keyCode == 256) {
                configTitleField.focused = false;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                configTitleField.focused = false;
                return true;
            }
        }
        if (configBypassField.focused) {
            if (keyCode == 259) {
                if (!configBypassField.text.isEmpty()) configBypassField.text = configBypassField.text.substring(0, configBypassField.text.length() - 1);
                return true;
            }
            if (keyCode == 256) {
                configBypassField.focused = false;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                configBypassField.focused = false;
                return true;
            }
        }

        for (TextFieldWidget tf : textFields.values()) {
            if (tf.focused) {
                if (keyCode == 259) {
                    if (!tf.text.isEmpty()) tf.text = tf.text.substring(0, tf.text.length() - 1);
                    return true;
                }
                if (keyCode == 256) {
                    tf.focused = false;
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) {
                    tf.focused = false;
                    executeModule();
                    return true;
                }
            }
        }
        return false;
    }

    private void buildWidgets() {
        textFields.clear();
        choiceBoxes.clear();
        toggleButtons.clear();
        String[] configArgs = ConfigHelper.loadedConfigs.get(moduleName);
        for (int i = 0; i < settings.size(); i++) {
            OptionUtil option = settings.get(i);
            if (option.type() == OptionType.BOOLEAN) {
                boolean initial = configArgs != null && configArgs.length > i && "true".equals(configArgs[i]);
                toggleButtons.put(i, new ToggleButtonWidget(i, initial));
                booleanMap.put(i, initial);
            } else if (option.type() == OptionType.LIST) {
                ChoiceBoxWidget cb = new ChoiceBoxWidget(i, option.args());
                if (configArgs != null && configArgs.length > i) cb.selectValue(configArgs[i]);
                choiceBoxes.put(i, cb);
            } else {
                TextFieldWidget tf = new TextFieldWidget(i);
                if (configArgs != null && configArgs.length > i) tf.text = configArgs[i];
                textFields.put(i, tf);
            }
        }
    }

    private void executeModule() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < settings.size(); i++) {
            OptionUtil option = settings.get(i);
            if (option.type() == OptionType.BOOLEAN) {
                builder.append(booleanMap.getOrDefault(i, false)).append(" ");
            } else if (option.type() == OptionType.LIST) {
                ChoiceBoxWidget cb = findChoiceById(i);
                builder.append(cb == null ? "" : cb.getSelected()).append(" ");
            } else {
                TextFieldWidget tf = findTextById(i);
                String text = tf == null ? "0" : tf.text.isEmpty() ? "0" : tf.text.replace(" ", "[-space-]");
                builder.append(text).append(" ");
            }
        }
        if (section == Section.CRASHERS) {
            CrashManager.getManager().handleMethod(String.format("!crash %s %s", moduleName, builder));
        } else {
            ExploitManager.getManager().handleExploit(String.format("!exploit %s %s", moduleName, builder));
        }
    }

    private void stopModule() {
        if (CrashManager.getManager().getCrashClassByName(moduleName).isPresent()) {
            Crasher crasher = CrashManager.getManager().getCrashClassByName(moduleName).get();
            if (crasher.getEnabled()) {
                crasher.setEnabled(false);
                msgHelper.sendMessage("", false);
                msgHelper.sendMessage("&f" + moduleName + " &7crash has been &cstopped!", true);
                msgHelper.sendMessage("", false);
            }
        }
    }

    private void reloadConfigList() {
        configList.clear();
        configHelper.getBypassList().stream()
                .filter(config -> config.moduleName().split(" ")[1].equalsIgnoreCase(moduleName))
                .forEach(config -> configList.add(config.bypassName() + " - " + config.bypassFor()));
        configChoice = new ChoiceBoxWidget(-999, configList.toArray(new String[0]));
        if (!configList.isEmpty()) configChoice.selected = 0;
        configTitleField.text = "";
        configBypassField.text = "";
    }

    private TextFieldWidget findTextById(int id) {
        for (TextFieldWidget tf : textFields.values()) {
            if (tf.id == id) return tf;
        }
        return null;
    }

    private ChoiceBoxWidget findChoiceById(int id) {
        for (ChoiceBoxWidget cb : choiceBoxes.values()) {
            if (cb.id == id) return cb;
        }
        return null;
    }

    private void createConfig(String title, String bypass, List<String> values) {
        if (title != null && !title.isEmpty() && bypass != null && !bypass.isEmpty()) {
            String bypassTitle = configHelper.antiDupeName(title);
            StringBuilder builder = new StringBuilder();
            for (String s : values) builder.append("[").append(s).append("] ");
            configHelper.updateBypassList(new BypassListUtil(
                    (section == Section.CRASHERS ? "Crash " : "Exploit ") + moduleName,
                    bypass,
                    bypassTitle,
                    builder.toString()
            ));
            reloadConfigList();
            configHelper.saveBypassList();
        }
    }

    private static final class TextFieldWidget {
        final int id;
        String text = "";
        int x, y, width, height;
        boolean focused = false;
        long caretTimer = 0;
        boolean caretVisible = true;

        TextFieldWidget(int id) {
            this.id = id;
        }

        void setBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        void render(PoseStack pose, double mx, double my) {
            RenderUtils.drawRoundedRect(pose, x, y, width, height, 6, new Color(50,50,50,204).getRGB());
            String disp = text.isEmpty() ? "0" : text;
            RenderUtils.drawCustomString(pose, disp, x + 6, y + 6, new Color(255,255,255).getRGB(), false, 14);
            if (focused) {
                caretTimer += 16;
                if (caretTimer >= 500) {
                    caretTimer = 0;
                    caretVisible = !caretVisible;
                }
                if (caretVisible) {
                    float px = x + 6 + RenderUtils.getCustomStringWidth(disp, false, 14);
                    RenderUtils.fillRect(pose, (int) px, y + 6, 1, height - 12, new Color(255,255,255).getRGB());
                }
            }
        }
    }

    private static final class ChoiceBoxWidget {
        final int id;
        final List<String> items = new ArrayList<>();
        int x, y, width, height;
        int selected = 0;
        boolean open = false;

        ChoiceBoxWidget(int id, String[] values) {
            this.id = id;
            Collections.addAll(items, values);
        }

        void setBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        void render(PoseStack pose, double mx, double my) {
            RenderUtils.drawRoundedRect(pose, x, y, width, height, 6, new Color(50,50,50,204).getRGB());
            String sel = items.isEmpty() ? "" : items.get(selected);
            RenderUtils.drawCustomString(pose, sel, x + 6, y + 6, new Color(255,255,255).getRGB(), false, 14);
        }

        void toggleOpen() {
            open = !open;
        }

        void selectValue(String v) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equals(v)) selected = i;
            }
        }

        String getSelected() {
            return items.isEmpty() ? "" : items.get(selected);
        }
    }

    private static final class ToggleButtonWidget {
        final int id;
        boolean state;
        int x, y, width, height;

        ToggleButtonWidget(int id, boolean initial) {
            this.id = id;
            this.state = initial;
        }

        void setBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        void toggle() {
            state = !state;
        }

        void render(PoseStack pose, double mx, double my, int themeColor) {
            int color = state ? new Color(46,125,50).getRGB() : new Color(185,28,28).getRGB();
            RenderUtils.drawRoundedRect(pose, x, y, width, height, 6, color);
            String txt = state ? "Enabled ✔" : "Disabled ✖";
            RenderUtils.drawCustomString(pose, txt, x + 8, y + 6, new Color(255,255,255).getRGB(), true, 14);
        }
    }

    private boolean isInside(double mx, double my, double x, double y, double w, double h) {
        return !(mx >= x) || !(mx <= x + w) || !(my >= y) || !(my <= y + h);
    }
}
