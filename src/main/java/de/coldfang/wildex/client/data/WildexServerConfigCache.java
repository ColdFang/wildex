package de.coldfang.wildex.client.data;

import de.coldfang.wildex.util.WildexIdFilterMatcher;

import java.util.List;

public final class WildexServerConfigCache {

    private static volatile boolean hasServerConfig = false;
    private static volatile boolean hiddenMode = true;
    private static volatile boolean requireBookForKeybind = true;
    private static volatile boolean debugMode = false;
    private static volatile boolean shareOffersEnabled = true;
    private static volatile boolean shareOffersPaymentEnabled = true;
    private static volatile String shareOfferCurrencyItem = "minecraft:emerald";
    private static volatile int shareOfferMaxPrice = 64;
    private static volatile List<String> excludedVariantMobIds = List.of();

    private WildexServerConfigCache() {
    }

    public static void set(
            boolean hiddenMode,
            boolean requireBookForKeybind,
            boolean debugMode,
            boolean shareOffersEnabled,
            boolean shareOffersPaymentEnabled,
            String shareOfferCurrencyItem,
            int shareOfferMaxPrice,
            List<String> excludedVariantMobIds
    ) {
        setAndCheckChanged(
                hiddenMode,
                requireBookForKeybind,
                debugMode,
                shareOffersEnabled,
                shareOffersPaymentEnabled,
                shareOfferCurrencyItem,
                shareOfferMaxPrice,
                excludedVariantMobIds
        );
    }

    public static boolean setAndCheckChanged(
            boolean hiddenMode,
            boolean requireBookForKeybind,
            boolean debugMode,
            boolean shareOffersEnabled,
            boolean shareOffersPaymentEnabled,
            String shareOfferCurrencyItem,
            int shareOfferMaxPrice,
            List<String> excludedVariantMobIds
    ) {
        String normalizedCurrency =
                (shareOfferCurrencyItem == null || shareOfferCurrencyItem.isBlank()) ? "minecraft:emerald" : shareOfferCurrencyItem;
        int normalizedMaxPrice = Math.max(0, shareOfferMaxPrice);
        List<String> normalizedExcluded = WildexIdFilterMatcher.normalizeEntries(excludedVariantMobIds);

        boolean changed = !hasServerConfig
                || WildexServerConfigCache.hiddenMode != hiddenMode
                || WildexServerConfigCache.requireBookForKeybind != requireBookForKeybind
                || WildexServerConfigCache.debugMode != debugMode
                || WildexServerConfigCache.shareOffersEnabled != shareOffersEnabled
                || WildexServerConfigCache.shareOffersPaymentEnabled != shareOffersPaymentEnabled
                || !WildexServerConfigCache.shareOfferCurrencyItem.equals(normalizedCurrency)
                || WildexServerConfigCache.shareOfferMaxPrice != normalizedMaxPrice
                || !WildexServerConfigCache.excludedVariantMobIds.equals(normalizedExcluded);

        WildexServerConfigCache.hiddenMode = hiddenMode;
        WildexServerConfigCache.requireBookForKeybind = requireBookForKeybind;
        WildexServerConfigCache.debugMode = debugMode;
        WildexServerConfigCache.shareOffersEnabled = shareOffersEnabled;
        WildexServerConfigCache.shareOffersPaymentEnabled = shareOffersPaymentEnabled;
        WildexServerConfigCache.shareOfferCurrencyItem = normalizedCurrency;
        WildexServerConfigCache.shareOfferMaxPrice = normalizedMaxPrice;
        WildexServerConfigCache.excludedVariantMobIds = normalizedExcluded;
        hasServerConfig = true;
        return changed;
    }

    public static void clear() {
        hasServerConfig = false;
        excludedVariantMobIds = List.of();
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

    public static List<String> excludedVariantMobIds() {
        return excludedVariantMobIds;
    }
}
