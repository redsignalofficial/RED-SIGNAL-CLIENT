package us.whitedev.gui.screen;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.protocolinfo.ProtocolInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import us.whitedev.helpers.ClientConfigHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ProtocolSelectorScreen extends Screen {
    private ProtocolList protocolList;

    public ProtocolSelectorScreen() {
        super(Component.literal("Protocol Selector"));
    }

    @Override
    protected void init() {
        super.init();

        int top = 40;
        int bottom = this.height - 80;
        int listWidth = Math.min(360, this.width - 60);
        int left = (this.width - listWidth) / 2;
        int itemHeight = 36;

        protocolList = new ProtocolList(this.minecraft, listWidth, this.height, top, bottom, itemHeight);
        protocolList.setLeftPos(left);

        List<ProtocolVersion> protocols = ViaLoadingBase.PROTOCOLS;
        for (ProtocolVersion ver : protocols) {
            ProtocolInfo info = null;
            try {
                info = ProtocolInfo.fromProtocolVersion(ver);
            } catch (Throwable ignored) {}
            protocolList.addEntry(new ProtocolEntry(ver, info));
        }

        ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target != null) {
            for (ProtocolEntry e : protocolList.children()) {
                if (e.protocol.equals(target)) {
                    protocolList.setSelected(e);
                    protocolList.centerScrollOn(e);
                    break;
                }
            }
        }

        this.addRenderableWidget(protocolList);

        int btnW = 140;
        int btnH = 20;
        int gap = 10;
        int y = this.height - 40;

        Button applyButton = Button.builder(Component.literal("Apply"), btn -> onApplyPressed())
                .bounds(this.width / 2 - btnW - gap / 2, y, btnW, btnH).build();
        Button backButton = Button.builder(Component.literal("Back"), btn -> this.onClose())
                .bounds(this.width / 2 + gap / 2, y, btnW, btnH).build();

        this.addRenderableWidget(applyButton);
        this.addRenderableWidget(backButton);
    }

    private void onApplyPressed() {
        ProtocolEntry sel = protocolList.getSelected();
        if (sel == null) {
            if (Objects.requireNonNull(this.minecraft).player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("No protocol selected"));
            }
            return;
        }

        ProtocolVersion chosen = sel.protocol;
        ViaLoadingBase.getInstance().reload(chosen);
        ClientConfigHelper.LAST_PROTOCOL_VERSION = chosen.getVersion();
        new ClientConfigHelper().saveClientConfig();
        this.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        ProtocolVersion tv = ViaLoadingBase.getInstance().getTargetVersion();
        String current = tv != null ? tv.getName() : "unknown";
        guiGraphics.drawCenteredString(this.font, Component.literal("Current target: " + current), this.width / 2, 26, 0xAAAAAA);

        this.protocolList.render(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    private static class ProtocolList extends ObjectSelectionList<ProtocolEntry> {
        public ProtocolList(Minecraft mc, int width, int screenHeight, int top, int bottom, int itemHeight) {
            super(mc, width, screenHeight, top, bottom, itemHeight);
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }
    }


    private static class ProtocolEntry extends ObjectSelectionList.Entry<ProtocolEntry> {
        final ProtocolVersion protocol;
        final ProtocolInfo info;

        ProtocolEntry(ProtocolVersion protocol, @Nullable ProtocolInfo info) {
            this.protocol = protocol;
            this.info = info;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int y, int x, int listWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            String name = protocol.getName();
            String id = String.valueOf(protocol.getVersion());
            String title = name + " (" + id + ")";
            String date = info != null ? info.releaseDate() : "";

            ObjectSelectionList<?> parent = mc.screen != null ? ((ProtocolSelectorScreen) mc.screen).protocolList : null;
            boolean isSelected = false;
            if (parent != null) {
                ObjectSelectionList.Entry<?> sel = parent.getSelected();
                isSelected = sel == this;
            }
            if (isSelected) {
                guiGraphics.fill(x - 2, y - 1, x + listWidth, y + entryHeight - 1, 0x33555555);
            } else if (hovered) {
                guiGraphics.fill(x - 2, y - 1, x + listWidth, y + entryHeight - 1, 0x11555555);
            }

            int paddingX = x + 6;
            int line1Y = y + 4;
            int line2Y = y + 18;

            guiGraphics.drawString(mc.font, Component.literal(title), paddingX, line1Y, 0xFFFFFF, false);
            if (!date.isEmpty()) {
                guiGraphics.drawString(mc.font, Component.literal(date), paddingX, line2Y, 0xAAAAAA, false);
            }
        }


        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof ProtocolSelectorScreen parent) {
                parent.protocolList.setSelected(this);
                parent.protocolList.centerScrollOn(this);
                return true;
            }
            return false;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(protocol.getName());
        }
    }
}
