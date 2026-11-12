package us.rin.gui.common;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderUtils {

    private static final Map<Integer, CustomFontRenderer> customFontMap = new HashMap<>();
    private static final Minecraft mc = Minecraft.getInstance();

    private static final int DEFAULT_FONT_SIZE = 13;

    private static final Map<Integer, float[]> UNIT_CIRCLE_TABLE = new HashMap<>();

    static {
        try {
            customFontMap.put(DEFAULT_FONT_SIZE, new CustomFontRenderer("Inter", DEFAULT_FONT_SIZE));
        } catch (Exception e) {
            customFontMap.put(DEFAULT_FONT_SIZE, new CustomFontRenderer("SansSerif", DEFAULT_FONT_SIZE));
        }
    }

    private static CustomFontRenderer getFontRenderer(int fontSize) {
        if (fontSize <= 0) fontSize = DEFAULT_FONT_SIZE;
        return customFontMap.computeIfAbsent(fontSize, sz -> {
            try {
                return new CustomFontRenderer("Inter", sz);
            } catch (Exception e) {
                return new CustomFontRenderer("SansSerif", sz);
            }
        });
    }

    public static void drawCustomString(PoseStack poseStack, String text, float x, float y, int color) {
        drawCustomString(poseStack, text, x, y, color, false, DEFAULT_FONT_SIZE);
    }

    public static void drawCustomString(PoseStack poseStack, String text, float x, float y, int color, boolean bold) {
        drawCustomString(poseStack, text, x, y, color, bold, DEFAULT_FONT_SIZE);
    }

    public static void drawCustomString(PoseStack poseStack, String text, float x, float y, int color, boolean bold, int fontSize) {
        drawCustomString(poseStack, text, x, y, color, bold, fontSize, false, 0x40000000, 1.0f, 1.0f);
    }

    public static void drawCustomString(PoseStack poseStack, String text, float x, float y, int color, boolean bold, int fontSize, boolean shadow, int shadowColor, float shadowOffsetX, float shadowOffsetY) {
        CustomFontRenderer r = getFontRenderer(fontSize);
        if (shadow) {
            r.drawString(poseStack, text, x + shadowOffsetX, y + shadowOffsetY, shadowColor, bold, false);
        }
        r.drawString(poseStack, text, x, y, color, bold, false);
    }

    public static void drawCustomCenteredString(PoseStack poseStack, String text, float x, float y, int color) {
        drawCustomCenteredString(poseStack, text, x, y, color, false, DEFAULT_FONT_SIZE);
    }

    public static void drawCustomCenteredString(PoseStack poseStack, String text, float x, float y, int color, boolean bold) {
        drawCustomCenteredString(poseStack, text, x, y, color, bold, DEFAULT_FONT_SIZE);
    }

    public static void drawCustomCenteredString(PoseStack poseStack, String text, float x, float y, int color, boolean bold, int fontSize) {
        CustomFontRenderer r = getFontRenderer(fontSize);
        r.drawCenteredString(poseStack, text, x, y, color, bold);
    }

    public static float getCustomStringWidth(String text) {
        return getCustomStringWidth(text, false, DEFAULT_FONT_SIZE);
    }

    public static float getCustomStringWidth(String text, boolean bold) {
        return getCustomStringWidth(text, bold, DEFAULT_FONT_SIZE);
    }

    public static float getCustomStringWidth(String text, boolean bold, int fontSize) {
        CustomFontRenderer r = getFontRenderer(fontSize);
        return r.getStringWidth(text, bold);
    }

    public static int getCustomFontHeight(int fontSize, boolean bold) {
        return getFontRenderer(fontSize).getFontHeight(bold);
    }

    public static int getCustomFontAscent(int fontSize, boolean bold) {
        return getFontRenderer(fontSize).getAscent(bold);
    }

    public static void fillRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, x, y + height, 0).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }

    private static float[] getUnitCircleTable(int segments) {
        if (segments <= 0) {
            segments = 24;
        }

        final int segFinal = segments;
        return UNIT_CIRCLE_TABLE.computeIfAbsent(segFinal, s -> {
            float[] t = new float[(s + 1) * 2];
            double step = (Math.PI * 2.0) / (double) s;
            for (int i = 0; i <= s; i++) {
                double ang = step * (double) i;
                t[i * 2] = (float) Math.cos(ang);
                t[i * 2 + 1] = (float) Math.sin(ang);
            }
            return t;
        });
    }

    public static void appendCircleTriangles(BufferBuilder bufferBuilder, Matrix4f matrix, float centerX, float centerY, float radius, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        int segments = Math.max(24, (int) (radius * 2));
        float[] table = getUnitCircleTable(segments);
        for (int i = 0; i < segments; i++) {
            int idx0 = i * 2;
            int idx1 = (i + 1) * 2;

            float x0 = centerX + table[idx0] * radius;
            float y0 = centerY + table[idx0 + 1] * radius;
            float x1 = centerX + table[idx1] * radius;
            float y1 = centerY + table[idx1 + 1] * radius;

            bufferBuilder.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, x0, y0, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        }
    }

    public static void drawCircleAdditive(PoseStack poseStack, float centerX, float centerY, float radius, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();

        int segments = Math.max(24, (int) (radius * 2));
        float[] table = getUnitCircleTable(segments);
        for (int i = 0; i <= segments; i++) {
            int idx = i * 2;
            float x = centerX + table[idx] * radius;
            float y = centerY + table[idx + 1] * radius;
            bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(PoseStack poseStack, int x, int y, int width, int height, int radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomRight, boolean roundBottomLeft) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        radius = Math.min(radius, Math.min(width, height) / 2);

        int leftRadius = (roundTopLeft || roundBottomLeft) ? radius : 0;
        int rightRadius = (roundTopRight || roundBottomRight) ? radius : 0;
        int topRadius = (roundTopLeft || roundTopRight) ? radius : 0;
        int bottomRadius = (roundBottomLeft || roundBottomRight) ? radius : 0;

        int centerX = x + leftRadius;
        int centerW = Math.max(0, width - leftRadius - rightRadius);
        fillRect(poseStack, centerX, y, centerW, height, color);

        int maxed = Math.max(0, height - topRadius - bottomRadius);
        if (leftRadius > 0 && maxed > 0) {
            fillRect(poseStack, x, y + topRadius, leftRadius, maxed, color);
        }

        if (rightRadius > 0 && maxed > 0) {
            fillRect(poseStack, x + width - rightRadius, y + topRadius, rightRadius, maxed, color);
        }
        if (roundTopLeft) drawFilledCorner(poseStack, x + radius, y + radius, radius, 180, 270, r, g, b, a);
        if (roundTopRight) drawFilledCorner(poseStack, x + width - radius, y + radius, radius, 270, 360, r, g, b, a);
        if (roundBottomRight) drawFilledCorner(poseStack, x + width - radius, y + height - radius, radius, 0, 90, r, g, b, a);
        if (roundBottomLeft) drawFilledCorner(poseStack, x + radius, y + height - radius, radius, 90, 180, r, g, b, a);
    }

    public static void drawRoundedRect(PoseStack poseStack, int x, int y, int width, int height, int radius, int color) {
        drawRoundedRect(poseStack, x, y, width, height, radius, color, true, true, true, true);
    }

    private static void drawFilledCorner(PoseStack poseStack, float centerX, float centerY, float radius, float startAngle, float endAngle, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();

        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean wasDepthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        if (wasCullEnabled) GL11.glDisable(GL11.GL_CULL_FACE);
        if (wasDepthEnabled) GL11.glDisable(GL11.GL_DEPTH_TEST);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();

        int segments = Math.max(8, (int) (radius * 2));
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / (double) segments;
            double angle = startRad + (endRad - startRad) * t;
            float px = centerX + (float) (Math.cos(angle) * radius);
            float py = centerY + (float) (Math.sin(angle) * radius);
            bufferBuilder.vertex(matrix, px, py, 0).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();

        if (wasCullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        else GL11.glDisable(GL11.GL_CULL_FACE);

        if (wasDepthEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
        else GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public static void drawRoundedRectOutline(PoseStack poseStack, int x, int y, int width, int height, int radius, int lineWidth, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomRight, boolean roundBottomLeft) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        radius = Math.min(radius, Math.min(width, height) / 2);
        if (lineWidth <= 0) return;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float half = lineWidth / 2f;
        int arcSegments = Math.max(8, radius * 2);

        List<float[]> pts = new ArrayList<>();
        float tx0 = x + (roundTopLeft ? radius : 0);
        int i4 = x + width - (roundTopRight ? radius : 0);
        pts.add(new float[]{tx0, y, 0f, -1f});
        pts.add(new float[]{(float) i4, y, 0f, -1f});

        float cyTR = y + (roundTopRight ? radius : 0);
        if (roundTopRight) {
            for (int i = 0; i <= arcSegments; i++) {
                double ang = Math.toRadians(270.0 + 90.0 * i / arcSegments);
                float px = (float) i4 + (float) (Math.cos(ang) * radius);
                float py = cyTR + (float) (Math.sin(ang) * radius);
                float nx = (px - (float) i4);
                float ny = (py - cyTR);
                float len = (float) Math.hypot(nx, ny);
                if (len == 0) { nx = 1; ny = 0; len = 1; }
                nx /= len; ny /= len;
                pts.add(new float[]{px, py, nx, ny});
            }
        } else {
            float px = x + width;
            float nx = 1f; float ny = -1f;
            float len = (float)Math.hypot(nx, ny); nx/=len; ny/=len;
            pts.add(new float[]{px, (float) y, nx, ny});
        }

        float rx0 = x + width;
        float ry0 = y + (roundTopRight ? radius : 0);
        int i3 = y + height - (roundBottomRight ? radius : 0);
        pts.add(new float[]{rx0, ry0, 1f, 0f});
        pts.add(new float[]{rx0, (float) i3, 1f, 0f});

        int i2 = x + width - (roundBottomRight ? radius : 0);
        if (roundBottomRight) {
            for (int i = 0; i <= arcSegments; i++) {
                double ang = Math.toRadians(0.0 + 90.0 * i / arcSegments);
                float px = (float) i2 + (float) (Math.cos(ang) * radius);
                float py = (float) i3 + (float) (Math.sin(ang) * radius);
                float nx = (px - (float) i2);
                float ny = (py - (float) i3);
                float len = (float) Math.hypot(nx, ny);
                if (len == 0) { nx = 1; ny = 0; len = 1; }
                nx /= len; ny /= len;
                pts.add(new float[]{px, py, nx, ny});
            }
        } else {
            float px = x + width;
            float py = y + height;
            float nx = 1f; float ny = 1f;
            float len = (float)Math.hypot(nx, ny); nx/=len; ny/=len;
            pts.add(new float[]{px, py, nx, ny});
        }

        float bx1 = x + (roundBottomLeft ? radius : 0);
        pts.add(new float[]{(float) i2, y + height, 0f, 1f});
        pts.add(new float[]{bx1, y + height, 0f, 1f});

        float cxBL = x + (roundBottomLeft ? radius : 0);
        int i1 = y + height - (roundBottomLeft ? radius : 0);
        if (roundBottomLeft) {
            for (int i = 0; i <= arcSegments; i++) {
                double ang = Math.toRadians(90.0 + 90.0 * i / arcSegments);
                float px = cxBL + (float) (Math.cos(ang) * radius);
                float py = (float) i1 + (float) (Math.sin(ang) * radius);
                float nx = (px - cxBL);
                float ny = (py - (float) i1);
                float len = (float) Math.hypot(nx, ny);
                if (len == 0) { nx = -1; ny = 0; len = 1; }
                nx /= len; ny /= len;
                pts.add(new float[]{px, py, nx, ny});
            }
        } else {
            float py = y + height;
            float nx = -1f; float ny = 1f;
            float len = (float)Math.hypot(nx, ny); nx/=len; ny/=len;
            pts.add(new float[]{(float) x, py, nx, ny});
        }

        float ly1 = y + (roundTopLeft ? radius : 0);
        pts.add(new float[]{(float) x, (float) i1, -1f, 0f});
        pts.add(new float[]{(float) x, ly1, -1f, 0f});

        float cxTL = x + (roundTopLeft ? radius : 0);
        float cyTL = y + (roundTopLeft ? radius : 0);
        if (roundTopLeft) {
            for (int i = 0; i <= arcSegments; i++) {
                double ang = Math.toRadians(180.0 + 90.0 * i / arcSegments);
                float px = cxTL + (float) (Math.cos(ang) * radius);
                float py = cyTL + (float) (Math.sin(ang) * radius);
                float nx = (px - cxTL);
                float ny = (py - cyTL);
                float len = (float) Math.hypot(nx, ny);
                if (len == 0) { nx = -1; ny = 0; len = 1; }
                nx /= len; ny /= len;
                pts.add(new float[]{px, py, nx, ny});
            }
        } else {
            float nx = -1f; float ny = -1f;
            float len = (float)Math.hypot(nx, ny); nx/=len; ny/=len;
            pts.add(new float[]{(float) x, (float) y, nx, ny});
        }

        if (!pts.isEmpty()) pts.add(pts.get(0));

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (float[] p : pts) {
            float px = p[0];
            float py = p[1];
            float nx = p[2];
            float ny = p[3];

            float ox = px + nx * half;
            float oy = py + ny * half;
            float ix = px - nx * half;
            float iy = py - ny * half;

            bufferBuilder.vertex(matrix, ox, oy, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, ix, iy, 0).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRectOutline(PoseStack poseStack, int x, int y, int width, int height, int radius, int lineWidth, int color) {
        drawRoundedRectOutline(poseStack, x, y, width, height, radius, lineWidth, color, true, true, true, true);
    }

    public static void drawCircle(PoseStack poseStack, float centerX, float centerY, float radius, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();

        int segments = Math.max(24, (int) (radius * 2));
        float[] table = getUnitCircleTable(segments);
        for (int i = 0; i <= segments; i++) {
            int idx = i * 2;
            float x = centerX + table[idx] * radius;
            float y = centerY + table[idx + 1] * radius;
            bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }

    public static void drawGradientRect(PoseStack poseStack, int x, int y, int width, int height, int colorTop, int colorBottom) {
        drawGradientRect(poseStack, x, y, width, height, colorTop, colorTop, colorBottom, colorBottom);
    }

    public static void drawGradientRect(PoseStack poseStack, int x, int y, int width, int height, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
        float a1 = (colorTopLeft >> 24 & 255) / 255.0F;
        float r1 = (colorTopLeft >> 16 & 255) / 255.0F;
        float g1 = (colorTopLeft >> 8 & 255) / 255.0F;
        float b1 = (colorTopLeft & 255) / 255.0F;

        float a2 = (colorTopRight >> 24 & 255) / 255.0F;
        float r2 = (colorTopRight >> 16 & 255) / 255.0F;
        float g2 = (colorTopRight >> 8 & 255) / 255.0F;
        float b2 = (colorTopRight & 255) / 255.0F;

        float a3 = (colorBottomRight >> 24 & 255) / 255.0F;
        float r3 = (colorBottomRight >> 16 & 255) / 255.0F;
        float g3 = (colorBottomRight >> 8 & 255) / 255.0F;
        float b3 = (colorBottomRight & 255) / 255.0F;

        float a4 = (colorBottomLeft >> 24 & 255) / 255.0F;
        float r4 = (colorBottomLeft >> 16 & 255) / 255.0F;
        float g4 = (colorBottomLeft >> 8 & 255) / 255.0F;
        float b4 = (colorBottomLeft & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(r3, g3, b3, a3).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).color(r2, g2, b2, a2).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0).color(r1, g1, b1, a1).endVertex();
        bufferBuilder.vertex(matrix, x, y + height, 0).color(r4, g4, b4, a4).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }

    public static void enableScissor(int x, int y, int width, int height) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> enableScissor(x, y, width, height));
            return;
        }

        Window window = mc.getWindow();
        double scale = window.getGuiScale();

        int scaledX = (int) Math.max(0, Math.floor(x * scale));
        int scaledY = (int) Math.max(0, Math.floor(window.getHeight() - (y + height) * scale));
        int scaledWidth = (int) Math.max(0, Math.floor(width * scale));
        int scaledHeight = (int) Math.max(0, Math.floor(height * scale));

        scaledX = Math.min(scaledX, window.getWidth());
        scaledY = Math.min(scaledY, window.getHeight());
        scaledWidth = Math.min(scaledWidth, Math.max(0, window.getWidth() - scaledX));
        scaledHeight = Math.min(scaledHeight, Math.max(0, window.getHeight() - scaledY));

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scaledX, scaledY, Math.max(0, scaledWidth), Math.max(0, scaledHeight));
    }

    public static void disableScissor() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(RenderUtils::disableScissor);
            return;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static int getStringWidth(String text) {
        return mc.font.width(text);
    }

    public static int getStringHeight() {
        return mc.font.lineHeight;
    }

    public static void drawCenteredString(GuiGraphics graphics, String text, int centerX, int y, int color) {
        Font font = mc.font;
        int width = font.width(text);
        graphics.drawString(font, text, centerX - width / 2, y, color, false);
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(mc.font, text, x, y, color, true);
    }
}
