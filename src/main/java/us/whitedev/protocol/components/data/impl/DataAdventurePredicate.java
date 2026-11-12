package us.whitedev.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import us.whitedev.protocol.components.ComponentsCodec;
import us.whitedev.protocol.components.data.DataComponent;
import us.whitedev.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataAdventurePredicate implements DataComponent {
    private final List<BlockPredicate> predicates;

    public DataAdventurePredicate(List<BlockPredicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 10, 767, 10,
                768, 11, 769, 11,
                770, 11
        );
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, predicates.size());
        for (BlockPredicate predicate : predicates) this.writeBlockPredicate(buf, predicate);
        buf.writeBoolean(true);
    }

    private void writeBlockPredicate(ByteBuf buf, BlockPredicate blockPredicate){
        PacketCodec.writeNullable(buf, blockPredicate.blocksIds(), PacketCodec::writeArray);
        PacketCodec.writeNullable(buf, blockPredicate.properties(), (output, properties) -> {
            PacketCodec.writeVarInt(buf, properties.size());
            for (PropertyMatcher matcher : properties) {
                PacketCodec.writeUtf(buf, matcher.name());
                if (matcher.value() != null) {
                    buf.writeBoolean(true);
                    PacketCodec.writeUtf(buf, matcher.value());
                } else {
                    buf.writeBoolean(false);
                    PacketCodec.writeUtf(buf, matcher.maxValue());
                    PacketCodec.writeUtf(buf, matcher.minValue());
                }
            }
        });

        PacketCodec.writeNullable(buf, blockPredicate.nbt(), ComponentsCodec::writeTag);
    }

    public record BlockPredicate(int[] blocksIds, List<PropertyMatcher> properties, CompoundTag nbt) {}

    public record PropertyMatcher(String name, String value, String minValue, String maxValue) {
        public PropertyMatcher(String name, String value) {
            this(name, value, null, null);
        }

        public PropertyMatcher(String name, String minValue, String maxValue) {
            this(name, null, minValue, maxValue);
        }
    }
}
