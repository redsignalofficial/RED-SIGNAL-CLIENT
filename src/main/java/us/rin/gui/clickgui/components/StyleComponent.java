package us.rin.gui.clickgui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.clickgui.utils.ColorTheme;
import us.rin.gui.common.RenderUtils;
import us.rin.helpers.ClientConfigHelper;
import us.rin.gui.hud.utils.HudOptions;
import us.rin.gui.hud.utils.NotificationManager;
import us.rin.utils.DiscordRP;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import static us.rin.gui.common.CommonUi.*;

public class StyleComponent {

    private static final int COLUMN_WIDTH = 150;
    private static final int COLUMN_GAP = 30;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 24;
    private static final int ELEMENT_HEIGHT = 24;
    private static final int SECTION_SPACING = 18;
    private static final int BORDER_RADIUS = 6;

    private static final String TITLE_ICON = "🎨";
    private static final String TITLE_TEXT = "Style Settings";
    private static final String SUBTITLE_TEXT = "Customize your client's look and feel";
    private static final int TITLE_SIZE = 24;
    private static final int SUBTITLE_SIZE = 14;
    private static final int LABEL_SIZE = 14;
    private static final int BUTTON_TEXT_SIZE = 12;

    private final ClientConfigHelper clientConfigHelper = new ClientConfigHelper();
    private final List<ChoiceBoxWidget> dropdowns = new ArrayList<>();
    private final List<ChoiceBoxWidget> openDropdowns = new ArrayList<>();

    private final ChoiceBoxWidget guiColorChoice;
    private final ChoiceBoxWidget clientStyleChoice;
    private final ChoiceBoxWidget discordRpChoice;
    private final ChoiceBoxWidget hudInfoChoice;

    private final TextFieldWidget hexField = new TextFieldWidget(999);

    private float hue = 0f;
    private float saturation = 0f;
    private float brightness = 1f;
    private boolean pickingColor = false;
    private boolean pickingHue = false;

    private int pickerX, pickerY, pickerW, pickerH;
    private int sliderX, sliderY, sliderW, sliderH;

    private final BoundsCache boundsCache = new BoundsCache();
    private int lastX, lastY, lastW, lastH;

    public StyleComponent() {
        guiColorChoice = createColorThemeDropdown();
        clientStyleChoice = createClientStyleDropdown();
        discordRpChoice = createDiscordDropdown();
        hudInfoChoice = createHudInfoDropdown();

        dropdowns.add(guiColorChoice);
        dropdowns.add(clientStyleChoice);
        dropdowns.add(discordRpChoice);
        dropdowns.add(hudInfoChoice);

        refresh();
    }

    private ChoiceBoxWidget createColorThemeDropdown() {
        String[] themes = Arrays.stream(ColorTheme.values())
                .map(Enum::name)
                .toArray(String[]::new);
        return new ChoiceBoxWidget(0, themes);
    }

    private ChoiceBoxWidget createClientStyleDropdown() {
        String[] styles = Arrays.stream(ClickGui.ClientStyle.values())
                .map(Enum::name)
                .toArray(String[]::new);
        return new ChoiceBoxWidget(1, styles);
    }

    private ChoiceBoxWidget createDiscordDropdown() {
        return new ChoiceBoxWidget(2, new String[]{"Enabled", "Disabled"});
    }

    private ChoiceBoxWidget createHudInfoDropdown() {
        String[] options = HudOptions.getMapKeys().toArray(new String[0]);
        return new ChoiceBoxWidget(3, options);
    }

    public void refresh() {
        guiColorChoice.selected = guiColorChoice.indexOf(ClickGui.COLOR_THEME.name());
        clientStyleChoice.selected = clientStyleChoice.indexOf(ClickGui.CLIENT_STYLE.name());
        discordRpChoice.selected = DiscordRP.rpSwitch ? 0 : 1;
        hudInfoChoice.selected = 0;
        hexField.text = ClickGui.CUSTOM_HEX;
        updateColorFromHex();
    }

    private void updateColorFromHex() {
        try {
            if (hexField.text.isEmpty()) return;
            Color c = Color.decode(hexField.text);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
        } catch (Exception ignored) {
        }
    }

