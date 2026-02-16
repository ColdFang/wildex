package de.coldfang.wildex.client.data;

import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WildexSpawnCache {

    private static final Map<ResourceLocation, SpawnData> CACHE = new HashMap<>();

    private WildexSpawnCache() {
    }

    public record SpawnData(
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections
    ) {
        public static SpawnData empty() {
            return new SpawnData(List.of(), List.of());
        }
    }

    public static void clear() {
        CACHE.clear();
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
                        structureSections == null ? List.of() : List.copyOf(structureSections)
                )
        );
    }

    public static SpawnData get(ResourceLocation mobId) {
        if (mobId == null) return SpawnData.empty();
        SpawnData v = CACHE.get(mobId);
        return v == null ? SpawnData.empty() : v;
    }
}
