package de.coldfang.wildex.client;

import de.coldfang.wildex.integration.WildexOptionalIntegrations;
import de.coldfang.wildex.integration.accessorify.WildexAccessorifyIntegration;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class WildexAccessorifyClientEvents {

    private static final int ACTIVE_KEEPALIVE_INTERVAL = 5;

    private static boolean lastSentActive = false;
    private static int activeKeepaliveTicks = 0;

    private WildexAccessorifyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        boolean active = false;
        if (mc.player != null && mc.getConnection() != null && WildexOptionalIntegrations.isAccessorifyLoaded()) {
            active = WildexAccessorifyIntegration.isClientAccessorySpyglassActive(mc.player);
        }

        if (active) {
            activeKeepaliveTicks++;
        } else {
            activeKeepaliveTicks = 0;
        }

        boolean shouldSend = active != lastSentActive || (active && activeKeepaliveTicks >= ACTIVE_KEEPALIVE_INTERVAL);
        if (!shouldSend) return;

        if (mc.getConnection() != null) {
            WildexNetworkClient.sendAccessorifySpyglassState(active);
        }

        lastSentActive = active;
        activeKeepaliveTicks = 0;
    }

    @SubscribeEvent
    public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        reset();
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        lastSentActive = false;
        activeKeepaliveTicks = 0;
    }
}
