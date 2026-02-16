package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.WildexSpawnCache;
import de.coldfang.wildex.config.CommonConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class WildexClientSessionEvents {

    private WildexClientSessionEvents() {
    }

    @SubscribeEvent
    public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        WildexKillCache.clear();
        WildexDiscoveryCache.clear();
        WildexLootCache.clear();
        WildexSpawnCache.clear();
        WildexPlayerUiStateCache.clear();
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
        WildexSpyglassKnownMobOverlayClient.clear();

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexNetworkClient.requestDiscoveredMobs();
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WildexKillCache.clear();
        WildexDiscoveryCache.clear();
        WildexLootCache.clear();
        WildexSpawnCache.clear();
        WildexPlayerUiStateCache.clear();
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
        WildexSpyglassKnownMobOverlayClient.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        WildexDiscoveryEffectClient.tickClient();
    }
}
