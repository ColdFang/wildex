package de.coldfang.wildex.client.data.model;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record WildexMiscData(
        boolean ownable,
        List<ResourceLocation> breedingItemIds
) {
    public static WildexMiscData empty() {
        return new WildexMiscData(false, List.of());
    }
}
