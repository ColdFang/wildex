package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.data.extractor.StatsExtractor;
import de.coldfang.wildex.client.data.model.WildexStatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class WildexVariantStatsCatalog {

    private static final int PROBE_INTERVAL_TICKS = 3;
    private static final int MAX_JOBS_PER_CYCLE = 1;
    private static final long PROBE_TIME_BUDGET_NS = 2_000_000L;

    private static final StatsExtractor STATS_EXTRACTOR = new StatsExtractor();
    private static final Map<VariantKey, WildexStatsData> CACHE = new ConcurrentHashMap<>();
    private static final Map<VariantKey, Boolean> UNSUPPORTED = new ConcurrentHashMap<>();
    private static final Map<VariantKey, ProbeJob> PENDING = new ConcurrentHashMap<>();
    private static final Queue<VariantKey> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicLong CACHE_REVISION = new AtomicLong(0L);

    private static int tickCounter = 0;

    private WildexVariantStatsCatalog() {
    }

    public static void tickClient(boolean enabled) {
        if (!enabled) {
            clearPendingJobs();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc == null ? null : mc.level;
        if (level == null) return;

        tickCounter++;
        if ((tickCounter % PROBE_INTERVAL_TICKS) != 0) return;

        long deadlineNs = System.nanoTime() + PROBE_TIME_BUDGET_NS;
        int processed = 0;
        while (processed < MAX_JOBS_PER_CYCLE && System.nanoTime() <= deadlineNs) {
            VariantKey key = QUEUE.poll();
            if (key == null) break;

            ProbeJob job = PENDING.remove(key);
            if (job == null) continue;

            StatsExtractor.Result result;
            try {
                result = STATS_EXTRACTOR.extractVariant(job.type(), key.optionId());
            } catch (Throwable ignored) {
                result = StatsExtractor.Result.unsupported();
            }

            if (result.supported()) {
                CACHE.put(key, result.stats());
                UNSUPPORTED.remove(key);
            } else {
                CACHE.remove(key);
                UNSUPPORTED.put(key, Boolean.TRUE);
            }
            CACHE_REVISION.incrementAndGet();
            processed++;
        }
    }

    public static QueryState request(EntityType<?> type, String variantOptionId, boolean backgroundMode) {
        VariantKey key = keyOf(type, variantOptionId);
        if (key == null) return QueryState.UNSUPPORTED;

        if (CACHE.containsKey(key)) return QueryState.READY;
        if (Boolean.TRUE.equals(UNSUPPORTED.get(key))) return QueryState.UNSUPPORTED;

        if (!backgroundMode) {
            StatsExtractor.Result result;
            try {
                result = STATS_EXTRACTOR.extractVariant(type, key.optionId());
            } catch (Throwable ignored) {
                result = StatsExtractor.Result.unsupported();
            }
            if (result.supported()) {
                CACHE.put(key, result.stats());
                UNSUPPORTED.remove(key);
                CACHE_REVISION.incrementAndGet();
                return QueryState.READY;
            }
            UNSUPPORTED.put(key, Boolean.TRUE);
            CACHE_REVISION.incrementAndGet();
            return QueryState.UNSUPPORTED;
        }

        PENDING.computeIfAbsent(key, ignored -> {
            QUEUE.offer(key);
            return new ProbeJob(type);
        });
        return QueryState.PENDING;
    }

    public static WildexStatsData cached(EntityType<?> type, String variantOptionId) {
        VariantKey key = keyOf(type, variantOptionId);
        if (key == null) return WildexStatsData.empty();
        WildexStatsData data = CACHE.get(key);
        return data == null ? WildexStatsData.empty() : data;
    }

    public static long cacheRevision() {
        return CACHE_REVISION.get();
    }

    public static void clearCache() {
        clearPendingJobs();
        CACHE.clear();
        UNSUPPORTED.clear();
        tickCounter = 0;
        CACHE_REVISION.incrementAndGet();
    }

    private static VariantKey keyOf(EntityType<?> type, String variantOptionId) {
        if (type == null || variantOptionId == null || variantOptionId.isBlank()) return null;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return null;
        return new VariantKey(id, variantOptionId);
    }

    private static void clearPendingJobs() {
        PENDING.clear();
        QUEUE.clear();
    }

    public enum QueryState {
        READY,
        PENDING,
        UNSUPPORTED
    }

    private record VariantKey(ResourceLocation mobId, String optionId) {
    }

    private record ProbeJob(EntityType<?> type) {
    }
}

