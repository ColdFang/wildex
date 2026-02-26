package de.coldfang.wildex.server;

import de.coldfang.wildex.api.WildexApi;
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
        if (!(player.level() instanceof ServerLevel level)) return ProgressSnapshot.empty();

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);

        int discovered = data.getFilteredDiscoveredCount(player.getUUID());
        int total = getTotalCount(level);
        boolean complete = WildexCompletionHelper.isCurrentlyComplete(level, player.getUUID());

        int percentScaled = computeScaledPercent(discovered, total);

        return new ProgressSnapshot(discovered, total, percentScaled, complete);
    }

    public static int getTotalCount(ServerLevel level) {
        if (level == null) return 0;
        return Math.max(0, WildexCompletionHelper.getTotalMobCount(level));
    }

    private static int computeScaledPercent(int discovered, int total) {
        if (total <= 0) return 0;

        long scaled = (Math.max(0, discovered) * (long) WildexApi.COMPLETION_PERCENT_SCALE) / (long) total;

        if (scaled < 0L) return 0;
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
