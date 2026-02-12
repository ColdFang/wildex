package de.coldfang.wildex.api;

import de.coldfang.wildex.server.WildexProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// read-only API surfce for external integrations (e.g. KubeJS/quests).
public final class WildexApi {

    public static final int COMPLETION_PERCENT_SCALE = 10_000;

    private WildexApi() {
    }

    public static int getDiscoveredCount(ServerPlayer player) {
        return WildexProgressService.getSnapshot(player).discoveredCount();
    }

    public static int getTotalCount(ServerLevel level) {
        return WildexProgressService.getTotalCount(level);
    }

    public static int getCompletionPercentScaled(ServerPlayer player) {
        return WildexProgressService.getSnapshot(player).completionPercentScaled();
    }

    public static boolean isComplete(ServerPlayer player) {
        return WildexProgressService.getSnapshot(player).complete();
    }
}
