package us.rin.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.components.objects.DebugSubscription;
import us.rin.protocol.components.objects.GameRuleEntry;
import us.rin.protocol.packets.Packet;
import us.rin.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Set;

public class PacketSetGameRule implements Packet {

    private static final int PACKET_ID = 57;

    private final List<GameRuleEntry> gameRuleEntries;

    public PacketSetGameRule(List<GameRuleEntry> gameRuleEntries) {
        this.gameRuleEntries = gameRuleEntries;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, PACKET_ID);

        PacketCodec.writeList(buf, gameRuleEntries, (b, entry) -> {
            PacketCodec.writeUtf(b, entry.gameRule());
            PacketCodec.writeUtf(b, entry.value());
        });
    }
}