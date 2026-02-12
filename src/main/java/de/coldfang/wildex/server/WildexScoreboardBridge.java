package de.coldfang.wildex.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class WildexScoreboardBridge {

    public static final String OBJ_DISCOVERED = "wildex_discovered";
    public static final String OBJ_TOTAL = "wildex_total";
    public static final String OBJ_PERCENT = "wildex_percent";
    public static final String OBJ_COMPLETE = "wildex_complete";

    private WildexScoreboardBridge() {
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player == null) return;
        if (player.getServer() == null) return;

        WildexProgressService.ProgressSnapshot s = WildexProgressService.getSnapshot(player);
        Scoreboard scoreboard = player.getServer().getScoreboard();

        Objective discoveredObj = ensureObjective(scoreboard, OBJ_DISCOVERED, "Wildex Discovered");
        Objective totalObj = ensureObjective(scoreboard, OBJ_TOTAL, "Wildex Total");
        Objective percentObj = ensureObjective(scoreboard, OBJ_PERCENT, "Wildex Percent x100");
        Objective completeObj = ensureObjective(scoreboard, OBJ_COMPLETE, "Wildex Complete");

        setScore(scoreboard, player, discoveredObj, s.discoveredCount());
        setScore(scoreboard, player, totalObj, s.totalCount());
        setScore(scoreboard, player, percentObj, s.completionPercentScaled());
        setScore(scoreboard, player, completeObj, s.complete() ? 1 : 0);
    }

    private static Objective ensureObjective(Scoreboard scoreboard, String name, String displayName) {
        Objective existing = scoreboard.getObjective(name);
        if (existing != null) return existing;

        return scoreboard.addObjective(
                name,
                ObjectiveCriteria.DUMMY,
                Component.literal(displayName),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );
    }

    private static void setScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int value) {
        if (objective == null) return;
        ScoreAccess access = scoreboard.getOrCreatePlayerScore(player, objective);
        access.set(Math.max(0, value));
    }
}
