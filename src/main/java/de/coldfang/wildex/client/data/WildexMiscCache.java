package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.client.data.model.WildexMiscData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexMiscCache {

    private static final long REQUEST_RETRY_MS = 750L;
    private static final Map<ResourceLocation, WildexMiscData> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Long> REQUESTED_AT = new ConcurrentHashMap<>();

    private WildexMiscCache() {
    }

    public static void clear() {
        CACHE.clear();
        REQUESTED_AT.clear();
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
                        tamingItemIds == null ? java.util.List.of() : java.util.List.copyOf(tamingItemIds),
                        true
                )
        );
        REQUESTED_AT.remove(mobId);
    }

    public static WildexMiscData get(ResourceLocation mobId) {
        if (mobId == null) return WildexMiscData.empty();
        WildexMiscData v = CACHE.get(mobId);
        return v == null ? WildexMiscData.empty() : v;
    }

    public static void getOrRequest(ResourceLocation mobId) {
        if (mobId == null) return;

        WildexMiscData cached = CACHE.get(mobId);
        if (cached != null) return;

        long now = System.currentTimeMillis();
        Long requestedAt = REQUESTED_AT.get(mobId);
        if (requestedAt == null || (now - requestedAt) >= REQUEST_RETRY_MS) {
            REQUESTED_AT.put(mobId, now);
            WildexNetworkClient.requestBreedingForSelected(mobId.toString());
        }
    }
}
