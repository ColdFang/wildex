package de.coldfang.wildex.server.loot;

import de.coldfang.wildex.util.WildexMobFilters;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WildexAnalyzerLootIndex {

    private static final int SAMPLES_PER_MOB = 72;

    private static final Map<ResourceLocation, Set<ResourceLocation>> MOB_TO_ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> ITEM_TO_MOBS = new HashMap<>();

    private static List<ResourceLocation> scanMobIds = List.of();
    private static int scanCursor = 0;
    private static boolean initialized = false;
    private static boolean complete = false;

    private WildexAnalyzerLootIndex() {
    }

    public static synchronized void clear() {
        MOB_TO_ITEMS.clear();
        ITEM_TO_MOBS.clear();
        scanMobIds = List.of();
        scanCursor = 0;
        initialized = false;
        complete = false;
    }

    public static synchronized Resolution resolve(ServerLevel level, ResourceLocation itemId, int scanBudgetMobs) {
        if (level == null || itemId == null) return Resolution.EMPTY_COMPLETE;
        process(level, scanBudgetMobs);
        List<ResourceLocation> mobs = ITEM_TO_MOBS.get(itemId);
        if (mobs == null || mobs.isEmpty()) {
            return new Resolution(List.of(), complete);
        }
        return new Resolution(List.copyOf(mobs), complete);
    }

    public static synchronized void process(ServerLevel level, int scanBudgetMobs) {
        if (level == null) return;
        if (scanBudgetMobs <= 0) return;

        ensureInitialized();
        if (complete) return;

        int budget = scanBudgetMobs;
        while (budget-- > 0 && scanCursor < scanMobIds.size()) {
            ResourceLocation mobId = scanMobIds.get(scanCursor++);
            if (mobId == null) continue;
            if (MOB_TO_ITEMS.containsKey(mobId)) continue;

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
            if (type == null) {
                MOB_TO_ITEMS.put(mobId, Set.of());
                continue;
            }

            Set<ResourceLocation> itemIds = collectMobLootItemIds(level, type);
            MOB_TO_ITEMS.put(mobId, itemIds);
            if (itemIds.isEmpty()) continue;

            for (ResourceLocation itemId : itemIds) {
                ITEM_TO_MOBS.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(mobId);
            }
        }

        if (scanCursor >= scanMobIds.size()) {
            finalizeIndex();
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;

        ArrayList<ResourceLocation> ids = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == null) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!WildexMobFilters.isTrackable(id)) continue;
            ids.add(id);
        }
        Collections.sort(ids);
        scanMobIds = List.copyOf(ids);
        scanCursor = 0;
        initialized = true;
        complete = scanMobIds.isEmpty();
    }

    private static void finalizeIndex() {
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> e : ITEM_TO_MOBS.entrySet()) {
            List<ResourceLocation> list = e.getValue();
            if (list == null || list.isEmpty()) {
                e.setValue(List.of());
                continue;
            }

            ArrayList<ResourceLocation> sorted = new ArrayList<>(list);
            Collections.sort(sorted);
            e.setValue(List.copyOf(sorted));
        }
        complete = true;
    }

    private static Set<ResourceLocation> collectMobLootItemIds(ServerLevel level, EntityType<?> type) {
        List<WildexLootExtractor.LootDropSummary> sampled = WildexLootExtractor.sampleEntityLoot(level, type, SAMPLES_PER_MOB);
        if (sampled.isEmpty()) return Set.of();

        HashSet<ResourceLocation> out = new HashSet<>();
        for (WildexLootExtractor.LootDropSummary row : sampled) {
            if (row == null) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(row.itemId());
            if (itemId == null) continue;

            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) continue;
            out.add(itemId);
        }
        if (out.isEmpty()) return Set.of();
        return Set.copyOf(out);
    }

    public record Resolution(List<ResourceLocation> mobIds, boolean complete) {
        private static final Resolution EMPTY_COMPLETE = new Resolution(List.of(), true);
    }
}
