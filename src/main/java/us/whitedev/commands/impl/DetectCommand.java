package us.whitedev.commands.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.SharedSuggestionProvider;
import us.whitedev.commands.Command;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DetectCommand implements Command {
    private static final String COMMAND_NAME = "detect";
    private final Map<String, String[]> knownAntiCrashers = Map.of(
            "LPX", new String[]{"lpx"},
            "ExploitFixer", new String[]{"ef", "exploitfixer"},
            "SpigotGuard", new String[]{"sg", "spigotguard"},
            "Sierra", new String[]{"sierra"},
            "IceGuard", new String[]{"iceguard"},
            "Honey", new String[]{"honey"},
            "ServerPatches", new String[]{"spatches"}
    );

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public void onCommand(String[] args) {
        CompletableFuture<Suggestions> tabSuggestions = getTabResponse();
        CompletableFuture<Set<String>> scannedPlugins = PluginListCommand.scanPlugins();

        CompletableFuture.allOf(tabSuggestions, scannedPlugins).thenRun(() -> {
            Set<String> candidates = new HashSet<>();

            try {
                Suggestions suggestions = tabSuggestions.getNow(null);
                if (suggestions != null) {
                    for (Suggestion sug : suggestions.getList()) {
                        String v = sug.getText();
                        if (v != null && !v.isBlank()) candidates.add(v.trim());
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                Set<String> plugins = scannedPlugins.getNow(Collections.emptySet());
                if (plugins != null) {
                    for (String p : plugins) {
                        if (p != null && !p.isBlank()) candidates.add(p.trim());
                    }
                }
            } catch (Exception ignored) {
            }

            List<String> detected = new ArrayList<>();
            for (Map.Entry<String, String[]> entry : knownAntiCrashers.entrySet()) {
                String name = entry.getKey();
                for (String alias : entry.getValue()) {
                    for (String candidate : candidates) {
                        if (candidate.equalsIgnoreCase(alias)) {
                            addIfDoesntExist(name, detected);
                        }
                    }
                }
            }

            if (!detected.isEmpty()) {
                msgHelper.sendMessage("Detected &8(&f" + detected.size() + "&8) &7AntiCrash Plugin" + (detected.size() > 1 ? "s" : ""), true);
                detected.forEach(n -> msgHelper.sendMessage("&8- &f" + n, true));
            } else {
                msgHelper.sendMessage("No AntiCrash detected!", true);
            }
        });
    }

    private CompletableFuture<Suggestions> getTabResponse() {
        CommandDispatcher<SharedSuggestionProvider> commanddispatcher = mc.player.connection.getCommands();
        ParseResults<SharedSuggestionProvider> currentParse = commanddispatcher.parse(new StringReader(""), mc.player.connection.getSuggestionsProvider());
        return commanddispatcher.getCompletionSuggestions(currentParse, 0);
    }

    private void addIfDoesntExist(String name, List<String> list) {
        if (!list.contains(name)) list.add(name);
    }
}
