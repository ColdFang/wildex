package de.coldfang.wildex.server.loot;

import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class WildexLootExtractor {

    public static final int LOOT_CONDITION_NONE = 0;
    public static final int LOOT_CONDITION_PLAYER_KILL = 1;
    public static final int LOOT_CONDITION_ON_FIRE = 1 << 1;
    public static final int LOOT_CONDITION_SLIME_SIZE = 1 << 2;
    public static final int LOOT_CONDITION_CAPTAIN = 1 << 3;
    public static final int LOOT_CONDITION_FROG_KILL = 1 << 4;
    public static final int LOOT_CONDITION_SHEEP_COLOR = 1 << 5;
    private static final int[] CONDITION_BITS = {
            LOOT_CONDITION_PLAYER_KILL,
            LOOT_CONDITION_ON_FIRE,
            LOOT_CONDITION_SLIME_SIZE,
            LOOT_CONDITION_CAPTAIN,
            LOOT_CONDITION_FROG_KILL,
            LOOT_CONDITION_SHEEP_COLOR
    };
    private static final int VERIFICATION_SAMPLES_PER_PLAN = 600;

    private WildexLootExtractor() {
    }

    public record LootDropSummary(
            String itemId,
            int minCountSeen,
            int maxCountSeen,
            int timesSeen,
            int samples,
            int conditionMask,
            List<Integer> conditionProfiles
    ) {
    }

    public static List<LootDropSummary> sampleEntityLoot(ServerLevel level, EntityType<?> type, int samples) {
        if (level == null || type == null) return List.of();
        if (samples <= 0) samples = 1;

        List<SamplingPlan> plans = buildSamplingPlans(level, type, samples);
        if (plans.isEmpty()) return List.of();

        Map<Integer, List<SamplingPlan>> plansByMask = groupPlansByMask(plans);
        Map<Integer, Integer> samplesByMask = computeSamplesByMask(plansByMask);
        Map<String, Stat> stats = new HashMap<>();

        for (SamplingPlan plan : plans) {
            int s = Math.max(1, plan.samples());
            List<LootDropSummary> out = sampleEntityLoot(
                    level,
                    plan.looter(),
                    plan.attackerType(),
                    type,
                    s,
                    plan.entityConfigurator(),
                    plan.conditionMask()
            );
            mergeInto(stats, out);
        }

        verifyAmbiguousConditions(level, type, plansByMask, samplesByMask, stats);
        applySamplesByMask(stats, samplesByMask);
        int totalSamples = samplesByMask.values().stream().mapToInt(Integer::intValue).sum();
        return mergeSummaries(stats, Math.max(1, totalSamples));
    }

    @SuppressWarnings("unused")
    public static List<LootDropSummary> sampleEntityLoot(ServerLevel level, ServerPlayer looter, EntityType<?> type, int samples) {
        int mask = (looter == null) ? LOOT_CONDITION_NONE : LOOT_CONDITION_PLAYER_KILL;
        return sampleEntityLoot(level, looter, null, type, samples, null, mask);
    }

    private static List<LootDropSummary> sampleEntityLoot(
            ServerLevel level,
            ServerPlayer looter,
            EntityType<?> attackerType,
            EntityType<?> type,
            int samples,
            Consumer<Entity> entityConfigurator,
            int conditionMask
    ) {
        if (level == null || type == null) return List.of();
        if (samples <= 0) samples = 1;

        Entity entity = WildexEntityFactory.tryCreate(type, level);
        if (entity == null) return List.of();
        Entity attacker = null;
        try {
            if (entityConfigurator != null) {
                try {
                    entityConfigurator.accept(entity);
                } catch (Throwable ignored) {
                    // State-aware profile hooks must never break loot extraction.
                }
            }
            applyVanillaLikeDefaultEquipment(entity, type);

            entity.setPos(0.0, 0.0, 0.0);

            // Use the concrete entity loot key (important for state-dependent overrides like sheep color tables).
            ResourceKey<LootTable> lootKey = (entity instanceof LivingEntity living)
                    ? living.getLootTable()
                    : type.getDefaultLootTable();
            LootTable table = resolveLootTable(level.getServer(), lootKey);
            if (table == LootTable.EMPTY) return List.of();

            DamageSource src;
            if (looter != null) {
                src = level.damageSources().playerAttack(looter);
            } else if (attackerType != null) {
                attacker = WildexEntityFactory.tryCreate(attackerType, level);
                if (attacker instanceof LivingEntity livingAttacker) {
                    attacker.setPos(0.0, 0.0, 0.0);
                    src = level.damageSources().mobAttack(livingAttacker);
                } else {
                    if (attacker != null) attacker.discard();
                    attacker = null;
                    src = level.damageSources().generic();
                }
            } else {
                src = level.damageSources().generic();
            }

            LootParams.Builder b = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, src);

            if (looter != null) {
                b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, looter);
                b.withParameter(LootContextParams.ATTACKING_ENTITY, looter);
                b.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, looter);
                b.withLuck(looter.getLuck());
            } else if (attacker != null) {
                b.withParameter(LootContextParams.ATTACKING_ENTITY, attacker);
                b.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, attacker);
            }

            LootParams params = b.create(LootContextParamSets.ENTITY);

            Map<String, Stat> stats = new HashMap<>();

            for (int i = 0; i < samples; i++) {
                List<ItemStack> drops = new ArrayList<>();
                table.getRandomItems(params, drops::add);
                collectEquipmentDropsForSample(level, entity, looter, src, drops);

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

            return toSummariesForCondition(stats, samples, conditionMask);
        } finally {
            entity.discard();
            if (attacker != null) attacker.discard();
        }
    }

    private static LootTable resolveLootTable(MinecraftServer server, ResourceKey<LootTable> key) {
        if (server == null || key == null) return LootTable.EMPTY;

        try {
            return server.reloadableRegistries().getLootTable(key);
        } catch (Throwable t) {
            return LootTable.EMPTY;
        }
    }

    private static List<LootDropSummary> mergeSummaries(Map<String, Stat> stats, int mergedSamples) {
        List<LootDropSummary> out = new ArrayList<>(stats.size());
        for (Map.Entry<String, Stat> e : stats.entrySet()) {
            Stat s = e.getValue();
            int min = (s.min == Integer.MAX_VALUE) ? 0 : s.min;
            List<Integer> effectiveProfiles = inferMinimalProfiles(s);
            int effectiveMask = LOOT_CONDITION_NONE;
            for (int profileMask : effectiveProfiles) {
                effectiveMask |= profileMask;
            }
            out.add(new LootDropSummary(
                    e.getKey(),
                    min,
                    s.max,
                    s.timesSeen,
                    Math.max(1, mergedSamples),
                    effectiveMask,
                    effectiveProfiles
            ));
        }

        out.sort(Comparator
                .comparingInt(LootDropSummary::timesSeen).reversed()
                .thenComparing(LootDropSummary::itemId));

        return List.copyOf(out);
    }

    private static List<LootDropSummary> toSummariesForCondition(Map<String, Stat> stats, int samples, int conditionMask) {
        List<LootDropSummary> out = new ArrayList<>(stats.size());
        for (Map.Entry<String, Stat> e : stats.entrySet()) {
            Stat s = e.getValue();
            int min = (s.min == Integer.MAX_VALUE) ? 0 : s.min;
            out.add(new LootDropSummary(
                    e.getKey(),
                    min,
                    s.max,
                    s.timesSeen,
                    samples,
                    conditionMask,
                    conditionMask == LOOT_CONDITION_NONE ? List.of() : List.of(conditionMask)
            ));
        }

        out.sort(Comparator
                .comparingInt(LootDropSummary::timesSeen).reversed()
                .thenComparing(LootDropSummary::itemId));
        return List.copyOf(out);
    }

    private static Map<Integer, List<SamplingPlan>> groupPlansByMask(List<SamplingPlan> plans) {
        Map<Integer, List<SamplingPlan>> out = new HashMap<>();
        for (SamplingPlan plan : plans) {
            if (plan == null) continue;
            out.computeIfAbsent(plan.conditionMask(), k -> new ArrayList<>()).add(plan);
        }
        return out;
    }

    private static Map<Integer, Integer> computeSamplesByMask(Map<Integer, List<SamplingPlan>> plansByMask) {
        Map<Integer, Integer> out = new HashMap<>();
        for (Map.Entry<Integer, List<SamplingPlan>> e : plansByMask.entrySet()) {
            int total = 0;
            for (SamplingPlan plan : e.getValue()) {
                if (plan == null) continue;
                total += Math.max(1, plan.samples());
            }
            out.put(e.getKey(), Math.max(0, total));
        }
        return out;
    }

    private static void verifyAmbiguousConditions(
            ServerLevel level,
            EntityType<?> type,
            Map<Integer, List<SamplingPlan>> plansByMask,
            Map<Integer, Integer> samplesByMask,
            Map<String, Stat> stats
    ) {
        if (stats.isEmpty() || plansByMask.isEmpty()) return;

        Set<Integer> verificationMasks = new HashSet<>();
        for (Stat stat : stats.values()) {
            if (stat == null) continue;

            Set<Integer> observed = observedConditionMasks(stat);
            if (observed.isEmpty()) continue;
            if (isSeenInDefault(stat)) continue;

            for (int mask : observed) {
                for (int bit : CONDITION_BITS) {
                    if ((mask & bit) == 0) continue;
                    int subsetMask = mask & ~bit;
                    if (!plansByMask.containsKey(subsetMask)) continue;

                    MaskStat subset = stat.byMask.get(subsetMask);
                    int subsetHits = subset == null ? 0 : subset.hits;
                    if (subsetHits <= 0) {
                        verificationMasks.add(subsetMask);
                    }
                }
            }
        }

        if (verificationMasks.isEmpty()) return;

        for (int verifyMask : verificationMasks) {
            List<SamplingPlan> subsetPlans = plansByMask.get(verifyMask);
            if (subsetPlans == null || subsetPlans.isEmpty()) continue;

            int addedSamples = 0;
            for (SamplingPlan plan : subsetPlans) {
                int runSamples = Math.max(Math.max(1, plan.samples()), VERIFICATION_SAMPLES_PER_PLAN);
                List<LootDropSummary> out = sampleEntityLoot(
                        level,
                        plan.looter(),
                        plan.attackerType(),
                        type,
                        runSamples,
                        plan.entityConfigurator(),
                        plan.conditionMask()
                );
                mergeInto(stats, out);
                addedSamples += runSamples;
            }

            if (addedSamples > 0) {
                int current = samplesByMask.getOrDefault(verifyMask, 0);
                samplesByMask.put(verifyMask, current + addedSamples);
            }
        }
    }

    private static boolean isSeenInDefault(Stat stat) {
        if (stat == null) return false;
        MaskStat base = stat.byMask.get(LOOT_CONDITION_NONE);
        return base != null && base.hits > 0;
    }

    private static Set<Integer> observedConditionMasks(Stat stat) {
        Set<Integer> out = new HashSet<>();
        if (stat == null || stat.byMask.isEmpty()) return out;

        for (Map.Entry<Integer, MaskStat> e : stat.byMask.entrySet()) {
            int mask = e.getKey();
            if (mask == LOOT_CONDITION_NONE) continue;
            MaskStat ms = e.getValue();
            if (ms != null && ms.hits > 0) {
                out.add(mask);
            }
        }
        return out;
    }

    private static void applySamplesByMask(Map<String, Stat> stats, Map<Integer, Integer> samplesByMask) {
        if (stats == null || stats.isEmpty() || samplesByMask == null || samplesByMask.isEmpty()) return;
        for (Stat stat : stats.values()) {
            if (stat == null) continue;
            for (Map.Entry<Integer, Integer> e : samplesByMask.entrySet()) {
                MaskStat ms = stat.byMask.computeIfAbsent(e.getKey(), k -> new MaskStat());
                ms.samples = Math.max(0, e.getValue());
            }
        }
    }

    private static List<SamplingPlan> buildSamplingPlans(ServerLevel level, EntityType<?> type, int baseSamples) {
        if (level == null || type == null) return List.of();
        int s = Math.max(1, baseSamples);

        ServerPlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        List<SamplingPlan> plans = new ArrayList<>();

        // Baseline: simple mob death without special state.
        plans.add(new SamplingPlan(s, null, null, null, LOOT_CONDITION_NONE));
        // Player-attributed profile for player-gated drops.
        plans.add(new SamplingPlan(s, fakePlayer, null, null, LOOT_CONDITION_PLAYER_KILL));

        // Burning profile to surface cooked variants gated by "on fire" state.
        plans.add(new SamplingPlan(Math.max(16, s / 4), null, null, e -> e.setRemainingFireTicks(200), LOOT_CONDITION_ON_FIRE));
        plans.add(new SamplingPlan(
                Math.max(16, s / 4),
                fakePlayer,
                null,
                e -> e.setRemainingFireTicks(200),
                LOOT_CONDITION_PLAYER_KILL | LOOT_CONDITION_ON_FIRE
        ));

        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            plans.add(new SamplingPlan(s, null, null, e -> configureSlimeSize(e, 1), LOOT_CONDITION_SLIME_SIZE));
            plans.add(new SamplingPlan(
                    s,
                    fakePlayer,
                    null,
                    e -> configureSlimeSize(e, 1),
                    LOOT_CONDITION_PLAYER_KILL | LOOT_CONDITION_SLIME_SIZE
            ));
            plans.add(new SamplingPlan(s, null, null, e -> configureSlimeSize(e, 2), LOOT_CONDITION_SLIME_SIZE));
            plans.add(new SamplingPlan(
                    s,
                    fakePlayer,
                    null,
                    e -> configureSlimeSize(e, 2),
                    LOOT_CONDITION_PLAYER_KILL | LOOT_CONDITION_SLIME_SIZE
            ));
        }

        if (type == EntityType.MAGMA_CUBE) {
            // Froglight path requires frog kill context.
            plans.add(new SamplingPlan(
                    s,
                    null,
                    EntityType.FROG,
                    e -> configureSlimeSize(e, 2),
                    LOOT_CONDITION_FROG_KILL | LOOT_CONDITION_SLIME_SIZE
            ));
        }

        if (type == EntityType.PILLAGER) {
            plans.add(new SamplingPlan(s, null, null, WildexLootExtractor::configurePillagerCaptain, LOOT_CONDITION_CAPTAIN));
            plans.add(new SamplingPlan(
                    s,
                    fakePlayer,
                    null,
                    WildexLootExtractor::configurePillagerCaptain,
                    LOOT_CONDITION_PLAYER_KILL | LOOT_CONDITION_CAPTAIN
            ));
        }

        if (type == EntityType.SHEEP) {
            for (DyeColor color : DyeColor.values()) {
                plans.add(new SamplingPlan(
                        Math.max(12, s / 6),
                        null,
                        null,
                        e -> configureSheepColor(e, color),
                        LOOT_CONDITION_SHEEP_COLOR
                ));
                plans.add(new SamplingPlan(
                        Math.max(12, s / 6),
                        fakePlayer,
                        null,
                        e -> configureSheepColor(e, color),
                        LOOT_CONDITION_PLAYER_KILL | LOOT_CONDITION_SHEEP_COLOR
                ));
            }
        }

        return List.copyOf(plans);
    }

    private static void configureSlimeSize(Entity entity, int size) {
        if (!(entity instanceof Slime slime)) return;
        try {
            slime.setSize(Math.max(1, size), true);
        } catch (Throwable ignored) {
        }
    }

    private static void configurePillagerCaptain(Entity entity) {
        if (!(entity instanceof Pillager pillager)) return;
        try {
            applyVanillaLikeDefaultEquipment(pillager, EntityType.PILLAGER);
            pillager.setPatrolLeader(true);
            ItemStack leaderBanner = Raid.getLeaderBannerInstance(
                    pillager.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)
            );
            pillager.setItemSlot(EquipmentSlot.HEAD, leaderBanner);
            pillager.setDropChance(EquipmentSlot.HEAD, 2.0F);
        } catch (Throwable ignored) {
        }
    }

    private static void applyVanillaLikeDefaultEquipment(Entity entity, EntityType<?> type) {
        if (entity == null || type == null) return;
        if (type == EntityType.PILLAGER && entity instanceof Pillager pillager) {
            if (pillager.getMainHandItem().isEmpty()) {
                pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            }
        }
    }

    private static void collectEquipmentDropsForSample(
            ServerLevel level,
            Entity victim,
            ServerPlayer looter,
            DamageSource source,
            List<ItemStack> outDrops
    ) {
        if (level == null || outDrops == null) return;
        if (!(victim instanceof Pillager pillager) || source == null) return;

        // Mirrors vanilla "recentlyHit" gating used in Mob#dropCustomDeathLoot.
        boolean recentlyHit = looter != null;

        EquipmentSlot[] pillagerSlots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD};
        for (EquipmentSlot slot : pillagerSlots) {
            ItemStack equipped = pillager.getItemBySlot(slot);
            if (equipped.isEmpty()) continue;
            if (EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;

            float chance = defaultPillagerEquipmentDropChance(pillager, slot, equipped);
            if (chance <= 0.0F) continue;

            boolean guaranteed = chance > 1.0F;
            Entity killer = source.getEntity();
            if (killer instanceof LivingEntity livingKiller) {
                chance = EnchantmentHelper.processEquipmentDropChance(level, livingKiller, source, chance);
            }

            if ((recentlyHit || guaranteed) && pillager.getRandom().nextFloat() < chance) {
                outDrops.add(equipped.copyWithCount(Math.max(1, equipped.getCount())));
            }
        }
    }

    private static float defaultPillagerEquipmentDropChance(Pillager pillager, EquipmentSlot slot, ItemStack equipped) {
        if (pillager == null || slot == null || equipped.isEmpty()) return 0.0F;

        if (slot == EquipmentSlot.MAINHAND) {
            return equipped.is(Items.CROSSBOW) ? 0.085F : 0.0F;
        }
        if (slot == EquipmentSlot.HEAD) {
            if (pillager.isPatrolLeader()) {
                ItemStack leaderBanner = Raid.getLeaderBannerInstance(
                        pillager.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)
                );
                if (ItemStack.matches(equipped, leaderBanner)) {
                    return 2.0F;
                }
            }
            return 0.085F;
        }
        return 0.0F;
    }

    private static void configureSheepColor(Entity entity, DyeColor color) {
        if (!(entity instanceof Sheep sheep) || color == null) return;
        try {
            sheep.setColor(color);
            sheep.setSheared(false);
        } catch (Throwable ignored) {
        }
    }

    private static void mergeInto(Map<String, Stat> stats, List<LootDropSummary> source) {
        if (stats == null || source == null || source.isEmpty()) return;

        for (LootDropSummary row : source) {
            if (row == null || row.itemId() == null || row.itemId().isBlank()) continue;

            Stat s = stats.computeIfAbsent(row.itemId(), k -> new Stat());
            s.timesSeen += Math.max(0, row.timesSeen());
            int rowMask = Math.max(LOOT_CONDITION_NONE, row.conditionMask());
            MaskStat byMask = s.byMask.computeIfAbsent(rowMask, k -> new MaskStat());
            byMask.hits += Math.max(0, row.timesSeen());

            int rowMin = row.minCountSeen();
            if (rowMin > 0) {
                s.min = Math.min(s.min, rowMin);
            } else if (s.min == Integer.MAX_VALUE) {
                s.min = 0;
            }

            s.max = Math.max(s.max, row.maxCountSeen());
        }
    }

    private static final class Stat {
        int timesSeen = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        final Map<Integer, MaskStat> byMask = new HashMap<>();
    }

    private static final class MaskStat {
        int hits = 0;
        int samples = 0;
    }

    private static List<Integer> inferMinimalProfiles(Stat stat) {
        if (stat == null) return List.of();
        if (isSeenInDefault(stat)) return List.of();

        Set<Integer> observed = observedConditionMasks(stat);
        if (observed.isEmpty()) return List.of();

        List<Integer> sorted = new ArrayList<>(observed);
        sorted.sort(Comparator
                .comparingInt(Integer::bitCount)
                .thenComparingInt(Integer::intValue));

        LinkedHashSet<Integer> minimal = new LinkedHashSet<>();
        for (int mask : sorted) {
            boolean redundant = false;
            for (int keep : minimal) {
                if (isStrictSubset(keep, mask)) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                minimal.add(mask);
            }
        }

        return normalizedConditionProfiles(minimal);
    }

    private static boolean isStrictSubset(int subset, int superset) {
        if (subset == superset) return false;
        return (subset & superset) == subset;
    }

    private static List<Integer> normalizedConditionProfiles(Iterable<Integer> rawProfiles) {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();

        if (rawProfiles != null) {
            for (int m : rawProfiles) {
                if (m != LOOT_CONDITION_NONE) {
                    unique.add(m);
                }
            }
        }
        if (unique.isEmpty()) return List.of();
        List<Integer> out = new ArrayList<>(unique);
        out.sort(Comparator
                .comparingInt(Integer::bitCount)
                .thenComparingInt(Integer::intValue));
        return List.copyOf(out);
    }

    private record SamplingPlan(
            int samples,
            ServerPlayer looter,
            EntityType<?> attackerType,
            Consumer<Entity> entityConfigurator,
            int conditionMask
    ) {
    }
}
