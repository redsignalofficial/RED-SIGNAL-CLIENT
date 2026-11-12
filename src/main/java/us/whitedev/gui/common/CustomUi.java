package us.whitedev.gui.common;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.awt.*;
import java.io.IOException;

public final class CustomUi {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("xynis", "textures/gui/custom_background.png");
    private static int backgroundTextureW = -1;
    private static int backgroundTextureH = -1;

    private static final int BG_GRADIENT_TOP = new Color(14, 16, 26, 120).getRGB();
    private static final int BG_GRADIENT_BOTTOM = new Color(4, 6, 12, 165).getRGB();
    private static final int VIGNETTE_TOP = new Color(0, 0, 0, 0).getRGB();
    private static final int VIGNETTE_BOTTOM = new Color(0, 0, 0, 95).getRGB();

    private CustomUi() {
    }

    public static void renderBackground(GuiGraphics g, int width, int height, int mouseX, int mouseY, float partialTick, int accentColor) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        ensureBackgroundTextureSize(width, height);
        g.setColor(1.0F, 1.0F, 1.0F, 0.95F);
        g.blit(BACKGROUND_TEXTURE, 0, 0, width, height, 0.0F, 0.0F, backgroundTextureW, backgroundTextureH, backgroundTextureW, backgroundTextureH);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        g.fillGradient(0, 0, width, height, BG_GRADIENT_TOP, BG_GRADIENT_BOTTOM);

        PoseStack pose = g.pose();

        float mx = width <= 0 ? 0.5f : (float) mouseX / (float) width;
        float my = height <= 0 ? 0.5f : (float) mouseY / (float) height;

        int glowColor = blend(accentColor, 0xFFFFFFFF, 0.25f);
        int glow1 = withAlpha(glowColor, 72);
        int glow2 = withAlpha(glowColor, 38);

        int gx1 = (int) (width * (0.25f + 0.18f * (mx - 0.5f)));
        int gy1 = (int) (height * (0.30f + 0.18f * (my - 0.5f)));
        RenderUtils.drawCircle(pose, gx1, gy1, 88, glow1);

        int gx2 = (int) (width * (0.80f + 0.22f * (mx - 0.5f)));
        int gy2 = (int) (height * (0.78f + 0.22f * (my - 0.5f)));
        RenderUtils.drawCircle(pose, gx2, gy2, 120, glow2);

        long t = Util.getMillis();
        float band = (t % 8000L) / 8000f;
        float bandFade = cycleFade(band);
        float bw = Math.max(220, width * 0.55f);
        float centerX = -bw + (width + bw * 2f) * band;
        float bx = centerX - bw * 0.5f;
        int b1 = withAlpha(glowColor, (int) (28 * bandFade));
        int b0 = withAlpha(accentColor, 0);
        int halfW = (int) Math.max(1, bw * 0.5f);
        RenderUtils.drawGradientRect(pose, (int) bx, 0, halfW, height, b0, b1, b1, b0);
        RenderUtils.drawGradientRect(pose, (int) bx + halfW, 0, halfW, height, b1, b0, b0, b1);

        float band2 = (t % 11000L) / 11000f;
        float bandFade2 = cycleFade(band2);
        float bh = Math.max(180, height * 0.40f);
        float centerY = -bh + (height + bh * 2f) * band2;
        float by = centerY - bh * 0.5f;
        int b2 = withAlpha(glowColor, (int) (18 * bandFade2));
        int halfH = (int) Math.max(1, bh * 0.5f);
        RenderUtils.drawGradientRect(pose, 0, (int) by, width, halfH, b0, b0, b2, b2);
        RenderUtils.drawGradientRect(pose, 0, (int) by + halfH, width, halfH, b2, b2, b0, b0);

