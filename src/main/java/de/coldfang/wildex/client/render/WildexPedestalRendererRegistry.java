package de.coldfang.wildex.client.render;

import de.coldfang.wildex.registry.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class WildexPedestalRendererRegistry {

    private WildexPedestalRendererRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(WildexPedestalRendererRegistry::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.WILDEX_PEDESTAL.get(),
                WildexPedestalBlockEntityRenderer::new
        );
    }
}
