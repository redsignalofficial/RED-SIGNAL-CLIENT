package us.rin.utils;

import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.arikia.dev.drpc.DiscordRichPresence.Builder;
import net.arikia.dev.drpc.DiscordUser;
import net.arikia.dev.drpc.callbacks.ReadyCallback;
import net.minecraft.client.Minecraft;
import us.rin.RedSignalClient;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DiscordRP implements ReadyCallback {
    public static boolean rpSwitch = true;
    private static final Minecraft mc = Minecraft.getInstance();
    DiscordRichPresence richPresence = (new Builder("Client Initialization")).setBigImage("logo", "RedSignalClient " + RedSignalClient.VERSION)
            .setDetails("Loading...")
            .setStartTimestamps(System.currentTimeMillis())
            .build();
    private boolean enabled = true;

    public void runDiscordRP() {
        try {
            this.init();
            this.startTask();
            DiscordRPC.discordUpdatePresence(this.richPresence);
        } catch (Throwable t) {
            System.out.println("Failed to initialize DiscordRichPresence API: " + t.getMessage());
            rpSwitch = false;
            enabled = false;
        }
    }

    public void apply(DiscordUser discordUser) {
        System.out.println("Initialized DiscordRichPresence API.");
    }

    private void init() {
        try {
            DiscordEventHandlers handlers = (new DiscordEventHandlers.Builder()).setReadyEventHandler((user) -> System.out.printf("Connected to %s#%s (%s)%n", user.username, user.discriminator, user.userId))
                    .build();
            DiscordRPC.discordInitialize("965944857870868480", handlers, true);
        } catch (Throwable t) {
            rpSwitch = false;
            enabled = false;
        }
    }

    public void startTask() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            try {
                if (rpSwitch) {
                    if (!enabled) {
                        DiscordEventHandlers handlers = (new DiscordEventHandlers.Builder()).setReadyEventHandler((user) -> System.out.printf("Connected to %s#%s (%s)%n", user.username, user.discriminator, user.userId))
                                .build();
                        DiscordRPC.discordInitialize("965944857870868480", handlers, true);
                        enabled = true;
                    }

                    this.richPresence.details = mc.player == null ? "Disconnected" : "Connected";
                    this.richPresence.state = "Discord: https://discord.gg/WK5vaag3hh";
                    DiscordRPC.discordUpdatePresence(this.richPresence);
                } else {
                    DiscordRPC.discordShutdown();
                    enabled = false;
                }
            } catch (Throwable ignored) {
                rpSwitch = false;
                enabled = false;
            }
        }, 10L, 10L, TimeUnit.SECONDS);
    }
}


