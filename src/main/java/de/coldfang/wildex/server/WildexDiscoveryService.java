package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CDiscoveredMobPayload;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

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

    public enum DiscoverySource {
        KILL,
        SPYGLASS,
        DEBUG,
        EXPOSURE,
        SHARE
    }
}
