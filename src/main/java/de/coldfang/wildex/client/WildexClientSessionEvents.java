package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexEntityDisplayNameResolver;
import de.coldfang.wildex.client.data.WildexEntityVariantCatalog;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.client.data.WildexMobIndexModel;
import de.coldfang.wildex.client.data.WildexMiscCache;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.WildexSpawnCache;
import de.coldfang.wildex.client.data.WildexVariantStatsCatalog;
import de.coldfang.wildex.client.data.WildexViewedMobEntriesCache;
import de.coldfang.wildex.client.screen.MobListWidget;
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
        WildexMiscCache.clear();
        WildexSpawnCache.clear();
        WildexPlayerUiStateCache.clear();
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
        WildexSpyglassKnownMobOverlayClient.clear();
        WildexNetworkClient.clearShareState();
        WildexViewedMobEntriesCache.clear();
        WildexEntityDisplayNameResolver.clearCache();
        WildexEntityVariantCatalog.clearCache();
        WildexVariantStatsCatalog.clearCache();
        WildexMobIndexModel.clearCaches();
        MobListWidget.clearVariantUiCache();

        WildexNetworkClient.requestDiscoveredMobs();
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestViewedMobEntries();
        }
        WildexNetworkClient.requestServerConfig();
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WildexKillCache.clear();
        WildexDiscoveryCache.clear();
        WildexLootCache.clear();
        WildexMiscCache.clear();
        WildexSpawnCache.clear();
        WildexPlayerUiStateCache.clear();
        WildexDiscoveryEffectClient.clear();
        WildexCompletionCache.clear();
        WildexSpyglassKnownMobOverlayClient.clear();
        WildexNetworkClient.clearShareState();
        WildexViewedMobEntriesCache.clear();
        WildexEntityDisplayNameResolver.clearCache();
        WildexEntityVariantCatalog.clearCache();
        WildexVariantStatsCatalog.clearCache();
        WildexMobIndexModel.clearCaches();
        MobListWidget.clearVariantUiCache();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        WildexEntityVariantCatalog.tickClient(
                WildexClientConfigView.showMobVariants() && WildexClientConfigView.backgroundMobVariantProbe()
        );
        WildexVariantStatsCatalog.tickClient(
                WildexClientConfigView.showMobVariants() && WildexClientConfigView.backgroundMobVariantProbe()
        );
        WildexDiscoveryEffectClient.tickClient();
    }
}
