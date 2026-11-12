package us.rin.crashers;

import net.minecraft.client.Minecraft;
import us.rin.helpers.BypassHelper;
import us.rin.helpers.MessageHelper;
import us.rin.utils.OptionUtil;

import java.util.List;

public interface Crasher {
    String getName();
    void onMethod(String[] args);
    default boolean getEnabled() { return false; }
    default void setEnabled(boolean bool) {}
    String getArgsUsage();
    String getDescription();
    List<OptionUtil> getOptions();

    MessageHelper msgHelper = new MessageHelper();
    BypassHelper bypassHelper = new BypassHelper();
    Minecraft mc = Minecraft.getInstance();
}