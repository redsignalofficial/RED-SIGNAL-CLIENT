package us.whitedev.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.Tag;
import us.whitedev.protocol.packets.Packet;
import us.whitedev.protocol.packets.PacketCodec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;

public class PacketCustomClick implements Packet {
    private static final int PACKET_ID = 65;

    private final String resource;
    private final Tag nbt;

    public PacketCustomClick(String resource, Tag nbt) {
        this.resource = resource;
        this.nbt = nbt;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, PACKET_ID);
        PacketCodec.writeUtf(buf, resource);
        writeNbtMap(buf, nbt);
    }

    public static <T> void writeLengthPrefixed(ByteBuf buf, T value, BiConsumer<DataOutputStream, T> writer) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            writer.accept(dos, value);
            dos.flush();

            byte[] bytes = baos.toByteArray();
            int length = bytes.length;

            PacketCodec.writeVarInt(buf, length);
            buf.writeBytes(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeNbtMap(ByteBuf buf, Tag nbt) {
        writeLengthPrefixed(buf, nbt, (dos, tag) -> {
            try {
                if (tag == null) {
                    dos.writeByte(0);
                } else {
                    dos.writeByte(10);
                    tag.write(dos);
                }
            }catch (Exception ignored){}
        });
    }
}
