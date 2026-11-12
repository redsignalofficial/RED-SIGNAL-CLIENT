package us.whitedev.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import us.whitedev.protocol.components.objects.DebugSubscription;
import us.whitedev.protocol.packets.Packet;
import us.whitedev.protocol.packets.PacketCodec;

import java.util.Set;

public class PacketDebugSubscriptionRequest implements Packet {

    private static final int PACKET_ID = 0x16;

    private final Set<DebugSubscription<?>> subscriptions;

    public PacketDebugSubscriptionRequest(Set<DebugSubscription<?>> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, PACKET_ID);

        PacketCodec.writeSet(buf, subscriptions, (b, subscription) ->
                PacketCodec.writeVarInt(b, subscription.getId())
        );
    }
}