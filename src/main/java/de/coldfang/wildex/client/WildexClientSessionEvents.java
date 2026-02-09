package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.client.data.WildexSpawnCache;
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
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WildexKillCache.clear();
        WildexDiscoveryCache.clear();
        WildexLootCache.clear();
        WildexSpawnCache.clear();
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        WildexDiscoveryEffectClient.tickClient();
    }
}
