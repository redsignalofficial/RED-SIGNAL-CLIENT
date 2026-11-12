package us.rin.gui.hud.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import us.rin.gui.common.RenderUtils;

import java.awt.Color;
import java.util.*;

public final class NotificationManager {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final Deque<Notification> queue = new ArrayDeque<>();
    private static final List<Notification> active = new ArrayList<>();

    private static final int MAX_VISIBLE = 3;
    private static final int NOTIF_WIDTH_MAX = 320;
    private static final int NOTIF_HEIGHT = 48;
    private static final int PADDING = 12;
    private static final int GAP = 8;
    private static final int ICON_SIZE = 16;

    private static final long APPEAR_TIME = 200;
    private static final long FADE_TIME = 300;
    private static final long HOTBAR_OFFSET = 50;

    private NotificationManager() {}

    public static void push(NotificationType type, String title, String message, long durationMs) {
        queue.addLast(new Notification(type, title, message, durationMs));
    }

    public static void info(String title, String msg, long d)    { push(NotificationType.INFO, title, msg, d); }
    public static void success(String title, String msg, long d) { push(NotificationType.SUCCESS, title, msg, d); }
    public static void warning(String title, String msg, long d) { push(NotificationType.WARNING, title, msg, d); }
    public static void error(String title, String msg, long d)   { push(NotificationType.ERROR, title, msg, d); }

    public static void render(GuiGraphics graphics) {
        while (active.size() < MAX_VISIBLE && !queue.isEmpty()) {
            active.add(queue.pollFirst());
        }

        long now = System.currentTimeMillis();
        active.removeIf(n -> n.started && now - n.startTime > n.duration + FADE_TIME);

        if (active.isEmpty()) return;

        PoseStack pose = graphics.pose();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int baseY = screenH - (int) HOTBAR_OFFSET;

        for (int i = 0; i < active.size(); i++) {
            Notification n = active.get(i);
            if (!n.started) {
                n.started = true;
                n.startTime = now;
            }

            long elapsed = now - n.startTime;
            float opacity;
            float slide;

            if (elapsed < APPEAR_TIME) {
                float t = elapsed / (float) APPEAR_TIME;
                opacity = t;
                slide = (1f - easeOutCubic(t)) * 40f;
            } else if (elapsed < n.duration) {
                opacity = 1f;
                slide = 0f;
            } else {
                float t = (elapsed - n.duration) / (float) FADE_TIME;
                opacity = 1f - t;
                slide = easeInCubic(t) * 40f;
            }

            int y = baseY - (i + 1) * (NOTIF_HEIGHT + GAP);

            float titleH = RenderUtils.getCustomFontHeight(14, true);
            float msgH = RenderUtils.getCustomFontHeight(12, false);
            float textBlockH = titleH + msgH + 2;

            float textStartY = y + (NOTIF_HEIGHT - textBlockH) / 2f;

            int width = Math.min(
                    (int) Math.max(
                            RenderUtils.getCustomStringWidth(n.title, true, 14),
                            RenderUtils.getCustomStringWidth(n.message, false, 12)
                    ) + PADDING * 3 + ICON_SIZE,
                    NOTIF_WIDTH_MAX
            );

            int x = screenW - width - 10 + (int) slide;

            Color accent = n.type.color;
            int bg = new Color(10, 10, 10, (int) (190 * opacity)).getRGB();
            int outline = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (200 * opacity)).getRGB();

            pose.pushPose();

            RenderUtils.drawRoundedRect(pose, x, y, width, NOTIF_HEIGHT, 8, bg);
            RenderUtils.drawRoundedRectOutline(pose, x, y, width, NOTIF_HEIGHT, 8, 2, outline);

            RenderUtils.drawCustomString(pose, n.type.icon, x + PADDING, y + (NOTIF_HEIGHT - ICON_SIZE) / 2f - 0.5f, outline, true, 16);

            float textX = x + PADDING * 2 + ICON_SIZE;

            drawFormattedString(pose, n.title, textX, textStartY, new Color(255, 255, 255, (int) (255 * opacity)).getRGB(), true, 14);

            drawFormattedString(pose, n.message, textX, textStartY + titleH + 2, new Color(180, 180, 180, (int) (220 * opacity)).getRGB(), false, 12);

            float progress = Math.max(0f, 1f - (elapsed / (float) n.duration));
            int barW = (int) ((width - PADDING * 2) * progress);
            RenderUtils.drawRoundedRect(pose, x + PADDING, y + NOTIF_HEIGHT - 5, barW, 3, 2, outline);

            pose.popPose();
        }
    }

    private static void drawFormattedString(
            PoseStack pose,
            String text,
            float x,
            float y,
            int defaultColor,
            boolean bold,
            int size
    ) {
        if (text == null || text.isEmpty()) return;

        int color = defaultColor;
        boolean isBold = bold;
        float drawX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '&' && i + 1 < text.length()) {
                ChatFormat f = ChatFormat.byCode(text.charAt(i + 1));
                if (f != null) {
                    if (f.color >= 0) color = f.color;
                    if (f == ChatFormat.BOLD) isBold = true;
                    if (f == ChatFormat.RESET) {
                        color = defaultColor;
                        isBold = bold;
                    }
                    i++;
                    continue;
                }
            }

            String s = String.valueOf(c);
            RenderUtils.drawCustomString(pose, s, drawX, y, color, isBold, size);
            drawX += RenderUtils.getCustomStringWidth(s, isBold, size);
        }
    }

    private static float easeOutCubic(float t) { return 1f - (float) Math.pow(1f - t, 3); }
    private static float easeInCubic(float t)  { return t * t * t; }

    private static final class Notification {
        final NotificationType type;
        final String title;
        final String message;
        final long duration;
        long startTime;
        boolean started;

        Notification(NotificationType type, String title, String message, long duration) {
            this.type = type;
            this.title = title == null ? "" : title;
            this.message = message == null ? "" : message;
            this.duration = Math.max(500, duration);
        }
    }

    public enum NotificationType {
        INFO("ℹ", new Color(59, 130, 246)),
        SUCCESS("✔", new Color(34, 197, 94)),
        WARNING("⚠", new Color(234, 179, 8)),
        ERROR("✖", new Color(239, 68, 68));

        final String icon;
        final Color color;

        NotificationType(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public enum ChatFormat {
        BLACK('0', 0x000000),
        DARK_BLUE('1', 0x0000AA),
        DARK_GREEN('2', 0x00AA00),
        DARK_AQUA('3', 0x00AAAA),
        DARK_RED('4', 0xAA0000),
        DARK_PURPLE('5', 0xAA00AA),
        GOLD('6', 0xFFAA00),
        GRAY('7', 0xAAAAAA),
        DARK_GRAY('8', 0x555555),
        BLUE('9', 0x5555FF),
        GREEN('a', 0x55FF55),
        AQUA('b', 0x55FFFF),
        RED('c', 0xFF5555),
        LIGHT_PURPLE('d', 0xFF55FF),
        YELLOW('e', 0xFFFF55),
        WHITE('f', 0xFFFFFF),
        BOLD('l', -1),
        RESET('r', -2);

        public final char code;
        public final int color;

        ChatFormat(char code, int color) {
            this.code = code;
            this.color = color;
        }

        public static ChatFormat byCode(char c) {
            for (ChatFormat f : values())
                if (f.code == c) return f;
            return null;
        }
    }
}
