package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CDiscoveredMobPayload;
import de.coldfang.wildex.network.S2CDiscoveredMobsPayload;
import de.coldfang.wildex.network.S2CWildexCompleteStatusPayload;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

@SuppressWarnings("resource")
public final class WildexDiscoveryService {

    private WildexDiscoveryService() {
    }

    public static boolean discover(ServerPlayer player, ResourceLocation mobId, DiscoverySource source) {
        if (player == null || mobId == null || source == null) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (!WildexMobFilters.isTrackable(mobId)) return false;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        boolean newlyDiscovered = data.markDiscovered(player.getUUID(), mobId);
        if (!newlyDiscovered) return false;
        WildexPedestalBlockEntity.invalidateDiscoverySnapshot(player.getUUID());

        PacketDistributor.sendToPlayer(player, new S2CDiscoveredMobPayload(mobId));

        boolean newlyCompleted = WildexCompletionHelper.markCompleteIfEligible(level, player);
        WildexProgressHooks.onDiscoveryChanged(player, mobId);
        if (newlyCompleted) {
            WildexCompletionHelper.notifyCompleted(player);
        }

        return true;
    }

    public static boolean undiscover(ServerPlayer player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
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
        PacketDistributor.sendToPlayer(player, new S2CDiscoveredMobsPayload(discovered));
        PacketDistributor.sendToPlayer(player, new S2CWildexCompleteStatusPayload(completeNow));
        WildexProgressHooks.onDiscoveryChanged(player, mobId);
        return true;
    }

    public enum DiscoverySource {
        KILL,
        SPYGLASS,
        ANALYZER,
        DEBUG,
        EXPOSURE,
        SHARE
    }
}
