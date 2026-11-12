package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import us.rin.protocol.components.ComponentsCodec;
import us.rin.protocol.components.data.DataComponent;
import us.rin.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataBlockEntity implements DataComponent {
    private final CompoundTag tags;

    public DataBlockEntity(CompoundTag tags) {
        this.tags = tags;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 37, 767, 37,
                768, 47, 769, 47,
                770, 78
        );
    }

    @Override
    public void write(ByteBuf buf) {
        ComponentsCodec.writeTag(buf, tags);
    }
}
