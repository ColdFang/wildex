package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import net.minecraft.resources.ResourceLocation;

public final class WildexScreenTextures {

    private static final ResourceLocation VINTAGE =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_vintage.png");

    private static final ResourceLocation MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_modern.png");
    private static final ResourceLocation JUNGLE =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_jungle.png");
    private static final ResourceLocation RUNES =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_runes.png");
    private static final ResourceLocation STEAMPUNK =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/wildex_steampunk.png");

    private WildexScreenTextures() {
    }

    public static ResourceLocation vintage() {
        return VINTAGE;
    }

    public static ResourceLocation modern() {
        return MODERN;
    }

    public static ResourceLocation jungle() {
        return JUNGLE;
    }

    public static ResourceLocation runes() {
        return RUNES;
    }

    public static ResourceLocation steampunk() {
        return STEAMPUNK;
    }
}



