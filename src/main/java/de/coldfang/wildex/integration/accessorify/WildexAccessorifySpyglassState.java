package de.coldfang.wildex.integration.accessorify;

import de.coldfang.wildex.integration.WildexOptionalIntegrations;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexAccessorifySpyglassState {

    private static final long ACTIVE_GRACE_TICKS = 10L;
    private static final Map<UUID, Long> ACTIVE_UNTIL_TICK = new ConcurrentHashMap<>();

    private WildexAccessorifySpyglassState() {
    }

    @SuppressWarnings("resource")
    public static void update(ServerPlayer player, boolean active) {
        if (player == null) return;

        UUID playerId = player.getUUID();
        if (!active) {
            ACTIVE_UNTIL_TICK.remove(playerId);
            return;
        }

        ACTIVE_UNTIL_TICK.put(playerId, player.level().getGameTime() + ACTIVE_GRACE_TICKS);
    }

    @SuppressWarnings("resource")
    public static boolean isActive(ServerPlayer player) {
        if (player == null || !WildexOptionalIntegrations.isAccessorifyLoaded()) return false;

        UUID playerId = player.getUUID();
        Long activeUntil = ACTIVE_UNTIL_TICK.get(playerId);
        if (activeUntil == null) return false;

        long now = player.level().getGameTime();
        if (activeUntil < now) {
            ACTIVE_UNTIL_TICK.remove(playerId);
            return false;
        }

        return WildexAccessorifyIntegration.isAccessorySpyglassEquipped(player);
    }

    public static void clear(UUID playerId) {
        if (playerId == null) return;
        ACTIVE_UNTIL_TICK.remove(playerId);
    }
}
