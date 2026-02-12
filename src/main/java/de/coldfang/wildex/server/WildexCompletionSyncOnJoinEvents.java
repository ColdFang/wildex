package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CWildexCompleteStatusPayload;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WildexCompletionSyncOnJoinEvents {

    private WildexCompletionSyncOnJoinEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event == null) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        WildexScoreboardBridge.syncPlayer(sp);

        boolean complete = WildexWorldPlayerDiscoveryData.get(level).isComplete(sp.getUUID());
        PacketDistributor.sendToPlayer(sp, new S2CWildexCompleteStatusPayload(complete));
    }
}
