package us.rin.gui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import us.rin.RedSignalClient;
import us.rin.crashers.CrashManager;
import us.rin.crashers.Crasher;
import us.rin.exploits.Exploit;
import us.rin.exploits.ExploitManager;
import us.rin.gui.clickgui.components.*;
import us.rin.gui.clickgui.utils.ColorTheme;
import us.rin.helpers.ClientConfigHelper;
import us.rin.gui.common.CommonUi;
import us.rin.gui.common.RenderUtils;
import us.rin.gui.clickgui.utils.Section;

import java.util.Objects;
import java.util.Optional;
import java.awt.Color;

public class ClickGui extends Screen {

    private static final int GUI_WIDTH = 800;
    private static final int GUI_HEIGHT = 500;
    private static final int SIDEBAR_WIDTH = 200;
    private static final int BORDER_RADIUS = 12;
    private static final int SIDEBAR_RADIUS = 8;

    private static final float BASE_GUI_SCALE = 0.75f;
    private static final int REFERENCE_MC_GUI_SCALE = 2;

    private int guiLeft;
    private int guiTop;

    private boolean dragging = false;
    private double dragOffsetScreenX;
    private double dragOffsetScreenY;

    public static boolean IS_VISIBLE = true;
    public static ColorTheme COLOR_THEME = ColorTheme.PINK;
    public static HudPlace HUD_PLACE = HudPlace.LEFT;
    public static ClientStyle CLIENT_STYLE = ClientStyle.RED SIGNAL;
    public static Section LAST_SELECTED_SECTION = null;
    public static boolean LAST_EDITING_OPEN = false;
    public static ModuleEditComponent.EditorState LAST_EDITOR_STATE = null;
    public static ScriptsComponent.ScriptsState LAST_SCRIPTS_STATE = null;

    public static String CURRENT_THEME_COLOR = "#ec4899";
    public static String CURRENT_THEME_COLOR_LIGHT = "#f9a8d4";
    public static String CUSTOM_HEX = "#FFFFFF";

    private static int themeColor = new Color(236, 72, 153).getRGB();
    private static int themeColorFill;
    private static int themeColorHoverFill;

    private Section selectedSection = Section.HOMEPAGE;
    private int playTimeSeconds = 0;
    private long lastTimeUpdate = 0;

    private float scrollOffset = 0;
    private float targetScrollOffset = 0;

    private final WelcomeComponent welcomeComponent = new WelcomeComponent();
    private final ModuleSelectorComponent moduleSelectorComponent = new ModuleSelectorComponent();
    private final ModuleEditComponent moduleEditComponent = new ModuleEditComponent();
    private final StyleComponent styleComponent = new StyleComponent();
    private final ScriptsComponent scriptsComponent = new ScriptsComponent();

    private boolean editingOpen = false;

    public ClickGui() {
        super(Component.literal("RedSignalClient"));
        updateThemeColors();
        moduleSelectorComponent.setOnEdit((name, section) -> {
            if (section == Section.CRASHERS) {
                Optional<Crasher> optCrasher = CrashManager.getManager().getCrashClassByName(name);
                optCrasher.ifPresent(crasher -> moduleEditComponent.createEditor(name, crasher.getDescription(), crasher.getOptions(), ClickGui.IS_VISIBLE, section));
            } else if (section == Section.EXPLOITS) {
                Optional<Exploit> optExploit = ExploitManager.getManager().getExploitClassByName(name);
                optExploit.ifPresent(exploit -> moduleEditComponent.createEditor(name, exploit.getDescription(), exploit.getOptions(), ClickGui.IS_VISIBLE, section));
            }
            editingOpen = true;
            selectedSection = section;

            LAST_SELECTED_SECTION = section;
            new ClientConfigHelper().saveClientConfig();
        });

    }

