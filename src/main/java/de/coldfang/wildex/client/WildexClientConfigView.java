package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexServerConfigCache;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;

public final class WildexClientConfigView {

    private WildexClientConfigView() {
    }

    private static boolean useServerValue() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null && WildexServerConfigCache.hasServerConfig();
    }

    private static boolean isRemoteSession() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null;
    }

    public static boolean hiddenMode() {
        if (isRemoteSession() && !WildexServerConfigCache.hasServerConfig()) return true;
        return useServerValue() ? WildexServerConfigCache.hiddenMode() : CommonConfig.INSTANCE.hiddenMode.get();
    }

    public static boolean requireBookForKeybind() {
        if (isRemoteSession() && !WildexServerConfigCache.hasServerConfig()) return true;
        return useServerValue() ? WildexServerConfigCache.requireBookForKeybind() : CommonConfig.INSTANCE.requireBookForKeybind.get();
    }

    public static boolean debugMode() {
        if (isRemoteSession() && !WildexServerConfigCache.hasServerConfig()) return false;
        return useServerValue() ? WildexServerConfigCache.debugMode() : CommonConfig.INSTANCE.debugMode.get();
    }

    public static boolean shareOffersEnabled() {
        return useServerValue() ? WildexServerConfigCache.shareOffersEnabled() : CommonConfig.INSTANCE.shareOffersEnabled.get();
    }

    public static boolean shareOffersPaymentEnabled() {
        return useServerValue() ? WildexServerConfigCache.shareOffersPaymentEnabled() : CommonConfig.INSTANCE.shareOffersPaymentEnabled.get();
    }

    public static String shareOfferCurrencyItem() {
        return useServerValue() ? WildexServerConfigCache.shareOfferCurrencyItem() : CommonConfig.INSTANCE.shareOfferCurrencyItem.get();
    }

    public static int shareOfferMaxPrice() {
        return useServerValue() ? WildexServerConfigCache.shareOfferMaxPrice() : CommonConfig.INSTANCE.shareOfferMaxPrice.get();
    }

    public static float wildexUiScale() {
        double raw = ClientConfig.INSTANCE.wildexUiScale.get();
        if (raw < 1.00d) return 1.00f;
        if (raw > 4.00d) return 4.00f;
        return (float) raw;
    }
}
