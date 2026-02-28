package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexCompletionHelper;
import de.coldfang.wildex.server.WildexDiscoveryService;
import de.coldfang.wildex.server.breeding.WildexBreedingExtractor;
import de.coldfang.wildex.server.WildexShareOfferService;
import de.coldfang.wildex.server.loot.WildexLootExtractor;
import de.coldfang.wildex.server.spawn.WildexSpawnExtractor;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerCooldownData;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerKillData;
import de.coldfang.wildex.world.WildexWorldPlayerUiStateData;
import de.coldfang.wildex.world.WildexWorldPlayerViewedEntriesData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("resource")
public final class WildexNetwork {

    public static final String MOD_ID = "wildex";

    private static final double PULSE_RANGE = 32.0;
    private static final int PULSE_GLOW_TICKS = 10 * 20;
    private static final int PULSE_COOLDOWN_TICKS = 15 * 20;
    private static final long REQUEST_COOLDOWN_MS = 300L;
    private static final int MAX_RUNTIME_CACHE_ENTRIES = 512;
    private static final int BREEDING_JOBS_PER_TICK = 1;

    private static final Map<RequestKey, Long> NEXT_ALLOWED_REQUEST_MS = new HashMap<>();
    private static final Map<ResourceLocation, CachedLoot> LOOT_CACHE = createLruCache();
    private static final Map<ResourceLocation, CachedSpawns> SPAWN_CACHE = createLruCache();
    private static final Map<ResourceLocation, CachedBreeding> BREEDING_CACHE = createLruCache();
    private static final Map<ResourceLocation, BreedingWork> BREEDING_IN_FLIGHT = new HashMap<>();
    private static final ArrayDeque<BreedingWork> BREEDING_QUEUE = new ArrayDeque<>();

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
            r.playToClient(S2CMobBreedingPayload.TYPE, S2CMobBreedingPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CMobSpawnsPayload.TYPE, S2CMobSpawnsPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CViewedMobEntriesPayload.TYPE, S2CViewedMobEntriesPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CMobEntryViewedPayload.TYPE, S2CMobEntryViewedPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CSpyglassDiscoveryEffectPayload.TYPE, S2CSpyglassDiscoveryEffectPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CWildexCompletePayload.TYPE, S2CWildexCompletePayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CWildexCompleteStatusPayload.TYPE, S2CWildexCompleteStatusPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CPlayerUiStatePayload.TYPE, S2CPlayerUiStatePayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CShareCandidatesPayload.TYPE, S2CShareCandidatesPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CSharePayoutStatusPayload.TYPE, S2CSharePayoutStatusPayload.STREAM_CODEC, (payload, ctx) -> {
            });
            r.playToClient(S2CServerConfigPayload.TYPE, S2CServerConfigPayload.STREAM_CODEC, (payload, ctx) -> {
            });
        }

        r.playToServer(
                C2SSpyglassPulsePayload.TYPE,
                C2SSpyglassPulsePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    if (!sp.getMainHandItem().is(Items.SPYGLASS)) return;
                    if (!WildexCompletionHelper.isCurrentlyComplete(serverLevel, sp.getUUID())) return;

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
                    if (!(sp.level() instanceof ServerLevel)) return;

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
                    if (isHiddenUndiscoveredInfoRequest(sp, serverLevel, mobId)) return;

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
                C2SRequestViewedMobEntriesPayload.TYPE,
                C2SRequestViewedMobEntriesPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    Set<ResourceLocation> viewed = WildexWorldPlayerViewedEntriesData
                            .get(serverLevel)
                            .getViewed(sp.getUUID());

                    PacketDistributor.sendToPlayer(sp, new S2CViewedMobEntriesPayload(viewed));
                })
        );

        r.playToServer(
                C2SMarkMobEntryViewedPayload.TYPE,
                C2SMarkMobEntryViewedPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    if (!WildexMobFilters.isTrackable(mobId)) return;

                    WildexWorldPlayerDiscoveryData discoveryData = WildexWorldPlayerDiscoveryData.get(serverLevel);
                    if (!discoveryData.isDiscovered(sp.getUUID(), mobId)) return;

                    WildexWorldPlayerViewedEntriesData viewedData = WildexWorldPlayerViewedEntriesData.get(serverLevel);
                    boolean added = viewedData.markViewed(sp.getUUID(), mobId);
                    if (added && CommonConfig.INSTANCE.newEntryRewardsXp.get()) {
                        int xp = Math.max(0, CommonConfig.INSTANCE.newEntryXpAmount.get());
                        if (xp > 0) {
                            sp.giveExperiencePoints(xp);
                            float pitch = 0.95f + (sp.getRandom().nextFloat() * 0.1f);
                            sp.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.10f, pitch);
                        }
                    }

                    PacketDistributor.sendToPlayer(sp, new S2CMobEntryViewedPayload(mobId));
                })
        );

        r.playToServer(
                C2SRequestMobLootPayload.TYPE,
                C2SRequestMobLootPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    if (isHiddenUndiscoveredInfoRequest(sp, serverLevel, mobId)) return;

                    long nowMs = System.currentTimeMillis();
                    if (isRequestBlocked(sp, mobId, RequestKind.LOOT, nowMs)) return;

                    List<S2CMobLootPayload.LootLine> cachedLines = getCachedLoot(mobId);
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

                        lines.add(new S2CMobLootPayload.LootLine(
                                itemId,
                                e.minCountSeen(),
                                e.maxCountSeen(),
                                Math.max(0, e.conditionMask()),
                                e.conditionProfiles()
                        ));
                    }

                    List<S2CMobLootPayload.LootLine> frozen = List.copyOf(lines);
                    putCachedLoot(mobId, frozen);
                    PacketDistributor.sendToPlayer(sp, new S2CMobLootPayload(mobId, frozen));
                })
        );

        r.playToServer(
                C2SRequestMobBreedingPayload.TYPE,
                C2SRequestMobBreedingPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    long nowMs = System.currentTimeMillis();
                    if (isRequestBlocked(sp, mobId, RequestKind.BREEDING, nowMs)) return;

                    CachedBreeding cached = getCachedBreeding(mobId);
                    if (cached != null) {
                        PacketDistributor.sendToPlayer(
                                sp,
                                new S2CMobBreedingPayload(mobId, cached.ownable(), cached.breedingItemIds(), cached.tamingItemIds())
                        );
                        return;
                    }

                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobBreedingPayload(mobId, false, List.of(), List.of()));
                        return;
                    }

                    enqueueBreedingWork(sp, mobId, type);
                })
        );

        r.playToServer(
                C2SRequestMobSpawnsPayload.TYPE,
                C2SRequestMobSpawnsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;

                    ResourceLocation mobId = payload.mobId();
                    if (isHiddenUndiscoveredInfoRequest(sp, serverLevel, mobId)) return;

                    long nowMs = System.currentTimeMillis();
                    if (isRequestBlocked(sp, mobId, RequestKind.SPAWNS, nowMs)) return;

                    CachedSpawns cached = getCachedSpawns(mobId);
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
                    putCachedSpawns(mobId, frozenNatural, frozenStructures);

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
                C2SRequestServerConfigPayload.TYPE,
                C2SRequestServerConfigPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel serverLevel)) return;
                    PacketDistributor.sendToPlayer(
                            sp,
                            new S2CServerConfigPayload(
                                    CommonConfig.INSTANCE.hiddenMode.get(),
                                    CommonConfig.INSTANCE.requireBookForKeybind.get(),
                                    CommonConfig.INSTANCE.debugMode.get(),
                                    CommonConfig.INSTANCE.shareOffersEnabled.get(),
                                    CommonConfig.INSTANCE.shareOffersPaymentEnabled.get(),
                                    CommonConfig.INSTANCE.shareOfferCurrencyItem.get(),
                                    Math.max(0, CommonConfig.INSTANCE.shareOfferMaxPrice.get())
                            )
                    );
                    PacketDistributor.sendToPlayer(
                            sp,
                            new S2CWildexCompleteStatusPayload(
                                    WildexCompletionHelper.isCurrentlyComplete(serverLevel, sp.getUUID())
                            )
                    );
                })
        );

        r.playToServer(
                C2SRequestShareCandidatesPayload.TYPE,
                C2SRequestShareCandidatesPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel)) return;

                    List<WildexShareOfferService.Candidate> candidates = WildexShareOfferService.listAcceptingCandidates(sp);
                    List<S2CShareCandidatesPayload.Candidate> out = new ArrayList<>(candidates.size());
                    for (WildexShareOfferService.Candidate c : candidates) {
                        out.add(new S2CShareCandidatesPayload.Candidate(c.playerId(), c.playerName()));
                    }
                    PacketDistributor.sendToPlayer(
                            sp,
                            new S2CShareCandidatesPayload(out, WildexShareOfferService.isAcceptingOffers(sp))
                    );
                })
        );

        r.playToServer(
                C2SSetShareAcceptOffersPayload.TYPE,
                C2SSetShareAcceptOffersPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    WildexShareOfferService.setAcceptingOffers(sp, payload.accepting());
                })
        );

        r.playToServer(
                C2SSendShareOfferPayload.TYPE,
                C2SSendShareOfferPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    WildexShareOfferService.createOffer(sp, payload.targetPlayerId(), payload.mobId(), payload.price());
                    PacketDistributor.sendToPlayer(sp, new S2CSharePayoutStatusPayload(WildexShareOfferService.getPendingPayoutTotal(sp)));
                })
        );

        r.playToServer(
                C2SRequestSharePayoutStatusPayload.TYPE,
                C2SRequestSharePayoutStatusPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    PacketDistributor.sendToPlayer(sp, new S2CSharePayoutStatusPayload(WildexShareOfferService.getPendingPayoutTotal(sp)));
                })
        );

        r.playToServer(
                C2SClaimSharePayoutsPayload.TYPE,
                C2SClaimSharePayoutsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    WildexShareOfferService.claimPendingPayouts(sp);
                    PacketDistributor.sendToPlayer(sp, new S2CSharePayoutStatusPayload(WildexShareOfferService.getPendingPayoutTotal(sp)));
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

    private static boolean isRequestBlocked(ServerPlayer player, ResourceLocation mobId, RequestKind kind, long nowMs) {
        if (player == null || mobId == null) return true;
        RequestKey key = new RequestKey(player.getUUID(), mobId, kind);
        Long allowedAt = NEXT_ALLOWED_REQUEST_MS.get(key);
        if (allowedAt != null && nowMs < allowedAt) return true;

        NEXT_ALLOWED_REQUEST_MS.put(key, nowMs + REQUEST_COOLDOWN_MS);
        if (NEXT_ALLOWED_REQUEST_MS.size() > (MAX_RUNTIME_CACHE_ENTRIES * 8)) {
            NEXT_ALLOWED_REQUEST_MS.entrySet().removeIf(e -> e.getValue() <= nowMs);
            if (NEXT_ALLOWED_REQUEST_MS.size() > (MAX_RUNTIME_CACHE_ENTRIES * 8)) {
                NEXT_ALLOWED_REQUEST_MS.clear();
            }
        }
        return false;
    }

    private static boolean isHiddenUndiscoveredInfoRequest(ServerPlayer player, ServerLevel level, ResourceLocation mobId) {
        if (player == null || level == null || mobId == null) return true;
        if (!CommonConfig.INSTANCE.hiddenMode.get()) return false;
        return !WildexWorldPlayerDiscoveryData.get(level).isDiscovered(player.getUUID(), mobId);
    }

    public static void clearRuntimeCaches() {
        NEXT_ALLOWED_REQUEST_MS.clear();
        LOOT_CACHE.clear();
        SPAWN_CACHE.clear();
        BREEDING_CACHE.clear();
        BREEDING_IN_FLIGHT.clear();
        BREEDING_QUEUE.clear();
    }

    public static void processBreedingQueue(MinecraftServer server) {
        if (server == null || BREEDING_QUEUE.isEmpty()) return;

        for (int i = 0; i < BREEDING_JOBS_PER_TICK; i++) {
            BreedingWork work = BREEDING_QUEUE.pollFirst();
            if (work == null) return;

            BREEDING_IN_FLIGHT.remove(work.mobId());
            completeBreedingWork(server, work.mobId(), work.type(), work.waitingPlayerIds());
        }
    }

    private static void enqueueBreedingWork(ServerPlayer requester, ResourceLocation mobId, EntityType<?> type) {
        if (requester == null || mobId == null || type == null) return;

        BreedingWork inFlight = BREEDING_IN_FLIGHT.get(mobId);
        if (inFlight != null) {
            inFlight.waitingPlayerIds().add(requester.getUUID());
            return;
        }

        BreedingWork work = new BreedingWork(mobId, type, new LinkedHashSet<>());
        work.waitingPlayerIds().add(requester.getUUID());
        BREEDING_IN_FLIGHT.put(mobId, work);
        BREEDING_QUEUE.addLast(work);
    }

    private static void completeBreedingWork(
            MinecraftServer server,
            ResourceLocation mobId,
            EntityType<?> type,
            Set<UUID> waitingPlayerIds
    ) {
        if (server == null || mobId == null || type == null || waitingPlayerIds == null || waitingPlayerIds.isEmpty()) return;

        CachedBreeding cached = getCachedBreeding(mobId);
        if (cached != null) {
            sendBreedingPayload(
                    server,
                    waitingPlayerIds,
                    new S2CMobBreedingPayload(mobId, cached.ownable(), cached.breedingItemIds(), cached.tamingItemIds())
            );
            return;
        }

        ServerLevel workLevel = resolveBreedingLevel(server);
        if (workLevel == null) {
            sendBreedingPayload(server, waitingPlayerIds, new S2CMobBreedingPayload(mobId, false, List.of(), List.of()));
            return;
        }

        WildexBreedingExtractor.Result result = WildexBreedingExtractor.extract(workLevel, type);
        List<ResourceLocation> breedingItems = List.copyOf(result.breedingItemIds());
        List<ResourceLocation> tamingItems = List.copyOf(result.tamingItemIds());
        putCachedBreeding(mobId, result.ownable(), breedingItems, tamingItems);

        sendBreedingPayload(
                server,
                waitingPlayerIds,
                new S2CMobBreedingPayload(mobId, result.ownable(), breedingItems, tamingItems)
        );
    }

    private static ServerLevel resolveBreedingLevel(MinecraftServer server) {
        if (server == null) return null;
        return server.overworld();
    }

    private static void sendBreedingPayload(MinecraftServer server, Set<UUID> waitingPlayerIds, S2CMobBreedingPayload payload) {
        if (server == null || waitingPlayerIds == null || payload == null) return;

        for (UUID playerId : waitingPlayerIds) {
            ServerPlayer target = server.getPlayerList().getPlayer(playerId);
            if (target == null) continue;
            PacketDistributor.sendToPlayer(target, payload);
        }
    }

    private static List<S2CMobLootPayload.LootLine> getCachedLoot(ResourceLocation mobId) {
        if (mobId == null) return null;
        CachedLoot cached = LOOT_CACHE.get(mobId);
        if (cached == null) return null;
        return cached.lines();
    }

    private static void putCachedLoot(ResourceLocation mobId, List<S2CMobLootPayload.LootLine> lines) {
        if (mobId == null || lines == null) return;
        LOOT_CACHE.put(mobId, new CachedLoot(lines));
    }

    private static CachedSpawns getCachedSpawns(ResourceLocation mobId) {
        if (mobId == null) return null;
        return SPAWN_CACHE.get(mobId);
    }

    private static void putCachedSpawns(
            ResourceLocation mobId,
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections
    ) {
        if (mobId == null || naturalSections == null || structureSections == null) return;
        SPAWN_CACHE.put(mobId, new CachedSpawns(naturalSections, structureSections));
    }

    private static CachedBreeding getCachedBreeding(ResourceLocation mobId) {
        if (mobId == null) return null;
        return BREEDING_CACHE.get(mobId);
    }

    private static void putCachedBreeding(
            ResourceLocation mobId,
            boolean ownable,
            List<ResourceLocation> breedingItemIds,
            List<ResourceLocation> tamingItemIds
    ) {
        if (mobId == null || breedingItemIds == null || tamingItemIds == null) return;
        BREEDING_CACHE.put(mobId, new CachedBreeding(ownable, breedingItemIds, tamingItemIds));
    }

    private static <K, V> Map<K, V> createLruCache() {
        return new LinkedHashMap<>(MAX_RUNTIME_CACHE_ENTRIES + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_RUNTIME_CACHE_ENTRIES;
            }
        };
    }

    private record RequestKey(UUID playerId, ResourceLocation mobId, RequestKind kind) {
    }

    private enum RequestKind {
        LOOT,
        SPAWNS,
        BREEDING
    }

    private record CachedLoot(List<S2CMobLootPayload.LootLine> lines) {
    }

    private record CachedSpawns(
            List<S2CMobSpawnsPayload.DimSection> naturalSections,
            List<S2CMobSpawnsPayload.StructureSection> structureSections
    ) {
    }

    private record CachedBreeding(
            boolean ownable,
            List<ResourceLocation> breedingItemIds,
            List<ResourceLocation> tamingItemIds
    ) {
    }

    private record BreedingWork(
            ResourceLocation mobId,
            EntityType<?> type,
            LinkedHashSet<UUID> waitingPlayerIds
    ) {
    }
}
