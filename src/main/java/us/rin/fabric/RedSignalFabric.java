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
            System.out.println("[RED SIGNAL CLIENT] Fabric load notice: " + t.getMessage());
        }
    }
}
