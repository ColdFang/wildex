package de.coldfang.wildex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {

    public static final ModConfigSpec SPEC;
    public static final ClientConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ClientConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.EnumValue<DesignStyle> designStyle;
    public final ModConfigSpec.BooleanValue showDiscoveryToasts;
    public final ModConfigSpec.BooleanValue showDiscoveredSpyglassOverlay;
    public final ModConfigSpec.BooleanValue hideGuiScaleSlider;
    public final ModConfigSpec.DoubleValue wildexUiScale;

    private ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("ui");

        designStyle = builder
                .comment("Wildex UI design style")
                .defineEnum("designStyle", DesignStyle.VINTAGE);

        showDiscoveryToasts = builder
                .comment("Show a toast notification when discovering a new mob in hidden mode.")
                .define("showDiscoveryToasts", true);

        showDiscoveredSpyglassOverlay = builder
                .comment("Show a small overlay when aiming at an already discovered mob with a spyglass.")
                .define("showDiscoveredSpyglassOverlay", true);

        hideGuiScaleSlider = builder
                .comment("Hide the GUI scale slider in the Wildex screen.")
                .define("hideGuiScaleSlider", true);

        wildexUiScale = builder
                .comment("Wildex UI scale factor. 2.0 = 100% display size.")
                .defineInRange("wildexUiScale", 2.0d, 1.00d, 4.00d);

        builder.pop();
    }

    public enum DesignStyle {
        VINTAGE,
        MODERN,
        JUNGLE,
        RUNES,
        STEAMPUNK
    }
}
