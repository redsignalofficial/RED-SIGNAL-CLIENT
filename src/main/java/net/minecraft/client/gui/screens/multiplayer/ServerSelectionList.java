package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.common.RenderUtils;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
   private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
   static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/server_selection.png");
   static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
   static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
   static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withStyle((p_264689_) -> {
      return p_264689_.withColor(-65536);
   });
   static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle((p_264688_) -> {
      return p_264688_.withColor(-65536);
   });
   static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
   static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
   static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
   static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
   private final JoinMultiplayerScreen screen;
   private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
   private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
   private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();
   @Nullable
   private Predicate<ServerData> serverFilter;

   public ServerSelectionList(JoinMultiplayerScreen p_99771_, Minecraft p_99772_, int p_99773_, int p_99774_, int p_99775_, int p_99776_, int p_99777_) {
      super(p_99772_, p_99773_, p_99774_, p_99775_, p_99776_, p_99777_);
      this.screen = p_99771_;
   }

   private void refreshEntries() {
      this.clearEntries();
      this.onlineServers.forEach((p_169979_) -> {
         this.addEntry(p_169979_);
      });
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) {
         this.addEntry(this.lanHeader);
         this.networkServers.forEach((p_169976_) -> {
            this.addEntry(p_169976_);
         });
      }
   }

   public void setSelected(@Nullable ServerSelectionList.Entry p_99790_) {
      super.setSelected(p_99790_);
      this.screen.onSelectedChange();
   }

   public boolean keyPressed(int p_99782_, int p_99783_, int p_99784_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.getSelected();
      return serverselectionlist$entry != null && serverselectionlist$entry.keyPressed(p_99782_, p_99783_, p_99784_) || super.keyPressed(p_99782_, p_99783_, p_99784_);
   }

   public void updateOnlineServers(ServerList p_99798_) {
      this.onlineServers.clear();

      for(int i = 0; i < p_99798_.size(); ++i) {
         ServerData serverData = p_99798_.get(i);
         if (this.serverFilter != null && !this.serverFilter.test(serverData)) {
            continue;
         }
         this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, serverData));
      }

      this.refreshEntries();
   }

   public void setServerFilter(@Nullable Predicate<ServerData> filter) {
      this.serverFilter = filter;
      this.updateOnlineServers(this.screen.getServers());
   }

   public void updateNetworkServers(List<LanServer> p_99800_) {
      int i = p_99800_.size() - this.networkServers.size();
      this.networkServers.clear();

      for(LanServer lanserver : p_99800_) {
         this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, lanserver));
      }

      this.refreshEntries();

      for(int i1 = this.networkServers.size() - i; i1 < this.networkServers.size(); ++i1) {
         ServerSelectionList.NetworkServerEntry serverselectionlist$networkserverentry = this.networkServers.get(i1);
         int j = i1 - this.networkServers.size() + this.children().size();
         int k = this.getRowTop(j);
         int l = this.getRowBottom(j);
         if (l >= this.y0 && k <= this.y1) {
            this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", serverselectionlist$networkserverentry.getServerNarration()));
         }
      }
   }

   protected int getScrollbarPosition() {
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL) {
         return this.x1 - 8;
      }

      return super.getScrollbarPosition() + 30;
   }

   public int getRowWidth() {
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL) {
         return Math.max(0, this.width - 22);
      }

      return super.getRowWidth() + 85;
   }

   public void removed() {
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements AutoCloseable {
      public void close() {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LANHeader extends ServerSelectionList.Entry {
      private final Minecraft minecraft = Minecraft.getInstance();

      public void render(GuiGraphics p_281475_, int p_282477_, int p_282819_, int p_282001_, int p_281911_, int p_283126_, int p_282303_, int p_281998_, boolean p_282625_, float p_281811_) {
         int i = p_282819_ + p_283126_ / 2 - 9 / 2;
         p_281475_.drawString(this.minecraft.font, ServerSelectionList.SCANNING_LABEL, this.minecraft.screen.width / 2 - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2, i, 16777215, false);
         String s = LoadingDotsText.get(Util.getMillis());
         p_281475_.drawString(this.minecraft.font, s, this.minecraft.screen.width / 2 - this.minecraft.font.width(s) / 2, i + 9, 8421504, false);
      }

      public Component getNarration() {
         return ServerSelectionList.SCANNING_LABEL;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NetworkServerEntry extends ServerSelectionList.Entry {
      private static final int BG_NORMAL = new Color(0, 0, 0, 120).getRGB();
      private static final int BG_SELECTED = new Color(0, 0, 0, 160).getRGB();
      private static final int OUTLINE_NORMAL = new Color(71, 85, 105, 170).getRGB();
      private static final int OUTLINE_HOVER = new Color(148, 163, 184, 170).getRGB();

      private static final int ICON_WIDTH = 32;
      private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
      private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
      private final JoinMultiplayerScreen screen;
      protected final Minecraft minecraft;
      protected final LanServer serverData;
      private long lastClickTime;

      protected NetworkServerEntry(JoinMultiplayerScreen p_99836_, LanServer p_99837_) {
         this.screen = p_99836_;
         this.serverData = p_99837_;
         this.minecraft = Minecraft.getInstance();
      }

      public void render(GuiGraphics p_282600_, int p_282649_, int p_283641_, int p_282277_, int p_283034_, int p_281533_, int p_282746_, int p_281454_, boolean p_283673_, float p_282694_) {
         if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL) {
            boolean selected = this.screen.serverSelectionList.getSelected() == this;
            int accent = ClickGui.getCurrentThemeColor();
            int bg = selected ? BG_SELECTED : BG_NORMAL;
            int outline = selected ? accent : (p_283673_ ? OUTLINE_HOVER : OUTLINE_NORMAL);
            int rw = p_283034_ - 4;
            p_282600_.fillRounded(p_282277_, p_283641_, rw, p_281533_, 14, bg);
            RenderUtils.drawRoundedRectOutline(p_282600_.pose(), p_282277_, p_283641_, rw, p_281533_, 14, 2, outline);
         }
         p_282600_.drawString(this.minecraft.font, LAN_SERVER_HEADER, p_282277_ + 32 + 3, p_283641_ + 1, 16777215, false);
         p_282600_.drawString(this.minecraft.font, this.serverData.getMotd(), p_282277_ + 32 + 3, p_283641_ + 12, 8421504, false);
         if (this.minecraft.options.hideServerAddress) {
            p_282600_.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064, false);
         } else {
            p_282600_.drawString(this.minecraft.font, this.serverData.getAddress(), p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064, false);
         }
      }

      public boolean mouseClicked(double p_99840_, double p_99841_, int p_99842_) {
         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return false;
      }

      public LanServer getServerData() {
         return this.serverData;
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", this.getServerNarration());
      }

      public Component getServerNarration() {
         return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class OnlineServerEntry extends ServerSelectionList.Entry {
      private static final int BG_NORMAL = new Color(0, 0, 0, 120).getRGB();
      private static final int BG_SELECTED = new Color(0, 0, 0, 160).getRGB();
      private static final int OUTLINE_NORMAL = new Color(71, 85, 105, 170).getRGB();
      private static final int OUTLINE_HOVER = new Color(148, 163, 184, 170).getRGB();

      private final JoinMultiplayerScreen screen;
      private final Minecraft minecraft;
      private final ServerData serverData;
      private final FaviconTexture icon;
      @Nullable
      private byte[] lastIconBytes;
      private long lastClickTime;

      protected OnlineServerEntry(JoinMultiplayerScreen p_99864_, ServerData p_99865_) {
         this.screen = p_99864_;
         this.serverData = p_99865_;
         this.minecraft = Minecraft.getInstance();
         this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), p_99865_.ip);
      }

      public void render(GuiGraphics p_281406_, int p_281506_, int p_282921_, int p_281363_, int p_283596_, int p_281630_, int p_283567_, int p_282864_, boolean p_282999_, float p_281423_) {
         final boolean customStyle = ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL;
         if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL) {
            boolean selected = this.screen.serverSelectionList.getSelected() == this;
            int accent = ClickGui.getCurrentThemeColor();
            int bg = selected ? BG_SELECTED : BG_NORMAL;
            int outline = selected ? accent : (p_282999_ ? OUTLINE_HOVER : OUTLINE_NORMAL);
            int rw = p_283596_ - 4;
            p_281406_.fillRounded(p_281363_, p_282921_, rw, p_281630_, 14, bg);
            RenderUtils.drawRoundedRectOutline(p_281406_.pose(), p_281363_, p_282921_, rw, p_281630_, 14, 2, outline);
         }
         if (!this.serverData.pinged) {
            this.serverData.pinged = true;
            this.serverData.ping = -2L;
            this.serverData.motd = CommonComponents.EMPTY;
            this.serverData.status = CommonComponents.EMPTY;
            ServerSelectionList.THREAD_POOL.submit(() -> {
               try {
                  this.screen.getPinger().pingServer(this.serverData, () -> {
                     this.minecraft.execute(this::updateServerList);
                  });
               } catch (UnknownHostException unknownhostexception) {
                  this.serverData.ping = -1L;
                  this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
               } catch (Exception exception) {
                  this.serverData.ping = -1L;
                  this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
               }

            });
         }

         boolean flag = !this.isCompatible();
         int contentPadX = customStyle ? 10 : 0;
         int contentPadY = customStyle ? 6 : 0;
         int iconX = p_281363_ + contentPadX;
         int iconY = p_282921_ + contentPadY;
         int textX = iconX + 32 + 8;
         int titleY = iconY + 2;
         int motdY = iconY + 14;
         int rightPad = customStyle ? 8 : 0;
         int availableW = p_283596_ - contentPadX - rightPad;

         p_281406_.drawString(this.minecraft.font, this.serverData.name, textX, titleY, 16777215, false);
         List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, availableW - (textX - p_281363_) - 2);

         for(int i = 0; i < Math.min(list.size(), 2); ++i) {
            p_281406_.drawString(this.minecraft.font, list.get(i), textX, motdY + 9 * i, 8421504, false);
         }

         Component component1 = (Component)(flag ? this.serverData.version.copy().withStyle(ChatFormatting.RED) : this.serverData.status);
         int j = this.minecraft.font.width(component1);
         p_281406_.drawString(this.minecraft.font, component1, p_281363_ + p_283596_ - j - 15 - 2 - rightPad, titleY, 8421504, false);
         int k = 0;
         int l;
         List<Component> list1;
         Component component;
         if (flag) {
            l = 5;
            component = ServerSelectionList.INCOMPATIBLE_STATUS;
            list1 = this.serverData.playerList;
         } else if (this.pingCompleted()) {
            if (this.serverData.ping < 0L) {
               l = 5;
            } else if (this.serverData.ping < 150L) {
               l = 0;
            } else if (this.serverData.ping < 300L) {
               l = 1;
            } else if (this.serverData.ping < 600L) {
               l = 2;
            } else if (this.serverData.ping < 1000L) {
               l = 3;
            } else {
               l = 4;
            }

            if (this.serverData.ping < 0L) {
               component = ServerSelectionList.NO_CONNECTION_STATUS;
               list1 = Collections.emptyList();
            } else {
               component = Component.translatable("multiplayer.status.ping", this.serverData.ping);
               list1 = this.serverData.playerList;
            }
         } else {
            k = 1;
            l = (int)(Util.getMillis() / 100L + (long)(p_281506_ * 2) & 7L);
            if (l > 4) {
               l = 8 - l;
            }

            component = ServerSelectionList.PINGING_STATUS;
            list1 = Collections.emptyList();
         }

         p_281406_.blit(ServerSelectionList.GUI_ICONS_LOCATION, p_281363_ + p_283596_ - 15 - rightPad, iconY, (float)(k * 10), (float)(176 + l * 8), 10, 8, 256, 256);
         byte[] abyte = this.serverData.getIconBytes();
         if (!Arrays.equals(abyte, this.lastIconBytes)) {
            if (this.uploadServerIcon(abyte)) {
               this.lastIconBytes = abyte;
            } else {
               this.serverData.setIconBytes((byte[])null);
               this.updateServerList();
            }
         }

         this.drawIcon(p_281406_, iconX, iconY, this.icon.textureLocation());
         int i1 = p_283567_ - p_281363_;
         int j1 = p_282864_ - p_282921_;
         if (i1 >= p_283596_ - 15 - rightPad && i1 <= p_283596_ - 5 - rightPad && j1 >= contentPadY && j1 <= contentPadY + 8) {
            this.screen.setToolTip(Collections.singletonList(component));
         } else if (i1 >= p_283596_ - j - 15 - 2 - rightPad && i1 <= p_283596_ - 15 - 2 - rightPad && j1 >= contentPadY && j1 <= contentPadY + 8) {
            this.screen.setToolTip(list1);
         }

         if ((ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) && (this.minecraft.options.touchscreen().get() || p_282999_)) {
            p_281406_.fill(p_281363_, p_282921_, p_281363_ + 32, p_282921_ + 32, -1601138544);
            int k1 = p_283567_ - p_281363_;
            int l1 = p_282864_ - p_282921_;
            if (this.canJoin()) {
               if (k1 < 32 && k1 > 16) {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 0.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 0.0F, 0.0F, 32, 32, 256, 256);
               }
            }

            if (p_281506_ > 0) {
               if (k1 < 16 && l1 < 16) {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 96.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 96.0F, 0.0F, 32, 32, 256, 256);
               }
            }

            if (p_281506_ < this.screen.getServers().size() - 1) {
               if (k1 < 16 && l1 > 16) {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 64.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  p_281406_.blit(ServerSelectionList.ICON_OVERLAY_LOCATION, p_281363_, p_282921_, 64.0F, 0.0F, 32, 32, 256, 256);
               }
            }
         }

      }

      private boolean pingCompleted() {
         return this.serverData.pinged && this.serverData.ping != -2L;
      }

      private boolean isCompatible() {
         return this.serverData.protocol == SharedConstants.getCurrentVersion().getProtocolVersion();
      }

      public void updateServerList() {
         this.screen.getServers().save();
      }

      protected void drawIcon(GuiGraphics p_281338_, int p_283001_, int p_282834_, ResourceLocation p_282534_) {
         RenderSystem.enableBlend();
         p_281338_.blit(p_282534_, p_283001_, p_282834_, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
      }

      private boolean canJoin() {
         return true;
      }

      private static void roundIconCorners(NativeImage img) {
         int w = img.getWidth();
         int h = img.getHeight();
         if (w <= 2 || h <= 2) {
            return;
         }

         int radius = Math.max(3, Math.min(w, h) / 6);
         float rr = (radius - 0.55f) * (radius - 0.55f);

         for (int y = 0; y < radius; y++) {
            for (int x = 0; x < radius; x++) {
               float dx = (radius - 1) - x;
               float dy = (radius - 1) - y;
               if (dx * dx + dy * dy > rr) {
                  clearAlpha(img, x, y);
                  clearAlpha(img, w - 1 - x, y);
                  clearAlpha(img, x, h - 1 - y);
                  clearAlpha(img, w - 1 - x, h - 1 - y);
               }
            }
         }
      }

      private static void clearAlpha(NativeImage img, int x, int y) {
         int c = img.getPixelRGBA(x, y);
         img.setPixelRGBA(x, y, c & 0x00FFFFFF);
      }

      private boolean uploadServerIcon(@Nullable byte[] p_273176_) {
         if (p_273176_ == null) {
            this.icon.clear();
         } else {
            try {
               NativeImage img = NativeImage.read(p_273176_);
               if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED_SIGNAL) {
                  roundIconCorners(img);
               }
               this.icon.upload(img);
            } catch (Throwable throwable) {
               ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", this.serverData.name, this.serverData.ip, throwable);
               return false;
            }
         }

         return true;
      }

      public boolean keyPressed(int p_99875_, int p_99876_, int p_99877_) {
         if (Screen.hasShiftDown()) {
            ServerSelectionList serverselectionlist = this.screen.serverSelectionList;
            int i = serverselectionlist.children().indexOf(this);
            if (i == -1) {
               return true;
            }

            if (p_99875_ == 264 && i < this.screen.getServers().size() - 1 || p_99875_ == 265 && i > 0) {
               this.swap(i, p_99875_ == 264 ? i + 1 : i - 1);
               return true;
            }
         }

         return super.keyPressed(p_99875_, p_99876_, p_99877_);
      }

      private void swap(int p_99872_, int p_99873_) {
         this.screen.getServers().swap(p_99872_, p_99873_);
         this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
         ServerSelectionList.Entry serverselectionlist$entry = this.screen.serverSelectionList.children().get(p_99873_);
         this.screen.serverSelectionList.setSelected(serverselectionlist$entry);
         ServerSelectionList.this.ensureVisible(serverselectionlist$entry);
      }

      public boolean mouseClicked(double p_99868_, double p_99869_, int p_99870_) {
         double d0 = p_99868_ - (double)ServerSelectionList.this.getRowLeft();
         double d1 = p_99869_ - (double)ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
         if (d0 <= 32.0D) {
            if (d0 < 32.0D && d0 > 16.0D && this.canJoin()) {
               this.screen.setSelected(this);
               this.screen.joinSelectedServer();
               return true;
            }

            int i = this.screen.serverSelectionList.children().indexOf(this);
            if (d0 < 16.0D && d1 < 16.0D && i > 0) {
               this.swap(i, i - 1);
               return true;
            }

            if (d0 < 16.0D && d1 > 16.0D && i < this.screen.getServers().size() - 1) {
               this.swap(i, i + 1);
               return true;
            }
         }

         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return true;
      }

      public ServerData getServerData() {
         return this.serverData;
      }

      public Component getNarration() {
         MutableComponent mutablecomponent = Component.empty();
         mutablecomponent.append(Component.translatable("narrator.select", this.serverData.name));
         mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
         if (!this.isCompatible()) {
            mutablecomponent.append(ServerSelectionList.INCOMPATIBLE_STATUS);
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            mutablecomponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
         } else if (this.serverData.ping < 0L) {
            mutablecomponent.append(ServerSelectionList.NO_CONNECTION_STATUS);
         } else if (!this.pingCompleted()) {
            mutablecomponent.append(ServerSelectionList.PINGING_STATUS);
         } else {
            mutablecomponent.append(ServerSelectionList.ONLINE_STATUS);
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            mutablecomponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
            if (this.serverData.players != null) {
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max()));
               mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
               mutablecomponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
            }
         }

         return mutablecomponent;
      }

      public void close() {
         this.icon.close();
      }
   }
}