        int noise = 110;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder noiseBuffer = Tesselator.getInstance().getBuilder();
        noiseBuffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < noise; i++) {
            int h = (int) (t / 250L) + i * 9973;
            float rx = (float) ((h * 1103515245L + 12345L) & 0x7fffffff) / 2147483647f;
            float ry = (float) (((h + 1337) * 1103515245L + 12345L) & 0x7fffffff) / 2147483647f;
            float px = rx * width;
            float py = ry * height;
            int a = 6 + (int) (12 * (0.5f + 0.5f * Mth.sin((t + i * 37L) / 500f)));
            int c = withAlpha(0xFFFFFFFF, a);
            RenderUtils.appendCircleTriangles(noiseBuffer, pose.last().pose(), px, py, 0.75f, c);
        }
        BufferUploader.drawWithShader(noiseBuffer.end());

        g.fillGradient(0, 0, width, height, VIGNETTE_TOP, VIGNETTE_BOTTOM);

        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    public static void renderBackground(PoseStack pose, int width, int height, int mouseX, int mouseY, float partialTick, int accentColor) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int top = new Color(14, 16, 26, 220).getRGB();
        int bottom = new Color(4, 6, 12, 235).getRGB();
        RenderUtils.drawGradientRect(pose, 0, 0, width, height, top, bottom);

        float mx = width <= 0 ? 0.5f : (float) mouseX / (float) width;
        float my = height <= 0 ? 0.5f : (float) mouseY / (float) height;

        int glowColor = blend(accentColor, 0xFFFFFFFF, 0.25f);
        int glow1 = withAlpha(glowColor, 72);
        int glow2 = withAlpha(glowColor, 38);

        int gx1 = (int) (width * (0.25f + 0.18f * (mx - 0.5f)));
        int gy1 = (int) (height * (0.30f + 0.18f * (my - 0.5f)));
        RenderUtils.drawCircle(pose, gx1, gy1, 88, glow1);

        int gx2 = (int) (width * (0.80f + 0.22f * (mx - 0.5f)));
        int gy2 = (int) (height * (0.78f + 0.22f * (my - 0.5f)));
        RenderUtils.drawCircle(pose, gx2, gy2, 120, glow2);

        long t = Util.getMillis();
        int noise = 110;
        for (int i = 0; i < noise; i++) {
            int h = (int) (t / 250L) + i * 9973;
            float rx = (float) ((h * 1103515245L + 12345L) & 0x7fffffff) / 2147483647f;
            float ry = (float) (((h + 1337) * 1103515245L + 12345L) & 0x7fffffff) / 2147483647f;
            float px = rx * width;
            float py = ry * height;
            int a = 6 + (int) (12 * (0.5f + 0.5f * Mth.sin((t + i * 37L) / 500f)));
            int c = withAlpha(0xFFFFFFFF, a);
            RenderUtils.drawCircle(pose, px, py, 0.75f, c);
        }

        int vignetteTop = new Color(0, 0, 0, 0).getRGB();
        int vignetteBottom = new Color(0, 0, 0, 95).getRGB();
        RenderUtils.drawGradientRect(pose, 0, 0, width, height, vignetteTop, vignetteBottom);

        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static void ensureBackgroundTextureSize(int fallbackW, int fallbackH) {
        if (backgroundTextureW > 0 && backgroundTextureH > 0) {
            return;
        }

        try (InputStream in = CustomUi.class.getResourceAsStream("/client/images/background.png")) {
            if (in != null) {
                NativeImage img = NativeImage.read(in);
                backgroundTextureW = img.getWidth();
                backgroundTextureH = img.getHeight();
                img.close();
                return;
            }
        } catch (IOException ignored) {
        }

        backgroundTextureW = Math.max(1, fallbackW);
        backgroundTextureH = Math.max(1, fallbackH);
    }

    private static float cycleFade(float t) {
        float in = smoothstep01(Mth.clamp((t - 0.04f) / 0.14f, 0f, 1f));
        float out = smoothstep01(Mth.clamp((0.96f - t) / 0.14f, 0f, 1f));
        return in * out;
    }

    private static float smoothstep01(float x) {
        return (float) Mth.smoothstep(x);
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
}
