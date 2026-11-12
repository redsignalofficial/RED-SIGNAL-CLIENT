package us.rin.gui.hud.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import us.rin.RedSignalClient;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.common.RenderUtils;
import us.rin.gui.hud.utils.stylesheet.HudStylesheet;
import us.rin.gui.hud.utils.stylesheet.segments.*;
import us.rin.helpers.PacketHelper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class HudStyleManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final ServerInfoUtil serverUtil = new ServerInfoUtil();
    private static final String DEFAULT_RESOURCE_PATH = "/client/HudStyle.css";
    private static final Path STYLE_FILE = mc.gameDirectory.toPath().resolve("HudStyle.css");
    private static final Map<String, ResourceLocation> IMAGE_CACHE = new HashMap<>();
    private static final Map<String, AnimatedImage> GIF_CACHE = new HashMap<>();

    private static HudStylesheet cachedStyle;
    private static long cachedModified = Long.MIN_VALUE;

    private HudStyleManager() {
    }

    public static void render(GuiGraphics graphics) {
        HudStylesheet style = getStylesheet();
        PoseStack pose = graphics.pose();
        int rootX = resolveRootX(style.root);
        int rootY = style.root.offsetY;
        float scale = getEffectiveScale(style.root);
        int contentBottom = computeContentBottom(style);

        pose.pushPose();
        pose.translate(rootX, rootY, 0);
        pose.scale(scale, scale, 1.0f);

        for (HudBox box : style.boxes) {
            if (!box.visible) {
                continue;
            }

            int boxHeight = "content".equalsIgnoreCase(box.heightMode)
                    ? Math.max(box.height, contentBottom + box.contentPaddingBottom)
                    : box.height;

            if ((box.fillColor >>> 24) != 0) {
                RenderUtils.drawRoundedRect(pose, box.x, box.y, box.width, boxHeight, box.radius, box.fillColor);
            }
            if (box.borderSize > 0) {
                int borderColor = resolveColorToken(box.borderColorToken, PacketHelper.getServerLagTime());
                if ((borderColor >>> 24) != 0) {
                    RenderUtils.drawRoundedRectOutline(pose, box.x, box.y, box.width, boxHeight, box.radius, box.borderSize, borderColor);
                }
            }
        }

        for (HudLine line : style.lines) {
            renderLine(pose, line);
        }

        for (HudMetricList list : style.metricLists) {
            renderMetricList(pose, list);
        }

        for (HudImage image : style.images) {
            renderImage(graphics, image);
        }

        pose.popPose();
    }

    public static void ensureStyleFileExists() {
        try {
            if (Files.exists(STYLE_FILE)) {
                return;
            }
            Files.createDirectories(STYLE_FILE.getParent());
            Files.writeString(STYLE_FILE, readDefaultStyle(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static HudStylesheet getStylesheet() {
        ensureStyleFileExists();

        try {
            long modified = Files.exists(STYLE_FILE) ? Files.getLastModifiedTime(STYLE_FILE).toMillis() : Long.MIN_VALUE;
            if (cachedStyle == null || modified != cachedModified) {
                cachedStyle = parseStyle(Files.readString(STYLE_FILE, StandardCharsets.UTF_8));
                cachedModified = modified;
            }
        } catch (Exception e) {
            if (cachedStyle == null) {
                cachedStyle = parseStyle(readDefaultStyle());
            }
        }

        return cachedStyle;
    }

    private static String readDefaultStyle() {
        try (InputStream in = HudStyleManager.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private static HudStylesheet parseStyle(String rawText) {
        Map<String, BlockData> blocks = new LinkedHashMap<>();
        BlockData current = null;

        for (String rawLine : rawText.replace("\r", "").split("\n")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.endsWith("{")) {
                String header = line.substring(0, line.length() - 1).trim();
                current = new BlockData(header);
                blocks.put(header, current);
                continue;
            }
            if (line.equals("}")) {
                current = null;
                continue;
            }
            if (current == null) {
                continue;
            }

            int colon = line.indexOf(':');
            int semi = line.lastIndexOf(';');
            if (colon < 0 || semi < colon) {
                continue;
            }

            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1, semi).trim();
            current.properties.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }

        HudStylesheet sheet = new HudStylesheet();
        BlockData hud = blocks.get("hud");
        if (hud != null) {
            sheet.root.width = hud.getInt("width", 280);
            sheet.root.offsetX = hud.getInt("offset-x", 5);
            sheet.root.offsetY = hud.getInt("offset-y", 5);
            sheet.root.screenAnchor = hud.get("screen-anchor", "hud-place");
            sheet.root.scale = hud.getFloat("scale", 0.85f);
            sheet.root.referenceGuiScale = hud.getInt("reference-gui-scale", 2);
            sheet.root.autoScale = hud.getBoolean("auto-scale", true);
        }

        for (BlockData block : blocks.values()) {
            if (block.name.startsWith("box.")) {
                sheet.boxes.add(parseBox(block, sheet.root.width));
            } else if (block.name.startsWith("line.")) {
                sheet.lines.add(parseLine(block));
            } else if (block.name.startsWith("metric-list.")) {
                sheet.metricLists.add(parseMetricList(block, sheet.root.width));
            } else if (block.name.startsWith("image.")) {
                sheet.images.add(parseImage(block));
            }
        }

        sheet.boxes.sort(Comparator.comparingInt(box -> box.zIndex));
        sheet.lines.sort(Comparator.comparingInt(line -> line.zIndex));
        sheet.metricLists.sort(Comparator.comparingInt(list -> list.zIndex));
        sheet.images.sort(Comparator.comparingInt(image -> image.zIndex));
        return sheet;
    }

    private static HudBox parseBox(BlockData block, int defaultWidth) {
        HudBox box = new HudBox();
        box.visible = block.getBoolean("visible", true);
        box.x = block.getInt("x", 0);
        box.y = block.getInt("y", 0);
        box.width = block.getInt("width", defaultWidth);
        box.height = block.getInt("height", 0);
        box.heightMode = block.get("height-mode", "fixed");
        box.contentPaddingBottom = block.getInt("content-padding-bottom", 0);
        box.radius = block.getInt("radius", 0);
        box.fillColor = parseColor(block.get("background-color", "#00000000"), 0x00000000);
        box.borderColorToken = block.get("border-color", "#00000000");
        box.borderSize = block.getInt("border-size", 0);
        box.zIndex = block.getInt("z-index", 0);
        return box;
    }

    private static HudLine parseLine(BlockData block) {
        HudLine line = new HudLine();
        line.visible = block.getBoolean("visible", true);
        line.x = block.getFloat("x", 0);
        line.y = block.getFloat("y", 0);
        line.align = block.get("align", "left");
        line.shadow = block.getBoolean("shadow", false);
        line.shadowColorToken = block.get("shadow-color", "rgba(0,0,0,90)");
        line.shadowOffsetX = block.getFloat("shadow-offset-x", 1.0f);
        line.shadowOffsetY = block.getFloat("shadow-offset-y", 1.0f);
        line.zIndex = block.getInt("z-index", 10);

        for (String segmentValue : block.getAll("segment")) {
            String[] parts = splitPipe(segmentValue, 7);
            HudSegment segment = new HudSegment();
            segment.text = unquote(parts[0]);
            segment.colorToken = parts.length > 1 ? parts[1].trim() : "#FFFFFFFF";
            segment.bold = parts.length > 2 && Boolean.parseBoolean(parts[2].trim());
            segment.size = parts.length > 3 ? parseInt(parts[3].trim(), 13) : 13;
            segment.shadow = parts.length > 4 ? Boolean.parseBoolean(parts[4].trim()) : line.shadow;
            segment.shadowColorToken = parts.length > 5 ? parts[5].trim() : line.shadowColorToken;
            segment.shadowOffset = parts.length > 6 ? parseFloat(parts[6].trim(), 1.0f) : -1f;
            line.segments.add(segment);
        }

        return line;
    }

    private static HudMetricList parseMetricList(BlockData block, int defaultWidth) {
        HudMetricList list = new HudMetricList();
        list.visible = block.getBoolean("visible", true);
        list.x = block.getFloat("x", 0);
        list.y = block.getFloat("y", 0);
        list.width = block.getInt("width", defaultWidth);
        list.rowHeight = block.getInt("row-height", 14);
        list.separator = unquote(block.get("separator", "\\u00BB "));
        list.separatorColor = parseColor(block.get("separator-color", "#FF6B7280"), 0xFF6B7280);
        list.labelColor = parseColor(block.get("label-color", "#FFFFFFFF"), 0xFFFFFFFF);
        list.labelBold = block.getBoolean("label-bold", true);
        list.labelSize = block.getInt("label-size", 13);
        list.labelShadow = block.getBoolean("label-shadow", false);
        list.labelShadowColorToken = block.get("label-shadow-color", "rgba(0,0,0,90)");
        list.labelShadowOffsetX = block.getFloat("label-shadow-offset-x", 1.0f);
        list.labelShadowOffsetY = block.getFloat("label-shadow-offset-y", 1.0f);
        list.valueColorToken = block.get("value-color", "theme");
        list.valueBold = block.getBoolean("value-bold", false);
        list.valueSize = block.getInt("value-size", 13);
        list.valueAlign = block.get("value-align", "right");
        list.valueShadow = block.getBoolean("value-shadow", false);
        list.valueShadowColorToken = block.get("value-shadow-color", "rgba(0,0,0,90)");
        list.valueShadowOffsetX = block.getFloat("value-shadow-offset-x", 1.0f);
        list.valueShadowOffsetY = block.getFloat("value-shadow-offset-y", 1.0f);
        list.respectHudOptions = block.getBoolean("respect-hud-options", true);
        list.zIndex = block.getInt("z-index", 20);

        for (String itemValue : block.getAll("item")) {
            String[] parts = splitPipe(itemValue, 8);
            HudMetricItem item = new HudMetricItem();
            item.label = unquote(parts[0]);
            item.source = parts.length > 1 ? parts[1].trim() : parts[0].trim();
            item.colorToken = parts.length > 2 ? parts[2].trim() : list.valueColorToken;
            item.visible = parts.length <= 3 || Boolean.parseBoolean(parts[3].trim());
            item.shadow = parts.length > 4 ? Boolean.parseBoolean(parts[4].trim()) : list.valueShadow;
            item.shadowColorToken = parts.length > 5 ? parts[5].trim() : list.valueShadowColorToken;
            item.shadowOffsetX = parts.length > 6 ? parseFloat(parts[6].trim(), list.valueShadowOffsetX) : list.valueShadowOffsetX;
            item.shadowOffsetY = parts.length > 7 ? parseFloat(parts[7].trim(), list.valueShadowOffsetY) : list.valueShadowOffsetY;
            list.items.add(item);
        }

        return list;
    }

    private static HudImage parseImage(BlockData block) {
        HudImage image = new HudImage();
        image.visible = block.getBoolean("visible", true);
        image.x = block.getInt("x", 0);
        image.y = block.getInt("y", 0);
        image.width = block.getInt("width", 16);
        image.height = block.getInt("height", 16);
        image.path = unquote(block.get("path", ""));
        image.zIndex = block.getInt("z-index", 30);
        return image;
    }

    private static int computeContentBottom(HudStylesheet style) {
        int max = 0;

        for (HudLine line : style.lines) {
            if (!line.visible || line.segments.isEmpty()) {
                continue;
            }

            int height = 0;
            for (HudSegment segment : line.segments) {
                height = Math.max(height, RenderUtils.getCustomFontHeight(segment.size, segment.bold));
            }
            max = Math.max(max, Math.round(line.y + height));
        }

        for (HudMetricList list : style.metricLists) {
            if (!list.visible) {
                continue;
            }

            int count = 0;
            for (HudMetricItem item : list.items) {
                if (isMetricVisible(item, list)) {
                    count++;
                }
            }
            max = Math.max(max, Math.round(list.y + count * list.rowHeight));
        }

        for (HudImage image : style.images) {
            if (image.visible) {
                max = Math.max(max, image.y + image.height);
            }
        }

        return max;
    }

    private static void renderLine(PoseStack pose, HudLine line) {
        if (!line.visible || line.segments.isEmpty()) {
            return;
        }

        float totalWidth = 0;
        for (HudSegment segment : line.segments) {
            String text = resolveTemplate(segment.text);
            totalWidth += RenderUtils.getCustomStringWidth(text, segment.bold, segment.size);
        }

        float drawX = line.x;
        if ("center".equalsIgnoreCase(line.align)) {
            drawX -= totalWidth / 2f;
        } else if ("right".equalsIgnoreCase(line.align)) {
            drawX -= totalWidth;
        }

        for (HudSegment segment : line.segments) {
            String text = resolveTemplate(segment.text);
            int color = resolveColorToken(segment.colorToken, PacketHelper.getServerLagTime());
            boolean shadow = segment.shadow;
            int shadowColor = resolveColorToken(segment.shadowColorToken, PacketHelper.getServerLagTime());
            float shadowOffsetX = segment.shadowOffset >= 0 ? segment.shadowOffset : line.shadowOffsetX;
            float shadowOffsetY = segment.shadowOffset >= 0 ? segment.shadowOffset : line.shadowOffsetY;
            float baselineY = line.y + RenderUtils.getCustomFontAscent(segment.size, segment.bold);
            RenderUtils.drawCustomString(pose, text, drawX, baselineY, color, segment.bold, segment.size, shadow, shadowColor, shadowOffsetX, shadowOffsetY);
            drawX += RenderUtils.getCustomStringWidth(text, segment.bold, segment.size);
        }
    }

    private static void renderMetricList(PoseStack pose, HudMetricList list) {
        if (!list.visible) {
            return;
        }

        float currentY = list.y;
        for (HudMetricItem item : list.items) {
            if (!isMetricVisible(item, list)) {
                continue;
            }

            String label = item.label + " ";
            RenderUtils.drawCustomString(
                    pose,
                    label,
                    list.x,
                    currentY + 2,
                    list.labelColor,
                    list.labelBold,
                    list.labelSize,
                    list.labelShadow,
                    resolveColorToken(list.labelShadowColorToken, PacketHelper.getServerLagTime()),
                    list.labelShadowOffsetX,
                    list.labelShadowOffsetY
            );
            float labelWidth = RenderUtils.getCustomStringWidth(label, list.labelBold, list.labelSize);
            RenderUtils.drawCustomString(pose, list.separator, list.x + labelWidth, currentY + 2, list.separatorColor, false, list.labelSize, false, 0, 0, 0);

            String value = resolveMetricValue(item.source);
            float valueWidth = RenderUtils.getCustomStringWidth(value, list.valueBold, list.valueSize);
            float valueX = "left".equalsIgnoreCase(list.valueAlign)
                    ? list.x + labelWidth + RenderUtils.getCustomStringWidth(list.separator, false, list.labelSize)
                    : list.x + list.width - valueWidth;
            int valueColor = resolveColorToken(item.colorToken, PacketHelper.getServerLagTime());
            RenderUtils.drawCustomString(
                    pose,
                    value,
                    valueX,
                    currentY + 2,
                    valueColor,
                    list.valueBold,
                    list.valueSize,
                    item.shadow,
                    resolveColorToken(item.shadowColorToken, PacketHelper.getServerLagTime()),
                    item.shadowOffsetX,
                    item.shadowOffsetY
            );

            currentY += list.rowHeight;
        }
    }

    private static void renderImage(GuiGraphics graphics, HudImage image) {
        if (!image.visible || image.path.isBlank()) {
            return;
        }

        ResourceLocation resource = resolveImageFrame(image.path);
        if (resource == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(resource, image.x, image.y, 0, 0, image.width, image.height, image.width, image.height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation resolveImageFrame(String pathToken) {
        if (pathToken.trim().toLowerCase(Locale.ROOT).endsWith(".gif")) {
            AnimatedImage animatedImage = resolveGif(pathToken);
            return animatedImage == null ? null : animatedImage.getCurrentFrame();
        }
        return resolveImage(pathToken);
    }

    private static ResourceLocation resolveImage(String pathToken) {
        return IMAGE_CACHE.computeIfAbsent(pathToken, token -> {
            try {
                String normalized = token.replace('\\', '/');

                if (normalized.startsWith("resource:")) {
                    String resourcePath = normalized.substring("resource:".length());
                    return registerImage(token, HudStyleManager.class.getResourceAsStream(resourcePath));
                }

                Path filePath = Path.of(normalized);
                if (!filePath.isAbsolute()) {
                    filePath = mc.gameDirectory.toPath().resolve(normalized);
                }

                if (Files.exists(filePath)) {
                    return registerImage(token, Files.newInputStream(filePath));
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    private static AnimatedImage resolveGif(String pathToken) {
        return GIF_CACHE.computeIfAbsent(pathToken, token -> {
            try {
                String normalized = token.replace('\\', '/');

                if (normalized.startsWith("resource:")) {
                    String resourcePath = normalized.substring("resource:".length());
                    return registerGif(token, HudStyleManager.class.getResourceAsStream(resourcePath));
                }

                Path filePath = Path.of(normalized);
                if (!filePath.isAbsolute()) {
                    filePath = mc.gameDirectory.toPath().resolve(normalized);
                }

                if (Files.exists(filePath)) {
                    return registerGif(token, Files.newInputStream(filePath));
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    private static ResourceLocation registerImage(String key, InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try (InputStream in = inputStream) {
            NativeImage image = NativeImage.read(in);
            String safeKey = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            ResourceLocation location = new ResourceLocation("redsignal", "hud/" + safeKey);
            mc.getTextureManager().register(location, new DynamicTexture(image));
            return location;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static AnimatedImage registerGif(String key, InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try (InputStream in = inputStream; ImageInputStream imageInput = ImageIO.createImageInputStream(in)) {
            if (imageInput == null || !ImageIO.getImageReadersByFormatName("gif").hasNext()) {
                return null;
            }

            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(imageInput, false);

            int frameCount = reader.getNumImages(true);
            if (frameCount <= 0) {
                reader.dispose();
                return null;
            }

            List<ResourceLocation> frames = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            String safeKey = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            int[] logicalSize = readLogicalScreenSize(reader.getStreamMetadata(), reader.getWidth(0), reader.getHeight(0));
            BufferedImage canvas = new BufferedImage(logicalSize[0], logicalSize[1], BufferedImage.TYPE_INT_ARGB);
            GifFrameMetadata previousFrame = null;
            BufferedImage restorePreviousBackup = null;

            for (int i = 0; i < frameCount; i++) {
                if (previousFrame != null) {
                    applyDisposal(canvas, previousFrame, restorePreviousBackup);
                }

                IIOMetadata metadata = reader.getImageMetadata(i);
                GifFrameMetadata frameMeta = readGifMetadata(metadata, canvas.getWidth(), canvas.getHeight());
                BufferedImage rawFrame = reader.read(i);

                if ("restoreToPrevious".equals(frameMeta.disposalMethod())) {
                    restorePreviousBackup = deepCopy(canvas);
                } else {
                    restorePreviousBackup = null;
                }

                Graphics2D graphics = canvas.createGraphics();
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.drawImage(rawFrame, frameMeta.left(), frameMeta.top(), null);
                graphics.dispose();

                NativeImage nativeImage = bufferedToNative(canvas);
                ResourceLocation location = new ResourceLocation("redsignal", "hud/" + safeKey + "_frame_" + i);
                mc.getTextureManager().register(location, new DynamicTexture(nativeImage));
                frames.add(location);
                delays.add(frameMeta.delayMs());
                previousFrame = frameMeta;
            }

            reader.dispose();
            return new AnimatedImage(frames, delays);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void applyDisposal(BufferedImage canvas, GifFrameMetadata previousFrame, BufferedImage restorePreviousBackup) {
        if ("restoreToBackgroundColor".equals(previousFrame.disposalMethod())) {
            Graphics2D graphics = canvas.createGraphics();
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(previousFrame.left(), previousFrame.top(), previousFrame.width(), previousFrame.height());
            graphics.dispose();
            return;
        }

        if ("restoreToPrevious".equals(previousFrame.disposalMethod()) && restorePreviousBackup != null) {
            Graphics2D graphics = canvas.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(restorePreviousBackup, 0, 0, null);
            graphics.dispose();
        }
    }

    private static int[] readLogicalScreenSize(IIOMetadata metadata, int fallbackWidth, int fallbackHeight) {
        try {
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            Node child = root.getFirstChild();
            while (child != null) {
                if ("LogicalScreenDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    int width = Integer.parseInt(attrs.getNamedItem("logicalScreenWidth").getNodeValue());
                    int height = Integer.parseInt(attrs.getNamedItem("logicalScreenHeight").getNodeValue());
                    return new int[]{Math.max(1, width), Math.max(1, height)};
                }
                child = child.getNextSibling();
            }
        } catch (Exception ignored) {
        }
        return new int[]{Math.max(1, fallbackWidth), Math.max(1, fallbackHeight)};
    }

    private static NativeImage bufferedToNative(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), true);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                int abgr = FastColor.ABGR32.color(
                        FastColor.ARGB32.alpha(argb),
                        FastColor.ARGB32.blue(argb),
                        FastColor.ARGB32.green(argb),
                        FastColor.ARGB32.red(argb)
                );
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }

    private static GifFrameMetadata readGifMetadata(IIOMetadata metadata, int fallbackWidth, int fallbackHeight) {
        int left = 0;
        int top = 0;
        int width = fallbackWidth;
        int height = fallbackHeight;
        int delayMs = 100;
        String disposalMethod = "none";

        try {
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            Node child = root.getFirstChild();
            while (child != null) {
                if ("ImageDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    left = parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue(), 0);
                    top = parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue(), 0);
                    width = parseInt(attrs.getNamedItem("imageWidth").getNodeValue(), fallbackWidth);
                    height = parseInt(attrs.getNamedItem("imageHeight").getNodeValue(), fallbackHeight);
                } else if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    disposalMethod = attrs.getNamedItem("disposalMethod").getNodeValue();
                    int hundredths = parseInt(attrs.getNamedItem("delayTime").getNodeValue(), 10);
                    delayMs = Math.max(20, hundredths * 10);
                }
                child = child.getNextSibling();
            }
        } catch (Exception ignored) {
        }

        return new GifFrameMetadata(left, top, width, height, delayMs, disposalMethod);
    }

    private static BufferedImage deepCopy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static boolean isMetricVisible(HudMetricItem item, HudMetricList list) {
        if (!item.visible) {
            return false;
        }
        if (!list.respectHudOptions) {
            return true;
        }
        Boolean option = HudOptions.getMap().get(item.label);
        return option == null || option;
    }

    private static String resolveTemplate(String text) {
        return text
                .replace("${version}", RedSignalClient.VERSION)
                .replace("${ip}", serverUtil.getServerIp())
                .replace("${engine}", serverUtil.getBrand())
                .replace("${last_packet}", PacketHelper.lagTimeForSec() + " sec")
                .replace("${xyz}", serverUtil.getPlayerCoordinates())
                .replace("${fps}", mc.getFps() + " fps")
                .replace("${pps}", PacketHelper.packetSent + "s / " + PacketHelper.packetReceived + "r");
    }

    private static String resolveMetricValue(String source) {
        return switch (source.trim().toUpperCase(Locale.ROOT)) {
            case "IP" -> serverUtil.getServerIp();
            case "ENGINE" -> serverUtil.getBrand();
            case "LASTPACKET", "LAST_PACKET", "LAST PACKET" -> PacketHelper.lagTimeForSec() + " sec";
            case "XYZ" -> serverUtil.getPlayerCoordinates();
            case "FPS" -> mc.getFps() + " fps";
            case "PPS" -> PacketHelper.packetSent + "s / " + PacketHelper.packetReceived + "r";
            default -> resolveTemplate(unquote(source));
        };
    }

    private static int resolveColorToken(String token, long lagTime) {
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if ("theme".equals(normalized)) {
            return ClickGui.getCurrentThemeColor();
        }
        if ("packet-status".equals(normalized)) {
            return parsePacketColor(lagTime);
        }
        return parseColor(token, Color.WHITE.getRGB());
    }

    private static int parsePacketColor(long lagTime) {
        if (lagTime > 25000) return new Color(220, 38, 38, 255).getRGB();
        if (lagTime > 15000) return new Color(234, 88, 12, 255).getRGB();
        if (lagTime > 10000) return new Color(217, 119, 6, 255).getRGB();
        if (lagTime > 5000) return new Color(202, 138, 4, 255).getRGB();
        if (lagTime > 100) return new Color(22, 163, 74, 255).getRGB();
        return ClickGui.getCurrentThemeColor();
    }

    private static int resolveRootX(HudRoot root) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        String anchor = root.screenAnchor.toLowerCase(Locale.ROOT);

        if ("hud-place".equals(anchor)) {
            return ClickGui.HUD_PLACE == ClickGui.HudPlace.RIGHT
                    ? Math.max(root.offsetX, screenW - root.width - root.offsetX)
                    : root.offsetX;
        }

        return switch (anchor) {
            case "right-top" -> Math.max(root.offsetX, screenW - root.width - root.offsetX);
            case "center-top" -> Math.max(0, (screenW - root.width) / 2 + root.offsetX);
            default -> root.offsetX;
        };
    }

    private static float getEffectiveScale(HudRoot root) {
        if (!root.autoScale) {
            return root.scale;
        }

        try {
            int guiScale = (int) mc.getWindow().getGuiScale();
            if (guiScale <= 0) {
                guiScale = 1;
            }
            return root.scale * ((float) root.referenceGuiScale / (float) guiScale);
        } catch (Exception e) {
            return root.scale;
        }
    }

    private static String stripComment(String line) {
        int slash = line.indexOf("//");
        if (slash >= 0) {
            line = line.substring(0, slash);
        }
        return line;
    }

    private static int parseColor(String rawValue, int fallback) {
        String value = rawValue.trim();
        try {
            if (value.startsWith("rgba(") && value.endsWith(")")) {
                String[] parts = value.substring(5, value.length() - 1).split(",");
                if (parts.length == 4) {
                    int r = parseInt(parts[0].trim(), 0);
                    int g = parseInt(parts[1].trim(), 0);
                    int b = parseInt(parts[2].trim(), 0);
                    int a = parseInt(parts[3].trim(), 255);
                    return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
                }
            }
            if (value.startsWith("#")) {
                String hex = value.substring(1);
                long parsed = Long.parseLong(hex, 16);
                if (hex.length() == 6) {
                    parsed |= 0xFF000000L;
                }
                return (int) parsed;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String[] splitPipe(String value, int limit) {
        return value.split("\\|", limit);
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class BlockData {
        private final String name;
        private final Map<String, List<String>> properties = new LinkedHashMap<>();

        private BlockData(String name) {
            this.name = name;
        }

        private String get(String key, String fallback) {
            List<String> values = properties.get(key);
            return values == null || values.isEmpty() ? fallback : values.get(values.size() - 1);
        }

        private List<String> getAll(String key) {
            return properties.getOrDefault(key, List.of());
        }

        private int getInt(String key, int fallback) {
            return parseInt(get(key, String.valueOf(fallback)), fallback);
        }

        private float getFloat(String key, float fallback) {
            return parseFloat(get(key, String.valueOf(fallback)), fallback);
        }

        private boolean getBoolean(String key, boolean fallback) {
            return Boolean.parseBoolean(get(key, String.valueOf(fallback)));
        }
    }

    private record GifFrameMetadata(int left, int top, int width, int height, int delayMs, String disposalMethod) {
    }
}
