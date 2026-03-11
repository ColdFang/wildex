package de.coldfang.wildex.client.data.model;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record WildexMiscData(
        boolean ownable,
        List<ResourceLocation> breedingItemIds,
        List<ResourceLocation> tamingItemIds,
        boolean loaded
) {
    public static WildexMiscData empty() {
        return new WildexMiscData(false, List.of(), List.of(), false);
    }

    public static WildexMiscData loading() {
        return new WildexMiscData(false, List.of(), List.of(), false);
    }

    public boolean isLoading() {
        return !loaded;
    }
}
