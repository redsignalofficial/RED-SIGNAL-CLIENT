package us.whitedev.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import us.whitedev.protocol.packets.Packet;
import us.whitedev.protocol.packets.PacketCodec;

public class PacketServerboundChunkBatchReceived implements Packet {

    private static final int PACKET_ID = 0x0A;

    private final float chunksPerTick;

    public PacketServerboundChunkBatchReceived(float chunksPerTick) {
        this.chunksPerTick = chunksPerTick;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, PACKET_ID);
        buf.writeFloat(chunksPerTick);
    }
}
