package de.coldfang.wildex.client.data;

import de.coldfang.wildex.network.S2CMobLootPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WildexLootCache {

    private static final Map<ResourceLocation, List<S2CMobLootPayload.LootLine>> CACHE = new HashMap<>();

    private WildexLootCache() {
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void set(ResourceLocation mobId, List<S2CMobLootPayload.LootLine> lines) {
        if (mobId == null) return;
        CACHE.put(mobId, lines == null ? List.of() : List.copyOf(lines));
    }

    public static List<S2CMobLootPayload.LootLine> get(ResourceLocation mobId) {
        if (mobId == null) return List.of();
        List<S2CMobLootPayload.LootLine> v = CACHE.get(mobId);
        return v == null ? List.of() : v;
    }
}
