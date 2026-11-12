package us.rin.commands.impl;

import us.rin.commands.Command;

public class AuthorsCommand implements Command {
    private static final String COMMAND_NAME = "authors";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public void onCommand(String[] args) {
        msgHelper.sendSeparateLine();
        msgHelper.sendMessage("Author of the client is: &fRin", true);
        msgHelper.sendSeparateLine();
    }
}
