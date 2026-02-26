package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.client.data.model.WildexMiscData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexMiscCache {

    private static final Map<ResourceLocation, WildexMiscData> CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> REQUESTED = ConcurrentHashMap.newKeySet();

    private WildexMiscCache() {
    }

    public static void clear() {
        CACHE.clear();
        REQUESTED.clear();
    }

    public static void set(
            ResourceLocation mobId,
            boolean ownable,
            java.util.List<ResourceLocation> breedingItemIds,
            java.util.List<ResourceLocation> tamingItemIds
    ) {
        if (mobId == null) return;
        CACHE.put(
                mobId,
                new WildexMiscData(
                        ownable,
                        breedingItemIds == null ? java.util.List.of() : java.util.List.copyOf(breedingItemIds),
                        tamingItemIds == null ? java.util.List.of() : java.util.List.copyOf(tamingItemIds)
                )
        );
    }

    public static WildexMiscData getOrRequest(ResourceLocation mobId) {
        if (mobId == null) return WildexMiscData.empty();
        WildexMiscData v = CACHE.get(mobId);
        if (v != null) return v;

        if (REQUESTED.add(mobId)) {
            WildexNetworkClient.requestBreedingForSelected(mobId.toString());
        }
        return WildexMiscData.empty();
    }
}
