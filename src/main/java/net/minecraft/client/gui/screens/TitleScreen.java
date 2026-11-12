package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import us.rin.RedSignalClient;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.clickgui.components.WelcomeComponent;
import us.rin.gui.common.CustomUi;
import us.rin.gui.common.CustomButton;
import us.rin.gui.common.RenderUtils;
import us.rin.gui.screen.AltManagerScreen;
import us.rin.proxy.RedSignalProxy;

@OnlyIn(Dist.CLIENT)
public class TitleScreen extends Screen {
   public static final Component COPYRIGHT_TEXT = Component.literal("Copyright Mojang AB. Do not distribute!");
   public static final CubeMap CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
   private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
   @Nullable
   private SplashRenderer splash;
   @Nullable
   private RealmsNotificationsScreen realmsNotificationsScreen;
   private final PanoramaRenderer panorama = new PanoramaRenderer(CUBE_MAP);
   private final boolean fading;
   private long fadeInStart;
   private long redSignalOpenTimeMs;
   private int redSignalPanelX;
   private int redSignalPanelY;
   private int redSignalPanelW;
   private int redSignalPanelH;
   private float lastPanelAlpha = -1.0F;
   private ResourceLocation cachedAvatarSkin;
   private boolean changelogOpen;
   private int changelogScroll;
   private int changelogMaxScroll;
   @Nullable
   private TitleScreen.WarningLabel warningLabel;
   private final LogoRenderer logoRenderer;

   public TitleScreen() {
      this(false);
   }

   private void initRed SignalUI() {
      this.redSignalOpenTimeMs = Util.getMillis();
      this.changelogOpen = false;
      this.changelogScroll = 0;
      this.changelogMaxScroll = 0;

      ResourceLocation initialSkin;
      try {
         initialSkin = this.minecraft.getSkinManager().getInsecureSkinLocation(this.minecraft.getUser().getGameProfile());
      } catch (Throwable t) {
         initialSkin = DefaultPlayerSkin.getDefaultSkin();
      }
      this.cachedAvatarSkin = initialSkin;

      int panelW = (int) Math.round(this.width * 0.42);
      int panelH = (int) Math.round(this.height * 0.52);
      panelW = Mth.clamp(panelW, 320, Math.max(320, this.width - 20));
      panelH = Mth.clamp(panelH, 320, Math.max(320, this.height - 20));

      this.redSignalPanelW = panelW;
      this.redSignalPanelH = panelH;
      this.redSignalPanelX = (this.width - panelW) / 2;
      this.redSignalPanelY = (this.height - panelH) / 2;

      int pad = 22;

      int titleSize = 34;
      boolean titleBold = true;
      int subtitleSize = 11;
      boolean subtitleBold = false;

      int headerH = RenderUtils.getCustomFontHeight(titleSize, titleBold) + RenderUtils.getCustomFontHeight(subtitleSize, subtitleBold) + 18;

      int btnW = (int) Math.round(panelW * 0.80);
      int btnH = 40;
      int btnGap = 14;
      int btnX = this.redSignalPanelX + (panelW - btnW) / 2;
      int firstBtnY = this.redSignalPanelY + pad + headerH;

      Component component = this.getMultiplayerDisabledReason();
      boolean allowMulti = component == null;
      Tooltip tooltip = component != null ? Tooltip.create(component) : null;

      CustomButton single = new CustomButton(btnX, firstBtnY, btnW, btnH, Component.translatable("menu.singleplayer"), b -> {
         this.minecraft.setScreen(new SelectWorldScreen(this));
      });
      single.setRadius(14)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 64)
              .setText(0xFFE6EAF2, false, 16)
              .setIcon("👤", CustomButton.IconSide.LEFT)
              .setIconStyle(0xFFE6EAF2, 14)
              .setIconScale(1f);
      this.addRenderableWidget(single);

