package us.rin.commands;

import net.minecraft.client.Minecraft;
import us.rin.helpers.BypassHelper;
import us.rin.helpers.MessageHelper;

public interface Command {
    String getName();
    void onCommand(String[] args);

    MessageHelper msgHelper = new MessageHelper();
    BypassHelper bypassHelper = new BypassHelper();
    Minecraft mc = Minecraft.getInstance();
}
