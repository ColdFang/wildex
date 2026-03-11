package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CWildexCompleteStatusPayload;
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

        WildexProgressService.ProgressSnapshot snapshot = WildexProgressService.getSnapshot(sp);
        WildexScoreboardBridge.syncPlayer(sp, snapshot);
        PacketDistributor.sendToPlayer(sp, new S2CWildexCompleteStatusPayload(snapshot.complete()));
    }
}
