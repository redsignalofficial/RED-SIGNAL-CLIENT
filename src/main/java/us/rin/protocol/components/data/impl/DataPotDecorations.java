package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.components.data.DataComponent;
import us.rin.protocol.packets.PacketCodec;

import java.util.List;
import java.util.Map;

public class DataPotDecorations implements DataComponent {
    private final List<Integer> idList;

    public DataPotDecorations(List<Integer> idList) {
        this.idList = idList;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 50, 767, 51,
                768, 61, 769, 61,
                770, 61
        );
    }

    @Override
    public void write(ByteBuf buf) {
        PacketCodec.writeVarInt(buf, idList.size());
        idList.forEach(id -> PacketCodec.writeVarInt(buf, id));
    }
}
