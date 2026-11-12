package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.components.data.DataComponent;
import us.rin.protocol.components.objects.PotionEffect;
import us.rin.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataPotionContent implements DataComponent {
    private final int protocolId;
    private final int potionId;
    private final int customColor;
    private final List<PotionEffect> customEffects;
    private final String customName;

    public DataPotionContent(int protocolId, int potionId, int customColor, List<PotionEffect> customEffects, String customName) {
        this.protocolId = protocolId;
        this.potionId = potionId;
        this.customColor = customColor;
        this.customEffects = customEffects;
        this.customName = customName;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 31, 767, 31,
                768, 41, 769, 41,
                770, 41
        );
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeNullable(buf, potionId, PacketCodec::writeVarInt);
        PacketCodec.writeNullable(buf, customColor, (i, i2) -> buf.writeInt(i2));
        PacketCodec.writeList(buf, customEffects, PotionEffect::write);
        if(protocolId >= 768){
            PacketCodec.writeNullable(buf, customName, PacketCodec::writeUtf);
        }
    }

}

