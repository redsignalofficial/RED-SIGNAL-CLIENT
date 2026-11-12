package us.whitedev.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import us.whitedev.protocol.components.ComponentsCodec;
import us.whitedev.protocol.components.data.DataComponent;
import us.whitedev.protocol.components.objects.ItemStack;

import java.util.Map;

public class DataUseRemainder implements DataComponent {
    private final ItemStack remainder;

    public DataUseRemainder(ItemStack remainder) {
        this.remainder = remainder;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 23, 767, 23,
                768, 23, 769, 23,
                770, 23
        );
    }


    @Override
    public void write(ByteBuf buf) {
        ComponentsCodec.writeItem(buf, remainder);
    }
}