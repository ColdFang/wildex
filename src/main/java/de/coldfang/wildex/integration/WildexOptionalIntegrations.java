package de.coldfang.wildex.integration;

import net.neoforged.fml.ModList;

public final class WildexOptionalIntegrations {

    private static final String MOD_ID_KUBEJS = "kubejs";
    private static final String MOD_ID_EXPOSURE = "exposure";
    private static final String MOD_ID_ACCESSORIFY = "accessorify";

    private WildexOptionalIntegrations() {
    }

    public static boolean isKubeJsLoaded() {
        return ModList.get().isLoaded(MOD_ID_KUBEJS);
    }

    public static boolean isExposureLoaded() {
        return ModList.get().isLoaded(MOD_ID_EXPOSURE);
    }

    public static boolean isAccessorifyLoaded() {
        return ModList.get().isLoaded(MOD_ID_ACCESSORIFY);
    }
}
