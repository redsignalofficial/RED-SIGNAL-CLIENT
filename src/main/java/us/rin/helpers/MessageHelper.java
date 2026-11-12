package us.rin.helpers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.clickgui.utils.ColorTheme;

public class MessageHelper {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public void sendMessage(String msg, boolean prefixBool) {
        if (Minecraft.getInstance().player == null) return;
        msg = legacyConvert(msg);

        String finalMessage;

        if (prefixBool) {
            String prefixColor = getGuiColor();

            String prefix = "<dark_gray>[</dark_gray>"
                    + "<" + prefixColor + ">RedSignalClient</" + prefixColor + ">"
                    + "<dark_gray>] » </dark_gray><gray>";

            finalMessage = prefix + msg;
        } else {
            finalMessage = msg;
        }

        Component comp = serializer(finalMessage);
        try {
            Object chat = Minecraft.getInstance().gui.getChat();
            java.lang.reflect.Method m = null;
            for (java.lang.reflect.Method method : chat.getClass().getMethods()) {
                if ("addClientSystemMessage".equals(method.getName()) || "addMessage".equals(method.getName())) {
                    if (method.getParameterCount() == 1) {
                        m = method;
                        break;
                    }
                }
            }
            if (m != null) {
                m.invoke(chat, comp);
            } else if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(comp);
            }
        } catch (Throwable t) {
            try {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(comp);
                }
            } catch (Throwable ignored) {}
        }
    }

    public void sendSeparateLine() {
        sendMessage("", false);
    }

    private Component serializer(String text) {
        try {
            String json = GsonComponentSerializer.gson().serialize(miniMessage.deserialize(text));
            try {
                Class<?> serializerClass = Class.forName("net.minecraft.network.chat.Component$Serializer");
                java.lang.reflect.Method m = serializerClass.getMethod("fromJson", String.class);
                return (Component) m.invoke(null, json);
            } catch (Throwable t) {
                try {
                    java.lang.reflect.Method literalMethod = Component.class.getMethod("literal", String.class);
                    String plain = miniMessage.stripTags(text);
                    return (Component) literalMethod.invoke(null, plain);
                } catch (Throwable t2) {
                    return Component.nullToEmpty(text);
                }
            }
        } catch (Throwable t) {
            try {
                return Component.literal(text);
            } catch (Throwable ignored) {
                return Component.nullToEmpty(text);
            }
        }
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