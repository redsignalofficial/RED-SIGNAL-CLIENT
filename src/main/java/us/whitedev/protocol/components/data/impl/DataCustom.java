package us.whitedev.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import us.whitedev.protocol.components.ComponentsCodec;
import us.whitedev.protocol.components.data.DataComponent;
import us.whitedev.protocol.components.objects.ArmorMaterial;
import us.whitedev.protocol.components.objects.ArmorPattern;
import us.whitedev.protocol.packets.PacketCodec;

import java.util.Map;

public class DataCustom implements DataComponent {
    private final Tag tag;

    public DataCustom(Tag tag) {
        this.tag = tag;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 0, 767, 0,
                768, 0, 769, 0,
                770, 0
        );
    }

    @Override
    public void write(ByteBuf buf) {
        ComponentsCodec.writeTag(buf, tag);
    }
}