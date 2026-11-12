package us.rin.fabric;

import net.fabricmc.api.ClientModInitializer;

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
            t.printStackTrace();
        }

        registerChatInterceptor();
    }

    private void registerChatInterceptor() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents");
            Class<?> allowChatInterface = Class.forName("net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents$AllowChat");
            Object allowChatEvent = eventsClass.getField("ALLOW_CHAT").get(null);

            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                allowChatInterface.getClassLoader(),
                new Class<?>[]{allowChatInterface},
                (proxy, method, args) -> {
                    if ("allowSendChatMessage".equals(method.getName()) && args != null && args.length > 0) {
                        String message = (String) args[0];
                        if (message != null && message.startsWith("!")) {
                            us.rin.commands.CommandManager.getManager().handleCommand(message);
                            return false;
                        }
                    }
                    return true;
                }
            );

            allowChatEvent.getClass().getMethod("register", Object.class).invoke(allowChatEvent, listener);
            System.out.println("[RED SIGNAL CLIENT] Chat command interceptor registered successfully!");
        } catch (Throwable t) {
            System.out.println("[RED SIGNAL CLIENT] Notice registering chat listener: " + t.getMessage());
        }
    }
}
