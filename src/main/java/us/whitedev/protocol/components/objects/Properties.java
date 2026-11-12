package us.whitedev.protocol.components.objects;

import io.netty.buffer.ByteBuf;
import us.whitedev.protocol.packets.PacketCodec;

public class Properties {

    private final int amplifier;
    private final int duration;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final Properties hiddenEffect;

    public Properties(
            int amplifier, int duration, boolean ambient, boolean showParticles,
            boolean showIcon, Properties hiddenEffect
    ) {
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
    }

    public static void write(ByteBuf buf, Properties prop) {
        PacketCodec.writeVarInt(buf, prop.amplifier);
        PacketCodec.writeVarInt(buf, prop.duration);
        buf.writeBoolean(prop.ambient);
        buf.writeBoolean(prop.showParticles);
        buf.writeBoolean(prop.showIcon);
        PacketCodec.writeNullable(buf, prop.hiddenEffect, Properties::write);
    }
}
