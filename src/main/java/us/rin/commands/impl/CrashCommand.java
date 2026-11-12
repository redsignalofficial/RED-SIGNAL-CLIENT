package us.rin.commands.impl;

import net.minecraft.client.Minecraft;
import us.rin.commands.Command;
import us.rin.commands.CommandManager;
import us.rin.crashers.CrashManager;
import us.rin.exploits.ExploitManager;

public class CrashCommand implements Command {
    private static final String COMMAND_NAME = "crash";
    private final Minecraft mc = Minecraft.getInstance();
    private final CrashManager crashManager = CrashManager.getManager();

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public void onCommand(String[] args) {
        if(mc.player != null && mc.getConnection() != null){
            StringBuilder builder = new StringBuilder();
            for (String arg : args) builder.append(arg).append(" ");
            crashManager.handleMethod(CommandManager.COMMAND_PREFIX + builder);
        }
    }
}
