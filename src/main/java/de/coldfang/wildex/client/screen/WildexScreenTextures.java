package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import net.minecraft.resources.ResourceLocation;

public final class WildexScreenTextures {

    private static final ResourceLocation VINTAGE =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_vintage.png");

    private static final ResourceLocation MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_modern.png");

    private WildexScreenTextures() {
    }

    public static ResourceLocation vintage() {
        return VINTAGE;
    }

    public static ResourceLocation modern() {
        return MODERN;
    }
}



