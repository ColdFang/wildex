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

    public static boolean hideGuiScaleSlider() {
        return ClientConfig.INSTANCE.hideGuiScaleSlider.get();
    }

    public static boolean showMobVariants() {
        return ClientConfig.INSTANCE.showMobVariants.get();
    }

    public static boolean backgroundMobVariantProbe() {
        return ClientConfig.INSTANCE.backgroundMobVariantProbe.get();
    }
}
