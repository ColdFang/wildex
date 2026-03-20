package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CDiscoveredMobPayload;
import de.coldfang.wildex.network.S2CDiscoveredMobsPayload;
import de.coldfang.wildex.network.S2CMobDiscoveryDetailsPayload;
import de.coldfang.wildex.network.S2CWildexCompleteStatusPayload;
import de.coldfang.wildex.util.WildexMobIdCanonicalizer;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryDetailsData;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;

public final class WildexDiscoveryService {

    private WildexDiscoveryService() {
    }

    public static boolean discover(ServerPlayer player, ResourceLocation mobId, DiscoverySource source) {
        return discover(player, mobId, source, DiscoveryCapture.atPlayer(player));
    }

    public static boolean discover(ServerPlayer player, ResourceLocation mobId, DiscoverySource source, DiscoveryCapture capture) {
        if (player == null || mobId == null || source == null) return false;
        ServerLevel level = player.serverLevel();
        mobId = WildexMobIdCanonicalizer.canonicalize(mobId);
        if (!WildexMobFilters.isTrackable(mobId)) return false;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        boolean newlyDiscovered = data.markDiscovered(player.getUUID(), mobId);
        if (!newlyDiscovered) return false;
        recordDiscoveryDetailsIfMissing(level, player.getUUID(), mobId, source, capture);
        WildexPedestalBlockEntity.invalidateDiscoverySnapshot(player.getUUID());

        PacketDistributor.sendToPlayer(player, new S2CDiscoveredMobPayload(mobId));
        PacketDistributor.sendToPlayer(player, buildDiscoveryDetailsPayload(level, player.getUUID(), mobId));

        boolean newlyCompleted = WildexCompletionHelper.markCompleteIfEligible(level, player);
        WildexProgressHooks.onDiscoveryChanged(player, mobId);
        if (newlyCompleted) {
            WildexCompletionHelper.notifyCompleted(player);
        }

        return true;
    }

    public static boolean undiscover(ServerPlayer player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        ServerLevel level = player.serverLevel();
        mobId = WildexMobIdCanonicalizer.canonicalize(mobId);
        if (!WildexMobFilters.isTrackable(mobId)) return false;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        boolean removed = data.unmarkDiscovered(player.getUUID(), mobId);
        if (!removed) return false;

        data.unmarkComplete(player.getUUID());
        boolean completeNow = WildexCompletionHelper.isCurrentlyComplete(level, player.getUUID());
        if (completeNow) {
            data.markComplete(player.getUUID());
        }

        WildexPedestalBlockEntity.invalidateDiscoverySnapshot(player.getUUID());

        Set<ResourceLocation> discovered = data.getDiscovered(player.getUUID());
        WildexWorldPlayerDiscoveryDetailsData.get(level).removeDetails(player.getUUID(), mobId);
        PacketDistributor.sendToPlayer(player, new S2CDiscoveredMobsPayload(discovered));
        PacketDistributor.sendToPlayer(player, buildDiscoveryDetailsPayload(level, player.getUUID(), mobId));
        PacketDistributor.sendToPlayer(player, new S2CWildexCompleteStatusPayload(completeNow));
        WildexProgressHooks.onDiscoveryChanged(player, mobId);
        return true;
    }

    public static void recordDiscoveryDetailsIfMissing(
            ServerLevel level,
            UUID playerId,
            ResourceLocation mobId,
            DiscoverySource source,
            DiscoveryCapture capture
    ) {
        if (level == null || playerId == null || mobId == null || source == null || capture == null) return;
        mobId = WildexMobIdCanonicalizer.canonicalize(mobId);
        if (!WildexMobFilters.isTrackable(mobId)) return;

        WildexWorldPlayerDiscoveryDetailsData.DiscoveryDetails details =
                new WildexWorldPlayerDiscoveryDetailsData.DiscoveryDetails(
                        source.name().toLowerCase(java.util.Locale.ROOT),
                        capture.detail() == null ? "" : capture.detail(),
                        capture.dimensionId() == null ? level.dimension().location() : capture.dimensionId(),
                        capture.x(),
                        capture.y(),
                        capture.z(),
                        System.currentTimeMillis()
                );
        WildexWorldPlayerDiscoveryDetailsData.get(level).setDetailsIfAbsent(playerId, mobId, details);
    }

    public static S2CMobDiscoveryDetailsPayload buildDiscoveryDetailsPayload(
            ServerLevel level,
            UUID playerId,
            ResourceLocation mobId
    ) {
        if (level == null || playerId == null || mobId == null) {
            return S2CMobDiscoveryDetailsPayload.empty(mobId, false);
        }
        mobId = WildexMobIdCanonicalizer.canonicalize(mobId);
        if (!WildexMobFilters.isTrackable(mobId)) {
            return S2CMobDiscoveryDetailsPayload.empty(mobId, false);
        }

        boolean discovered = WildexWorldPlayerDiscoveryData.get(level).isDiscovered(playerId, mobId);
        WildexWorldPlayerDiscoveryDetailsData.DiscoveryDetails details =
                WildexWorldPlayerDiscoveryDetailsData.get(level).getDetails(playerId, mobId);
        if (details == null) {
            return S2CMobDiscoveryDetailsPayload.empty(mobId, discovered);
        }

        return new S2CMobDiscoveryDetailsPayload(
                mobId,
                true,
                false,
                details.sourceId(),
                details.sourceDetail(),
                details.dimensionId(),
                details.x(),
                details.y(),
                details.z(),
                details.discoveredAtEpochMillis()
        );
    }

    public enum DiscoverySource {
        KILL,
        SPYGLASS,
        ANALYZER,
        DEBUG,
        EXPOSURE,
        SHARE
    }

    public record DiscoveryCapture(
            ResourceLocation dimensionId,
            int x,
            int y,
            int z,
            String detail
    ) {
        @SuppressWarnings("resource")
        public static DiscoveryCapture atPlayer(ServerPlayer player) {
            if (player == null) return null;
            return new DiscoveryCapture(
                    player.serverLevel().dimension().location(),
                    player.blockPosition().getX(),
                    player.blockPosition().getY(),
                    player.blockPosition().getZ(),
                    ""
            );
        }

        public static DiscoveryCapture at(ServerLevel level, BlockPos pos) {
            return at(level, pos, "");
        }

        public static DiscoveryCapture at(ServerLevel level, BlockPos pos, String detail) {
            if (level == null || pos == null) return null;
            return new DiscoveryCapture(
                    level.dimension().location(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    detail == null ? "" : detail
            );
        }

        @SuppressWarnings("resource")
        public static DiscoveryCapture atEntity(Entity entity) {
            if (entity == null || !(entity.level() instanceof ServerLevel)) return null;
            return at((ServerLevel) entity.level(), entity.blockPosition(), "");
        }
    }
}