    private void updateHexFromColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        String hex = String.format("#%06X", (0xFFFFFF & rgb));
        hexField.text = hex;
        ClickGui.CUSTOM_HEX = hex;
        ClickGui.setThemeColor(ColorTheme.CUSTOM);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, int themeColor) {
        this.lastX = x;
        this.lastY = y;
        this.lastW = width;
        this.lastH = height;

        openDropdowns.clear();

        PoseStack pose = graphics.pose();

        y = renderHeader(pose, x, y, themeColor);
        y = renderSettingsRow1(pose, x, y, mouseX, mouseY);
        y = renderSettingsRow2(pose, x, y);

        renderActionButtons(pose, x, y, mouseX, mouseY, themeColor);

        renderDropdownOverlays(pose);
    }

    private int renderHeader(PoseStack pose, int x, int y, int themeColor) {
        float titleAscent = RenderUtils.getCustomFontAscent(TITLE_SIZE, true);
        String title = TITLE_ICON + " " + TITLE_TEXT;
        RenderUtils.drawCustomString(pose, title, x, y + titleAscent, themeColor, true, TITLE_SIZE);
        y += RenderUtils.getCustomFontHeight(TITLE_SIZE, true) + 6;

        float subtitleAscent = RenderUtils.getCustomFontAscent(SUBTITLE_SIZE, false);
        RenderUtils.drawCustomString(pose, SUBTITLE_TEXT, x, y + subtitleAscent, new Color(209,213,219).getRGB(), false, SUBTITLE_SIZE);
        y += RenderUtils.getCustomFontHeight(SUBTITLE_SIZE, false) + 12;

        return y;
    }

    private int renderSettingsRow1(PoseStack pose, int x, int y, int mouseX, int mouseY) {
        int rowY = y + 10;

        renderLabel(pose, "Gui Color", x, rowY);
        renderLabel(pose, "Client Style", x + COLUMN_WIDTH + COLUMN_GAP, rowY);
        renderLabel(pose, "DiscordRP", x + 2 * (COLUMN_WIDTH + COLUMN_GAP), rowY);

        rowY += SECTION_SPACING;

        setupAndRenderDropdown(pose, guiColorChoice, x, rowY, 0);

        int extraHeight = 0;

        if (guiColorChoice.getSelected().equals("CUSTOM")) {
            int hexX = x;
            int hexY = rowY + ELEMENT_HEIGHT + 4;
            hexField.setBounds(hexX, hexY, COLUMN_WIDTH, ELEMENT_HEIGHT);
            hexField.render(pose, mouseX, mouseY);

            int pickerHeight = 80;
            int sliderHeight = 10;
            int pickerGap = 6;

            int px = hexX;
            int py = hexY + ELEMENT_HEIGHT + 8;

            this.pickerX = px;
            this.pickerY = py;
            this.pickerW = COLUMN_WIDTH;
            this.pickerH = pickerHeight;

            this.sliderX = px;
            this.sliderY = py + pickerHeight + pickerGap;
            this.sliderW = COLUMN_WIDTH;
            this.sliderH = sliderHeight;

            int hueColor = Color.HSBtoRGB(hue, 1f, 1f);
            RenderUtils.drawGradientRect(pose, px, py, pickerW, pickerH,
                    0xFFFFFFFF, hueColor, 0xFF000000, 0xFF000000);
            RenderUtils.drawRoundedRectOutline(pose, px, py, pickerW, pickerH, 0, 1, 0xFF888888);

            int indX = px + (int)(saturation * pickerW);
            int indY = py + (int)((1f - brightness) * pickerH);
            indX = Math.max(px, Math.min(px + pickerW, indX));
            indY = Math.max(py, Math.min(py + pickerH, indY));

            RenderUtils.drawCircle(pose, indX, indY, 3, 0xFFFFFFFF);
            RenderUtils.drawCircle(pose, indX, indY, 2, hueColor);

            for (int i = 0; i < sliderW; i++) {
                float h = (float) i / sliderW;
                int c = Color.HSBtoRGB(h, 1f, 1f);
                RenderUtils.fillRect(pose, sliderX + i, sliderY, 1, sliderH, c);
            }
            RenderUtils.drawRoundedRectOutline(pose, sliderX, sliderY, sliderW, sliderH, 0, 1, 0xFF888888);

            int sIndX = sliderX + (int)(hue * sliderW);
            sIndX = Math.max(sliderX, Math.min(sliderX + sliderW, sIndX));
            RenderUtils.fillRect(pose, sIndX - 1, sliderY - 1, 2, sliderH + 2, 0xFFFFFFFF);

            extraHeight = ELEMENT_HEIGHT + 8 + pickerHeight + pickerGap + sliderHeight + 10;
        }

        setupAndRenderDropdown(pose, clientStyleChoice, x + COLUMN_WIDTH + COLUMN_GAP, rowY, 1);
        setupAndRenderDropdown(pose, discordRpChoice, x + 2 * (COLUMN_WIDTH + COLUMN_GAP), rowY, 2);

        return rowY + 32 + extraHeight;
    }

    private int renderSettingsRow2(PoseStack pose, int x, int y) {
        renderLabel(pose, "Hud Info", x, y);
        y += SECTION_SPACING;

        setupAndRenderDropdown(pose, hudInfoChoice, x, y, 3);

        return y + 48;
    }

    private void renderLabel(PoseStack pose, String text, int x, int y) {
        RenderUtils.drawCustomString(pose, text, x, y, new Color(255,255,255).getRGB(), true, LABEL_SIZE);
    }

    private void setupAndRenderDropdown(PoseStack pose, ChoiceBoxWidget dropdown, int x, int y, int boundsIndex) {
        dropdown.setBounds(x, y, COLUMN_WIDTH, ELEMENT_HEIGHT);
        dropdown.renderBox(pose);

        boundsCache.setBounds(boundsIndex, x, y, COLUMN_WIDTH, ELEMENT_HEIGHT);

        if (dropdown.open) {
            openDropdowns.add(dropdown);
        }
    }

    private void renderActionButtons(PoseStack pose, int x, int y, int mouseX, int mouseY, int themeColor) {
        renderActionButton(pose, "Apply Hud", x, y, mouseX, mouseY, themeColor, 4);
        renderActionButton(pose, "Save Config", x + COLUMN_WIDTH + COLUMN_GAP, y, mouseX, mouseY, themeColor, 5);
    }

    private void renderActionButton(PoseStack pose, String text, int x, int y, int mouseX, int mouseY, int themeColor, int boundsIndex) {
        boolean hover = UiUtils.isInside(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        int buttonColor = hover ? UiUtils.lightenColor(themeColor) : themeColor;

        float textWidth = RenderUtils.getCustomStringWidth(text, true);

        RenderUtils.drawRoundedRect(pose, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, BORDER_RADIUS, buttonColor);
        RenderUtils.drawCustomString(pose, text, x + 8 + textWidth / 2, y + 6, new Color(255,255,255).getRGB(), true, BUTTON_TEXT_SIZE);

        boundsCache.setBounds(boundsIndex, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

    }

    private void renderDropdownOverlays(PoseStack pose) {
        for (ChoiceBoxWidget dropdown : openDropdowns) {
            dropdown.renderOverlayItems(pose, 0);
        }
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (guiColorChoice.getSelected().equals("CUSTOM") && hexField.focused) {
            if (codePoint >= 32 && codePoint < 127) {
                hexField.text += codePoint;
                ClickGui.CUSTOM_HEX = hexField.text;
                updateColorFromHex();

                try {
                    Color.decode(hexField.text);
                    ClickGui.setThemeColor(ColorTheme.CUSTOM);
                } catch(Exception ignored) {}

                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (guiColorChoice.getSelected().equals("CUSTOM") && hexField.focused) {
            if (keyCode == 259) {
                if (!hexField.text.isEmpty()) {
                    hexField.text = hexField.text.substring(0, hexField.text.length() - 1);
                    ClickGui.CUSTOM_HEX = hexField.text;
                    updateColorFromHex();

                    try {
                        Color.decode(hexField.text);
                        ClickGui.setThemeColor(ColorTheme.CUSTOM);
                    } catch(Exception ignored) {}
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (guiColorChoice.getSelected().equals("CUSTOM")) {
            hexField.focused = hexField.isMouseOver(mouseX, mouseY);

            if (UiUtils.isInside(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH)) {
                pickingColor = true;
                updatePicker(mouseX, mouseY);
                return true;
            }
            if (UiUtils.isInside(mouseX, mouseY, sliderX, sliderY, sliderW, sliderH)) {
                pickingHue = true;
                updateSlider(mouseX);
                return true;
            }
        }

        if (!isInRenderArea(mouseX, mouseY)) return false;
        if (handleDropdownClicks(mouseX, mouseY)) return true;
        return handleButtonClicks(mouseX, mouseY);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        pickingColor = false;
        pickingHue = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pickingColor) {
            updatePicker(mouseX, mouseY);
        } else if (pickingHue) {
            updateSlider(mouseX);
        }
    }

    private void updatePicker(double mouseX, double mouseY) {
        float valX = (float)(mouseX - pickerX) / pickerW;
        float valY = (float)(mouseY - pickerY) / pickerH;
        this.saturation = Math.max(0f, Math.min(1f, valX));
        this.brightness = Math.max(0f, Math.min(1f, 1f - valY));
        updateHexFromColor();
    }

    private void updateSlider(double mouseX) {
        float val = (float)(mouseX - sliderX) / sliderW;
        this.hue = Math.max(0f, Math.min(1f, val));
        updateHexFromColor();
    }

    private boolean isInRenderArea(double mouseX, double mouseY) {
        return UiUtils.isInside(mouseX, mouseY, lastX, lastY, lastW, lastH);
    }

    private boolean handleDropdownClicks(double mouseX, double mouseY) {
        for (ChoiceBoxWidget dropdown : dropdowns) {
            if (dropdown.handleClickAbsolute(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleButtonClicks(double mouseX, double mouseY) {
        if (boundsCache.isInside(4, mouseX, mouseY)) {
            applyHudSettings();
            NotificationManager.push(NotificationManager.NotificationType.SUCCESS, "Style Settings", "Hud settings applied successfully!", 5000);
            return true;
        }

        if (boundsCache.isInside(5, mouseX, mouseY)) {
            saveConfiguration();
            NotificationManager.push(NotificationManager.NotificationType.SUCCESS, "Style Settings", "Configuration saved successfully!", 5000);
            return true;
        }

        return false;
    }

    private void applyHudSettings() {
        String hudInfoName = hudInfoChoice.getSelected();
        if (hudInfoName != null && !hudInfoName.isEmpty()) {
            HudOptions.setOptionValue(hudInfoName, !HudOptions.getOption(hudInfoName));
            clientConfigHelper.saveClientConfig();
        }
    }

    private void saveConfiguration() {
        String guiColorName = guiColorChoice.getSelected();
        if (guiColorName != null && !guiColorName.isEmpty()) {
            ColorTheme colorTheme = ColorTheme.valueOf(guiColorName);
            ClickGui.COLOR_THEME = colorTheme;
            if (colorTheme == ColorTheme.CUSTOM) {
                if (hexField.text.isEmpty()) {
                    hexField.text = "#FFFFFF";
                }
                ClickGui.CUSTOM_HEX = hexField.text;
            }
            ClickGui.setThemeColor(colorTheme);
        }

        String clientStyleName = clientStyleChoice.getSelected();
        if (clientStyleName != null && !clientStyleName.isEmpty()) {
            ClickGui.CLIENT_STYLE = ClickGui.ClientStyle.valueOf(clientStyleName);
        }

        String discordRpResult = discordRpChoice.getSelected();
        if (discordRpResult != null) {
            DiscordRP.rpSwitch = discordRpResult.equalsIgnoreCase("Enabled");
        }

        clientConfigHelper.saveClientConfig();
    }

    private static class BoundsCache {
        private final int[][] bounds = new int[6][4];

        public void setBounds(int index, int x, int y, int width, int height) {
            if (index >= 0 && index < bounds.length) {
                bounds[index][0] = x;
                bounds[index][1] = y;
                bounds[index][2] = width;
                bounds[index][3] = height;
            }
        }

        public boolean isInside(int index, double mouseX, double mouseY) {
            if (index >= 0 && index < bounds.length) {
                return UiUtils.isInside(mouseX, mouseY,
                        bounds[index][0], bounds[index][1],
                        bounds[index][2], bounds[index][3]);
            }
            return false;
        }
    }

    private static final class TextFieldWidget {
        final int id;
        String text = "#FFFFFF";
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

        boolean isMouseOver(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }

        void render(PoseStack pose, double mx, double my) {
            RenderUtils.drawRoundedRect(pose, x, y, width, height, 6, new Color(50,50,50,204).getRGB());
            String disp = text.isEmpty() ? "" : text;
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
}