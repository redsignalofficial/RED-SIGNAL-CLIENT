package us.whitedev.gui.clickgui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import us.whitedev.XynisClient;
import us.whitedev.gui.common.RenderUtils;
import us.whitedev.proxy.XynisProxy;

import java.awt.Color;

public class WelcomeComponent {

    private static final String TITLE = "Welcome to XynisClient!";
    private static final String SUBTITLE = "Exclusive Crash/Exploit Client based on Minecraft 1.20.1";
    private static final String BULLET_POINT = "• ";

    private static final int TITLE_SIZE = 24;
    private static final int SUBTITLE_SIZE = 14;
    private static final int CHANGELOG_TITLE_SIZE = 16;
    private static final int CHANGELOG_ITEM_SIZE = 13;

    private static final int SUBTITLE_COLOR = new Color(209, 213, 219).getRGB();
    private static final int TEXT_COLOR = new Color(255, 255, 255).getRGB();

    private static final int TITLE_SPACING = 6;
    private static final int SUBTITLE_SPACING = 10;
    private static final int SECTION_SPACING = 8;
    private static final int ITEM_SPACING = 4;
    private static final int CHANGELOG_HEADER_SPACING = 6;

    public static final String[] CLIENT_CHANGELOG = {
            "Update Banshee1 Crash Method",
            "Add Banshee2 (Paper 26.1) Crash Method",
            "Add LPX Crash Config for Banshee1",
            "Add Hud Style Editor",
            "Add Server Payload Channels Catcher",
            "Add TAB for Xynis Commands",
            "Fix Detect Command",
            "Add Rainbow prefix for Rainbow GUI",
            "Add writeItemUntrusted for Creative Packet",
            "Add MiniMessage support"
    };

    public static final String[] PROXY_CHANGELOG = {
            "Reworked proxy system",
            "Reworked proxy commands",
            "Reworked proxy handler"
    };

    public void render(GuiGraphics graphics, int x, int y, int themeColor) {
        PoseStack poseStack = graphics.pose();
        y = renderHeader(poseStack, x, y, themeColor);

        y = renderChangelog(poseStack, x, y, themeColor,
                "XynisClient", XynisClient.VERSION, CLIENT_CHANGELOG);

        y += SECTION_SPACING;

        renderChangelog(poseStack, x, y, themeColor,
                "XynisProxy", XynisProxy.VERSION, PROXY_CHANGELOG);
    }

    private int renderHeader(PoseStack poseStack, int x, int y, int themeColor) {
        y = drawText(poseStack, TITLE, x, y, themeColor, TITLE_SIZE, true);
        y += TITLE_SPACING;

        y = drawText(poseStack, SUBTITLE, x, y, SUBTITLE_COLOR, SUBTITLE_SIZE, false);
        y += SUBTITLE_SPACING;

        return y;
    }

    private int renderChangelog(PoseStack poseStack, int x, int y, int themeColor, String productName, String version, String[] items) {
        String title = String.format("Changelog for %s %s", productName, version);
        y = drawChangelogTitle(poseStack, title, x, y, themeColor);

        for (String item : items) {
            y = drawBulletPoint(poseStack, item, x, y, themeColor);
        }

        return y;
    }

    private int drawChangelogTitle(PoseStack poseStack, String text, int x, int y, int themeColor) {
        y = drawText(poseStack, text, x, y, themeColor, CHANGELOG_TITLE_SIZE, true);
        return y + CHANGELOG_HEADER_SPACING;
    }

    private int drawBulletPoint(PoseStack poseStack, String text, int x, int y, int themeColor) {
        float ascent = RenderUtils.getCustomFontAscent(CHANGELOG_ITEM_SIZE, false);
        int textY = y + (int)ascent;

        RenderUtils.drawCustomString(poseStack, BULLET_POINT, x, textY, themeColor, false, CHANGELOG_ITEM_SIZE);

        float bulletWidth = RenderUtils.getCustomStringWidth(BULLET_POINT, false, CHANGELOG_ITEM_SIZE);
        RenderUtils.drawCustomString(poseStack, text, x + (int)bulletWidth, textY, TEXT_COLOR, false, CHANGELOG_ITEM_SIZE);

        return y + RenderUtils.getCustomFontHeight(CHANGELOG_ITEM_SIZE, false) + ITEM_SPACING;
    }

    private int drawText(PoseStack poseStack, String text, int x, int y, int color, int size, boolean bold) {
        float ascent = RenderUtils.getCustomFontAscent(size, bold);
        RenderUtils.drawCustomString(poseStack, text, x, y + ascent, color, bold, size);
        return y + RenderUtils.getCustomFontHeight(size, bold);
    }
}
