package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexSpawnCache {

    private static final long REQUEST_RETRY_MS = 750L;
    private static final Map<ResourceLocation, SpawnData> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Long> REQUESTED_AT = new ConcurrentHashMap<>();

    private WildexSpawnCache() {
    }

    public record SpawnData(
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections,
            boolean loaded
    ) {
        public static SpawnData empty() {
            return new SpawnData(List.of(), List.of(), false);
        }

        public static SpawnData loading() {
            return new SpawnData(List.of(), List.of(), false);
        }

        public boolean isLoading() {
            return !loaded;
        }
    }

    public static void clear() {
        CACHE.clear();
        REQUESTED_AT.clear();
    }

    public static void set(
            ResourceLocation mobId,
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections
    ) {
        if (mobId == null) return;
        CACHE.put(
                mobId,
                new SpawnData(
                        naturalSections == null ? List.of() : List.copyOf(naturalSections),
                        structureSections == null ? List.of() : List.copyOf(structureSections),
                        true
                )
        );
        REQUESTED_AT.remove(mobId);
    }

    public static SpawnData get(ResourceLocation mobId) {
        if (mobId == null) return SpawnData.empty();
        SpawnData v = CACHE.get(mobId);
        return v == null ? SpawnData.empty() : v;
    }

    public static SpawnData getOrRequest(ResourceLocation mobId) {
        if (mobId == null) return SpawnData.empty();

        SpawnData cached = CACHE.get(mobId);
        if (cached != null) return cached;

        long now = System.currentTimeMillis();
        Long requestedAt = REQUESTED_AT.get(mobId);
        if (requestedAt == null || (now - requestedAt) >= REQUEST_RETRY_MS) {
            REQUESTED_AT.put(mobId, now);
            WildexNetworkClient.requestSpawnsForSelected(mobId.toString());
        }
        return SpawnData.loading();
    }
}
