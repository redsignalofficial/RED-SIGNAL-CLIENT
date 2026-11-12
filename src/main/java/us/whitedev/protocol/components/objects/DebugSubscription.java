package us.whitedev.protocol.components.objects;

import io.netty.buffer.ByteBuf;
import us.whitedev.protocol.packets.PacketCodec;

import javax.annotation.Nullable;

public interface DebugSubscription<T> {

    int getId();

    void write(ByteBuf buf, T value);

    final class Event<T> {

        private final DebugSubscription<T> subscription;
        private final T value;

        public Event(DebugSubscription<T> subscription, T value) {
            this.subscription = subscription;
            this.value = value;
        }

        public static <T> void write(ByteBuf buf, Event<T> event) {
            PacketCodec.writeVarInt(buf, event.subscription.getId());
            event.subscription.write(buf, event.value);
        }
    }

    final class Update<T> {

        private final DebugSubscription<T> subscription;
        private final @Nullable T value;

        public Update(DebugSubscription<T> subscription, @Nullable T value) {
            this.subscription = subscription;
            this.value = value;
        }

        public static <T> void write(ByteBuf buf, Update<T> update) {
            PacketCodec.writeVarInt(buf, update.subscription.getId());
            PacketCodec.writeNullable(buf, update.value, update.subscription::write);
        }
    }
}