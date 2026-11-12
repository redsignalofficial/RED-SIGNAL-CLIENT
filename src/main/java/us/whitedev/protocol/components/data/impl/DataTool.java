package us.whitedev.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.NonNull;
import us.whitedev.protocol.components.data.DataComponent;
import us.whitedev.protocol.packets.PacketCodec;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class DataTool implements DataComponent {
    private final List<Rule> rules;
    private final float defaultMiningSpeed;
    private final int damagePerBlock;

    public DataTool(List<Rule> rules, float defaultMiningSpeed, int damagePerBlock) {
        this.rules = rules;
        this.defaultMiningSpeed = defaultMiningSpeed;
        this.damagePerBlock = damagePerBlock;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 22, 767, 22,
                768, 26, 769, 26,
                770, 26
        );
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeList(buf, rules, (output, rule) -> {
            PacketCodec.writeArray(output, rule.blocks());
            PacketCodec.writeNullable(output, rule.speed(), ByteBuf::writeFloat);
            PacketCodec.writeNullable(output, rule.correctForDrops(), ByteBuf::writeBoolean);
        });

        buf.writeFloat(defaultMiningSpeed);
        PacketCodec.writeVarInt(buf, damagePerBlock);
    }

    public record Rule(int @NonNull [] blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {}
}
