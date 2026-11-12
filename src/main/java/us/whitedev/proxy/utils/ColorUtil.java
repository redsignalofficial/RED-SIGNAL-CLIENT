package us.whitedev.proxy.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.Minecraft;

public class ColorUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static String format(String message){
        return message.replace("&", "§");
    }


    public static void sendMsg(String message) {
        if (Minecraft.getInstance().player == null) return;

        String json = GsonComponentSerializer.gson().serialize(miniMessage.deserialize(message));

        net.minecraft.network.chat.Component component =
                net.minecraft.network.chat.Component.Serializer.fromJson(json);

        Minecraft.getInstance().player.sendSystemMessage(component);
    }
}