    @Override
    protected void init() {
        super.init();

        boolean restoredEditor = false;
        if (LAST_EDITING_OPEN && LAST_EDITOR_STATE != null) {
            ModuleEditComponent.EditorState state = LAST_EDITOR_STATE;
            this.selectedSection = state.section();
            if (this.selectedSection == Section.CRASHERS || this.selectedSection == Section.EXPLOITS) {
                this.moduleSelectorComponent.setSection(this.selectedSection);
            }
            if (this.selectedSection == Section.CRASHERS) {
                Optional<Crasher> optCrasher = CrashManager.getManager().getCrashClassByName(state.moduleName());
                if (optCrasher.isPresent()) {
                    Crasher crasher = optCrasher.get();
                    moduleEditComponent.createEditor(state.moduleName(), crasher.getDescription(), crasher.getOptions(), ClickGui.IS_VISIBLE, this.selectedSection);
                    moduleEditComponent.applyEditorState(state);
                    this.editingOpen = true;
                    restoredEditor = true;
                }
            } else if (this.selectedSection == Section.EXPLOITS) {
                Optional<Exploit> optExploit = ExploitManager.getManager().getExploitClassByName(state.moduleName());
                if (optExploit.isPresent()) {
                    Exploit exploit = optExploit.get();
                    moduleEditComponent.createEditor(state.moduleName(), exploit.getDescription(), exploit.getOptions(), ClickGui.IS_VISIBLE, this.selectedSection);
                    moduleEditComponent.applyEditorState(state);
                    this.editingOpen = true;
                    restoredEditor = true;
                }
            }

            if (!restoredEditor) {
                LAST_EDITING_OPEN = false;
                LAST_EDITOR_STATE = null;
                this.editingOpen = false;
            }
        }

        if (!restoredEditor && LAST_SELECTED_SECTION != null) {
            this.selectedSection = LAST_SELECTED_SECTION;
            if (this.selectedSection == Section.CRASHERS || this.selectedSection == Section.EXPLOITS) {
                this.moduleSelectorComponent.setSection(this.selectedSection);
            }
            if (this.selectedSection == Section.STYLES) {
                this.styleComponent.refresh();
            }
            if (this.selectedSection == Section.SCRIPTS) {
                this.scriptsComponent.applyState(LAST_SCRIPTS_STATE);
                this.scriptsComponent.onOpen();
            }
            this.editingOpen = false;
        }

        float effective = getEffectiveGuiScale();

        int screenGuiWidth = Math.round(GUI_WIDTH * effective);
        int screenGuiHeight = Math.round(GUI_HEIGHT * effective);
        double screenLeft = (this.width - screenGuiWidth) / 2.0;
        double screenTop = (this.height - screenGuiHeight) / 2.0;
        this.guiLeft = (int) Math.round(screenLeft - (GUI_WIDTH / 2f) * (1f - effective));
        this.guiTop = (int) Math.round(screenTop - (GUI_HEIGHT / 2f) * (1f - effective));

        double clampedScreenLeft = clamp(screenLeftFromGuiLeft(this.guiLeft), 0, this.width - screenGuiWidth);
        double clampedScreenTop = clamp(screenTopFromGuiTop(this.guiTop), 0, this.height - screenGuiHeight);
        setGuiLeftFromScreenLeft(clampedScreenLeft);
        setGuiTopFromScreenTop(clampedScreenTop);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (Math.abs(targetScrollOffset - scrollOffset) > 0.1f)
            scrollOffset += (targetScrollOffset - scrollOffset) * 0.15f;
        updateThemeColors();
        updatePlayTime();
        renderBackground(graphics);
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float effective = getEffectiveGuiScale();

        float centerX = guiLeft + GUI_WIDTH / 2f;
        float centerY = guiTop + GUI_HEIGHT / 2f;
        poseStack.translate(centerX, centerY, 0);
        poseStack.scale(effective, effective, 1f);
        poseStack.translate(-centerX, -centerY, 0);

        double localMouseX = toLocalX(mouseX);
        double localMouseY = toLocalY(mouseY);

        renderMainContainer(graphics);
        renderSidebar(graphics, (int) localMouseX, (int) localMouseY);
        renderMainContent(graphics, (int) localMouseX, (int) localMouseY);

        poseStack.popPose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fillGradient(0, 0, this.width, this.height, new Color(0,0,0,192).getRGB(), new Color(0,0,0,208).getRGB());
    }

