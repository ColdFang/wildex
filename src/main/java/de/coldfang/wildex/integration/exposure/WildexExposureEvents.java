package de.coldfang.wildex.integration.exposure;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexDiscoveryService;
import de.coldfang.wildex.util.WildexMobIdCanonicalizer;
import io.github.mortuusars.exposure.neoforge.api.event.FrameAddedEvent;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.camera.frame.Photographer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WildexExposureEvents {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private WildexExposureEvents() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        NeoForge.EVENT_BUS.register(WildexExposureEvents.class);
        Wildex.LOGGER.info("Wildex Exposure integration hooks registered.");
    }

    @SubscribeEvent
    public static void onFrameAdded(FrameAddedEvent event) {
        if (event == null) return;
        if (!CommonConfig.INSTANCE.hiddenMode.get()) return;
        if (!CommonConfig.INSTANCE.exposureDiscoveryEnabled.get()) return;

        ServerPlayer player = resolvePlayer(event);
        if (player == null) return;

        Set<ResourceLocation> uniqueMobs = new HashSet<>();
        for (LivingEntity living : event.getEntitiesInFrame()) {
            if (living == null) continue;

            ResourceLocation mobId = WildexMobIdCanonicalizer.canonicalize(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
            if (!uniqueMobs.add(mobId)) continue;

            WildexDiscoveryService.discover(
                    player,
                    mobId,
                    WildexDiscoveryService.DiscoverySource.EXPOSURE,
                    WildexDiscoveryService.DiscoveryCapture.atEntity(living)
            );
        }
    }

    private static ServerPlayer resolvePlayer(FrameAddedEvent event) {
        Entity holderEntity = event.getCameraHolderEntity();
        if (holderEntity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        if (holderEntity == null) {
            return null;
        }
        MinecraftServer server = holderEntity.getServer();
        if (server == null) return null;

        Frame frame = event.getFrame();
        if (frame == null) return null;

        Photographer photographer = frame.photographer();
        if (photographer == null || !photographer.isPlayer()) return null;

        UUID playerId = photographer.uuid();
        if (playerId == null) return null;

        return server.getPlayerList().getPlayer(playerId);
    }
}
