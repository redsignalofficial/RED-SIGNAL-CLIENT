package us.whitedev.commands.impl;

import us.whitedev.commands.Command;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

public class PlayerListCommand implements Command {

    private static final String COMMAND_NAME = "playerlist";

    private static int playerCount;
    private static int requestId;

    private static boolean active = false;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public void onCommand(String[] args) {
        if (active) return;

        active = true;
        playerCount = 0;

        mc.getConnection().send(new ServerboundCommandSuggestionPacket(++requestId, "/msg "));

        msgHelper.sendSeparateLine();
        msgHelper.sendMessage("&7Fetching player count...", true);
        msgHelper.sendSeparateLine();
    }

    public static void handleSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!active) return;

        Suggestions suggestions = packet.getSuggestions();
        int count = 0;

        for (Suggestion suggestion : suggestions.getList()) {
            String value = suggestion.getText();
            if (value.matches("^[A-Za-z0-9_]{3,16}$")) {
                count++;
            }
        }

        playerCount = count;
        msgHelper.sendMessage("&7Players online: &f" + playerCount, true);

        active = false;
    }
}