    private void renderMainContainer(GuiGraphics graphics) {
        PoseStack poseStack = graphics.pose();
        RenderUtils.drawRoundedRect(poseStack, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, BORDER_RADIUS, new Color(0,0,0,204).getRGB());
        RenderUtils.drawRoundedRectOutline(poseStack, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, BORDER_RADIUS, 2, themeColor);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack poseStack = graphics.pose();
        int sidebarLeft = guiLeft;
        int sidebarTop = guiTop;
        RenderUtils.drawRoundedRect(poseStack, sidebarLeft + 2, sidebarTop + 1, SIDEBAR_WIDTH - 2, GUI_HEIGHT - 3, SIDEBAR_RADIUS, new Color(10,10,10,230).getRGB(), true, false, false, true);
        int yOffset = sidebarTop + 10;
        yOffset = renderLogoSection(graphics, sidebarLeft + SIDEBAR_WIDTH / 2, yOffset);
        yOffset += 20;
        renderNavigation(graphics, mouseX, mouseY, sidebarLeft, yOffset);
        renderSidebarFooter(graphics, sidebarLeft + SIDEBAR_WIDTH / 2, sidebarTop + GUI_HEIGHT - 80);
    }

    private int renderLogoSection(GuiGraphics graphics, int centerX, int y) {
        String title = "RedSignalClient ";
        String version = RedSignalClient.VERSION;
        int titleSize = 20;
        boolean titleBold = true;
        int subtitleSize = 10;
        boolean subtitleBold = false;
        float titleWidth = RenderUtils.getCustomStringWidth(title, titleBold, titleSize);
        float versionWidth = RenderUtils.getCustomStringWidth(version, titleBold, titleSize);
        float totalWidth = titleWidth + versionWidth;
        float startX = centerX - totalWidth / 2f;
        float titleAscent = RenderUtils.getCustomFontAscent(titleSize, titleBold);
        float baselineY = y + titleAscent;
        RenderUtils.drawCustomString(graphics.pose(), title, startX, baselineY, themeColor, titleBold, titleSize);
        RenderUtils.drawCustomString(graphics.pose(), version, startX + titleWidth, baselineY, new Color(255,255,255).getRGB(), titleBold, titleSize);
        y += RenderUtils.getCustomFontHeight(titleSize, titleBold) + 8;
        String subtitle = "Exclusive Minecraft Crash/Exploit Client";
        float subtitleWidth = RenderUtils.getCustomStringWidth(subtitle, subtitleBold, subtitleSize);
        float subtitleAscent = RenderUtils.getCustomFontAscent(subtitleSize, subtitleBold);
        float subtitleBaselineY = y + subtitleAscent;
        RenderUtils.drawCustomString(graphics.pose(), subtitle, centerX - subtitleWidth / 2f, subtitleBaselineY, new Color(156,163,175).getRGB(), subtitleBold, subtitleSize);
        return y + RenderUtils.getCustomFontHeight(subtitleSize, subtitleBold) + 4;
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY, int sidebarLeft, int startY) {
        PoseStack poseStack = graphics.pose();
        Section[] sections = Section.values();
        int y = startY;
        for (Section section : sections) {
            int itemX = sidebarLeft + 10;
            int itemY = y;
            int itemWidth = SIDEBAR_WIDTH - 20;
            int itemHeight = 35;
            boolean isHovered = mouseX >= itemX && mouseX <= itemX + itemWidth && mouseY >= itemY && mouseY <= itemY + itemHeight;
            boolean isSelected = section == selectedSection;
            if (isSelected) {
                RenderUtils.drawRoundedRect(poseStack, itemX, itemY, itemWidth, itemHeight, 6, themeColorFill);
                RenderUtils.fillRect(poseStack, itemX, itemY, 2, itemHeight, themeColor);
            } else if (isHovered) {
                RenderUtils.drawRoundedRect(poseStack, itemX, itemY, itemWidth, itemHeight, 6, themeColorHoverFill);
            }
            RenderUtils.drawCustomString(graphics.pose(), section.getDisplayName(), itemX + 15, itemY + 12, new Color(255,255,255).getRGB(), true);
            y += itemHeight + 8;
        }
    }

    private void renderSidebarFooter(GuiGraphics graphics, int centerX, int y) {
        PoseStack poseStack = graphics.pose();
        int lineX = guiLeft + 25;
        int lineWidth = SIDEBAR_WIDTH - 50;
        RenderUtils.fillRect(poseStack, lineX, y, lineWidth, 1, new Color(55,65,81).getRGB());
        y += 15;
        int hours = playTimeSeconds / 3600;
        int minutes = (playTimeSeconds % 3600) / 60;
        int seconds = playTimeSeconds % 60;
        String timeString = String.format("PlayTime: %02d:%02d:%02d", hours, minutes, seconds);
        float timeWidth = RenderUtils.getCustomStringWidth(timeString);
        RenderUtils.drawCustomString(graphics.pose(), timeString, centerX - timeWidth / 2, y, new Color(156,163,175).getRGB());
        y += 15;
        String nickname = "Name: " + (ClickGui.IS_VISIBLE ? this.minecraft.getUser().getName() : "RedSignalUser");
        float nicknameWidth = RenderUtils.getCustomStringWidth(nickname);
        RenderUtils.drawCustomString(graphics.pose(), nickname, centerX - nicknameWidth / 2, y, new Color(156,163,175).getRGB());
    }

