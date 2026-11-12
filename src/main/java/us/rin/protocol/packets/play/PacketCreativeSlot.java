package us.rin.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.components.ComponentsCodec;
import us.rin.protocol.components.objects.ItemStack;
import us.rin.protocol.packets.Packet;
import us.rin.protocol.packets.PacketCodec;

import java.util.Map;

public class PacketCreativeSlot implements Packet {
    private static final Map<Integer, Integer> PACKET_IDS = Map.of(
            766, 50,
            767, 50,
            768, 52,
            769, 54,
            770, 54,
            774, 55
    );

    private final int protocolId;
    private final short slot;
    private final ItemStack itemStack;

    public PacketCreativeSlot(int protocolId, short slot, ItemStack itemStack) {
        this.protocolId = protocolId;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, PACKET_IDS.getOrDefault(protocolId, 16));
        buf.writeShort(slot);
        if(protocolId >= 770) {
            ComponentsCodec.writeItemUntrusted(buf, itemStack, true);
        }else{
            ComponentsCodec.writeItem(buf, itemStack);
        }
    }
}
