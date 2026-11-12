package us.rin.protocol.components.objects;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.packets.PacketCodec;

public class PotionEffect {
    private final int typeId;
    private final Properties properties;

    public PotionEffect(int typeId, Properties properties) {
        this.typeId = typeId;
        this.properties = properties;
    }

    public static void write(ByteBuf buf, PotionEffect potionEffect){
        PacketCodec.writeVarInt(buf, potionEffect.typeId);
        Properties.write(buf, potionEffect.properties);
    }

}
