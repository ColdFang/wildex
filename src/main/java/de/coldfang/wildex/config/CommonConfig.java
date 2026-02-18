package de.coldfang.wildex.config;

import de.coldfang.wildex.Wildex;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class CommonConfig {

    public static final ModConfigSpec SPEC;
    public static final CommonConfig INSTANCE;
    private static final List<String> DEFAULT_EXCLUDED_MOB_IDS = List.of(
            "minecraft:giant",
            "minecraft:illusioner"
    );
    private static final String MIGRATIONS_FILE = "wildex-migrations.properties";
    private static final String MIGRATION_KEY_EXCLUDED_IDS_V130 = "excluded_mob_ids_v130";
    private static final String MIGRATION_KEY_CONFIG_LAYOUT_V200 = "config_layout_v200";
    private static boolean migrationsRunning = false;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new CommonConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.BooleanValue hiddenMode;
    public final ModConfigSpec.BooleanValue requireBookForKeybind;
    public final ModConfigSpec.BooleanValue giveBookOnFirstJoin;

    public final ModConfigSpec.BooleanValue debugMode;
    public final ModConfigSpec.BooleanValue kubejsBridgeEnabled;
    public final ModConfigSpec.BooleanValue exposureDiscoveryEnabled;
    public final ModConfigSpec.BooleanValue shareOffersEnabled;
    public final ModConfigSpec.BooleanValue shareOffersPaymentEnabled;
    public final ModConfigSpec.ConfigValue<String> shareOfferCurrencyItem;
    public final ModConfigSpec.IntValue shareOfferMaxPrice;
    public final ModConfigSpec.IntValue spyglassDiscoveryChargeTicks;

    public final ModConfigSpec.ConfigValue<List<? extends String>> excludedModIds;

    private CommonConfig(ModConfigSpec.Builder builder) {
        builder.push("general");

        hiddenMode = builder
                .comment("Enable Hidden Mode: mobs are hidden until discovered by killing or spying")
                .define("hiddenMode", true);

        requireBookForKeybind = builder
                .comment("Require the Wildex book in the inventory to use the keybind")
                .define("requireBookForKeybind", true);

        giveBookOnFirstJoin = builder
                .comment("Give the Wildex book once per player when they join a world for the first time.")
                .define("giveBookOnFirstJoin", true);

        spyglassDiscoveryChargeTicks = builder
                .comment("Ticks required to discover a mob while continuously aiming with a Spyglass.")
                .defineInRange("spyglassDiscoveryChargeTicks", 28, 1, 200);

        builder.pop();

        builder.push("wildexList");

        excludedModIds = builder
                .comment(
                        "Entries to exclude from Wildex.\n"
                                + "- Use a namespace to exclude an entire mod (e.g. \"alexsmobs\").\n"
                                + "- Use a full entity id to exclude a specific mob (e.g. \"alexsmobs:grizzly_bear\").\n"
                                + "Applies to UI + discovery + completion."
                )
                .defineList(
                        "excludedModIds",
                        List.of(),
                        o -> {
                            if (!(o instanceof String s)) return false;
                            String v = s.trim();
                            if (v.isEmpty()) return false;

                            int colon = v.indexOf(':');
                            if (colon < 0) {
                                return isValidNamespace(v);
                            }

                            if (colon != v.lastIndexOf(':')) return false;

                            String ns = v.substring(0, colon).trim();
                            String path = v.substring(colon + 1).trim();
                            if (!isValidNamespace(ns)) return false;
                            return isValidPath(path);
                        }
                );

        builder.pop();

        builder.push("multiplayer");

        shareOffersEnabled = builder
                .comment("Enable the Wildex entry share-offer UI in the mob details panel.")
                .define("shareOffersEnabled", true);

        shareOffersPaymentEnabled = builder
                .comment("Enable optional price input for share offers.")
                .define("shareOffersPaymentEnabled", true);

        shareOfferCurrencyItem = builder
                .comment("Currency item id used for share offers (single item type, e.g. minecraft:emerald).")
                .define("shareOfferCurrencyItem", "minecraft:emerald", o -> {
                    if (!(o instanceof String s)) return false;
                    String v = s.trim();
                    if (v.isEmpty()) return false;
                    return net.minecraft.resources.ResourceLocation.tryParse(v) != null;
                });

        shareOfferMaxPrice = builder
                .comment("Maximum allowed price value for share offers.")
                .defineInRange("shareOfferMaxPrice", 64, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("integrationDebug");

        kubejsBridgeEnabled = builder
                .comment(
                        "Enable Wildex KubeJS bridge emits (discovery/completed).\n"
                                + "Keep this enabled for normal use.\n"
                                + "Disable only for troubleshooting or if a modpack/addon has compatibility issues with KubeJS event handling."
                )
                .define("kubejsBridgeEnabled", true);

        exposureDiscoveryEnabled = builder
                .comment(
                        "Enable mob discovery from Exposure photo frames.\n"
                                + "Only applies when Exposure is installed."
                )
                .define("exposureDiscoveryEnabled", true);

        debugMode = builder
                .comment("Enable Debug Mode: allows manual discovery of mobs via UI (hidden mode only)")
                .define("debugMode", false);

        builder.pop();
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event == null || event.getConfig() == null) return;
        if (event.getConfig().getSpec() != SPEC) return;
        runMigrationsIfNeeded(event.getConfig());
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event == null || event.getConfig() == null) return;
        if (event.getConfig().getSpec() != SPEC) return;
        runMigrationsIfNeeded(event.getConfig());
    }

    private static void runMigrationsIfNeeded(ModConfig modConfig) {
        if (modConfig == null) return;
        if (migrationsRunning) return;

        Path migrationsFile = resolveMigrationsFile(modConfig);
        Properties props = loadMigrationProperties(migrationsFile);
        boolean changedAny = false;

        migrationsRunning = true;
        try {
            if (!isMigrationDone(props, MIGRATION_KEY_CONFIG_LAYOUT_V200)) {
                migrateConfigLayoutV200(modConfig);
                props.setProperty(MIGRATION_KEY_CONFIG_LAYOUT_V200, "true");
                changedAny = true;
            }

            if (!isMigrationDone(props, MIGRATION_KEY_EXCLUDED_IDS_V130)) {
                migrateExcludedMobIdsV130();
                props.setProperty(MIGRATION_KEY_EXCLUDED_IDS_V130, "true");
                changedAny = true;
            }

            if (changedAny) {
                // Write marker before config save so sync reload events do not recurse forever.
                saveMigrationProperties(migrationsFile, props);

                var loaded = modConfig.getLoadedConfig();
                if (loaded != null) {
                    loaded.save();
                }
            }
        } finally {
            migrationsRunning = false;
        }
    }

    private static boolean migrateExcludedMobIdsV130() {
        List<? extends String> current = INSTANCE.excludedModIds.get();
        ArrayList<String> merged = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();

        for (String s : current) {
            if (s == null) continue;
            String n = s.trim().toLowerCase(Locale.ROOT);
            if (n.isBlank() || !seen.add(n)) continue;
            merged.add(n);
        }
        for (String required : DEFAULT_EXCLUDED_MOB_IDS) {
            String n = required.trim().toLowerCase(Locale.ROOT);
            if (n.isBlank() || !seen.add(n)) continue;
            merged.add(n);
        }

        List<String> normalized = List.copyOf(merged);
        if (normalized.equals(current)) return false;
        INSTANCE.excludedModIds.set(normalized);
        return true;
    }

    private static boolean migrateConfigLayoutV200(ModConfig modConfig) {
        Object loaded = modConfig == null ? null : modConfig.getLoadedConfig();
        if (loaded == null) return false;

        boolean changed = false;

        changed |= migrateBooleanPath(loaded, "debugMode", INSTANCE.debugMode);
        changed |= migrateBooleanPath(loaded, "kubejsBridgeEnabled", INSTANCE.kubejsBridgeEnabled);
        changed |= migrateBooleanPath(loaded, "exposureDiscoveryEnabled", INSTANCE.exposureDiscoveryEnabled);
        changed |= migrateExcludedIdsPath(loaded);
        changed |= migrateBooleanPathBetweenSections(loaded, "integrationDebug", "multiplayer", "shareOffersEnabled", INSTANCE.shareOffersEnabled);
        changed |= migrateBooleanPathBetweenSections(loaded, "integrationDebug", "multiplayer", "shareOffersPaymentEnabled", INSTANCE.shareOffersPaymentEnabled);
        changed |= migrateStringPathBetweenSections(loaded, "integrationDebug", "multiplayer", "shareOfferCurrencyItem", INSTANCE.shareOfferCurrencyItem);
        changed |= migrateIntPathBetweenSections(loaded, "integrationDebug", "multiplayer", "shareOfferMaxPrice", INSTANCE.shareOfferMaxPrice);

        return changed;
    }

    private static boolean migrateBooleanPath(Object loaded, String key, ModConfigSpec.BooleanValue target) {
        if (target == null) return false;
        Object existingNew = getPathValue(loaded, "integrationDebug", key);
        if (existingNew != null) return false;

        Object old = getPathValue(loaded, "general", key);
        if (!(old instanceof Boolean oldBool)) return false;

        if (target.get().equals(oldBool)) return false;
        target.set(oldBool);
        return true;
    }

    private static boolean migrateExcludedIdsPath(Object loaded) {
        Object existingNew = getPathValue(loaded, "wildexList", "excludedModIds");
        if (existingNew != null) return false;

        Object old = getPathValue(loaded, "general", "excludedModIds");
        if (!(old instanceof List<?> oldList)) return false;

        ArrayList<String> normalized = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Object entry : oldList) {
            if (!(entry instanceof String s)) continue;
            String n = s.trim().toLowerCase(Locale.ROOT);
            if (n.isBlank() || !seen.add(n)) continue;
            normalized.add(n);
        }

        List<String> next = List.copyOf(normalized);
        if (next.equals(INSTANCE.excludedModIds.get())) return false;
        INSTANCE.excludedModIds.set(next);
        return true;
    }

    private static boolean migrateBooleanPathBetweenSections(
            Object loaded,
            String sourceSection,
            String targetSection,
            String key,
            ModConfigSpec.BooleanValue target
    ) {
        if (target == null) return false;
        Object existingNew = getPathValue(loaded, targetSection, key);
        if (existingNew != null) return false;

        Object old = getPathValue(loaded, sourceSection, key);
        if (!(old instanceof Boolean oldBool)) return false;

        if (target.get().equals(oldBool)) return false;
        target.set(oldBool);
        return true;
    }

    private static boolean migrateStringPathBetweenSections(
            Object loaded,
            String sourceSection,
            String targetSection,
            String key,
            ModConfigSpec.ConfigValue<String> target
    ) {
        if (target == null) return false;
        Object existingNew = getPathValue(loaded, targetSection, key);
        if (existingNew != null) return false;

        Object old = getPathValue(loaded, sourceSection, key);
        if (!(old instanceof String s)) return false;
        String normalized = s.trim();
        if (normalized.isEmpty()) return false;
        if (normalized.equals(target.get())) return false;

        target.set(normalized);
        return true;
    }

    private static boolean migrateIntPathBetweenSections(
            Object loaded,
            String sourceSection,
            String targetSection,
            String key,
            ModConfigSpec.IntValue target
    ) {
        if (target == null) return false;
        Object existingNew = getPathValue(loaded, targetSection, key);
        if (existingNew != null) return false;

        Object old = getPathValue(loaded, sourceSection, key);
        if (!(old instanceof Number n)) return false;

        int raw = n.intValue();
        int migrated = Math.max(0, raw);
        if (target.get() == migrated) return false;
        target.set(migrated);
        return true;
    }

    private static Object getPathValue(Object config, String... path) {
        if (config == null || path == null || path.length == 0) return null;

        try {
            Method getByPath = config.getClass().getMethod("get", List.class);
            return getByPath.invoke(config, List.of(path));
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Path resolveMigrationsFile(ModConfig modConfig) {
        Path configFile = modConfig.getFullPath();
        Path dir = configFile == null ? null : configFile.getParent();
        if (dir == null) return Path.of(MIGRATIONS_FILE);
        return dir.resolve(MIGRATIONS_FILE);
    }

    private static boolean isMigrationDone(Properties props, String key) {
        return "true".equalsIgnoreCase(props.getProperty(key, "false"));
    }

    private static Properties loadMigrationProperties(Path file) {
        Properties p = new Properties();
        if (file == null || !Files.exists(file)) return p;
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            Wildex.LOGGER.warn("Failed to read Wildex migration markers from {}", file, e);
        }
        return p;
    }

    private static void saveMigrationProperties(Path file, Properties props) {
        if (file == null || props == null) return;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Wildex internal migration markers");
            }
        } catch (IOException e) {
            Wildex.LOGGER.warn("Failed to write Wildex migration markers to {}", file, e);
        }
    }

    private static boolean isValidNamespace(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-'
                    || c == '.';
            if (!ok) return false;
        }
        return true;
    }

    private static boolean isValidPath(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-'
                    || c == '.'
                    || c == '/';
            if (!ok) return false;
        }
        return true;
    }
}
