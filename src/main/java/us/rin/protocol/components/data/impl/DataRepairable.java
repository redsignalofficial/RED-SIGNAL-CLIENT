package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import us.rin.protocol.components.data.DataComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataRepairable implements DataComponent {
    private final String tagKey;
    private final List<Integer> idList;

    public DataRepairable(String tagKey, List<Integer> idList) {
        this.tagKey = tagKey;
        this.idList = idList;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                768, 29,
                769, 29,
                770, 29
        );
    }

    @Override
    public void write(ByteBuf buf) {
        MappedEntitySet.write(buf, tagKey, idList);
    }
}
