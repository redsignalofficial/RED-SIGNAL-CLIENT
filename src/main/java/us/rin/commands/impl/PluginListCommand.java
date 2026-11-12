package us.rin.commands.impl;

import us.rin.commands.Command;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import com.mojang.brigadier.suggestion.Suggestion;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PluginListCommand implements Command {

    private static final String NAME = "pluginlist";
    private static final List<String> COMMANDS = Arrays.asList("/plugins ", "/pl ", "/ver ", "/version ", "/bukkit:plugins ", "/", "/? ");
    private static final Set<String> found = new HashSet<>();
    private static final Set<Integer> pendingIds = new HashSet<>();
    private static int requestId = 1000;
    private static Consumer<Set<String>> finishCallback;
    private static boolean active = false;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void onCommand(String[] args) {
        if (active) return;
        scanPlugins().thenAccept(plugins -> {
            if (plugins == null || plugins.isEmpty()) return;
            showPlugins(plugins);
        });
    }

    public static CompletableFuture<Set<String>> scanPlugins() {
        msgHelper.sendMessage("&7Checking plugins...", true);
        CompletableFuture<Set<String>> future = new CompletableFuture<>();
        if (active) {
            future.complete(Collections.emptySet());
            return future;
        }
        if (mc.getConnection() == null) {
            future.complete(Collections.emptySet());
            return future;
        }

        active = true;
        found.clear();
        pendingIds.clear();
        finishCallback = future::complete;

        for (String cmd : COMMANDS) {
            int id = ++requestId;
            pendingIds.add(id);
            mc.getConnection().send(new ServerboundCommandSuggestionPacket(id, cmd));
        }

        if (pendingIds.isEmpty()) {
            active = false;
            finishCallback = null;
            future.complete(Collections.emptySet());
        }
        return future;
    }

    public static void handleSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!active) return;

        if (!pendingIds.remove(packet.getId())) {
            return;
        }

        for (Suggestion s : packet.getSuggestions().getList()) {
            String value = s.getText().trim();
            if (value.matches("[A-Za-z0-9_\\-]{2,}") && !value.matches("plugins|pl|help|version|ver|bukkit")) {
                found.add(value);
            }
        }

        if (pendingIds.isEmpty()) {
            active = false;
            Consumer<Set<String>> cb = finishCallback;
            finishCallback = null;
            if (cb != null) cb.accept(new HashSet<>(found));
        }
    }

    private static void showPlugins(Set<String> plugins) {
        if (plugins == null || plugins.isEmpty()) return;

        msgHelper.sendSeparateLine();
        msgHelper.sendMessage("&7: &f" + plugins.size(), true);
        msgHelper.sendSeparateLine();

        List<String> list = new ArrayList<>(plugins);
        Collections.sort(list);

        for (String plugin : list) {
            msgHelper.sendMessage("&a" + plugin, false);
        }

        msgHelper.sendSeparateLine();
    }
}