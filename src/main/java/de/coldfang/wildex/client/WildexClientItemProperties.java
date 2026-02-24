package de.coldfang.wildex.client;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import de.coldfang.wildex.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class WildexClientItemProperties {

    private static final ResourceLocation BOOK_THEME_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "book_theme");

    private WildexClientItemProperties() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(WildexClientItemProperties::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.WILDEX_BOOK.get(),
                BOOK_THEME_PROPERTY,
                (stack, level, entity, seed) -> styleToModelValue(ClientConfig.INSTANCE.designStyle.get())
        ));
    }

    private static float styleToModelValue(DesignStyle style) {
        if (style == null) return 0.0f;
        return switch (style) {
            case VINTAGE -> 0.0f;
            case MODERN -> 1.0f;
            case JUNGLE -> 2.0f;
            case RUNES -> 3.0f;
            case STEAMPUNK -> 4.0f;
        };
    }
}
