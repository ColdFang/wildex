package de.coldfang.wildex.client.data.model;

import net.minecraft.resources.ResourceLocation;

public record WildexDiscoveryDetails(
        boolean hasData,
        boolean legacyMissingData,
        String sourceId,
        String sourceDetail,
        ResourceLocation dimensionId,
        int x,
        int y,
        int z,
        long discoveredAtEpochMillis
) {

    public static WildexDiscoveryDetails empty() {
        return new WildexDiscoveryDetails(
                false,
                false,
                "",
                "",
                ResourceLocation.withDefaultNamespace("overworld"),
                0,
                0,
                0,
                0L
        );
    }
}
