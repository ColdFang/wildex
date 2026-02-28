package de.coldfang.wildex.client.data;

import de.coldfang.wildex.integration.cobblemon.WildexCobblemonBridge;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class WildexEntityVariantCatalog {

    private static final int PROBE_INTERVAL_TICKS = 3;
    private static final int MAX_JOBS_PER_CYCLE = 1;
    private static final long PROBE_TIME_BUDGET_NS = 2_000_000L;
    private static final Map<ResourceLocation, Boolean> SUPPORT_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<WildexEntityVariantProbe.VariantOption>> OPTIONS_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ProbeJob> PENDING_JOBS = new ConcurrentHashMap<>();
    private static final Queue<ResourceLocation> PROBE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicLong CACHE_REVISION = new AtomicLong(0L);

    private static int clientTickCounter = 0;

    private WildexEntityVariantCatalog() {
    }

    public static void tickClient(boolean enabled) {
        if (!enabled) {
            clearPendingJobs();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc == null ? null : mc.level;
        if (level == null) return;

        clientTickCounter++;
        if ((clientTickCounter % PROBE_INTERVAL_TICKS) != 0) return;

        long deadlineNs = System.nanoTime() + PROBE_TIME_BUDGET_NS;
        int processed = 0;

        while (processed < MAX_JOBS_PER_CYCLE && System.nanoTime() <= deadlineNs) {
            ResourceLocation id = PROBE_QUEUE.poll();
            if (id == null) break;

            ProbeJob job = PENDING_JOBS.remove(id);
            if (job == null) continue;

            if (job.mode() == ProbeMode.SUPPORT) {
                boolean supported;
                try {
                    supported = probeSupport(job.type(), level);
                } catch (Throwable ignored) {
                    supported = false;
                }
                SUPPORT_CACHE.put(id, supported);
                if (!supported) {
                    OPTIONS_CACHE.put(id, List.of());
                }
            } else {
                List<WildexEntityVariantProbe.VariantOption> discovered;
                try {
                    discovered = discoverOptions(job.type(), level);
                } catch (Throwable ignored) {
                    discovered = List.of();
                }
                OPTIONS_CACHE.put(id, discovered);
                SUPPORT_CACHE.put(id, !discovered.isEmpty());
            }
            CACHE_REVISION.incrementAndGet();
            processed++;
        }
    }

    public static SupportState requestSupport(EntityType<?> type) {
        ResourceLocation id = idOf(type);
        if (id == null) return SupportState.UNSUPPORTED;

        List<WildexEntityVariantProbe.VariantOption> cachedOptions = OPTIONS_CACHE.get(id);
        if (cachedOptions != null) {
            return cachedOptions.isEmpty() ? SupportState.UNSUPPORTED : SupportState.SUPPORTED;
        }

        Boolean cachedSupport = SUPPORT_CACHE.get(id);
        if (cachedSupport != null) {
            return cachedSupport ? SupportState.SUPPORTED : SupportState.UNSUPPORTED;
        }

        ProbeJob existing = PENDING_JOBS.get(id);
        if (existing != null) return SupportState.PENDING;

        PENDING_JOBS.computeIfAbsent(id, key -> {
            PROBE_QUEUE.offer(key);
            return new ProbeJob(type, ProbeMode.SUPPORT);
        });
        return SupportState.PENDING;
    }

    public static ProbeState requestOptions(EntityType<?> type) {
        ResourceLocation id = idOf(type);
        if (id == null) return ProbeState.UNSUPPORTED;

        List<WildexEntityVariantProbe.VariantOption> cached = OPTIONS_CACHE.get(id);
        if (cached != null) {
            return cached.isEmpty() ? ProbeState.UNSUPPORTED : ProbeState.READY;
        }

        Boolean supported = SUPPORT_CACHE.get(id);
        if (Boolean.FALSE.equals(supported)) return ProbeState.UNSUPPORTED;

        PENDING_JOBS.compute(id, (key, existing) -> {
            if (existing == null) {
                PROBE_QUEUE.offer(key);
                return new ProbeJob(type, ProbeMode.OPTIONS);
            }
            if (existing.mode() == ProbeMode.SUPPORT) {
                return new ProbeJob(type, ProbeMode.OPTIONS);
            }
            return existing;
        });
        return ProbeState.PENDING;
    }

    public static List<WildexEntityVariantProbe.VariantOption> cachedOptions(EntityType<?> type) {
        ResourceLocation id = idOf(type);
        if (id == null) return List.of();
        List<WildexEntityVariantProbe.VariantOption> cached = OPTIONS_CACHE.get(id);
        return cached == null ? List.of() : cached;
    }

    public static long cacheRevision() {
        return CACHE_REVISION.get();
    }

    public static boolean hasVariants(EntityType<?> type, Level level) {
        ResourceLocation id = idOf(type);
        if (id == null) return false;

        Boolean cached = SUPPORT_CACHE.get(id);
        if (cached != null) return cached;

        boolean supported = probeSupport(type, level);
        SUPPORT_CACHE.put(id, supported);
        return supported;
    }

    public static List<WildexEntityVariantProbe.VariantOption> options(EntityType<?> type, Level level) {
        ResourceLocation id = idOf(type);
        if (id == null) return List.of();

        List<WildexEntityVariantProbe.VariantOption> cached = OPTIONS_CACHE.get(id);
        if (cached != null) return cached;

        List<WildexEntityVariantProbe.VariantOption> discovered = discoverOptions(type, level);
        OPTIONS_CACHE.put(id, discovered);
        SUPPORT_CACHE.put(id, !discovered.isEmpty());
        return discovered;
    }

    public static void clearCache() {
        clearPendingJobs();
        SUPPORT_CACHE.clear();
        OPTIONS_CACHE.clear();
        WildexCobblemonBridge.clearCache();
        CACHE_REVISION.incrementAndGet();
        clientTickCounter = 0;
    }

    private static boolean probeSupport(EntityType<?> type, Level level) {
        Entity entity = WildexEntityFactory.tryCreate(type, level);
        if (!(entity instanceof Mob)) {
            if (entity != null) entity.discard();
            return false;
        }

        try {
            return WildexEntityVariantProbe.supportsVariants(entity);
        } finally {
            entity.discard();
        }
    }

    private static List<WildexEntityVariantProbe.VariantOption> discoverOptions(EntityType<?> type, Level level) {
        Entity entity = WildexEntityFactory.tryCreate(type, level);
        if (!(entity instanceof Mob)) {
            if (entity != null) entity.discard();
            return List.of();
        }

        try {
            return WildexEntityVariantProbe.discoverOptions(entity, Integer.MAX_VALUE);
        } finally {
            entity.discard();
        }
    }

    private static void clearPendingJobs() {
        PENDING_JOBS.clear();
        PROBE_QUEUE.clear();
    }

    private static ResourceLocation idOf(EntityType<?> type) {
        if (type == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public enum ProbeState {
        READY,
        PENDING,
        UNSUPPORTED
    }

    public enum SupportState {
        SUPPORTED,
        PENDING,
        UNSUPPORTED
    }

    private enum ProbeMode {
        SUPPORT,
        OPTIONS
    }

    private record ProbeJob(EntityType<?> type, ProbeMode mode) {
    }
}
