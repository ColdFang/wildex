package de.coldfang.wildex.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class WildexCompletedEvent extends Event {

    private final ServerPlayer player;
    private final int discoveredCount;
    private final int totalCount;
    private final int completionPercentScaled;

    public WildexCompletedEvent(
            ServerPlayer player,
            int discoveredCount,
            int totalCount,
            int completionPercentScaled
    ) {
        this.player = player;
        this.discoveredCount = discoveredCount;
        this.totalCount = totalCount;
        this.completionPercentScaled = completionPercentScaled;
    }

    public ServerPlayer player() {
        return player;
    }

    public int discoveredCount() {
        return discoveredCount;
    }

    public int totalCount() {
        return totalCount;
    }

    public int completionPercentScaled() {
        return completionPercentScaled;
    }
}
