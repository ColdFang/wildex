package de.coldfang.wildex.client.data;

import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WildexSpawnCache {

    private static final Map<ResourceLocation, List<S2CMobSpawnsPayload.DimSection>> CACHE = new HashMap<>();

    private WildexSpawnCache() {
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void set(ResourceLocation mobId, List<S2CMobSpawnsPayload.DimSection> sections) {
        if (mobId == null) return;
        CACHE.put(mobId, sections == null ? List.of() : List.copyOf(sections));
    }

    public static List<S2CMobSpawnsPayload.DimSection> get(ResourceLocation mobId) {
        if (mobId == null) return List.of();
        List<S2CMobSpawnsPayload.DimSection> v = CACHE.get(mobId);
        return v == null ? List.of() : v;
    }
}
