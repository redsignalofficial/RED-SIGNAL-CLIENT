package us.rin.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class ReconnectManager {

    private static ServerAddress lastAddress;
    private static ServerData lastData;
    private static long reconnectDeadline = -1L;

    public static void save(ServerAddress address, ServerData data) {
        lastAddress = address;
        lastData = data;
    }

    public static boolean hasServer() {
        return lastAddress != null;
    }

    public static void cancelCountdown() {
        reconnectDeadline = -1L;
    }

    public static void reconnect() {
        if (!hasServer()) return;
        cancelCountdown();

        Minecraft mc = Minecraft.getInstance();
        if(mc.screen != null){
        ConnectScreen.startConnecting(mc.screen, mc, lastAddress, lastData, false);
        }
    }
}
