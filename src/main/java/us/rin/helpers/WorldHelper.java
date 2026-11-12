package us.rin.helpers;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class WorldHelper {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void reloadWorldFix(){
        LocalPlayer player = mc.player;
        ClientLevel clientLevel = mc.level;
        MultiPlayerGameMode gameMode = mc.gameMode;
        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();

        if(player != null && clientLevel != null && gameMode != null && protocol >= 766) {
            Level level = player.level();

            ClientboundRespawnPacket respawnPacket = new ClientboundRespawnPacket(
                    level.dimensionTypeId(),
                    level.dimension(),
                    level.getBiomeManager().biomeZoomSeed,
                    gameMode.getPlayerMode(),
                    gameMode.getPreviousPlayerMode(),
                    level.isDebug(),
                    clientLevel.getLevelData().isFlat,
                    (byte) 0,
                    player.getLastDeathLocation(),
                    player.getPortalCooldown()
            );

            Objects.requireNonNull(mc.getConnection()).handleRespawn(respawnPacket);
            PacketHelper.send(new ServerboundContainerClickPacket(0, 0, 0, 0, ClickType.PICKUP, ItemStack.EMPTY, new Int2ObjectArrayMap<>())); // only for gui reload
        }
    }
}