      CustomButton multi = new CustomButton(btnX, firstBtnY + (btnH + btnGap), btnW, btnH, Component.translatable("menu.multiplayer"), b -> {
         Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
         this.minecraft.setScreen(screen);
      });
      multi.setRadius(14)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 64)
              .setText(0xFFE6EAF2, false, 16)
              .setIcon("👥", CustomButton.IconSide.LEFT)
              .setIconStyle(0xFFE6EAF2, 14)
              .setIconScale(1f);
      multi.active = allowMulti;
      multi.setTooltip(tooltip);
      this.addRenderableWidget(multi);

      CustomButton realms = new CustomButton(btnX, firstBtnY + (btnH + btnGap) * 2, btnW, btnH, Component.translatable("menu.online"), b -> {
         this.realmsButtonClicked();
      });
      realms.setRadius(14)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 64)
              .setText(0xFFE6EAF2, false, 16)
              .setIcon("👑", CustomButton.IconSide.LEFT)
              .setIconStyle(0xFFE6EAF2, 14)
              .setIconScale(1f);
      realms.active = allowMulti;
      realms.setTooltip(tooltip);
      this.addRenderableWidget(realms);

      int smallH = 28;
      int smallW = (int) Math.round(panelW * 0.35);
      int smallGap = 12;
      int smallY = this.redSignalPanelY + panelH - pad - smallH;
      int smallRowW = smallW * 2 + smallGap;
      int smallStartX = this.redSignalPanelX + (panelW - smallRowW) / 2;

      CustomButton options = new CustomButton(smallStartX, smallY, smallW, smallH, Component.translatable("menu.options"), b -> {
         this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
      });
      options.setRadius(10)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 42)
              .setText(0xFFE6EAF2, false, 13);
      this.addRenderableWidget(options);

      CustomButton quit = new CustomButton(smallStartX + smallW + smallGap, smallY, smallW, smallH, Component.translatable("menu.quit"), b -> {
         this.minecraft.stop();
      });
      quit.setRadius(10)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 42)
              .setText(0xFFE6EAF2, false, 13);
      this.addRenderableWidget(quit);

      int avatarSize = 40;
      int avatarX = this.redSignalPanelX + panelW - pad - avatarSize;
      int avatarY = this.redSignalPanelY + pad;

      CustomButton avatar = new CustomButton(avatarX, avatarY, avatarSize, avatarSize, Component.literal("Alt Manager"), b -> {
         this.minecraft.setScreen(new AltManagerScreen());
      });

      avatar.setRadius(10)
              .setDrawLabel(false)
              .setDrawBackground(false)
              .setDrawBorder(false)
              .setRenderer((button, g, mouseX, mouseY, pt, alphaMul) -> {
                 int ax = button.getX();
                 int ay = button.getY();
                 int bg = withAlpha(0xFF101319, (int) (255 * alphaMul));
                 g.fillRounded(ax, ay, avatarSize, avatarSize, 10, bg);

                 if (button.isHovered()) {
                    int glow = withAlpha(0xFF6C7CFF, (int) (54 * alphaMul));
                    RenderUtils.drawRoundedRect(g.pose(), ax - 2, ay - 2, avatarSize + 4, avatarSize + 4, 12, glow);
                 }

                 ResourceLocation skin = TitleScreen.this.cachedAvatarSkin;

                 int inset = 6;
                 int headSize = avatarSize - inset * 2;
                 PlayerFaceRenderer.draw(g, skin, ax + inset, ay + inset, headSize);

                 int border = withAlpha(0xFFFFFFFF, (int) (13 * alphaMul));
                 RenderUtils.drawRoundedRectOutline(g.pose(), ax, ay, avatarSize, avatarSize, 10, 1, border);
              });
      this.addRenderableWidget(avatar);

      int cornerSize = 30;
      int cornerPad = 12;
      int cornerX = this.width - cornerPad - cornerSize;
       CustomButton changeLogButton = new CustomButton(cornerX, cornerPad, cornerSize, cornerSize, Component.empty(), b -> {
         this.changelogOpen = true;
         this.changelogScroll = 0;
      });
      changeLogButton.setRadius(10)
              .setBackground(0xFF101319, 0xFF101319)
              .setBorderColor(0x0AFFFFFF)
              .setGlow(0xFF6C7CFF, 64)
              .setText(0xFFE6EAF2, false, 16)
              .setIcon("📝", CustomButton.IconSide.CENTER)
              .setIconStyle(0xFFE6EAF2, 14)
              .setIconScale(1f);
      this.addRenderableWidget(changeLogButton);
   }

   public TitleScreen(boolean p_96733_) {
      this(p_96733_, null);
   }

   public TitleScreen(boolean p_265779_, @Nullable LogoRenderer p_265067_) {
      super(Component.translatable("narrator.screen.title"));
      this.fading = p_265779_;
      this.logoRenderer = Objects.requireNonNullElseGet(p_265067_, () -> new LogoRenderer(false));
   }

   private boolean realmsNotificationsEnabled() {
      return this.realmsNotificationsScreen != null;
   }

   public void tick() {
      if (this.realmsNotificationsEnabled()) {
         this.realmsNotificationsScreen.tick();
      }

      this.minecraft.getRealms32BitWarningStatus().showRealms32BitWarningIfNeeded(this);
   }

   public static CompletableFuture<Void> preloadResources(TextureManager p_96755_, Executor p_96756_) {
      return CompletableFuture.allOf(p_96755_.preload(LogoRenderer.MINECRAFT_LOGO, p_96756_), p_96755_.preload(LogoRenderer.MINECRAFT_EDITION, p_96756_), p_96755_.preload(PANORAMA_OVERLAY, p_96756_), CUBE_MAP.preload(p_96755_, p_96756_));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void init() {
      this.clearWidgets();
      this.initRed SignalUI();
      this.minecraft.setConnectedToRealms(false);
      if (this.realmsNotificationsScreen == null) {
         this.realmsNotificationsScreen = new RealmsNotificationsScreen();
      }

      this.realmsNotificationsScreen.init(this.minecraft, this.width, this.height);
   }

   @Nullable
   private Component getMultiplayerDisabledReason() {
      if (this.minecraft.allowsMultiplayer()) {
         return null;
      } else {
         BanDetails bandetails = this.minecraft.multiplayerBan();
         if (bandetails != null) {
            return bandetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
         } else {
            return Component.translatable("title.multiplayer.disabled");
         }
      }
   }

   private void realmsButtonClicked() {
      this.minecraft.setScreen(new RealmsMainScreen(this));
   }

   public void render(GuiGraphics p_282860_, int p_281753_, int p_283539_, float p_282628_) {
      this.renderRed SignalUI(p_282860_, p_281753_, p_283539_, p_282628_);
   }

   private void renderRed SignalUI(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.redSignalOpenTimeMs == 0L) {
         this.redSignalOpenTimeMs = Util.getMillis();
      }

      float panelAlpha = Mth.clamp((Util.getMillis() - this.redSignalOpenTimeMs) / 400.0F, 0.0F, 1.0F);

      int accent = ClickGui.getCurrentThemeColor();
      CustomUi.renderBackground(g, this.width, this.height, 0, 0, 0, accent);

      int panelX = this.redSignalPanelX;
      int panelY = this.redSignalPanelY;
      int panelW = this.redSignalPanelW;
      int panelH = this.redSignalPanelH;
      int radius = 20;

      int panelColor = withAlpha(0xFF0F1116, (int) (190 * panelAlpha));
      g.fillRounded(panelX, panelY, panelW, panelH, radius, panelColor);

      int borderColor = withAlpha(0xFFFFFFFF, (int) (13 * panelAlpha));
      RenderUtils.drawRoundedRectOutline(g.pose(), panelX, panelY, panelW, panelH, radius, 1, borderColor);

      int pad = 16;
      String title = "RedSignalClient";
      int titleSize = 34;
      boolean titleBold = true;
      float titleW = RenderUtils.getCustomStringWidth(title, titleBold, 34);
      float titleBaseY = panelY + pad;
      float titleX = this.width / 2f - titleW / 2f;

      drawTitleGradient(g, title, titleX, titleBaseY, titleW, titleBold, titleSize, panelAlpha);

      String subtitle = "Exclusive Minecraft Crash/Exploit Client";
      int subtitleSize = 10;
      float subtitleW = RenderUtils.getCustomStringWidth(subtitle, false, subtitleSize);
      float subtitleBaseY = titleBaseY + RenderUtils.getCustomFontHeight(titleSize, titleBold);
      RenderUtils.drawCustomString(g.pose(), subtitle, this.width / 2f - subtitleW / 2f, subtitleBaseY, withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha)), false, subtitleSize);

      String footer = "RedSignalClient (" + RedSignalClient.VERSION + ")";
      String footer2 = "Created by: Rin";
      RenderUtils.drawCustomString(g.pose(), footer, 12, this.height - 30, withAlpha(0xFF94A3B8, (int) (255 * panelAlpha)), false, 12);
      RenderUtils.drawCustomString(g.pose(), footer2, 12, this.height - 18, withAlpha(0xFF64748B, (int) (255 * panelAlpha)), false, 12);

      if (Math.abs(panelAlpha - this.lastPanelAlpha) > 0.001F) {
         this.lastPanelAlpha = panelAlpha;
         for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget) {
               ((AbstractWidget) guieventlistener).setAlpha(panelAlpha);
            }
         }
      }

      super.render(g, mouseX, mouseY, partialTick);

      if (this.changelogOpen) {
         this.renderChangelogModal(g, mouseX, mouseY, panelAlpha);
      }

      if (this.realmsNotificationsEnabled() && panelAlpha >= 1.0F) {
         RenderSystem.enableDepthTest();
         this.realmsNotificationsScreen.render(g, mouseX, mouseY, partialTick);
      }
   }

   private static void drawTitleGradient(GuiGraphics g, String text, float x, float baselineY, float width, boolean bold, int size, float alphaMul) {
      int left = withAlpha(0xFFE6EAF2, (int) (255 * alphaMul));
      int right = withAlpha(0xFFC7D2FE, (int) (255 * alphaMul));
      int scLeft = (int) Math.floor(x);
      int scRight = (int) Math.ceil(x + width);
      int mid = (scLeft + scRight) / 2;

      g.enableScissor(scLeft, 0, mid, g.guiHeight());
      RenderUtils.drawCustomString(g.pose(), text, x, baselineY, left, bold, size);
      g.disableScissor();

      g.enableScissor(mid, 0, scRight, g.guiHeight());
      RenderUtils.drawCustomString(g.pose(), text, x, baselineY, right, bold, size);
      g.disableScissor();
   }

   private static int withAlpha(int argb, int alpha) {
      alpha = Mth.clamp(alpha, 0, 255);
      return (alpha << 24) | (argb & 0x00FFFFFF);
   }

   private void renderChangelogModal(GuiGraphics g, int mouseX, int mouseY, float panelAlpha) {
      int accent = ClickGui.getCurrentThemeColor();

      int overlay = withAlpha(0xFF000000, (int) (150 * panelAlpha));
      g.fill(0, 0, this.width, this.height, overlay);

      int modalW = (int) Math.round(this.width * 0.58);
      int modalH = (int) Math.round(this.height * 0.62);
      modalW = Mth.clamp(modalW, 360, Math.max(360, this.width - 20));
      modalH = Mth.clamp(modalH, 300, Math.max(300, this.height - 20));
      int modalX = (this.width - modalW) / 2;
      int modalY = (this.height - modalH) / 2;

      int radius = 18;
      int bg = withAlpha(0xFF0F1116, (int) (220 * panelAlpha));
      g.fillRounded(modalX, modalY, modalW, modalH, radius, bg);
      int border = withAlpha(0xFFFFFFFF, (int) (18 * panelAlpha));
      RenderUtils.drawRoundedRectOutline(g.pose(), modalX, modalY, modalW, modalH, radius, 1, border);

      int pad = 16;
      String title = "Changelog";
      int titleSize = 20;
      boolean titleBold = true;
      RenderUtils.drawCustomString(g.pose(), title, modalX + pad, modalY + pad, withAlpha(0xFFE6EAF2, (int) (255 * panelAlpha)), titleBold, titleSize);

      int closeW = 88;
      int closeH = 26;
      int closeX = modalX + modalW - pad - closeW;
      int closeY = modalY + pad - 2;

      boolean closeHover = RenderUtils.isMouseOver(mouseX, mouseY, closeX, closeY, closeW, closeH);
      int closeTop = closeHover ? 0xFF161A22 : 0xFF101319;
      int closeBgTop = withAlpha(closeTop, (int) (255 * panelAlpha));
      int closeBorder = withAlpha(0x0AFFFFFF, (int) (255 * panelAlpha));
      g.fillRounded(closeX, closeY, closeW, closeH, 10, closeBgTop);
      RenderUtils.drawRoundedRectOutline(g.pose(), closeX, closeY, closeW, closeH, 10, 1, closeBorder);
      String closeText = "Close";
      int closeSize = 13;
      float closeTextW = RenderUtils.getCustomStringWidth(closeText, false, closeSize);
      float closeTextX = closeX + closeW / 2f - closeTextW / 2f;
      float closeTextY = closeY + closeH / 4f - 2;
      RenderUtils.drawCustomString(g.pose(), closeText, closeTextX, closeTextY, withAlpha(0xFFE6EAF2, (int) (255 * panelAlpha)), false, closeSize);

      int headerH = 54;
      int contentX = modalX + pad;
      int contentY = modalY + headerH;
      int contentW = modalW - pad * 2;
      int contentH = modalH - headerH - pad;

      int contentBg = withAlpha(0xFF101319, (int) (160 * panelAlpha));
      g.fillRounded(contentX, contentY, contentW, contentH, 14, contentBg);
      int contentBorder = withAlpha(0xFFFFFFFF, (int) (10 * panelAlpha));
      RenderUtils.drawRoundedRectOutline(g.pose(), contentX, contentY, contentW, contentH, 14, 1, contentBorder);

      int textPad = 14;
      int innerX = contentX + textPad;
      int innerY = contentY + textPad / 2;
      int innerW = contentW - textPad * 2;
      int innerH = contentH - textPad * 2;

      String[] client = WelcomeComponent.CLIENT_CHANGELOG;
      String[] proxy = WelcomeComponent.PROXY_CHANGELOG;

      int lineSize = 13;
      int sectionSize = 15;

      int y = innerY - this.changelogScroll - 10;
      int lineH = RenderUtils.getCustomFontHeight(lineSize, false) + 4;

      int total = 0;
      total += RenderUtils.getCustomFontHeight(sectionSize, true) + 10;
      total += client.length * lineH;
      total += 14;
      total += RenderUtils.getCustomFontHeight(sectionSize, true) + 10;
      total += proxy.length * lineH;
      total += 6;

      this.changelogMaxScroll = Math.max(0, total - innerH);
      this.changelogScroll = Mth.clamp(this.changelogScroll, 0, this.changelogMaxScroll);

      g.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH + 10);

      String sec1 = "RedSignalClient " + RedSignalClient.VERSION;
      RenderUtils.drawCustomString(g.pose(), sec1, innerX, y + RenderUtils.getCustomFontAscent(sectionSize, true), withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha)), true, sectionSize);
      y += RenderUtils.getCustomFontHeight(sectionSize, true) + 10;

      for (String s : client) {
         RenderUtils.drawCustomString(g.pose(), "-", innerX, y + RenderUtils.getCustomFontAscent(lineSize, false), withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha)), false, lineSize);
         RenderUtils.drawCustomString(g.pose(), s, innerX + 14, y + RenderUtils.getCustomFontAscent(lineSize, false), withAlpha(0xFFE6EAF2, (int) (255 * panelAlpha)), false, lineSize);
         y += lineH;
      }

      y += 14;

      String sec2 = "RedSignalProxy " + RedSignalProxy.VERSION;
      RenderUtils.drawCustomString(g.pose(), sec2, innerX, y + RenderUtils.getCustomFontAscent(sectionSize, true), withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha)), true, sectionSize);
      y += RenderUtils.getCustomFontHeight(sectionSize, true) + 10;
      for (String s : proxy) {
         RenderUtils.drawCustomString(g.pose(), "-", innerX, y + RenderUtils.getCustomFontAscent(lineSize, false), withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha)), false, lineSize);
         RenderUtils.drawCustomString(g.pose(), s, innerX + 14, y + RenderUtils.getCustomFontAscent(lineSize, false), withAlpha(0xFFE6EAF2, (int) (255 * panelAlpha)), false, lineSize);
         y += lineH;
      }

      g.disableScissor();

      if (this.changelogMaxScroll > 0) {
         float t = this.changelogScroll / (float) this.changelogMaxScroll;
         int barW = 4;
         int barX = contentX + contentW - 10;

          int thumbH = Math.max(24, (int) (innerH * (innerH / (float) (innerH + this.changelogMaxScroll))));
         int thumbY = innerY + (int) ((innerH - thumbH) * t);

         int track = withAlpha(0xFFFFFFFF, (int) (10 * panelAlpha));
         int thumb = withAlpha(0xFF9CA3AF, (int) (255 * panelAlpha));
         RenderUtils.drawRoundedRect(g.pose(), barX, innerY, barW, innerH, 2, track);
         RenderUtils.drawRoundedRect(g.pose(), barX, thumbY, barW, thumbH, 2, thumb);
      }
   }

   public boolean mouseClicked(double p_96735_, double p_96736_, int p_96737_) {
      if (this.changelogOpen) {
         int modalW = (int) Math.round(this.width * 0.58);
         int modalH = (int) Math.round(this.height * 0.62);
         modalW = Mth.clamp(modalW, 360, Math.max(360, this.width - 20));
         modalH = Mth.clamp(modalH, 300, Math.max(300, this.height - 20));
         int modalX = (this.width - modalW) / 2;
         int modalY = (this.height - modalH) / 2;
         int pad = 16;
         int closeW = 88;
         int closeH = 26;
         int closeX = modalX + modalW - pad - closeW;
         int closeY = modalY + pad - 2;

         if (RenderUtils.isMouseOver(p_96735_, p_96736_, closeX, closeY, closeW, closeH)) {
            this.changelogOpen = false;
            return true;
         }

         if (RenderUtils.isMouseOver(p_96735_, p_96736_, modalX, modalY, modalW, modalH)) {
            return true;
         }

         this.changelogOpen = false;
         return true;
      }

      if (super.mouseClicked(p_96735_, p_96736_, p_96737_)) {
         return true;
      } else {
         return this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(p_96735_, p_96736_, p_96737_);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.changelogOpen) {
         int modalW = (int) Math.round(this.width * 0.58);
         int modalH = (int) Math.round(this.height * 0.62);
         modalW = Mth.clamp(modalW, 360, Math.max(360, this.width - 20));
         modalH = Mth.clamp(modalH, 300, Math.max(300, this.height - 20));
         int modalX = (this.width - modalW) / 2;
         int modalY = (this.height - modalH) / 2;
         int pad = 16;
         int headerH = 54;
         int contentX = modalX + pad;
         int contentY = modalY + headerH;
         int contentW = modalW - pad * 2;
         int contentH = modalH - headerH - pad;
         int textPad = 14;
         int innerX = contentX + textPad;
         int innerY = contentY + textPad;
         int innerW = contentW - textPad * 2;
         int innerH = contentH - textPad * 2;

         if (RenderUtils.isMouseOver(mouseX, mouseY, innerX, innerY, innerW, innerH)) {
            int step = 18;
            this.changelogScroll = Mth.clamp(this.changelogScroll - (int) Math.signum(delta) * step, 0, this.changelogMaxScroll);
         }
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, delta);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.changelogOpen && keyCode == 256) {
         this.changelogOpen = false;
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void removed() {
      if (this.realmsNotificationsScreen != null) {
         this.realmsNotificationsScreen.removed();
      }

   }

   public void added() {
      super.added();
      if (this.realmsNotificationsScreen != null) {
         this.realmsNotificationsScreen.added();
      }

   }

   @OnlyIn(Dist.CLIENT)
   record WarningLabel(Font font, MultiLineLabel label, int x, int y) {
      public void render(GuiGraphics p_281783_, int p_281383_) {
         this.label.renderBackgroundCentered(p_281783_, this.x, this.y, 9, 2, 2097152 | Math.min(p_281383_, 1426063360));
         this.label.renderCentered(p_281783_, this.x, this.y, 9, 16777215 | p_281383_);
      }
   }
}