package us.whitedev.gui.common;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class CustomButton extends AbstractWidget {

    public enum IconSide {
        LEFT,
        RIGHT,
        CENTER
    }

    @FunctionalInterface
    public interface Renderer {
        void render(CustomButton button, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, float alphaMul);
    }

    private final Consumer<CustomButton> onPress;

    private int radius = 14;
    private int bgTop = 0xFF141821;
    private int bgBottom = 0xFF0D1016;
    private int borderColor = 0x0AFFFFFF;

    private int glowRgb = 0xFF6C7CFF;
    private int glowMaxAlpha = 64;

    private boolean drawBackground = true;
    private boolean drawBorder = true;
    private boolean drawLabel = true;

    private int textColor = 0xFFE6EAF2;
    private boolean textBold = false;
    private int textSize = 16;

    private String iconText;
    private IconSide iconSide = IconSide.RIGHT;
    private int iconColor = 0xFFE6EAF2;
    private int iconPadding = 12;
    private float iconScale = 1.0f;

    private Renderer customRenderer;

    private long lastUpdateMs = Util.getMillis();
    private float hoverAnim = 0f;
    private float pressAnim = 0f;
    private boolean pressed = false;

    public CustomButton(int x, int y, int width, int height, Component message, Consumer<CustomButton> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public CustomButton setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public CustomButton setBackground(int top, int bottom) {
        this.bgTop = top;
        this.bgBottom = bottom;
        return this;
    }

    public CustomButton setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public CustomButton setGlow(int glowRgb, int glowMaxAlpha) {
        this.glowRgb = glowRgb;
        this.glowMaxAlpha = glowMaxAlpha;
        return this;
    }

    public CustomButton setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
        return this;
    }

    public CustomButton setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        return this;
    }

    public CustomButton setDrawLabel(boolean drawLabel) {
        this.drawLabel = drawLabel;
        return this;
    }

    public CustomButton setText(int textColor, boolean bold, int size) {
        this.textColor = textColor;
        this.textBold = bold;
        this.textSize = size;
        return this;
    }

    public CustomButton setIcon(String iconText) {
        return setIcon(iconText, IconSide.RIGHT);
    }

    public CustomButton setIcon(String iconText, IconSide side) {
        this.iconText = (iconText == null || iconText.isBlank()) ? null : iconText;
        this.iconSide = side == null ? IconSide.RIGHT : side;
        return this;
    }

    public CustomButton setIconStyle(int iconColor, int iconPadding) {
        this.iconColor = iconColor;
        this.iconPadding = Math.max(0, iconPadding);
        return this;
    }

    public CustomButton setIconScale(float iconScale) {
        this.iconScale = Mth.clamp(iconScale, 0.5f, 3.0f);
        return this;
    }

    public CustomButton setRenderer(Renderer renderer) {
        this.customRenderer = renderer;
        return this;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.pressed = true;
        if (this.onPress != null) {
            this.onPress.accept(this);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.pressed = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = (now - this.lastUpdateMs) / 1000f;
        this.lastUpdateMs = now;
        dt = Mth.clamp(dt, 0f, 0.05f);

        boolean enabled = this.active;
        float targetHover = (enabled && this.isHovered) ? 1f : 0f;
        this.hoverAnim = smoothApproach(this.hoverAnim, targetHover, dt, 0.15f);

        float targetPress = (enabled && this.pressed) ? 1f : 0f;
        this.pressAnim = smoothApproach(this.pressAnim, targetPress, dt, 0.08f);

        float yLift = -2f * this.hoverAnim;
        float scale = 1f - 0.02f * this.pressAnim;

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        if (this.hoverAnim > 0.001f || this.pressAnim > 0.001f) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + w / 2f, y + h / 2f, 0);
            graphics.pose().translate(0, yLift, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.pose().translate(-(x + w / 2f), -(y + h / 2f), 0);
        }

        float aMul = Mth.clamp(this.alpha, 0f, 1f);
        if (!enabled) {
            aMul *= 0.55f;
        }

        if (this.hoverAnim > 0.001f) {
            int ga = (int) (this.glowMaxAlpha * this.hoverAnim * aMul);
            if (ga > 0) {
                int glow = (ga << 24) | (this.glowRgb & 0x00FFFFFF);
                RenderUtils.drawRoundedRect(graphics.pose(), x - 2, y - 2, w + 4, h + 4, this.radius + 2, glow);
            }
        }

        int top = lighten(this.bgTop, 0.08f * this.hoverAnim);
        int bottom = lighten(this.bgBottom, 0.08f * this.hoverAnim);

        if (this.drawBackground) {
            drawRoundedVerticalGradient(graphics, x, y, w, h, this.radius, top, bottom, aMul);
        }

        if (this.customRenderer != null) {
            this.customRenderer.render(this, graphics, mouseX, mouseY, partialTick, aMul);
        } else if (this.drawLabel) {
            renderLabel(graphics, aMul);
        }

        if (this.drawBorder) {
            int bc = applyAlpha(this.borderColor, aMul);
            RenderUtils.drawRoundedRectOutline(graphics.pose(), x, y, w, h, this.radius, 1, bc);
        }

        if (this.hoverAnim > 0.001f || this.pressAnim > 0.001f) {
            graphics.pose().popPose();
        }
    }

    private void renderLabel(GuiGraphics graphics, float alphaMul) {
        String text = this.getMessage().getString();
        float textW = RenderUtils.getCustomStringWidth(text, this.textBold, this.textSize);
        int fontH = RenderUtils.getCustomFontHeight(this.textSize, this.textBold);

        float tx = this.getX() + (this.getWidth() - textW) / 2f;
        float ty = this.getY() + (this.getHeight() - fontH) / 2f;

        int c = applyAlpha(this.textColor, alphaMul);
        RenderUtils.drawCustomString(graphics.pose(), text, tx, ty, c, this.textBold, this.textSize);

        if (this.iconText != null && !this.iconText.isEmpty()) {
            Font iconFont = Minecraft.getInstance().font;
            int iconW = iconFont.width(this.iconText);
            int iconH = iconFont.lineHeight;

            float scaledIconW = iconW * this.iconScale;
            float scaledIconH = iconH * this.iconScale;

            float iconX = switch (this.iconSide) {
                case RIGHT -> this.getX() + this.getWidth() - this.iconPadding - scaledIconW;
                case CENTER -> this.getX() + (this.width / 2f) - (scaledIconW);
                default -> this.getX() + this.iconPadding;
            };

            float iconY = this.getY() + (this.getHeight() - scaledIconH) / 2f - 4f;

            int iconColor = applyAlpha(this.iconColor, alphaMul);
            RenderUtils.drawCustomString(graphics.pose(), this.iconText, iconX, iconY, iconColor, this.textBold, (int) (this.textSize * this.iconScale));
        }
    }

    private static void drawRoundedVerticalGradient(GuiGraphics g, int x, int y, int w, int h, int radius, int top, int bottom, float alphaMul) {
        if (w <= 0 || h <= 0) return;

        int bands = 4;
        int overlap = 2;

        for (int i = 0; i < bands; i++) {
            float t = (float) i / (float) (bands - 1);
            int c = blend(top, bottom, t);
            c = applyAlpha(c, alphaMul);

            int y0 = y + (int) Math.floor(h * (i / (float) bands));
            int y1 = y + (int) Math.ceil(h * ((i + 1) / (float) bands));
            if (i > 0) y0 -= overlap;
            if (i < bands - 1) y1 += overlap;

            y0 = Math.max(y0, y);
            y1 = Math.min(y1, y + h);

            if (y1 <= y0) continue;

            g.enableScissor(x, y0, x + w, y1);
            g.fillRounded(x, y, w, h, radius, c);
            g.disableScissor();
        }
    }

    private static float smoothApproach(float current, float target, float dt, float timeSeconds) {
        if (timeSeconds <= 0f) return target;
        float k = 1f - (float) Math.exp(-dt / timeSeconds);
        return current + (target - current) * k;
    }

    private static int applyAlpha(int argb, float alphaMul) {
        int a = (argb >>> 24) & 0xFF;
        int na = Mth.clamp((int) (a * alphaMul), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    private static int lighten(int argb, float amount) {
        return blend(argb, 0xFFFFFFFF, Mth.clamp(amount, 0f, 1f));
    }

    private static int blend(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);

        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
