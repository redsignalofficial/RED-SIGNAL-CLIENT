package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.CommonInputs;

import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import us.whitedev.gui.screen.AltManagerScreen;
import us.whitedev.gui.screen.ProtocolSelectorScreen;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.gui.common.CustomButton;
import us.whitedev.gui.common.CustomUi;
import us.whitedev.gui.common.RenderUtils;

import java.awt.*;

import java.util.Locale;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class JoinMultiplayerScreen extends Screen {

   private static final Logger LOGGER = LogUtils.getLogger();

   private static final int PANEL_RGB = new Color(15, 17, 22).getRGB();
   private static final int BORDER_RGB = new Color(255, 255, 255).getRGB();
   private static final int HEADER_RGB = new Color(230, 234, 242).getRGB();
   private static final int SUBTITLE_RGB = new Color(156, 163, 175).getRGB();

   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private final Screen lastScreen;
   protected ServerSelectionList serverSelectionList;
   private ServerList servers;
   private AbstractWidget editButton;
   private AbstractWidget selectButton;
   private AbstractWidget deleteButton;
   @Nullable
   private List<Component> toolTip;
   private ServerData editingServer;
   private LanServerDetection.LanServerList lanServerList;
   @Nullable
   private LanServerDetection.LanServerDetector lanServerDetector;
   private boolean initedOnce;
   private EditBox searchBox;

   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int leftW;
   private int rightW;
   private long xynisOpenTimeMs;
   private float lastPanelAlpha = -1.0F;

   private float cachedHeaderW = -1;
   private float cachedSubtitleW = -1;
   private String lastHeader;
   private String lastSubtitle;

   public JoinMultiplayerScreen(Screen p_99688_) {
      super(Component.translatable("multiplayer.title"));
      this.lastScreen = p_99688_;
   }

   protected void init() {
      if (!this.initedOnce) {
         this.initedOnce = true;
         this.servers = new ServerList(this.minecraft);
         this.servers.load();
         this.lanServerList = new LanServerDetection.LanServerList();
      }

      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) {
         if (this.lanServerDetector == null) {
            try {
               this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
               this.lanServerDetector.start();
            } catch (Exception exception) {
               LOGGER.warn("Unable to start LAN server detection: {}", (Object)exception.getMessage());
            }
         }
      } else {
         this.lanServerDetector = null;
      }

      int itemH = ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.XYNIS ? 46 : 36;
      this.serverSelectionList = new ServerSelectionList(this, this.minecraft, this.width, this.height, 32, this.height - 64, itemH);
      this.serverSelectionList.updateOnlineServers(this.servers);
      this.addWidget(this.serverSelectionList);

      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.XYNIS) {
         this.xynisOpenTimeMs = Util.getMillis();
         this.panelW = Math.min(980, this.width - 60);
         this.panelH = this.height - 100;
         this.panelX = (this.width - this.panelW) / 2;
         this.panelY = 40;

         int gap = 16;
         this.leftW = (int) Math.round(this.panelW * 0.64);
         this.rightW = this.panelW - this.leftW - gap;

         int pad = 18;

         int leftInnerX = this.panelX + pad;
         int leftInnerY = this.panelY + pad;
         int leftInnerW = this.leftW - pad * 2;

         int headerSize = 22;
         boolean headerBold = true;
         int subtitleSize = 11;
         int headerH = RenderUtils.getCustomFontHeight(headerSize, headerBold);
         int subtitleH = RenderUtils.getCustomFontHeight(subtitleSize, false);
         int searchH = 24;
         int searchY = leftInnerY + headerH + subtitleH + 12;
         int listTop = searchY + searchH + 10;
         int listBottom = this.panelY + this.panelH - pad;

         this.serverSelectionList.updateSize(leftInnerW, this.height, listTop, listBottom);
         this.serverSelectionList.setLeftPos(leftInnerX);
         this.serverSelectionList.setRenderBackground(false);
         this.serverSelectionList.setRenderTopAndBottom(false);
         this.serverSelectionList.setRenderSelection(false);

         this.searchBox = new EditBox(this.font, leftInnerX, searchY, leftInnerW, searchH, Component.literal("Search"));
         this.searchBox.setBordered(true);
         this.searchBox.setTextColor(new Color(230, 234, 242).getRGB());
         this.searchBox.setTextColorUneditable(new Color(148, 163, 184).getRGB());
         this.searchBox.setHint(Component.literal("Search servers..."));
         this.searchBox.setResponder(s -> {
            String q = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
               this.serverSelectionList.setServerFilter(null);
            } else {
               this.serverSelectionList.setServerFilter(sd -> {
                  if (sd == null) return false;
                  String n = sd.name.toLowerCase(Locale.ROOT);
                  String ip = sd.ip.toLowerCase(Locale.ROOT);
                  return n.contains(q) || ip.contains(q);
               });
            }
         });
         this.addRenderableWidget(this.searchBox);

         int rightX = this.panelX + this.leftW + gap;
         int rightInnerX = rightX + 16;
         int rightInnerY = this.panelY + 16;
         int rightInnerW = this.rightW - 32;

         int btnH = 28;
         int btnGap = 8;

         this.addRenderableWidget(this.createButton(rightInnerX, rightInnerY + 34, rightInnerW, btnH, Component.literal("Protocol"), btn -> {
            this.minecraft.setScreen(new ProtocolSelectorScreen());
         }, 13, "🌐"));

         this.addRenderableWidget(this.createButton(rightInnerX, rightInnerY + 34 + btnH + btnGap, rightInnerW, btnH, Component.literal("Alt Manager"), btn -> {
            this.minecraft.setScreen(new AltManagerScreen());
         }, 13, "👤"));

         int btnY = rightInnerY + 34 + (btnH + btnGap) * 2 + 12;

         this.selectButton = this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.select"), (p_99728_) -> {
            this.joinSelectedServer();
         }, 13, "▶"));
         btnY += btnH + btnGap;

         this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.direct"), (p_280868_) -> {
            this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
            this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
         }, 13, "\uD83D\uDD17"));
         btnY += btnH + btnGap;

         this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.add"), (p_280869_) -> {
            this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
            this.minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, this.editingServer));
         }, 13, "➕"));
         btnY += btnH + btnGap;

         this.editButton = this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.edit"), (p_99715_) -> {
            ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
            if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
               ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
               this.editingServer = new ServerData(serverdata.name, serverdata.ip, false);
               this.editingServer.copyFrom(serverdata);
               this.minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, this.editingServer));
            }
         }, 13, "✏"));
         btnY += btnH + btnGap;

         this.deleteButton = this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.delete"), (p_99710_) -> {
            ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
            if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
               String s = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData().name;
                Component component = Component.translatable("selectServer.deleteQuestion");
                Component component1 = Component.translatable("selectServer.deleteWarning", s);
                Component component2 = Component.translatable("selectServer.deleteButton");
                Component component3 = CommonComponents.GUI_CANCEL;
                this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, component, component1, component2, component3));
            }
         }, 13, "🗑️"));
         btnY += btnH + btnGap;

         this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, Component.translatable("selectServer.refresh"), (p_99706_) -> {
            this.refreshServerList();
         }, 13, "↻"));
         btnY += btnH + btnGap;

         this.addRenderableWidget(this.createButton(rightInnerX, btnY, rightInnerW, btnH, CommonComponents.GUI_CANCEL, (p_280867_) -> {
            this.minecraft.setScreen(this.lastScreen);
         }, 13, "✖"));
      } else {
         this.addRenderableWidget(Button.builder(Component.literal("Protocol Selector"), btn -> {
            this.minecraft.setScreen(new ProtocolSelectorScreen());
         }).pos(10, 8).build());

         this.addRenderableWidget(Button.builder(Component.literal("AltManager"), btn -> {
            this.minecraft.setScreen(new AltManagerScreen());
         }).pos(this.width - 10 - 150, 8).build());

         this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.select"), (p_99728_) -> {
            this.joinSelectedServer();
         }).width(100).build());
         Button button = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.direct"), (p_280868_) -> {
            this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
            this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
         }).width(100).build());
         Button button1 = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.add"), (p_280869_) -> {
            this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
            this.minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, this.editingServer));
         }).width(100).build());
         this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.edit"), (p_99715_) -> {
            ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
            if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
               ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
               this.editingServer = new ServerData(serverdata.name, serverdata.ip, false);
               this.editingServer.copyFrom(serverdata);
               this.minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, this.editingServer));
            }

         }).width(74).build());
         this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.delete"), (p_99710_) -> {
            ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
            if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
               String s = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData().name;
                Component component = Component.translatable("selectServer.deleteQuestion");
                Component component1 = Component.translatable("selectServer.deleteWarning", s);
                Component component2 = Component.translatable("selectServer.deleteButton");
                Component component3 = CommonComponents.GUI_CANCEL;
                this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, component, component1, component2, component3));
            }

         }).width(74).build());
         Button button2 = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.refresh"), (p_99706_) -> {
            this.refreshServerList();
         }).width(74).build());
         Button button3 = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_280867_) -> {
            this.minecraft.setScreen(this.lastScreen);
         }).width(74).build());
         GridLayout gridlayout = new GridLayout();
         GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(1);
         LinearLayout linearlayout = gridlayout$rowhelper.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL));
         linearlayout.addChild(this.selectButton);
         linearlayout.addChild(button);
         linearlayout.addChild(button1);
         gridlayout$rowhelper.addChild(SpacerElement.height(4));
         LinearLayout linearlayout1 = gridlayout$rowhelper.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL));
         linearlayout1.addChild(this.editButton);
         linearlayout1.addChild(this.deleteButton);
         linearlayout1.addChild(button2);
         linearlayout1.addChild(button3);
         gridlayout.arrangeElements();
         FrameLayout.centerInRectangle(gridlayout, 0, this.height - 64, this.width, 64);
      }
      this.onSelectedChange();
   }

   public void tick() {
      super.tick();
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) {
         List<LanServer> list = this.lanServerList.takeDirtyServers();
         if (list != null) {
            this.serverSelectionList.updateNetworkServers(list);
         }
      }

      this.pinger.tick();
   }

   public void removed() {
      if (this.lanServerDetector != null) {
         this.lanServerDetector.interrupt();
         this.lanServerDetector = null;
      }

      this.pinger.removeAll();
      this.serverSelectionList.removed();
   }

   private void refreshServerList() {
      this.minecraft.setScreen(new JoinMultiplayerScreen(this.lastScreen));
   }

   private void deleteCallback(boolean p_99712_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99712_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         this.servers.remove(((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData());
         this.servers.save();
         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void editServerCallback(boolean p_99717_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99717_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
         serverdata.name = this.editingServer.name;
         serverdata.ip = this.editingServer.ip;
         serverdata.copyFrom(this.editingServer);
         this.servers.save();
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void addServerCallback(boolean p_99722_) {
      if (p_99722_) {
         ServerData serverdata = this.servers.unhide(this.editingServer.ip);
         if (serverdata != null) {
            serverdata.copyNameIconFrom(this.editingServer);
            this.servers.save();
         } else {
            this.servers.add(this.editingServer, false);
            this.servers.save();
         }

         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void directJoinCallback(boolean p_99726_) {
      if (p_99726_) {
         ServerData serverdata = this.servers.get(this.editingServer.ip);
         if (serverdata == null) {
            this.servers.add(this.editingServer, true);
            this.servers.save();
            this.join(this.editingServer);
         } else {
            this.join(serverdata);
         }
      } else {
         this.minecraft.setScreen(this);
      }

   }

   private CustomButton createButton(int x, int y, int width, int height, Component label, Consumer<CustomButton> onPress, int textSize, @Nullable String iconText) {
      CustomButton button = (new CustomButton(x, y, width, height, label, onPress)).setRadius(10).setBackground(new Color(16, 19, 25).getRGB(), new Color(16, 19, 25).getRGB()).setBorderColor(new Color(255, 255, 255, 10).getRGB()).setGlow(new Color(108, 124, 255).getRGB(), 46).setText(new Color(230, 234, 242).getRGB(), false, textSize);
      if (iconText != null && !iconText.isBlank()) {
         button.setIcon(iconText, CustomButton.IconSide.LEFT).setIconStyle(new Color(230, 234, 242).getRGB(), 10).setIconScale(0.9f);
      }

      return button;
   }

   private static int withAlpha(int color, int alpha) {
      return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
   }

   public boolean keyPressed(int p_99690_, int p_99691_, int p_99692_) {
      if (super.keyPressed(p_99690_, p_99691_, p_99692_)) {
         return true;
      } else if (p_99690_ == 294) {
         this.refreshServerList();
         return true;
      } else if (this.serverSelectionList.getSelected() != null) {
         if (CommonInputs.selected(p_99690_)) {
            this.joinSelectedServer();
            return true;
         } else {
            return this.serverSelectionList.keyPressed(p_99690_, p_99691_, p_99692_);
         }
      } else {
         return false;
      }
   }

   public void render(GuiGraphics p_281617_, int p_281629_, int p_281983_, float p_283431_) {
      this.toolTip = null;
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) {
         this.renderBackground(p_281617_);
         this.serverSelectionList.render(p_281617_, p_281629_, p_281983_, p_283431_);
         p_281617_.drawCenteredString(this.font, this.title, this.width / 2, 20, new Color(255, 255, 255).getRGB());
         super.render(p_281617_, p_281629_, p_281983_, p_283431_);
      } else {
         if (this.xynisOpenTimeMs == 0L) {
            this.xynisOpenTimeMs = Util.getMillis();
         }

         float panelAlpha = Mth.clamp((Util.getMillis() - this.xynisOpenTimeMs) / 400.0F, 0.0F, 1.0F);
         int accent = ClickGui.getCurrentThemeColor();
         CustomUi.renderBackground(p_281617_, this.width, this.height, p_281629_, p_281983_, p_283431_, accent);

         int gap = 16;
         int pad = 18;
         int leftX = this.panelX;
         int leftY = this.panelY;
         int rightX = this.panelX + this.leftW + gap;
         int radius = 18;

         int panelColor = withAlpha(PANEL_RGB, (int) (190 * panelAlpha));
         int borderColor = withAlpha(BORDER_RGB, (int) (13 * panelAlpha));

         p_281617_.fillRounded(leftX, leftY, this.leftW, this.panelH, radius, panelColor);
         p_281617_.fillRounded(rightX, leftY, this.rightW, this.panelH, radius, panelColor);
         RenderUtils.drawRoundedRectOutline(p_281617_.pose(), leftX, leftY, this.leftW, this.panelH, radius, 1, borderColor);
         RenderUtils.drawRoundedRectOutline(p_281617_.pose(), rightX, leftY, this.rightW, this.panelH, radius, 1, borderColor);

         String header = "Multiplayer";
         int headerSize = 22;
         boolean headerBold = true;
         if (cachedHeaderW == -1 || !header.equals(lastHeader)) {
            cachedHeaderW = RenderUtils.getCustomStringWidth(header, headerBold, headerSize);
            lastHeader = header;
         }
         int headerColor = withAlpha(HEADER_RGB, (int) (255 * panelAlpha));
         RenderUtils.drawCustomString(p_281617_.pose(), header, leftX + pad, leftY + gap, headerColor, headerBold, headerSize);

         String subtitle = "Choose and manage your servers";
         int subtitleSize = 11;
         if (cachedSubtitleW == -1 || !subtitle.equals(lastSubtitle)) {
            cachedSubtitleW = RenderUtils.getCustomFontAscent(subtitleSize, false);
            lastSubtitle = subtitle;
         }
         float subtitleAscent = cachedSubtitleW;
         int subtitleColor = withAlpha(SUBTITLE_RGB, (int) (255 * panelAlpha));
         RenderUtils.drawCustomString(p_281617_.pose(), subtitle, leftX + pad, leftY + gap + 16 + subtitleAscent, subtitleColor, false, subtitleSize);

         if (Math.abs(panelAlpha - this.lastPanelAlpha) > 0.001F) {
            this.lastPanelAlpha = panelAlpha;
            for (GuiEventListener guieventlistener : this.children()) {
               if (guieventlistener instanceof AbstractWidget) {
                  ((AbstractWidget)guieventlistener).setAlpha(panelAlpha);
               }
            }
         }

         RenderUtils.enableScissor(leftX, this.panelY + 70, this.leftW, this.panelH - 80);
         this.serverSelectionList.render(p_281617_, p_281629_, p_281983_, p_283431_);
         RenderUtils.disableScissor();

         super.render(p_281617_, p_281629_, p_281983_, p_283431_);
      }
      if (this.toolTip != null) {
         p_281617_.renderComponentTooltip(this.font, this.toolTip, p_281629_, p_281983_);
      }

   }

   public void joinSelectedServer() {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         this.join(((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData());
      } else if (serverselectionlist$entry instanceof ServerSelectionList.NetworkServerEntry) {
         LanServer lanserver = ((ServerSelectionList.NetworkServerEntry)serverselectionlist$entry).getServerData();
         this.join(new ServerData(lanserver.getMotd(), lanserver.getAddress(), true));
      }

   }

   private void join(ServerData p_99703_) {
      ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(p_99703_.ip), p_99703_, false);
   }

   public void setSelected(ServerSelectionList.Entry p_99701_) {
      this.serverSelectionList.setSelected(p_99701_);
      this.onSelectedChange();
   }

   protected void onSelectedChange() {
      if (this.selectButton == null || this.editButton == null || this.deleteButton == null) {
         return;
      }

      this.selectButton.active = false;
      this.editButton.active = false;
      this.deleteButton.active = false;
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (serverselectionlist$entry != null && !(serverselectionlist$entry instanceof ServerSelectionList.LANHeader)) {
         this.selectButton.active = true;
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
            this.editButton.active = true;
            this.deleteButton.active = true;
         }
      }

   }

   public ServerStatusPinger getPinger() {
      return this.pinger;
   }

   public void setToolTip(List<Component> p_99708_) {
      this.toolTip = p_99708_;
   }

   public ServerList getServers() {
      return this.servers;
   }
}