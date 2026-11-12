package us.rin.fabric;

import net.fabricmc.api.ClientModInitializer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class RedSignalFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[RED SIGNAL CLIENT] Initializing Fabric mod entrypoint...");
        try {
            Class<?> clientClass = Class.forName("us.rin.RedSignalClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            clientClass.getMethod("start").invoke(instance);
            System.out.println("[RED SIGNAL CLIENT] Successfully loaded client on Fabric!");
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Fabric load notice: " + t);
        }

        registerChatInterceptor();
        registerTitleScreenCustomizer();
    }

    private void registerChatInterceptor() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents");
            Class<?> allowChatInterface = Class.forName("net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents$AllowChat");
            Object allowChatEvent = eventsClass.getField("ALLOW_CHAT").get(null);

            Object listener = Proxy.newProxyInstance(
                allowChatInterface.getClassLoader(),
                new Class<?>[]{allowChatInterface},
                (proxy, method, args) -> {
                    if ("allowSendChatMessage".equals(method.getName()) && args != null && args.length > 0) {
                        String message = (String) args[0];
                        if (message != null && message.startsWith("!")) {
                            System.out.println("[RED SIGNAL CLIENT] Intercepted chat command: " + message);
                            try {
                                us.rin.commands.CommandManager.getManager().handleCommand(message);
                            } catch (Throwable t) {
                                System.out.println("[RED SIGNAL CLIENT] Error executing command: " + t);
                                t.printStackTrace();
                            }
                            return false;
                        }
                    }
                    return true;
                }
            );

            Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.event.Event");
            Method registerMethod = eventClass.getMethod("register", Object.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(allowChatEvent, listener);
            System.out.println("[RED SIGNAL CLIENT] Chat command interceptor registered successfully!");
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Notice registering chat listener: " + t);
        }
    }

    private void registerTitleScreenCustomizer() {
        try {
            Class<?> screenEventsClass = Class.forName("net.fabricmc.fabric.api.client.screen.v1.ScreenEvents");
            Class<?> afterInitInterface = Class.forName("net.fabricmc.fabric.api.client.screen.v1.ScreenEvents$AfterInit");
            Object afterInitEvent = screenEventsClass.getField("AFTER_INIT").get(null);

            Object listener = Proxy.newProxyInstance(
                afterInitInterface.getClassLoader(),
                new Class<?>[]{afterInitInterface},
                (proxy, method, args) -> {
                    if ("afterInit".equals(method.getName()) && args != null && args.length >= 4) {
                        Object screen = args[1];
                        int scaledWidth = (int) args[2];
                        int scaledHeight = (int) args[3];
                        handleTitleScreenInit(screen, scaledWidth, scaledHeight, screenEventsClass);
                    }
                    return null;
                }
            );

            Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.event.Event");
            Method registerMethod = eventClass.getMethod("register", Object.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(afterInitEvent, listener);
            System.out.println("[RED SIGNAL CLIENT] TitleScreen UI customizer registered successfully!");
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Notice registering screen listener: " + t);
        }
    }

    private void handleTitleScreenInit(Object screen, int scaledWidth, int scaledHeight, Class<?> screenEventsClass) {
        if (screen == null) return;
        String screenName = screen.getClass().getName();
        if (!screenName.contains("TitleScreen") && !screenName.equals("net.minecraft.class_442")) {
            return;
        }

        try {
            Class<?> screensClass = Class.forName("net.fabricmc.fabric.api.client.screen.v1.Screens");
            Method getWidgetsMethod = null;
            for (Method m : screensClass.getMethods()) {
                if ("getWidgets".equals(m.getName())) {
                    getWidgetsMethod = m;
                    break;
                }
            }
            if (getWidgetsMethod == null) return;
            getWidgetsMethod.setAccessible(true);

            List<?> widgets = (List<?>) getWidgetsMethod.invoke(null, screen);
            if (widgets == null) return;

            int panelW = Math.min(270, (int) Math.round(scaledWidth * 0.45));
            panelW = Math.max(220, Math.min(panelW, scaledWidth - 24));
            int panelH = Math.min(210, (int) Math.round(scaledHeight * 0.76));
            panelH = Math.max(185, Math.min(panelH, scaledHeight - 24));

            int panelX = (scaledWidth - panelW) / 2;
            int panelY = (scaledHeight - panelH) / 2;

            int pad = 14;
            int headerH = 38;

            int btnW = (int) Math.round(panelW * 0.82);
            int btnH = 22;
            int btnGap = 5;
            int btnX = panelX + (panelW - btnW) / 2;
            int firstBtnY = panelY + pad + headerH;

            int smallH = 18;
            int smallW = (int) Math.round(panelW * 0.38);
            int smallGap = 8;
            int smallY = panelY + panelH - pad - smallH;
            int smallStartX = panelX + (panelW - (smallW * 2 + smallGap)) / 2;

            Object singleBtn = null;
            Object multiBtn = null;
            Object realmsBtn = null;
            Object optionsBtn = null;
            Object quitBtn = null;

            for (Object widget : widgets) {
                String text = getWidgetText(widget);
                if (text.contains("singleplayer")) {
                    singleBtn = widget;
                } else if (text.contains("multiplayer")) {
                    multiBtn = widget;
                } else if (text.contains("realms") || text.contains("online")) {
                    realmsBtn = widget;
                } else if (text.contains("options")) {
                    optionsBtn = widget;
                } else if (text.contains("quit")) {
                    quitBtn = widget;
                }
            }

            if (singleBtn != null) setWidgetBounds(singleBtn, btnX, firstBtnY, btnW, btnH);
            if (multiBtn != null) setWidgetBounds(multiBtn, btnX, firstBtnY + (btnH + btnGap), btnW, btnH);
            if (realmsBtn != null) setWidgetBounds(realmsBtn, btnX, firstBtnY + (btnH + btnGap) * 2, btnW, btnH);
            if (optionsBtn != null) setWidgetBounds(optionsBtn, smallStartX, smallY, smallW, smallH);
            if (quitBtn != null) setWidgetBounds(quitBtn, smallStartX + smallW + smallGap, smallY, smallW, smallH);

            hookScreenDraw(screen, panelX, panelY, panelW, panelH, scaledWidth, scaledHeight, screenEventsClass, screensClass);

        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Notice customizing TitleScreen: " + t);
        }
    }

    private void hookScreenDraw(Object screen, int px, int py, int pw, int ph, int sw, int sh, Class<?> screenEventsClass, Class<?> screensClass) {
        try {
            Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.event.Event");
            Method registerMethod = eventClass.getMethod("register", Object.class);
            registerMethod.setAccessible(true);

            Method afterBgMethod = null;
            Method afterExtractMethod = null;
            for (Method m : screenEventsClass.getMethods()) {
                if ("afterBackground".equals(m.getName())) afterBgMethod = m;
                if ("afterExtract".equals(m.getName())) afterExtractMethod = m;
            }

            if (afterBgMethod != null) {
                Object afterBgEvent = afterBgMethod.invoke(null, screen);
                Class<?> afterBgInterface = Class.forName("net.fabricmc.fabric.api.client.screen.v1.ScreenEvents$AfterBackground");
                Object bgListener = Proxy.newProxyInstance(
                    afterBgInterface.getClassLoader(),
                    new Class<?>[]{afterBgInterface},
                    (proxy, method, args) -> {
                        if ("afterBackground".equals(method.getName()) && args != null && args.length >= 2) {
                            Object graphics = args[1];
                            drawCardBackground(graphics, px, py, pw, ph);
                        }
                        return null;
                    }
                );
                registerMethod.invoke(afterBgEvent, bgListener);
            }

            if (afterExtractMethod != null) {
                Object afterExtractEvent = afterExtractMethod.invoke(null, screen);
                Class<?> afterExtractInterface = Class.forName("net.fabricmc.fabric.api.client.screen.v1.ScreenEvents$AfterExtract");
                Object extractListener = Proxy.newProxyInstance(
                    afterExtractInterface.getClassLoader(),
                    new Class<?>[]{afterExtractInterface},
                    (proxy, method, args) -> {
                        if ("afterExtract".equals(method.getName()) && args != null && args.length >= 2) {
                            Object graphics = args[1];
                            drawCardForeground(graphics, screen, screensClass, px, py, pw, ph, sh);
                        }
                        return null;
                    }
                );
                registerMethod.invoke(afterExtractEvent, extractListener);
            }

        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Notice registering screen draw hook: " + t);
        }
    }

    private void drawCardBackground(Object graphics, int px, int py, int pw, int ph) {
        if (graphics == null) return;
        try {
            // Draw dark sleek glass panel
            fill(graphics, px, py, px + pw, py + ph, 0xDD101319);
            // Draw neon border outline
            outline(graphics, px, py, pw, ph, 0x446C7CFF);
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] drawCardBackground error: " + t);
        }
    }

    private void drawCardForeground(Object graphics, Object screen, Class<?> screensClass, int px, int py, int pw, int ph, int sh) {
        if (graphics == null) return;
        try {
            Method getFontMethod = null;
            for (Method m : screensClass.getMethods()) {
                if ("getFont".equals(m.getName())) {
                    getFontMethod = m;
                    break;
                }
            }
            if (getFontMethod == null) return;
            getFontMethod.setAccessible(true);
            Object font = getFontMethod.invoke(null, screen);
            if (font == null) return;

            // Small caps header
            String title = toSmallCaps("RED SIGNAL CLIENT");
            String subtitle = toSmallCaps("Exclusive Minecraft Crash/Exploit Client");
            String footer1 = toSmallCaps("RedSignalClient 6.4");
            String footer2 = toSmallCaps("Created by Rin");

            centeredText(graphics, font, title, px + pw / 2, py + 12, 0xFFFF3355);
            centeredText(graphics, font, subtitle, px + pw / 2, py + 25, 0xFF9CA3AF);

            text(graphics, font, footer1, 12, sh - 22, 0xFF94A3B8);
            text(graphics, font, footer2, 12, sh - 12, 0xFF64748B);

        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] drawCardForeground error: " + t);
        }
    }

    private static String getWidgetText(Object widget) {
        if (widget == null) return "";
        try {
            for (Method m : widget.getClass().getMethods()) {
                if ("getMessage".equals(m.getName()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object msg = m.invoke(widget);
                    if (msg != null) {
                        try {
                            Method getString = msg.getClass().getMethod("getString");
                            getString.setAccessible(true);
                            return ((String) getString.invoke(msg)).toLowerCase() + " " + msg.toString().toLowerCase();
                        } catch (Throwable t) {
                            return msg.toString().toLowerCase();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static void setWidgetBounds(Object widget, int x, int y, int w, int h) {
        try {
            for (Method m : widget.getClass().getMethods()) {
                if ("setX".equals(m.getName()) && m.getParameterCount() == 1) { m.setAccessible(true); m.invoke(widget, x); }
                if ("setY".equals(m.getName()) && m.getParameterCount() == 1) { m.setAccessible(true); m.invoke(widget, y); }
                if ("setWidth".equals(m.getName()) && m.getParameterCount() == 1) { m.setAccessible(true); m.invoke(widget, w); }
                if ("setHeight".equals(m.getName()) && m.getParameterCount() == 1) { m.setAccessible(true); m.invoke(widget, h); }
            }
        } catch (Throwable ignored) {}
    }

    private static void fill(Object graphics, int x1, int y1, int x2, int y2, int color) {
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if ("fill".equals(m.getName()) && m.getParameterCount() == 5 && m.getParameterTypes()[0] == int.class) {
                    m.setAccessible(true);
                    m.invoke(graphics, x1, y1, x2, y2, color);
                    return;
                }
            }
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] fill error: " + t);
        }
    }

    private static void outline(Object graphics, int x, int y, int w, int h, int color) {
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if ("outline".equals(m.getName()) && m.getParameterCount() == 5) {
                    m.setAccessible(true);
                    m.invoke(graphics, x, y, w, h, color);
                    return;
                }
            }
            // Fallback outline with fill lines
            fill(graphics, x, y, x + w, y + 1, color);
            fill(graphics, x, y + h - 1, x + w, y + h, color);
            fill(graphics, x, y + 1, x + 1, y + h - 1, color);
            fill(graphics, x + w - 1, y + 1, x + w, y + h - 1, color);
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] outline error: " + t);
        }
    }

    private static void centeredText(Object graphics, Object font, String text, int x, int y, int color) {
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if ("centeredText".equals(m.getName()) && m.getParameterCount() == 5 && m.getParameterTypes()[1] == String.class) {
                    m.setAccessible(true);
                    m.invoke(graphics, font, text, x, y, color);
                    return;
                }
            }
            // Fallback: calculate width
            int width = 0;
            try {
                for (Method m : font.getClass().getMethods()) {
                    if ("width".equals(m.getName()) && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        m.setAccessible(true);
                        width = (int) m.invoke(font, text);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            text(graphics, font, text, x - width / 2, y, color);
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] centeredText error: " + t);
        }
    }

    private static void text(Object graphics, Object font, String text, int x, int y, int color) {
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if ("text".equals(m.getName()) && m.getParameterCount() == 5 && m.getParameterTypes()[1] == String.class) {
                    m.setAccessible(true);
                    m.invoke(graphics, font, text, x, y, color);
                    return;
                }
            }
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] text error: " + t);
        }
    }

    public static String toSmallCaps(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (Character.toLowerCase(c)) {
                case 'a': sb.append('ᴀ'); break;
                case 'b': sb.append('ʙ'); break;
                case 'c': sb.append('ᴄ'); break;
                case 'd': sb.append('ᴅ'); break;
                case 'e': sb.append('ᴇ'); break;
                case 'f': sb.append('ғ'); break;
                case 'g': sb.append('ɢ'); break;
                case 'h': sb.append('ʜ'); break;
                case 'i': sb.append('ɪ'); break;
                case 'j': sb.append('ᴊ'); break;
                case 'k': sb.append('ᴋ'); break;
                case 'l': sb.append('ʟ'); break;
                case 'm': sb.append('ᴍ'); break;
                case 'n': sb.append('ɴ'); break;
                case 'o': sb.append('ᴏ'); break;
                case 'p': sb.append('ᴘ'); break;
                case 'q': sb.append('ǫ'); break;
                case 'r': sb.append('ʀ'); break;
                case 's': sb.append('s'); break;
                case 't': sb.append('ᴛ'); break;
                case 'u': sb.append('ᴜ'); break;
                case 'v': sb.append('ᴠ'); break;
                case 'w': sb.append('ᴡ'); break;
                case 'x': sb.append('x'); break;
                case 'y': sb.append('ʏ'); break;
                case 'z': sb.append('ᴢ'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
