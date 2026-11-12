package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import us.rin.protocol.components.ComponentsCodec;
import us.rin.protocol.components.data.DataComponent;
import us.rin.protocol.components.objects.ItemStack;
import us.rin.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataCustomName implements DataComponent {
    private final CompoundTag compoundTag;

    public DataCustomName(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 5, 767, 5,
                768, 5, 769, 5,
                770, 5
        );
    }

    @Override
    public void write(ByteBuf buf) {
        ComponentsCodec.writeTag(buf, compoundTag);
    }
}
