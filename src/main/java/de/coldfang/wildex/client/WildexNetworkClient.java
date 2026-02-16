package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.WildexSpawnCache;
import de.coldfang.wildex.client.screen.WildexDiscoveryToast;
import de.coldfang.wildex.client.screen.WildexScreen;
import de.coldfang.wildex.network.C2SDebugDiscoverMobPayload;
import de.coldfang.wildex.network.C2SRequestPlayerUiStatePayload;
import de.coldfang.wildex.network.C2SRequestDiscoveredMobsPayload;
import de.coldfang.wildex.network.C2SRequestMobKillsPayload;
import de.coldfang.wildex.network.C2SRequestMobLootPayload;
import de.coldfang.wildex.network.C2SRequestMobSpawnsPayload;
import de.coldfang.wildex.network.C2SSavePlayerUiStatePayload;
import de.coldfang.wildex.network.S2CDiscoveredMobPayload;
import de.coldfang.wildex.network.S2CDiscoveredMobsPayload;
import de.coldfang.wildex.network.S2CMobKillsPayload;
import de.coldfang.wildex.network.S2CMobLootPayload;
import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import de.coldfang.wildex.network.S2CPlayerUiStatePayload;
import de.coldfang.wildex.network.S2CSpyglassDiscoveryEffectPayload;
import de.coldfang.wildex.network.S2CWildexCompletePayload;
import de.coldfang.wildex.network.S2CWildexCompleteStatusPayload;
import de.coldfang.wildex.network.WildexNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WildexNetworkClient {

    private WildexNetworkClient() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(WildexNetwork.MOD_ID);

        r.playToClient(
                S2CMobKillsPayload.TYPE,
                S2CMobKillsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> WildexKillCache.set(payload.mobId(), payload.kills()))
        );

        r.playToClient(
                S2CDiscoveredMobsPayload.TYPE,
                S2CDiscoveredMobsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> WildexDiscoveryCache.setAll(payload.mobIds()))
        );

        r.playToClient(
                S2CDiscoveredMobPayload.TYPE,
                S2CDiscoveredMobPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    ResourceLocation mobId = payload.mobId();
                    boolean already = WildexDiscoveryCache.isDiscovered(mobId);

                    WildexDiscoveryCache.add(mobId);
                    if (already) return;

                    Minecraft mc = Minecraft.getInstance();
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
                    Component name = (type != null) ? type.getDescription() : Component.literal(mobId.toString());
                    Component title = Component.translatable("toast.wildex.discovery", name);
                    mc.getToasts().addToast(new WildexDiscoveryToast(mobId, title));
                })
        );

        r.playToClient(
                S2CSpyglassDiscoveryEffectPayload.TYPE,
                S2CSpyglassDiscoveryEffectPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;

                    Entity e = mc.level.getEntity(payload.entityId());
                    if (e == null) return;

                    WildexDiscoveryEffectClient.play(e);
                })
        );

        r.playToClient(
                S2CWildexCompletePayload.TYPE,
                S2CWildexCompletePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(WildexCompletionCache::markCompleteFromServer)
        );

        r.playToClient(
                S2CWildexCompleteStatusPayload.TYPE,
                S2CWildexCompleteStatusPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        WildexCompletionCache.setCompleteStatusFromServer(payload.complete())
                )
        );

        r.playToClient(
                S2CMobLootPayload.TYPE,
                S2CMobLootPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> WildexLootCache.set(payload.mobId(), payload.lines()))
        );

        r.playToClient(
                S2CMobSpawnsPayload.TYPE,
                S2CMobSpawnsPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        WildexSpawnCache.set(
                                payload.mobId(),
                                payload.naturalSections(),
                                payload.structureSections()
                        )
                )
        );

        r.playToClient(
                S2CPlayerUiStatePayload.TYPE,
                S2CPlayerUiStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    WildexPlayerUiStateCache.set(payload.tabId(), payload.mobId());

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.screen instanceof WildexScreen screen) {
                        screen.applyServerUiState(payload.tabId(), payload.mobId());
                    }
                })
        );
    }

    public static void requestKillsForSelected(String mobId) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (rl == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SRequestMobKillsPayload(rl));
    }

    public static void requestLootForSelected(String mobId) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (rl == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SRequestMobLootPayload(rl));
    }

    public static void requestSpawnsForSelected(String mobId) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (rl == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SRequestMobSpawnsPayload(rl));
    }

    public static void requestDiscoveredMobs() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SRequestDiscoveredMobsPayload());
    }

    public static void requestPlayerUiState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SRequestPlayerUiStatePayload());
    }

    public static void savePlayerUiState(String tabId, String mobId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        String safeTab = tabId == null ? "" : tabId;
        String safeMob = mobId == null ? "" : mobId;
        PacketDistributor.sendToServer(new C2SSavePlayerUiStatePayload(safeTab, safeMob));
    }

    public static void sendDebugDiscoverMob(ResourceLocation mobId) {
        if (mobId == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PacketDistributor.sendToServer(new C2SDebugDiscoverMobPayload(mobId));
    }
}
