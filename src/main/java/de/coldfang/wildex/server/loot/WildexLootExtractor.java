package de.coldfang.wildex.server.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WildexLootExtractor {

    private WildexLootExtractor() {
    }

    public record LootDropSummary(
            String itemId,
            int minCountSeen,
            int maxCountSeen,
            int timesSeen,
            int samples
    ) {
    }

    public static List<LootDropSummary> sampleEntityLoot(ServerLevel level, EntityType<?> type, int samples) {
        return sampleEntityLoot(level, null, type, samples);
    }

    public static List<LootDropSummary> sampleEntityLoot(ServerLevel level, ServerPlayer looter, EntityType<?> type, int samples) {
        if (level == null || type == null) return List.of();
        if (samples <= 0) samples = 1;

        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return List.of();

        entity.setPos(0.0, 0.0, 0.0);

        LootTable table = resolveLootTable(level.getServer(), type.getDefaultLootTable());
        if (table == LootTable.EMPTY) return List.of();

        DamageSource src = (looter != null)
                ? level.damageSources().playerAttack(looter)
                : level.damageSources().generic();

        LootParams.Builder b = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, src);

        if (looter != null) {
            b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, looter);
            b.withParameter(LootContextParams.ATTACKING_ENTITY, looter);
            b.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, looter);
            b.withLuck(looter.getLuck());
        }

        LootParams params = b.create(LootContextParamSets.ENTITY);

        Map<String, Stat> stats = new HashMap<>();

        for (int i = 0; i < samples; i++) {
            List<ItemStack> drops = new ArrayList<>();
            table.getRandomItems(params, drops::add);

            for (ItemStack st : drops) {
                if (st == null || st.isEmpty()) continue;

                ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(st.getItem());
                String id = itemKey.toString();

                Stat s = stats.computeIfAbsent(id, k -> new Stat());
                s.timesSeen++;
                s.min = Math.min(s.min, st.getCount());
                s.max = Math.max(s.max, st.getCount());
            }
        }

        int finalSamples = samples;
        List<LootDropSummary> out = new ArrayList<>(stats.size());
        for (Map.Entry<String, Stat> e : stats.entrySet()) {
            Stat s = e.getValue();
            int min = (s.min == Integer.MAX_VALUE) ? 0 : s.min;
            out.add(new LootDropSummary(e.getKey(), min, s.max, s.timesSeen, finalSamples));
        }

        out.sort(Comparator
                .comparingInt(LootDropSummary::timesSeen).reversed()
                .thenComparing(LootDropSummary::itemId));

        return List.copyOf(out);
    }

    private static LootTable resolveLootTable(MinecraftServer server, Optional<ResourceKey<LootTable>> key) {
        if (server == null || key == null || key.isEmpty()) return LootTable.EMPTY;
        return resolveLootTable(server, key.get());
    }

    private static LootTable resolveLootTable(MinecraftServer server, ResourceKey<LootTable> key) {
        if (server == null || key == null) return LootTable.EMPTY;

        try {
            LootTable table = server.reloadableRegistries().getLootTable(key);
            return table == null ? LootTable.EMPTY : table;
        } catch (Throwable t) {
            return LootTable.EMPTY;
        }
    }

    private static final class Stat {
        int timesSeen = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
    }
}
