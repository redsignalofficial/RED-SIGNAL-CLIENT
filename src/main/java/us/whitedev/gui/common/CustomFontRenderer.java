package us.whitedev.gui.common;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CustomFontRenderer {
    private static final Minecraft mc = Minecraft.getInstance();
    private final Font awtFont;
    private final Font awtFontBold;
    private final Map<String, GlyphData> glyphCache = new HashMap<>();
    private final Map<String, ResourceLocation> textureCache = new HashMap<>();
    private final FontMetrics fontMetrics;
    private final FontMetrics fontMetricsBold;
    private int textureCounter = 0;
    private final float scaleFactor = 3.0f;

    private record GlyphData(int width, int height, int ascent, DynamicTexture texture, int textureWidth, int textureHeight) { }

    public CustomFontRenderer(String fontName, int fontSize) {
        this.awtFont = new Font(fontName, Font.PLAIN, (int)(fontSize * scaleFactor));
        this.awtFontBold = new Font(fontName, Font.BOLD, (int)(fontSize * scaleFactor));

        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        setupGraphicsHints(g2d);

        this.fontMetrics = g2d.getFontMetrics(awtFont);
        this.fontMetricsBold = g2d.getFontMetrics(awtFontBold);
        g2d.dispose();
    }

    private void setupGraphicsHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    }

    private GlyphData getOrCreateGlyph(String str, boolean bold) {
        String key = str + "_" + (bold ? "b" : "n");
        if (glyphCache.containsKey(key)) {
            return glyphCache.get(key);
        }

        FontMetrics metrics = bold ? fontMetricsBold : fontMetrics;
        Font font = bold ? awtFontBold : awtFont;

        int width = metrics.stringWidth(str);
        int height = metrics.getHeight();
        int ascent = metrics.getAscent();

        int padding = (int)(4 * scaleFactor);
        int textureWidth = Math.max(1, width + padding * 2);
        int textureHeight = Math.max(1, (int) (height + padding * 2.2));

        BufferedImage image = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, textureWidth, textureHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        setupGraphicsHints(g2d);

        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.drawString(str, padding, padding + ascent);
        g2d.dispose();

        for (int px = 0; px < textureWidth; px++) {
            for (int py = 0; py < textureHeight; py++) {
                int argb = image.getRGB(px, py);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    image.setRGB(px, py, 0);
                } else {
                    int whiteArgb = (a << 24) | 0x00FFFFFF;
                    image.setRGB(px, py, whiteArgb);
                }
            }
        }

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, textureWidth, textureHeight, true);
        for (int x = 0; x < textureWidth; x++) {
            for (int y = 0; y < textureHeight; y++) {
                int argb = image.getRGB(x, y);
                nativeImage.setPixelRGBA(x, y, argb);
            }
        }

        DynamicTexture texture = new DynamicTexture(nativeImage);
        texture.setFilter(true, true);

        String textureName = "customfont/glyph_" + textureCounter++;
        ResourceLocation textureId = mc.getTextureManager().register(textureName, texture);

        GlyphData data = new GlyphData(
                (int)(width / scaleFactor),
                (int)(height / scaleFactor),
                (int)(ascent / scaleFactor),
                texture,
                textureWidth,
                textureHeight
        );
        glyphCache.put(key, data);
        textureCache.put(key, textureId);

        return data;
    }

    public int getFontHeight(boolean bold) {
        return (int)((bold ? fontMetricsBold.getHeight() : fontMetrics.getHeight()) / scaleFactor);
    }

    public int getAscent(boolean bold) {
        return (int)((bold ? fontMetricsBold.getAscent() : fontMetrics.getAscent()) / scaleFactor);
    }

    public void drawString(PoseStack poseStack, String text, float x, float y, int color) {
        drawString(poseStack, text, x, y, color, false, false);
    }

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, boolean bold) {
        drawString(poseStack, text, x, y, color, bold, false);
    }

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, boolean bold, boolean shadow) {
        if (shadow) {
            drawStringInternal(poseStack, text, x + 1, y + 1, (color & 0x00FFFFFF) | 0x40000000, bold);
        }
        drawStringInternal(poseStack, text, x, y, color, bold);
    }

    private void drawStringInternal(PoseStack poseStack, String text, float x, float y, int color, boolean bold) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        if (a == 0) a = 1.0F;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        float currentX = x;
        FontMetrics metrics = bold ? fontMetricsBold : fontMetrics;

        int i = 0;
        int len = text.length();
        while (i < len) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            String str = new String(Character.toChars(codePoint));
            i += charCount;

            if (str.equals(" ")) {
                currentX += metrics.charWidth(' ') / scaleFactor;
                continue;
            }

            GlyphData glyph = getOrCreateGlyph(str, bold);
            String key = str + "_" + (bold ? "b" : "n");
            ResourceLocation textureId = textureCache.get(key);

            if (textureId != null && glyph != null) {
                RenderSystem.setShaderTexture(0, textureId);

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                float renderWidth = glyph.width;
                float renderHeight = glyph.height;

                float x0 = currentX;
                float x1 = currentX + renderWidth;
                float y1 = y + renderHeight;

                float padding = 4.0f * scaleFactor;
                float u0 = padding / glyph.textureWidth;
                float u1 = (glyph.textureWidth - padding) / glyph.textureWidth;
                float v0 = padding / glyph.textureHeight;
                float v1 = (glyph.textureHeight - padding) / glyph.textureHeight;

                buffer.vertex(matrix, x0, y1, 0).uv(u0, v1).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, x1, y1, 0).uv(u1, v1).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, x1, y, 0).uv(u1, v0).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, x0, y, 0).uv(u0, v0).color(r, g, b, a).endVertex();

                BufferUploader.drawWithShader(buffer.end());

                currentX += renderWidth;
            }
        }

        RenderSystem.disableBlend();
    }

    public void drawCenteredString(PoseStack poseStack, String text, float x, float y, int color, boolean bold) {
        float width = getStringWidth(text, bold);
        drawString(poseStack, text, x - width / 2, y, color, bold, false);
    }

    public float getStringWidth(String text, boolean bold) {
        float width = 0;
        FontMetrics metrics = bold ? fontMetricsBold : fontMetrics;
        int i = 0;
        int len = text.length();
        while (i < len) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            i += charCount;
            if (cp == ' ') {
                width += metrics.charWidth(' ') / scaleFactor;
            } else {
                String s = new String(Character.toChars(cp));
                GlyphData glyph = getOrCreateGlyph(s, bold);
                if (glyph != null) width += glyph.width;
            }
        }
        return width;
    }
}