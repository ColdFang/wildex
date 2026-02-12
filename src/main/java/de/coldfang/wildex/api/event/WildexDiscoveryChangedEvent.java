package de.coldfang.wildex.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class WildexDiscoveryChangedEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation mobId;
    private final int discoveredCount;
    private final int totalCount;
    private final int completionPercentScaled;

    public WildexDiscoveryChangedEvent(
            ServerPlayer player,
            ResourceLocation mobId,
            int discoveredCount,
            int totalCount,
            int completionPercentScaled
    ) {
        this.player = player;
        this.mobId = mobId;
        this.discoveredCount = discoveredCount;
        this.totalCount = totalCount;
        this.completionPercentScaled = completionPercentScaled;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceLocation mobId() {
        return mobId;
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
