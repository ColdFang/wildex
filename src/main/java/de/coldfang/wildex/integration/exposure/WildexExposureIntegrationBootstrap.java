package de.coldfang.wildex.integration.exposure;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.integration.WildexOptionalIntegrations;

public final class WildexExposureIntegrationBootstrap {

    private WildexExposureIntegrationBootstrap() {
    }

    public static void registerIfAvailable() {
        if (!WildexOptionalIntegrations.isExposureLoaded()) return;

        try {
            Class<?> integrationClass = Class.forName("de.coldfang.wildex.integration.exposure.WildexExposureEvents");
            integrationClass.getMethod("register").invoke(null);
            Wildex.LOGGER.info("Wildex Exposure integration is active.");
        } catch (Throwable t) {
            Wildex.LOGGER.warn("Wildex failed to initialize Exposure integration. Continuing without it.", t);
        }
    }
}
