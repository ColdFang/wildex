package de.coldfang.wildex.network;

import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import de.coldfang.wildex.world.block.entity.WildexAnalyzerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class WildexRuntimeCacheEvents {

    private WildexRuntimeCacheEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        WildexNetwork.clearRuntimeCaches();
        WildexPedestalBlockEntity.clearMobTypeValidationCache();
        WildexAnalyzerBlockEntity.clearAnalyzerCaches();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WildexNetwork.clearRuntimeCaches();
        WildexPedestalBlockEntity.clearMobTypeValidationCache();
        WildexAnalyzerBlockEntity.clearAnalyzerCaches();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        WildexNetwork.clearRuntimeCaches();
        WildexPedestalBlockEntity.clearMobTypeValidationCache();
        WildexAnalyzerBlockEntity.clearAnalyzerCaches();
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        WildexNetwork.processBreedingQueue(event.getServer());
    }
}
