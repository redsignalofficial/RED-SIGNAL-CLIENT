package us.rin.protocol.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import us.rin.helpers.MessageHelper;
import us.rin.protocol.RedSignalProtocol;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class PacketCodec {
    private static final RedSignalProtocol redSignalProtocol = new RedSignalProtocol();
    private static final MessageHelper msgHelper = new MessageHelper();

    public static void sendPacket(Packet packet, int packetAmount) {
        ByteBuf buf = Unpooled.buffer();
        packet.write(buf);

        try {
            List<ByteBuf> buffs = new ArrayList<>();
            for (int i = 0; i < packetAmount; i++)
                buffs.add(buf.copy());

            redSignalProtocol.addPacketsToSend(buffs);
        }catch (OutOfMemoryError e){
            final int BATCH_SIZE = 100;

            msgHelper.sendMessage("&cOOM was detected&7, changing the buf writer system to async!", true);

            CompletableFuture.runAsync(() -> {
                ByteBuf buf2 = Unpooled.buffer();
                packet.write(buf2);

                try {
                    for (int processed = 0; processed < packetAmount; processed += BATCH_SIZE) {
                        int currentBatchSize = Math.min(BATCH_SIZE, packetAmount - processed);
                        List<ByteBuf> buffs = new ArrayList<>(currentBatchSize);

                        for (int i = 0; i < currentBatchSize; i++)
                            buffs.add(buf2.copy());

                        redSignalProtocol.addPacketsToSend(buffs);

                        if (processed % (BATCH_SIZE * 5) == 0) System.gc();
                    }
                } finally {
                    buf2.release();
                }
            });
        }

        buf.release();
    }


    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static void writeUtf(ByteBuf buf, String string) {
        byte[] abyte = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, abyte.length);
        buf.writeBytes(abyte);
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static <T> void writeNullable(ByteBuf buf, @Nullable T value, BiConsumer<ByteBuf, T> ifPresent) {
        if (value != null) {
            buf.writeBoolean(true);
            ifPresent.accept(buf, value);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static void writeArray(ByteBuf buf, int[] array) {
        PacketCodec.writeVarInt(buf, array.length + 1);
        for (int holder : array) {
            PacketCodec.writeVarInt(buf, holder);
        }
    }

    public static <T> void writeList(ByteBuf buf, List<T> value, BiConsumer<ByteBuf, T> writer) {
        PacketCodec.writeVarInt(buf, value.size());
        for (T t : value) {
            writer.accept(buf, t);
        }
    }

    public static <K> void writeSet(ByteBuf buf, Set<K> set, BiConsumer<ByteBuf, K> writer) {
        PacketCodec.writeVarInt(buf, set.size());
        for (K key : set) {
            writer.accept(buf, key);
        }
    }
}
