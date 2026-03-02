package de.coldfang.wildex.client.data;

import de.coldfang.wildex.network.S2CMobLootPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WildexLootCache {

    private static final Map<ResourceLocation, LootData> CACHE = new HashMap<>();

    private WildexLootCache() {
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void set(
            ResourceLocation mobId,
            List<S2CMobLootPayload.LootLine> lines,
            boolean hasPlayerKillXp,
            int playerKillXpMin,
            int playerKillXpMax
    ) {
        if (mobId == null) return;
        CACHE.put(
                mobId,
                new LootData(
                        lines == null ? List.of() : List.copyOf(lines),
                        hasPlayerKillXp,
                        Math.max(0, playerKillXpMin),
                        Math.max(0, playerKillXpMax)
                )
        );
    }

    public static LootData get(ResourceLocation mobId) {
        if (mobId == null) return LootData.empty();
        LootData v = CACHE.get(mobId);
        return v == null ? LootData.empty() : v;
    }

    public record LootData(
            List<S2CMobLootPayload.LootLine> lines,
            boolean hasPlayerKillXp,
            int playerKillXpMin,
            int playerKillXpMax
    ) {
        public LootData {
            List<S2CMobLootPayload.LootLine> normalizedLines = lines == null ? List.of() : List.copyOf(lines);
            int min = Math.max(0, playerKillXpMin);
            int max = Math.max(min, Math.max(0, playerKillXpMax));
            if (!hasPlayerKillXp) {
                min = 0;
                max = 0;
            }

            lines = normalizedLines;
            playerKillXpMin = min;
            playerKillXpMax = max;
        }

        public static LootData empty() {
            return new LootData(List.of(), false, 0, 0);
        }
    }
}
