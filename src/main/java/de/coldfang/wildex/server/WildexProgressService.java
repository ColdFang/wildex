package de.coldfang.wildex.server;

import de.coldfang.wildex.api.WildexApi;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// Central read model for Wildex progress values.
// Used as a single source of truth for API and future integrations.
public final class WildexProgressService {

    private WildexProgressService() {
    }

    public static ProgressSnapshot getSnapshot(ServerPlayer player) {
        if (player == null) return ProgressSnapshot.empty();
        ServerLevel level = player.serverLevel();

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);

        var playerId = player.getUUID();
        int discovered = data.getFilteredDiscoveredCount(playerId);
        int total = getTotalCount(level);
        boolean complete = isComplete(data, playerId, total, discovered);

        int percentScaled = computeScaledPercent(discovered, total);

        return new ProgressSnapshot(discovered, total, percentScaled, complete);
    }

    public static int getTotalCount(ServerLevel level) {
        if (level == null) return 0;
        return Math.max(0, WildexCompletionHelper.getTotalMobCount(level));
    }

    private static boolean isComplete(
            WildexWorldPlayerDiscoveryData data,
            java.util.UUID playerId,
            int total,
            int discovered
    ) {
        if (data == null || playerId == null) return false;

        boolean keepCompletion = CommonConfig.INSTANCE.keepCompletionAfterNewMobs.get();
        if (keepCompletion && data.isComplete(playerId)) {
            return true;
        }

        return total > 0 && discovered >= total;
    }

    private static int computeScaledPercent(int discovered, int total) {
        if (total <= 0) return 0;

        long scaled = (Math.max(0, discovered) * (long) WildexApi.COMPLETION_PERCENT_SCALE) / (long) total;

        if (scaled > WildexApi.COMPLETION_PERCENT_SCALE) return WildexApi.COMPLETION_PERCENT_SCALE;
        return (int) scaled;
    }

    public record ProgressSnapshot(
            int discoveredCount,
            int totalCount,
            int completionPercentScaled,
            boolean complete
    ) {
        public static ProgressSnapshot empty() {
            return new ProgressSnapshot(0, 0, 0, false);
        }
    }
}
