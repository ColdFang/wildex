package de.coldfang.wildex.server;

import de.coldfang.wildex.api.event.WildexCompletedEvent;
import de.coldfang.wildex.api.event.WildexDiscoveryChangedEvent;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.integration.kubejs.WildexKubeJsBridge;
import de.coldfang.wildex.integration.kubejs.WildexKubeJsBridgeContract;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

public final class WildexProgressHooks {

    private WildexProgressHooks() {
    }

    public static void onDiscoveryChanged(ServerPlayer player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;

        WildexScoreboardBridge.syncPlayer(player);

        WildexProgressService.ProgressSnapshot s = WildexProgressService.getSnapshot(player);
        NeoForge.EVENT_BUS.post(new WildexDiscoveryChangedEvent(
                player,
                mobId,
                s.discoveredCount(),
                s.totalCount(),
                s.completionPercentScaled()
        ));
        if (CommonConfig.INSTANCE.kubejsBridgeEnabled.get()) {
            WildexKubeJsBridge.emit(
                    WildexKubeJsBridgeContract.EVENT_DISCOVERY_CHANGED,
                    WildexKubeJsBridgeContract.discoveryChanged(player, mobId, s).toMap()
            );
        }
    }

    public static void onCompleted(ServerPlayer player) {
        if (player == null) return;

        WildexScoreboardBridge.syncPlayer(player);

        WildexProgressService.ProgressSnapshot s = WildexProgressService.getSnapshot(player);
        NeoForge.EVENT_BUS.post(new WildexCompletedEvent(
                player,
                s.discoveredCount(),
                s.totalCount(),
                s.completionPercentScaled()
        ));
        if (CommonConfig.INSTANCE.kubejsBridgeEnabled.get()) {
            WildexKubeJsBridge.emit(
                    WildexKubeJsBridgeContract.EVENT_COMPLETED,
                    WildexKubeJsBridgeContract.completed(player, s).toMap()
            );
        }
    }
}
