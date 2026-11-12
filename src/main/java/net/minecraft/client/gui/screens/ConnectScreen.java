package net.minecraft.client.gui.screens;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.gui.common.CustomButton;
import us.whitedev.gui.common.CustomUi;
import us.whitedev.gui.common.RenderUtils;
import us.whitedev.helpers.ReconnectManager;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class ConnectScreen extends Screen {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogUtils.getLogger();
   private static final long NARRATION_DELAY_MS = 2000L;
   static final Component ABORT_CONNECTION = Component.translatable("connect.aborted");
   public static final Component UNKNOWN_HOST_MESSAGE = Component.translatable("disconnect.genericReason", Component.translatable("disconnect.unknownHost"));
   @Nullable
   volatile Connection connection;
   @Nullable
   ChannelFuture channelFuture;
   volatile boolean aborted;
   final Screen parent;
   private Component status = Component.translatable("connect.connecting");
   private long lastNarration = -1L;
   final Component connectFailedTitle;
   private final List<String> connectionSteps = new ArrayList<>();
   private String serverIP;
   private int serverPort;
   private long redSignalOpenTimeMs;

   public ConnectScreen(Screen p_279215_, Component p_279228_) {
      super(GameNarrator.NO_TITLE);
      this.parent = p_279215_;
      this.connectFailedTitle = p_279228_;
   }

   public static void startConnecting(Screen p_279473_, Minecraft p_279200_, ServerAddress p_279150_, ServerData p_279481_, boolean p_279117_) {
      if (p_279200_.screen instanceof ConnectScreen) {
         LOGGER.error("Attempt to connect while already connecting");
      } else {
         ConnectScreen connectscreen = new ConnectScreen(p_279473_, p_279117_ ? QuickPlay.ERROR_TITLE : CommonComponents.CONNECT_FAILED);
         p_279200_.clearLevel();
         p_279200_.prepareForMultiplayer();
         p_279200_.updateReportEnvironment(ReportEnvironment.thirdParty(p_279481_ != null ? p_279481_.ip : p_279150_.getHost()));
         p_279200_.quickPlayLog().setWorldData(QuickPlayLog.Type.MULTIPLAYER, p_279481_.ip, p_279481_.name);
         p_279200_.setScreen(connectscreen);
         connectscreen.connect(p_279200_, p_279150_, p_279481_);
      }
   }

   public void connect(final Minecraft p_251955_, final ServerAddress p_249536_, @Nullable final ServerData p_252078_) {
      LOGGER.info("Connecting to {}, {}", p_249536_.getHost(), p_249536_.getPort());
      serverIP = p_249536_.getHost();
      serverPort = p_249536_.getPort();
      Thread thread = new Thread("Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()) {
         public void run() {
            InetSocketAddress inetsocketaddress = null;

            try {
               if (ConnectScreen.this.aborted) {
                  return;
               }

               addConnectionStep("Resolving Host");
               Optional<InetSocketAddress> optional = ServerNameResolver.DEFAULT.resolveAddress(p_249536_).map(ResolvedServerAddress::asInetSocketAddress);
               if (ConnectScreen.this.aborted) {
                  return;
               }

               if (!optional.isPresent()) {
                  p_251955_.execute(() -> {
                     p_251955_.setScreen(new DisconnectedScreen(ConnectScreen.this.parent, ConnectScreen.this.connectFailedTitle, ConnectScreen.UNKNOWN_HOST_MESSAGE));
                  });
                  return;
               }

               inetsocketaddress = optional.get();
               Connection connection;
               synchronized(ConnectScreen.this) {
                  if (ConnectScreen.this.aborted) {
                     return;
                  }

                  addConnectionStep("Establishing Connection");
                  connection = new Connection(PacketFlow.CLIENTBOUND);
                  ConnectScreen.this.channelFuture = Connection.connect(inetsocketaddress, p_251955_.options.useNativeTransport(), connection);
               }

               ConnectScreen.this.channelFuture.syncUninterruptibly();
               synchronized(ConnectScreen.this) {
                  if (ConnectScreen.this.aborted) {
                     connection.disconnect(ConnectScreen.ABORT_CONNECTION);
                     return;
                  }

                  ConnectScreen.this.connection = connection;
               }

               addConnectionStep("Handshaking");
               ConnectScreen.this.connection.setListener(new ClientHandshakePacketListenerImpl(ConnectScreen.this.connection, p_251955_, p_252078_, ConnectScreen.this.parent, false, (Duration)null, ConnectScreen.this::updateStatus));
               ConnectScreen.this.connection.send(new ClientIntentionPacket(inetsocketaddress.getHostName(), inetsocketaddress.getPort(), ConnectionProtocol.LOGIN));
               ConnectScreen.this.connection.send(new ServerboundHelloPacket(p_251955_.getUser().getName(), Optional.ofNullable(p_251955_.getUser().getProfileId())));
               ReconnectManager.save(p_249536_, p_252078_);
            } catch (Exception exception2) {
               if (ConnectScreen.this.aborted) {
                  return;
               }

               Throwable throwable = exception2.getCause();
               Exception exception;
               if (throwable instanceof Exception exception1) {
                  exception = exception1;
               } else {
                  exception = exception2;
               }

               ConnectScreen.LOGGER.error("Couldn't connect to server", (Throwable)exception2);
               String s = inetsocketaddress == null ? exception.getMessage() : exception.getMessage().replaceAll(inetsocketaddress.getHostName() + ":" + inetsocketaddress.getPort(), "").replaceAll(inetsocketaddress.toString(), "");
               p_251955_.execute(() -> {
                  p_251955_.setScreen(new DisconnectedScreen(ConnectScreen.this.parent, ConnectScreen.this.connectFailedTitle, Component.translatable("disconnect.genericReason", s)));
               });
            }

         }
      };
      thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   private void addConnectionStep(String step) {
      String coloredStep;
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED SIGNAL) {
         coloredStep = step;
      } else {
         switch (connectionSteps.size()) {
            case 0 -> coloredStep = "§4" + step;
            case 1 -> coloredStep = "§c" + step;
            case 2 -> coloredStep = "§6" + step;
            case 3 -> coloredStep = "§e" + step;
            case 4 -> coloredStep = "§2" + step;
            default -> coloredStep = "§a" + step;
         }
      }
      connectionSteps.add(coloredStep);
   }

   private void updateStatus(Component p_95718_) {
      this.status = p_95718_;
      addConnectionStep(p_95718_.getString());
   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void init() {
      this.redSignalOpenTimeMs = Util.getMillis();
      int panelW = Math.min(520, this.width - 80);
      int panelH = Math.min(280, this.height - 120);
      int panelX = (this.width - panelW) / 2;
      int panelY = (this.height - panelH) / 2;
      int cancelY = ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED SIGNAL ? panelY + panelH - 38 : this.height / 4 + 120 + 12;

      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.RED SIGNAL) {
         this.addRenderableWidget((new CustomButton(panelX + (panelW - 220) / 2, cancelY, 220, 24, CommonComponents.GUI_CANCEL, p_289624_ -> {
            this.abortAndReturn();
         })).setRadius(10).setBackground(new Color(16, 19, 25).getRGB(), new Color(16, 19, 25).getRGB()).setBorderColor(new Color(255, 255, 255, 10).getRGB()).setGlow(new Color(108, 124, 255).getRGB(), 46).setText(new Color(230, 234, 242).getRGB(), false, 13));
      } else {
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_289624_) -> {
            this.abortAndReturn();
         }).bounds(this.width / 2 - 110, cancelY, 220, 22).build());
      }
   }

   private void abortAndReturn() {
      synchronized(this) {
         this.aborted = true;
         if (this.channelFuture != null) {
            this.channelFuture.cancel(true);
            this.channelFuture = null;
         }

         if (this.connection != null) {
            this.connection.disconnect(ABORT_CONNECTION);
         }
      }

      this.minecraft.setScreen(this.parent);
   }

   private static int withAlpha(int color, int alpha) {
      Color base = new Color(color, true);
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), Mth.clamp(alpha, 0, 255)).getRGB();
   }

   private static void drawTitleGradient(GuiGraphics g, String text, float x, float baselineY, float width, boolean bold, int size, float alphaMul) {
      int left = withAlpha(new Color(230, 234, 242).getRGB(), (int)(255 * alphaMul));
      int right = withAlpha(new Color(199, 210, 254).getRGB(), (int)(255 * alphaMul));
      int scLeft = (int)Math.floor(x);
      int scRight = (int)Math.ceil(x + width);
      int mid = (scLeft + scRight) / 2;

      g.enableScissor(scLeft, 0, mid, g.guiHeight());
      RenderUtils.drawCustomString(g.pose(), text, x, baselineY, left, bold, size);
      g.disableScissor();

      g.enableScissor(mid, 0, scRight, g.guiHeight());
      RenderUtils.drawCustomString(g.pose(), text, x, baselineY, right, bold, size);
      g.disableScissor();
   }

   public void render(GuiGraphics p_283201_, int p_95701_, int p_95702_, float p_95703_) {
      if (ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT) {
         this.renderBackground(p_283201_);
      } else {
         CustomUi.renderBackground(p_283201_, this.width, this.height, p_95701_, p_95702_, p_95703_, ClickGui.getCurrentThemeColor());
      }
      long i = Util.getMillis();
      if (i - this.lastNarration > 2000L) {
         this.lastNarration = i;
         this.minecraft.getNarrator().sayNow(Component.translatable("narrator.joining"));
      }

      if(ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT){
         p_283201_.drawCenteredString(this.font, this.status, this.width / 2, this.height / 2 - 50, new Color(255, 255, 255).getRGB());
      }else {
         if (this.redSignalOpenTimeMs == 0L) {
            this.redSignalOpenTimeMs = Util.getMillis();
         }

         float panelAlpha = Mth.clamp((Util.getMillis() - this.redSignalOpenTimeMs) / 400.0F, 0.0F, 1.0F);
         int accent = ClickGui.getCurrentThemeColor();
         int panelW = Math.min(520, this.width - 80);
         int panelH = Math.min(280, this.height - 120);
         int panelX = (this.width - panelW) / 2;
         int panelY = (this.height - panelH) / 2;
         int radius = 18;

         int panelColor = withAlpha(new Color(15, 17, 22).getRGB(), (int)(190 * panelAlpha));
         int borderColor = withAlpha(new Color(255, 255, 255).getRGB(), (int)(13 * panelAlpha));
         p_283201_.fillRounded(panelX, panelY, panelW, panelH, radius, panelColor);
         RenderUtils.drawRoundedRectOutline(p_283201_.pose(), panelX, panelY, panelW, panelH, radius, 1, borderColor);

         String header = "Connecting";
         int headerSize = 24;
         boolean headerBold = true;
         float headerWidth = RenderUtils.getCustomStringWidth(header, headerBold, headerSize);
         drawTitleGradient(p_283201_, header, this.width / 2f - headerWidth / 2f, panelY + 18, headerWidth, headerBold, headerSize, panelAlpha);

         String target = serverIP == null ? "" : (serverIP + ":" + serverPort);
         int targetSize = 12;
         float targetWidth = RenderUtils.getCustomStringWidth(target, false, targetSize);
         float targetAscent = RenderUtils.getCustomFontAscent(targetSize, false);
         int targetColor = withAlpha(new Color(148, 163, 184).getRGB(), (int)(255 * panelAlpha));
         RenderUtils.drawCustomString(p_283201_.pose(), target, this.width / 2f - targetWidth / 2f, panelY + 48 + targetAscent, targetColor, false, targetSize);

         String statusText = this.status == null ? Component.translatable("connect.connecting").getString() : this.status.getString();
         int statusSize = 12;
         float statusW = RenderUtils.getCustomStringWidth(statusText, false, statusSize);
         float statusAscent = RenderUtils.getCustomFontAscent(statusSize, false);
         int statusColor = withAlpha(new Color(203, 213, 225).getRGB(), (int)(255 * panelAlpha));
         RenderUtils.drawCustomString(p_283201_.pose(), statusText, this.width / 2f - statusW / 2f, panelY + 68 + statusAscent, statusColor, false, statusSize);

         long t = Util.getMillis();
         float phase = (t % 900L) / 900f;
         int dots = (int) (phase * 3.0f);
         float dotY = panelY + 94;
         float dotStartX = this.width / 2f - 14;
         for (int di = 0; di < 3; di++) {
            float a = di == dots ? 1.0f : 0.25f;
            int c = CustomUi.withAlpha(accent, (int) (a * 210));
            RenderUtils.drawCircle(p_283201_.pose(), dotStartX + di * 14f, dotY, 3.6f, c);
         }

         int maxSteps = 8;
         List<String> snapshot;
         try {
            snapshot = new ArrayList<>(connectionSteps);
         } catch (ConcurrentModificationException e) {
            snapshot = List.of();
         }

         int stepFont = 12;
         int stepY = panelY + 118;
         int centerX = panelX + panelW / 2;
         float maxW = panelW - 48;
         for (int si = Math.max(0, snapshot.size() - maxSteps); si < snapshot.size(); si++) {
            String step = snapshot.get(si);
            int c;
            switch (si) {
               case 0 -> c = withAlpha(new Color(170, 0, 0).getRGB(), (int)(255 * panelAlpha));
               case 1 -> c = withAlpha(new Color(255, 85, 85).getRGB(), (int)(255 * panelAlpha));
               case 2 -> c = withAlpha(new Color(255, 170, 0).getRGB(), (int)(255 * panelAlpha));
               case 3 -> c = withAlpha(new Color(255, 255, 85).getRGB(), (int)(255 * panelAlpha));
               case 4 -> c = withAlpha(new Color(0, 170, 0).getRGB(), (int)(255 * panelAlpha));
               default -> c = withAlpha(new Color(85, 255, 85).getRGB(), (int)(255 * panelAlpha));
            }

            String clipped = step;
            while (!clipped.isEmpty() && RenderUtils.getCustomStringWidth(clipped, false, stepFont) > maxW) {
               clipped = clipped.substring(0, clipped.length() - 1);
            }
            if (clipped.length() < step.length()) {
               String withDots = clipped;
               if (withDots.length() > 3) {
                  withDots = withDots.substring(0, withDots.length() - 3) + "...";
               }
               clipped = withDots;
            }

            float w = RenderUtils.getCustomStringWidth(clipped, false, stepFont);
            RenderUtils.drawCustomString(p_283201_.pose(), clipped, centerX - w / 2f, stepY, c, false, stepFont);
            stepY += 14;
         }

         for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget) {
               ((AbstractWidget)guieventlistener).setAlpha(panelAlpha);
            }
         }
      }

      super.render(p_283201_, p_95701_, p_95702_, p_95703_);
   }
}