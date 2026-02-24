package de.coldfang.wildex.client;

import de.coldfang.wildex.network.WildexSpyglassPulseEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class WildexClientBootstrap {

    private WildexClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.register(WildexNetworkClient.class);
        WildexClientItemProperties.register(modEventBus);
        NeoForge.EVENT_BUS.register(WildexSpyglassPulseEvents.class);
        NeoForge.EVENT_BUS.register(WildexClientSessionEvents.class);
        NeoForge.EVENT_BUS.register(WildexCompletionClientEvents.class);
        NeoForge.EVENT_BUS.register(WildexSpyglassKnownMobOverlayClient.class);
    }
}
