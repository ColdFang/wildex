package de.coldfang.wildex.integration.kubejs;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class WildexKubeJsLifecycleEvents {

    private WildexKubeJsLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        WildexKubeJsScriptBridge.clearAll();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WildexKubeJsScriptBridge.clearAll();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // Null player means a full datapack reload sync.
        if (event.getPlayer() != null) return;
        WildexKubeJsScriptBridge.clearAll();
    }
}

