package de.coldfang.wildex.config;

import de.coldfang.wildex.Wildex;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    private static boolean excludedMobIdsMigrationRunning = false;

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

        debugMode = builder
                .comment("Enable Debug Mode: allows manual discovery of mobs via UI (hidden mode only)")
                .define("debugMode", false);

        kubejsBridgeEnabled = builder
                .comment(
                        "Enable Wildex KubeJS bridge emits (discovery/completed).\n"
                                + "Keep this enabled for normal use.\n"
                                + "Disable only for troubleshooting or if a modpack/addon has compatibility issues with KubeJS event handling."
                )
                .define("kubejsBridgeEnabled", true);

        spyglassDiscoveryChargeTicks = builder
                .comment("Ticks required to discover a mob while continuously aiming with a Spyglass.")
                .defineInRange("spyglassDiscoveryChargeTicks", 28, 1, 200);

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
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event == null || event.getConfig() == null) return;
        if (event.getConfig().getSpec() != SPEC) return;
        runExcludedMobIdsMigrationIfNeeded(event.getConfig());
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event == null || event.getConfig() == null) return;
        if (event.getConfig().getSpec() != SPEC) return;
        runExcludedMobIdsMigrationIfNeeded(event.getConfig());
    }

    private static void runExcludedMobIdsMigrationIfNeeded(ModConfig modConfig) {
        if (modConfig == null) return;
        if (excludedMobIdsMigrationRunning) return;
        Path migrationsFile = resolveMigrationsFile(modConfig);
        Properties props = loadMigrationProperties(migrationsFile);
        if (isMigrationDone(props, MIGRATION_KEY_EXCLUDED_IDS_V130)) return;
        excludedMobIdsMigrationRunning = true;
        try {
            List<? extends String> current = INSTANCE.excludedModIds.get();
            java.util.ArrayList<String> merged = new java.util.ArrayList<>();
            java.util.HashSet<String> seen = new java.util.HashSet<>();
            for (String s : current) {
                if (s == null) continue;
                String n = s.trim().toLowerCase(java.util.Locale.ROOT);
                if (n.isBlank() || !seen.add(n)) continue;
                merged.add(n);
            }
            for (String required : DEFAULT_EXCLUDED_MOB_IDS) {
                String n = required.trim().toLowerCase(java.util.Locale.ROOT);
                if (n.isBlank() || !seen.add(n)) continue;
                merged.add(n);
            }

            INSTANCE.excludedModIds.set(List.copyOf(merged));

            // Write marker before config save so sync reload events do not recurse forever.
            props.setProperty(MIGRATION_KEY_EXCLUDED_IDS_V130, "true");
            saveMigrationProperties(migrationsFile, props);

            var loaded = modConfig.getLoadedConfig();
            if (loaded != null) {
                loaded.save();
            }
        } finally {
            excludedMobIdsMigrationRunning = false;
        }
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
