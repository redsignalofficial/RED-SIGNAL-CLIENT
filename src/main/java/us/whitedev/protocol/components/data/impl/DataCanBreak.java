package us.whitedev.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import us.whitedev.protocol.components.ComponentsCodec;
import us.whitedev.protocol.components.data.DataComponent;
import us.whitedev.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataCanBreak implements DataComponent {

    private final List<BlockPredicate> predicates;
    private final boolean showInTooltip;

    public DataCanBreak(List<BlockPredicate> predicates) {
        this(predicates, true);
    }

    public DataCanBreak(List<BlockPredicate> predicates, boolean showInTooltip) {
        this.predicates = predicates;
        this.showInTooltip = showInTooltip;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 11, 767, 11,
                768, 12, 769, 12
        );
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, predicates.size());

        for (BlockPredicate predicate : predicates) {
            writePredicate(buf, predicate);
        }
        buf.writeBoolean(showInTooltip);
    }

    private static void writePredicate(ByteBuf buf, BlockPredicate predicate) {
        if (predicate.blocks() != null && !predicate.blocks().isEmpty()) {
            buf.writeBoolean(true);
            PacketCodec.writeVarInt(buf, predicate.blocks().size());
            PacketCodec.writeVarInt(buf, 1);
        } else {
            buf.writeBoolean(false);
        }

        if (predicate.state() != null && !predicate.state().isEmpty()) {
            buf.writeBoolean(true);
            PacketCodec.writeVarInt(buf, predicate.state().size());
            for (Map.Entry<String, String> entry : predicate.state().entrySet()) {
                PacketCodec.writeUtf(buf, entry.getKey());
                buf.writeBoolean(true);
                PacketCodec.writeUtf(buf, entry.getValue());
            }
        } else {
            buf.writeBoolean(false);
        }

        if (predicate.nbt() != null) {
            buf.writeBoolean(true);
            ComponentsCodec.writeTag(buf, predicate.nbt());
        } else {
            buf.writeBoolean(false);
        }
    }

    public record BlockPredicate(List<String> blocks, CompoundTag nbt, Map<String, String> state) {
        public BlockPredicate(List<String> blocks) {
            this(blocks, null, null);
        }
        public BlockPredicate(List<String> blocks, Map<String, String> state) {
            this(blocks, null, state);
        }
    }
}