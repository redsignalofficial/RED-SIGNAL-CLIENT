package us.rin.protocol.components.objects;

import us.rin.protocol.components.data.DataComponents;

public record ItemStack(int id, int amount, DataComponents dataComponents) {
}