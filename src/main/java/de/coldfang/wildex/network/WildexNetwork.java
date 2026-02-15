package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexCompletionHelper;
import de.coldfang.wildex.server.WildexProgressHooks;
import de.coldfang.wildex.server.loot.WildexLootExtractor;
import de.coldfang.wildex.server.spawn.WildexSpawnExtractor;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerCooldownData;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerKillData;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class WildexNetwork {

    public static final String MOD_ID = "wildex";

    private static final double PULSE_RANGE = 32.0;
    private static final int PULSE_GLOW_TICKS = 10 * 20;
    private static final int PULSE_COOLDOWN_TICKS = 15 * 20;

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
                                Component.literal("Spyglass Pulse ready in " + seconds + "s")
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

                    WildexWorldPlayerDiscoveryData disc = WildexWorldPlayerDiscoveryData.get(serverLevel);

                    boolean newly = disc.markDiscovered(sp.getUUID(), mobId);
                    if (!newly) return;

                    PacketDistributor.sendToPlayer(sp, new S2CDiscoveredMobPayload(mobId));
                    boolean newlyCompleted = WildexCompletionHelper.markCompleteIfEligible(serverLevel, sp);
                    WildexProgressHooks.onDiscoveryChanged(sp, mobId);
                    if (newlyCompleted) {
                        WildexCompletionHelper.notifyCompleted(sp);
                    }
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

                    PacketDistributor.sendToPlayer(sp, new S2CMobLootPayload(mobId, List.copyOf(lines)));
                })
        );

        r.playToServer(
                C2SRequestMobSpawnsPayload.TYPE,
                C2SRequestMobSpawnsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    ResourceLocation mobId = payload.mobId();
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    if (type == null) {
                        PacketDistributor.sendToPlayer(sp, new S2CMobSpawnsPayload(mobId, List.of()));
                        return;
                    }

                    Map<ResourceLocation, List<ResourceLocation>> byDim =
                            WildexSpawnExtractor.collectSpawnBiomesByDimension(sp.server, type);

                    List<S2CMobSpawnsPayload.DimSection> sections = new ArrayList<>(byDim.size());
                    for (var e : byDim.entrySet()) {
                        sections.add(new S2CMobSpawnsPayload.DimSection(e.getKey(), e.getValue()));
                    }

                    PacketDistributor.sendToPlayer(sp, new S2CMobSpawnsPayload(mobId, List.copyOf(sections)));
                })
        );
    }
}
