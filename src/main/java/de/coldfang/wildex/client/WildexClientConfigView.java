package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexServerConfigCache;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

public final class WildexClientConfigView {

    private static final boolean DEFAULT_HIDDEN_MODE = true;
    private static final boolean DEFAULT_REQUIRE_BOOK_FOR_KEYBIND = true;
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static final boolean DEFAULT_SHARE_OFFERS_ENABLED = true;
    private static final boolean DEFAULT_SHARE_OFFERS_PAYMENT_ENABLED = true;
    private static final String DEFAULT_SHARE_OFFER_CURRENCY_ITEM = "minecraft:emerald";
    private static final int DEFAULT_SHARE_OFFER_MAX_PRICE = 64;
    private static final boolean DEFAULT_HIDE_GUI_SCALE_SLIDER = true;
    private static final boolean DEFAULT_SHOW_MOB_VARIANTS = true;
    private static final boolean DEFAULT_BACKGROUND_MOB_VARIANT_PROBE = true;
    private static final ClientConfig.DesignStyle DEFAULT_DESIGN_STYLE = ClientConfig.DesignStyle.VINTAGE;
    private static final boolean DEFAULT_SHOW_DISCOVERED_SPYGLASS_OVERLAY = true;
    private static final double DEFAULT_WILDEX_UI_SCALE = 2.0d;

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
        return useServerValue()
                ? WildexServerConfigCache.hiddenMode()
                : configValue(() -> CommonConfig.INSTANCE.hiddenMode.get(), DEFAULT_HIDDEN_MODE);
    }

    public static boolean requireBookForKeybind() {
        if (isRemoteSession() && !WildexServerConfigCache.hasServerConfig()) return true;
        return useServerValue()
                ? WildexServerConfigCache.requireBookForKeybind()
                : configValue(() -> CommonConfig.INSTANCE.requireBookForKeybind.get(), DEFAULT_REQUIRE_BOOK_FOR_KEYBIND);
    }

    public static boolean debugMode() {
        if (isRemoteSession() && !WildexServerConfigCache.hasServerConfig()) return false;
        return useServerValue()
                ? WildexServerConfigCache.debugMode()
                : configValue(() -> CommonConfig.INSTANCE.debugMode.get(), DEFAULT_DEBUG_MODE);
    }

    public static boolean shareOffersEnabled() {
        return useServerValue()
                ? WildexServerConfigCache.shareOffersEnabled()
                : configValue(() -> CommonConfig.INSTANCE.shareOffersEnabled.get(), DEFAULT_SHARE_OFFERS_ENABLED);
    }

    public static boolean shareOffersPaymentEnabled() {
        return useServerValue()
                ? WildexServerConfigCache.shareOffersPaymentEnabled()
                : configValue(() -> CommonConfig.INSTANCE.shareOffersPaymentEnabled.get(), DEFAULT_SHARE_OFFERS_PAYMENT_ENABLED);
    }

    public static String shareOfferCurrencyItem() {
        return useServerValue()
                ? WildexServerConfigCache.shareOfferCurrencyItem()
                : configValue(() -> CommonConfig.INSTANCE.shareOfferCurrencyItem.get(), DEFAULT_SHARE_OFFER_CURRENCY_ITEM);
    }

    public static int shareOfferMaxPrice() {
        return useServerValue()
                ? WildexServerConfigCache.shareOfferMaxPrice()
                : configValue(() -> CommonConfig.INSTANCE.shareOfferMaxPrice.get(), DEFAULT_SHARE_OFFER_MAX_PRICE);
    }

    public static List<String> excludedVariantMobIds() {
        return useServerValue()
                ? WildexServerConfigCache.excludedVariantMobIds()
                : configValue(() -> List.copyOf(CommonConfig.INSTANCE.excludedVariantMobIds.get()), List.of());
    }

    public static boolean hideGuiScaleSlider() {
        return configValue(() -> ClientConfig.INSTANCE.hideGuiScaleSlider.get(), DEFAULT_HIDE_GUI_SCALE_SLIDER);
    }

    public static boolean showMobVariants() {
        return configValue(() -> ClientConfig.INSTANCE.showMobVariants.get(), DEFAULT_SHOW_MOB_VARIANTS);
    }

    public static boolean backgroundMobVariantProbe() {
        return configValue(() -> ClientConfig.INSTANCE.backgroundMobVariantProbe.get(), DEFAULT_BACKGROUND_MOB_VARIANT_PROBE);
    }

    public static ClientConfig.DesignStyle designStyle() {
        return configValue(() -> ClientConfig.INSTANCE.designStyle.get(), DEFAULT_DESIGN_STYLE);
    }

    public static boolean showDiscoveredSpyglassOverlay() {
        return configValue(
                () -> ClientConfig.INSTANCE.showDiscoveredSpyglassOverlay.get(),
                DEFAULT_SHOW_DISCOVERED_SPYGLASS_OVERLAY
        );
    }

    public static double wildexUiScale() {
        return configValue(() -> ClientConfig.INSTANCE.wildexUiScale.get(), DEFAULT_WILDEX_UI_SCALE);
    }

    private static <T> T configValue(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }
}
