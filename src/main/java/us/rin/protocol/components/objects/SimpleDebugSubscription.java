package us.rin.protocol.components.objects;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.packets.PacketCodec;

public class SimpleDebugSubscription implements DebugSubscription<String> {

    private final int id;

    public SimpleDebugSubscription(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void write(ByteBuf buf, String value) {
        PacketCodec.writeUtf(buf, value);
    }
}
