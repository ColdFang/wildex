package de.coldfang.wildex.client;

import de.coldfang.wildex.client.render.WildexPedestalRendererRegistry;
import de.coldfang.wildex.network.WildexSpyglassPulseEvents;
import de.coldfang.wildex.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class WildexClientBootstrap {

    private WildexClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.register(WildexNetworkClient.class);
        modEventBus.addListener(WildexClientBootstrap::onClientSetup);
        WildexClientItemProperties.register(modEventBus);
        WildexPedestalRendererRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.register(WildexAccessorifyClientEvents.class);
        NeoForge.EVENT_BUS.register(WildexSpyglassPulseEvents.class);
        NeoForge.EVENT_BUS.register(WildexClientSessionEvents.class);
        NeoForge.EVENT_BUS.register(WildexCompletionClientEvents.class);
        NeoForge.EVENT_BUS.register(WildexSpyglassKnownMobOverlayClient.class);
    }

    @SuppressWarnings("deprecation")
    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WILDEX_PEDESTAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WILDEX_ANALYZER.get(), RenderType.cutout());
        });
    }
}
