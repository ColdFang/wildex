package de.coldfang.wildex.integration;

import net.neoforged.fml.ModList;

public final class WildexOptionalIntegrations {

    private static final String MOD_ID_KUBEJS = "kubejs";

    private WildexOptionalIntegrations() {
    }

    public static boolean isKubeJsLoaded() {
        return ModList.get().isLoaded(MOD_ID_KUBEJS);
    }
}