    private void renderMainContent(GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack poseStack = graphics.pose();
        int contentLeft = guiLeft + SIDEBAR_WIDTH;
        int contentTop = guiTop;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 50;
        int contentHeight = GUI_HEIGHT - 50;
        RenderUtils.drawRoundedRect(poseStack, contentLeft + 25, contentTop + 25, contentWidth, contentHeight, SIDEBAR_RADIUS, new Color(25,25,25,204).getRGB());
        int scissorX = contentLeft + 25;
        int scissorY = contentTop + 25;
        RenderUtils.enableScissor(scissorX, scissorY, contentWidth, contentHeight);
        poseStack.pushPose();
        poseStack.translate(0, -scrollOffset, 0);
        int contentStartY = contentTop + 25;
        renderSectionContent(graphics, contentLeft + 50, contentStartY, mouseX, mouseY);
        poseStack.popPose();
        RenderUtils.disableScissor();
    }

    private void renderSectionContent(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (selectedSection == null || selectedSection == Section.HOMEPAGE) {
            welcomeComponent.render(graphics, x, y, themeColor);
        } else {
            double screenOriginX = screenLeftFromGuiLeft(guiLeft) + (x - guiLeft) * getEffectiveGuiScale();
            double screenOriginY = screenTopFromGuiTop(guiTop) + (y - guiTop) * getEffectiveGuiScale();
            if (selectedSection == Section.STYLES) {
                styleComponent.render(graphics, x, y, GUI_WIDTH - SIDEBAR_WIDTH - 50, GUI_HEIGHT - 50, mouseX, mouseY, themeColor);
            } else if (selectedSection == Section.SCRIPTS) {
                scriptsComponent.render(graphics, x, y, GUI_WIDTH - SIDEBAR_WIDTH - 50, GUI_HEIGHT - 50,
                        mouseX, mouseY, themeColor, screenOriginX, screenOriginY, getEffectiveGuiScale());
            } else if (editingOpen) {
                moduleEditComponent.render(graphics, x, y, GUI_WIDTH - SIDEBAR_WIDTH - 90, GUI_HEIGHT - 50, mouseX, mouseY, themeColor, screenOriginX, screenOriginY, getEffectiveGuiScale());
            } else if (selectedSection == Section.CRASHERS || selectedSection == Section.EXPLOITS) {
                moduleSelectorComponent.setSection(selectedSection);
                moduleSelectorComponent.render(graphics, x, y, GUI_WIDTH - SIDEBAR_WIDTH - 50, GUI_HEIGHT - 50, mouseX, mouseY, themeColor, screenOriginX, screenOriginY, getEffectiveGuiScale());
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedSection != null) {
            if (selectedSection == Section.STYLES) {
                if (styleComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
            } else if (selectedSection == Section.SCRIPTS) {
                if (scriptsComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
            } else {
                if (editingOpen && moduleEditComponent.keyPressed(keyCode)) return true;
                if (!editingOpen && moduleSelectorComponent.keyPressed(keyCode)) return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (selectedSection != null) {
            if (selectedSection == Section.STYLES) {
                if (styleComponent.charTyped(codePoint, modifiers)) return true;
            } else if (selectedSection == Section.SCRIPTS) {
                if (scriptsComponent.charTyped(codePoint, modifiers)) return true;
            } else {
                if (editingOpen && moduleEditComponent.charTyped(codePoint)) return true;
                if (!editingOpen && moduleSelectorComponent.charTyped(codePoint)) return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = toLocalX(mouseX);
        double ly = toLocalY(mouseY);

        if (button == 0) {
            Section[] sections = Section.values();
            int y = guiTop + 25 + 45 + 20;
            int sidebarLeft = guiLeft;
            for (Section section : sections) {
                int itemX = sidebarLeft + 10;
                int itemY = y;
                int itemWidth = SIDEBAR_WIDTH - 20;
                int itemHeight = 35;
                if (lx >= itemX && lx <= itemX + itemWidth && ly >= itemY && ly <= itemY + itemHeight) {
                    if (selectedSection == Section.SCRIPTS && section != Section.SCRIPTS) {
                        LAST_SCRIPTS_STATE = scriptsComponent.exportState();
                        scriptsComponent.onClose();
                    }
                    selectedSection = section;
                    if (section == Section.STYLES) {
                        styleComponent.refresh();
                    }
                    if (section == Section.CRASHERS || section == Section.EXPLOITS) {
                        moduleSelectorComponent.setSection(section);
                    } else if (section == Section.SCRIPTS) {
                        scriptsComponent.applyState(LAST_SCRIPTS_STATE);
                        scriptsComponent.onOpen();
                    }
                    scrollOffset = 0;
                    targetScrollOffset = 0;
                    editingOpen = false;

                    LAST_SELECTED_SECTION = section;
                    new ClientConfigHelper().saveClientConfig();

                    return true;
                }
                y += itemHeight + 8;
            }
            if (lx >= guiLeft && lx <= guiLeft + GUI_WIDTH && ly >= guiTop && ly <= guiTop + 30) {
                dragging = true;
                double screenLeft = screenLeftFromGuiLeft(guiLeft);
                double screenTop = screenTopFromGuiTop(guiTop);
                this.dragOffsetScreenX = mouseX - screenLeft;
                this.dragOffsetScreenY = mouseY - screenTop;
                return true;
            }
        }
        boolean consumed = false;
        if (selectedSection != null) {
            if (selectedSection == Section.STYLES) {
                consumed = styleComponent.mouseClicked(lx, ly, button);
            } else if (selectedSection == Section.SCRIPTS) {
                consumed = scriptsComponent.mouseClicked(lx, ly, button);
            } else if (editingOpen) {
                consumed = moduleEditComponent.mouseClicked(lx, ly, button);
            } else {
                consumed = moduleSelectorComponent.mouseClicked(lx, ly, button);
            }
        }
        return consumed || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;

        double lx = toLocalX(mouseX);
        double ly = toLocalY(mouseY);
        if (selectedSection == Section.STYLES) {
            styleComponent.mouseReleased(lx, ly, button);
        } else if (selectedSection == Section.SCRIPTS) {
            scriptsComponent.mouseReleased(lx, ly, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double lx = toLocalX(mouseX);
        double ly = toLocalY(mouseY);

        if (selectedSection == Section.STYLES) {
            styleComponent.mouseDragged(lx, ly, button, 0, 0);
        } else if (selectedSection == Section.SCRIPTS) {
            scriptsComponent.mouseDragged(lx, ly, button, 0, 0);
        }

        if (dragging) {
            double newScreenLeft = mouseX - this.dragOffsetScreenX;
            double newScreenTop = mouseY - this.dragOffsetScreenY;

            float effective = getEffectiveGuiScale();
            double screenGuiWidth = GUI_WIDTH * effective;
            double screenGuiHeight = GUI_HEIGHT * effective;
            newScreenLeft = clamp(newScreenLeft, 0, this.width - screenGuiWidth);
            newScreenTop = clamp(newScreenTop, 0, this.height - screenGuiHeight);

            setGuiLeftFromScreenLeft(newScreenLeft);
            setGuiTopFromScreenTop(newScreenTop);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        double lx = toLocalX(mouseX);
        double ly = toLocalY(mouseY);
        if (selectedSection != null) {

            if (selectedSection == Section.STYLES) {
                return true;
            }

            if (selectedSection == Section.SCRIPTS) {
                return scriptsComponent.mouseScrolled(lx, ly, scrollY);
            }

            int contentLeft = guiLeft + SIDEBAR_WIDTH + 25;
            int contentTop = guiTop + 25;
            int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 50;
            int contentHeight = GUI_HEIGHT - 50;
            if (lx >= contentLeft && lx <= contentLeft + contentWidth && ly >= contentTop && ly <= contentTop + contentHeight) {
                if (editingOpen && moduleEditComponent.mouseScrolled(lx, ly, scrollY))
                    return true;
                if (!editingOpen && moduleSelectorComponent.mouseScrolled(lx, ly, scrollY)) return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (selectedSection == Section.SCRIPTS) {
            LAST_SCRIPTS_STATE = scriptsComponent.exportState();
        }
        scriptsComponent.onClose();
        if (editingOpen) {
            LAST_EDITING_OPEN = true;
            LAST_EDITOR_STATE = moduleEditComponent.exportState();
        } else {
            LAST_EDITING_OPEN = false;
            LAST_EDITOR_STATE = null;
        }
        new ClientConfigHelper().saveClientConfig();
        super.removed();
    }

    private void updatePlayTime() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTimeUpdate >= 1000) {
            playTimeSeconds++;
            lastTimeUpdate = currentTime;
        }
    }

    public static int getCurrentThemeColor() {
        if (COLOR_THEME == ColorTheme.RAINBOW) {
            return getRainbowThemeColor();
        }
        if (CURRENT_THEME_COLOR != null) {
            try {
                return Color.decode(CURRENT_THEME_COLOR).getRGB();
            } catch (Exception ignored) {
            }
        }
        return new Color(236, 72, 153).getRGB();
    }

    private static int getRainbowThemeColor() {
        long period = 6000L;
        float hue = (System.currentTimeMillis() % period) / (float) period;
        return Color.HSBtoRGB(hue, 1f, 0.8f);
    }

    public static void setThemeColor(ColorTheme theme) {
        if (theme == ColorTheme.CUSTOM) {
            CURRENT_THEME_COLOR = CUSTOM_HEX;
            CURRENT_THEME_COLOR_LIGHT = CommonUi.UiUtils.lightenColor(Color.decode(CUSTOM_HEX).getRGB()) + "";
            COLOR_THEME = theme;
            updateThemeColors();
            return;
        }
        CURRENT_THEME_COLOR = theme.getPrimary();
        CURRENT_THEME_COLOR_LIGHT = theme.getLight();
        COLOR_THEME = theme;
        updateThemeColors();
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static void updateThemeColors() {
        themeColor = getCurrentThemeColor();
        int themeColorAlpha = COLOR_THEME.getAlpha();
        int themeColorAlphaHover = COLOR_THEME.getAlphaHover();

        themeColorFill = withAlpha(themeColor, themeColorAlpha);
        themeColorHoverFill = withAlpha(themeColor, themeColorAlphaHover);
    }

    private float getEffectiveGuiScale() {
        try {
            int mcGuiScale = (int) Objects.requireNonNullElseGet(this.minecraft, Minecraft::getInstance).getWindow().getGuiScale();
            if (mcGuiScale <= 0) mcGuiScale = 1;
            return BASE_GUI_SCALE * ((float) REFERENCE_MC_GUI_SCALE / (float) mcGuiScale);
        } catch (Exception e) {
            return BASE_GUI_SCALE;
        }
    }

    private double toLocalX(double mouseX) {
        double cx = guiLeft + GUI_WIDTH / 2f;
        float effective = getEffectiveGuiScale();
        return cx + (mouseX - cx) / effective;
    }

    private double toLocalY(double mouseY) {
        double cy = guiTop + GUI_HEIGHT / 2f;
        float effective = getEffectiveGuiScale();
        return cy + (mouseY - cy) / effective;
    }

    private double screenLeftFromGuiLeft(int guiLeft) {
        float effective = getEffectiveGuiScale();
        return guiLeft + (GUI_WIDTH / 2f) * (1f - effective);
    }

    private double screenTopFromGuiTop(int guiTop) {
        float effective = getEffectiveGuiScale();
        return guiTop + (GUI_HEIGHT / 2f) * (1f - effective);
    }

    private void setGuiLeftFromScreenLeft(double screenLeft) {
        float effective = getEffectiveGuiScale();
        this.guiLeft = (int) Math.round(screenLeft - (GUI_WIDTH / 2f) * (1f - effective));
    }

    private void setGuiTopFromScreenTop(double screenTop) {
        float effective = getEffectiveGuiScale();
        this.guiTop = (int) Math.round(screenTop - (GUI_HEIGHT / 2f) * (1f - effective));
    }

    private double clamp(double val, double min, double max) {
        if (val < min) return min;
        return Math.min(val, max);
    }

    public enum HudPlace {LEFT, RIGHT, HIDE}

    public enum ClientStyle {MINECRAFT, RED SIGNAL}
}