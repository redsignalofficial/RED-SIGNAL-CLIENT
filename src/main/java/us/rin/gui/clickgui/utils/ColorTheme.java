package us.rin.gui.clickgui.utils;

import java.awt.Color;

public enum ColorTheme {
    PINK("#ec4899", "#f9a8d4", alphaFromHex("#ec4899", 0.25), alphaFromHex("#ec4899", 0.15), "&d"),
    PURPLE("#8b5cf6", "#c4b5fd", alphaFromHex("#8b5cf6", 0.25), alphaFromHex("#8b5cf6", 0.15), "&5"),
    BLUE("#3b82f6", "#93c5fd", alphaFromHex("#3b82f6", 0.25), alphaFromHex("#3b82f6", 0.15), "&9"),
    GREEN("#10b981", "#6ee7b7", alphaFromHex("#10b981", 0.25), alphaFromHex("#10b981", 0.15), "&a"),
    RED("#ef4444", "#fca5a5", alphaFromHex("#ef4444", 0.25), alphaFromHex("#ef4444", 0.15), "&c"),
    ORANGE("#f97316", "#fdba74", alphaFromHex("#f97316", 0.25), alphaFromHex("#f97316", 0.15), "&6"),
    YELLOW("#eab308", "#fde047", alphaFromHex("#eab308", 0.25), alphaFromHex("#eab308", 0.15), "&e"),
    CYAN("#06b6d4", "#67e8f9", alphaFromHex("#06b6d4", 0.25), alphaFromHex("#06b6d4", 0.15), "&b"),
    EMERALD("#059669", "#6ee7b7", alphaFromHex("#059669", 0.25), alphaFromHex("#059669", 0.15), "&a"),
    INDIGO("#6366f1", "#a5b4fc", alphaFromHex("#6366f1", 0.25), alphaFromHex("#6366f1", 0.15), "&9"),
    RAINBOW("#ffffff", "#ffffff", 64, 38, "&f"),
    CUSTOM("#ffffff", "#ffffff", 64, 38, "&f");

    private final String primary;
    private final String light;
    private final int alpha;
    private final int alphaHover;
    private final String minecraftColorCode;

    ColorTheme(String primary, String light, int alpha, int alphaHover, String minecraftColorCode) {
        this.primary = primary;
        this.light = light;
        this.alpha = alpha;
        this.alphaHover = alphaHover;
        this.minecraftColorCode = minecraftColorCode;
    }

    public String getPrimary() { return primary; }
    public String getLight() { return light; }
    public int getAlpha() { return alpha; }
    public int getAlphaHover() { return alphaHover; }
    public String getMinecraftColorCode() { return minecraftColorCode; }

    private static int alphaFromHex(String hexColor, double alpha) {
        double a1 = Math.max(0, Math.min(1.0, alpha)) * 255;
        try {
            Color color = Color.decode(hexColor);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            long round = Math.round(a1);
            return new Color(r, g, b, round).getRGB();
        } catch (Exception e) {
            int a = (int) Math.round(a1);
            return new Color(236, 72, 153, a).getRGB();
        }
    }
}