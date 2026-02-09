package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import net.minecraft.resources.ResourceLocation;

public final class WildexScreenTextures {

    private static final ResourceLocation VINTAGE =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_vintage.png");

    private static final ResourceLocation MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_modern.png");

    private WildexScreenTextures() {
    }

    public static ResourceLocation background(DesignStyle style) {
        return switch (style) {
            case MODERN -> MODERN;
            case VINTAGE -> VINTAGE;
        };
    }
}
