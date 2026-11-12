package us.whitedev.helpers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.gui.clickgui.utils.ColorTheme;

public class MessageHelper {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public void sendMessage(String msg, boolean prefixBool) {
        if (Minecraft.getInstance().player == null) return;
        msg = legacyConvert(msg);

        String finalMessage;

        if (prefixBool) {
            String prefixColor = getGuiColor();

            String prefix = "<dark_gray>[</dark_gray>"
                    + "<" + prefixColor + ">XynisClient</" + prefixColor + ">"
                    + "<dark_gray>] » </dark_gray><gray>";

            finalMessage = prefix + msg;
        } else {
            finalMessage = msg;
        }

        Minecraft.getInstance().gui.getChat().addMessage(serializer(finalMessage));
    }

    public void sendSeparateLine() {
        sendMessage("", false);
    }

    private Component serializer(String text) {
        String json = GsonComponentSerializer.gson().serialize(miniMessage.deserialize(text));
        return net.minecraft.network.chat.Component.Serializer.fromJson(json);
    }

    private String getGuiColor() {
        try {
            if (ClickGui.COLOR_THEME == ColorTheme.RAINBOW) {
                return "rainbow";
            }
            if (ClickGui.COLOR_THEME == ColorTheme.CUSTOM) {
                String hex = ClickGui.CUSTOM_HEX;
                if (hex == null || hex.isEmpty()) return "#FFFFFF";
                return hex;
            } else {
                return chatFormatting(ClickGui.COLOR_THEME.getMinecraftColorCode());
            }
        } catch (Exception e) {
            return "#FFFFFF";
        }
    }

    private String legacyConvert(String msg) {
        return msg
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")

                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }

    private String chatFormatting(String code) {
        return switch (code) {
            case "&0" -> "black";
            case "&1" -> "dark_blue";
            case "&2" -> "dark_green";
            case "&3" -> "dark_aqua";
            case "&4" -> "dark_red";
            case "&5" -> "dark_purple";
            case "&6" -> "gold";
            case "&7" -> "gray";
            case "&8" -> "dark_gray";
            case "&9" -> "blue";
            case "&a" -> "green";
            case "&b" -> "aqua";
            case "&c" -> "red";
            case "&d" -> "light_purple";
            case "&e" -> "yellow";
            case "&f" -> "white";
            default -> "white";
        };
    }
}