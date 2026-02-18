package de.coldfang.wildex.client.data;

public final class WildexServerConfigCache {

    private static volatile boolean hasServerConfig = false;
    private static volatile boolean hiddenMode = true;
    private static volatile boolean requireBookForKeybind = true;
    private static volatile boolean debugMode = false;
    private static volatile boolean shareOffersEnabled = true;
    private static volatile boolean shareOffersPaymentEnabled = true;
    private static volatile String shareOfferCurrencyItem = "minecraft:emerald";
    private static volatile int shareOfferMaxPrice = 64;

    private WildexServerConfigCache() {
    }

    public static void set(
            boolean hiddenMode,
            boolean requireBookForKeybind,
            boolean debugMode,
            boolean shareOffersEnabled,
            boolean shareOffersPaymentEnabled,
            String shareOfferCurrencyItem,
            int shareOfferMaxPrice
    ) {
        WildexServerConfigCache.hiddenMode = hiddenMode;
        WildexServerConfigCache.requireBookForKeybind = requireBookForKeybind;
        WildexServerConfigCache.debugMode = debugMode;
        WildexServerConfigCache.shareOffersEnabled = shareOffersEnabled;
        WildexServerConfigCache.shareOffersPaymentEnabled = shareOffersPaymentEnabled;
        WildexServerConfigCache.shareOfferCurrencyItem =
                (shareOfferCurrencyItem == null || shareOfferCurrencyItem.isBlank()) ? "minecraft:emerald" : shareOfferCurrencyItem;
        WildexServerConfigCache.shareOfferMaxPrice = Math.max(0, shareOfferMaxPrice);
        hasServerConfig = true;
    }

    public static void clear() {
        hasServerConfig = false;
    }

    public static boolean hasServerConfig() {
        return hasServerConfig;
    }

    public static boolean hiddenMode() {
        return hiddenMode;
    }

    public static boolean requireBookForKeybind() {
        return requireBookForKeybind;
    }

    public static boolean debugMode() {
        return debugMode;
    }

    public static boolean shareOffersEnabled() {
        return shareOffersEnabled;
    }

    public static boolean shareOffersPaymentEnabled() {
        return shareOffersPaymentEnabled;
    }

    public static String shareOfferCurrencyItem() {
        return shareOfferCurrencyItem;
    }

    public static int shareOfferMaxPrice() {
        return shareOfferMaxPrice;
    }
}
