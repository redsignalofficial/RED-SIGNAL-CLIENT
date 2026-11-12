package us.rin.protocol.components.data.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import us.rin.protocol.components.ComponentsCodec;
import us.rin.protocol.components.data.DataComponent;

import java.util.Map;

public class DataContainerLoot implements DataComponent {

    private final String lootTable;
    private final Long seed;

    public DataContainerLoot(String lootTable, Long seed) {
        this.lootTable = lootTable;
        this.seed = seed;
    }

    @Override
    public Map<Integer, Integer> getIds() {
        return Map.of(
                766, 55,
                767, 56,
                768, 66,
                769, 70,
                770, 77
        );
    }

    @Override
    public void write(ByteBuf buf) {
        CompoundTag tag = new CompoundTag();

        if (lootTable != null) {
            tag.putString("loot_table", lootTable);
        }

        if (seed != null) {
            tag.putLong("seed", seed);
        }

        ComponentsCodec.writeTag(buf, tag);
    }
}
