package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexDiscoveryService;
import de.coldfang.wildex.server.loot.WildexLootExtractor;
import de.coldfang.wildex.server.spawn.WildexSpawnExtractor;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerCooldownData;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerKillData;
import de.coldfang.wildex.world.WildexWorldPlayerUiStateData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WildexNetwork {

    public static final String MOD_ID = "wildex";

    private static final double PULSE_RANGE = 32.0;
    private static final int PULSE_GLOW_TICKS = 10 * 20;
    private static final int PULSE_COOLDOWN_TICKS = 15 * 20;
    private static final long REQUEST_COOLDOWN_MS = 300L;
    private static final long LOOT_CACHE_TTL_MS = 30_000L;
    private static final long SPAWN_CACHE_TTL_MS = 30_000L;
    private static final int MAX_RUNTIME_CACHE_ENTRIES = 512;

    private static final Map<RequestKey, Long> NEXT_ALLOWED_REQUEST_MS = new HashMap<>();
    private static final Map<ResourceLocation, TimedLoot> LOOT_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, TimedSpawns> SPAWN_CACHE = new HashMap<>();

    private WildexNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(MOD_ID);

        // Dedicated servers must still advertise clientbound Wildex channels during handshake.
        // Handlers stay client-only; these are no-op registrations for channel presence.
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            r.playToClient(S2CDiscoveredMobPayload.TYPE, S2CDiscoveredMobPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CDiscoveredMobsPayload.TYPE, S2CDiscoveredMobsPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CMobKillsPayload.TYPE, S2CMobKillsPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CMobLootPayload.TYPE, S2CMobLootPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CMobSpawnsPayload.TYPE, S2CMobSpawnsPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CSpyglassDiscoveryEffectPayload.TYPE, S2CSpyglassDiscoveryEffectPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CWildexCompletePayload.TYPE, S2CWildexCompletePayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CWildexCompleteStatusPayload.TYPE, S2CWildexCompleteStatusPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CPlayerUiStatePayload.TYPE, S2CPlayerUiStatePayload.STREAM_CODEC, (payload, ctx) -> {
            });
        }

        r.playToServer(
                C2SSpyglassPulsePayload.TYPE,
                C2SSpyglassPulsePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    if (!sp.getMainHandItem().is(Items.SPYGLASS)) return;
                    if (!WildexWorldPlayerDiscoveryData.get(serverLevel).isComplete(sp.getUUID())) return;

                    long now = serverLevel.getGameTime();
                    WildexWorldPlayerCooldownData cd = WildexWorldPlayerCooldownData.get(serverLevel);
                    long end = cd.getSpyglassPulseCooldownEnd(sp.getUUID());

                    if (now < end) {
                        long remainingTicks = end - now;
                        long seconds = (remainingTicks + 19) / 20;

                        sp.displayClientMessage(
                                Component.translatable("message.wildex.spyglass_pulse_ready_in", seconds)
                                        .withStyle(style -> style.withColor(0xAAAAAA)),
                                true
                        );
                        return;
                    }

                    cd.setSpyglassPulseCooldownEnd(sp.getUUID(), now + PULSE_COOLDOWN_TICKS);

                    AABB box = sp.getBoundingBox().inflate(PULSE_RANGE);

                    for (LivingEntity le : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e ->
                            e != null && e.isAlive() && !e.isSpectator() && e != sp
                    )) {
                        le.addEffect(new MobEffectInstance(MobEffects.GLOWING, PULSE_GLOW_TICKS, 0, false, false));
                    }
                })
        );

        r.playToServer(
                C2SDebugDiscoverMobPayload.TYPE,
                C2SDebugDiscoverMobPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    if (!CommonConfig.INSTANCE.hiddenMode.get()) return;
                    if (!CommonConfig.INSTANCE.debugMode.get()) return;

                    ResourceLocation mobId = payload.mobId();
                    if (mobId == null) return;
                    if (!WildexMobFilters.isTrackable(mobId)) return;

                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) return;

                    WildexDiscoveryService.discover(sp, mobId, WildexDiscoveryService.DiscoverySource.DEBUG);
                })
        );

        r.playToServer(
                C2SRequestMobKillsPayload.TYPE,
                C2SRequestMobKillsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobKillsPayload(mobId, 0));
                        return;
                    }

                    int kills = WildexWorldPlayerKillData.get(serverLevel).getKills(sp.getUUID(), mobId);
                    PacketDistributor.sendToPlayer(sp, new S2CMobKillsPayload(mobId, kills));
                })
        );

        r.playToServer(
                C2SRequestDiscoveredMobsPayload.TYPE,
                C2SRequestDiscoveredMobsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    Set<ResourceLocation> raw = WildexWorldPlayerDiscoveryData.get(serverLevel).getDiscovered(sp.getUUID());
                    Set<ResourceLocation> filtered = raw.stream()
                            .filter(WildexMobFilters::isTrackable)
                            .collect(Collectors.toSet());

                    PacketDistributor.sendToPlayer(sp, new S2CDiscoveredMobsPayload(filtered));
                })
        );

        r.playToServer(
                C2SRequestMobLootPayload.TYPE,
                C2SRequestMobLootPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    long nowMs = System.currentTimeMillis();
                    if (!allowRequest(sp, mobId, RequestKind.LOOT, nowMs)) return;

                    List<S2CMobLootPayload.LootLine> cachedLines = getCachedLoot(mobId, nowMs);
                    if (cachedLines != null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobLootPayload(mobId, cachedLines));
                        return;
                    }

                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobLootPayload(mobId, List.of()));
                        return;
                    }

                    int samples = 250;
                    List<WildexLootExtractor.LootDropSummary> raw = WildexLootExtractor.sampleEntityLoot(serverLevel, type, samples);

                    int cap = Math.min(raw.size(), 64);
                    List<S2CMobLootPayload.LootLine> lines = new ArrayList<>(cap);

                    for (int i = 0; i < cap; i++) {
                        WildexLootExtractor.LootDropSummary e = raw.get(i);
                        ResourceLocation itemId = ResourceLocation.tryParse(e.itemId());
                        if (itemId == null) continue;

                        lines.add(new S2CMobLootPayload.LootLine(itemId, e.minCountSeen(), e.maxCountSeen()));
                    }

                    List<S2CMobLootPayload.LootLine> frozen = List.copyOf(lines);
                    putCachedLoot(mobId, frozen, nowMs);
                    PacketDistributor.sendToPlayer(sp, new S2CMobLootPayload(mobId, frozen));
                })
        );

        r.playToServer(
                C2SRequestMobSpawnsPayload.TYPE,
                C2SRequestMobSpawnsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    ResourceLocation mobId = payload.mobId();
                    long nowMs = System.currentTimeMillis();
                    if (!allowRequest(sp, mobId, RequestKind.SPAWNS, nowMs)) return;

                    TimedSpawns cached = getCachedSpawns(mobId, nowMs);
                    if (cached != null) {
                        PacketDistributor.sendToPlayer(
                                sp,
                                new S2CMobSpawnsPayload(mobId, cached.naturalSections(), cached.structureSections())
                        );
                        return;
                    }

                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobSpawnsPayload(mobId, List.of(), List.of()));
                        return;
                    }

                    Map<ResourceLocation, List<ResourceLocation>> byDim =
                            WildexSpawnExtractor.collectSpawnBiomesByDimension(sp.server, type);
                    Map<ResourceLocation, List<ResourceLocation>> byStructure =
                            WildexSpawnExtractor.collectStructureOverrideBiomes(sp.server, type);

                    List<S2CMobSpawnsPayload.DimSection> naturalSections = new ArrayList<>(byDim.size());
                    for (var e : byDim.entrySet()) {
                        naturalSections.add(new S2CMobSpawnsPayload.DimSection(e.getKey(), e.getValue()));
                    }

                    List<S2CMobSpawnsPayload.StructureSection> structureSections = new ArrayList<>(byStructure.size());
                    for (var e : byStructure.entrySet()) {
                        structureSections.add(new S2CMobSpawnsPayload.StructureSection(e.getKey(), e.getValue()));
                    }

                    List<S2CMobSpawnsPayload.DimSection> frozenNatural = List.copyOf(naturalSections);
                    List<S2CMobSpawnsPayload.StructureSection> frozenStructures = List.copyOf(structureSections);
                    putCachedSpawns(mobId, frozenNatural, frozenStructures, nowMs);

                    PacketDistributor.sendToPlayer(
                            sp,
                            new S2CMobSpawnsPayload(
                                    mobId,
                                    frozenNatural,
                                    frozenStructures
                            )
                    );
                })
        );

        r.playToServer(
                C2SRequestPlayerUiStatePayload.TYPE,
                C2SRequestPlayerUiStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    WildexWorldPlayerUiStateData.UiState state = WildexWorldPlayerUiStateData
                            .get(serverLevel)
                            .getState(sp.getUUID());

                    PacketDistributor.sendToPlayer(sp, new S2CPlayerUiStatePayload(state.tabId(), state.mobId()));
                })
        );

        r.playToServer(
                C2SSavePlayerUiStatePayload.TYPE,
                C2SSavePlayerUiStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    String tabId = sanitizeTab(payload.tabId());
                    String mobId = sanitizeMob(payload.mobId());

                    WildexWorldPlayerUiStateData.get(serverLevel).setState(sp.getUUID(), tabId, mobId);
                })
        );
    }

    private static String sanitizeTab(String raw) {
        if (raw == null || raw.isBlank()) return "STATS";
        String s = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (s.length() > 32) return "STATS";
        return switch (s) {
            case "STATS", "LOOT", "SPAWNS", "MISC" -> s;
            default -> "STATS";
        };
    }

    private static String sanitizeMob(String raw) {
        ResourceLocation rl = ResourceLocation.tryParse(raw == null ? "" : raw);
        if (rl == null) return "";
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return "";
        if (!WildexMobFilters.isTrackable(rl)) return "";
        return rl.toString();
    }

    private static boolean allowRequest(ServerPlayer player, ResourceLocation mobId, RequestKind kind, long nowMs) {
        if (player == null || mobId == null) return false;
        RequestKey key = new RequestKey(player.getUUID(), mobId, kind);
        Long allowedAt = NEXT_ALLOWED_REQUEST_MS.get(key);
        if (allowedAt != null && nowMs < allowedAt) return false;

        NEXT_ALLOWED_REQUEST_MS.put(key, nowMs + REQUEST_COOLDOWN_MS);
        if (NEXT_ALLOWED_REQUEST_MS.size() > (MAX_RUNTIME_CACHE_ENTRIES * 8)) {
            NEXT_ALLOWED_REQUEST_MS.entrySet().removeIf(e -> e.getValue() <= nowMs);
            if (NEXT_ALLOWED_REQUEST_MS.size() > (MAX_RUNTIME_CACHE_ENTRIES * 8)) {
                NEXT_ALLOWED_REQUEST_MS.clear();
            }
        }
        return true;
    }

    private static List<S2CMobLootPayload.LootLine> getCachedLoot(ResourceLocation mobId, long nowMs) {
        if (mobId == null) return null;
        TimedLoot cached = LOOT_CACHE.get(mobId);
        if (cached == null) return null;
        if ((nowMs - cached.createdAtMs()) > LOOT_CACHE_TTL_MS) {
            LOOT_CACHE.remove(mobId);
            return null;
        }
        return cached.lines();
    }

    private static void putCachedLoot(ResourceLocation mobId, List<S2CMobLootPayload.LootLine> lines, long nowMs) {
        if (mobId == null || lines == null) return;
        LOOT_CACHE.put(mobId, new TimedLoot(nowMs, lines));
        if (LOOT_CACHE.size() > MAX_RUNTIME_CACHE_ENTRIES) {
            LOOT_CACHE.entrySet().removeIf(e -> (nowMs - e.getValue().createdAtMs()) > LOOT_CACHE_TTL_MS);
            if (LOOT_CACHE.size() > MAX_RUNTIME_CACHE_ENTRIES) {
                LOOT_CACHE.clear();
            }
        }
    }

    private static TimedSpawns getCachedSpawns(ResourceLocation mobId, long nowMs) {
        if (mobId == null) return null;
        TimedSpawns cached = SPAWN_CACHE.get(mobId);
        if (cached == null) return null;
        if ((nowMs - cached.createdAtMs()) > SPAWN_CACHE_TTL_MS) {
            SPAWN_CACHE.remove(mobId);
            return null;
        }
        return cached;
    }

    private static void putCachedSpawns(
            ResourceLocation mobId,
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections,
            long nowMs
    ) {
        if (mobId == null || naturalSections == null || structureSections == null) return;
        SPAWN_CACHE.put(mobId, new TimedSpawns(nowMs, naturalSections, structureSections));
        if (SPAWN_CACHE.size() > MAX_RUNTIME_CACHE_ENTRIES) {
            SPAWN_CACHE.entrySet().removeIf(e -> (nowMs - e.getValue().createdAtMs()) > SPAWN_CACHE_TTL_MS);
            if (SPAWN_CACHE.size() > MAX_RUNTIME_CACHE_ENTRIES) {
                SPAWN_CACHE.clear();
            }
        }
    }

    private record RequestKey(UUID playerId, ResourceLocation mobId, RequestKind kind) {
    }

    private enum RequestKind {
        LOOT,
        SPAWNS
    }

    private record TimedLoot(long createdAtMs, List<S2CMobLootPayload.LootLine> lines) {
    }

    private record TimedSpawns(
            long createdAtMs,
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections
    ) {
    }
}
