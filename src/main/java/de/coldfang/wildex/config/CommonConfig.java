package de.coldfang.wildex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class CommonConfig {

    public static final ModConfigSpec SPEC;
    public static final CommonConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new CommonConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.BooleanValue hiddenMode;
    public final ModConfigSpec.BooleanValue requireBookForKeybind;
    public final ModConfigSpec.BooleanValue giveBookOnFirstJoin;

    public final ModConfigSpec.BooleanValue debugMode;

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

        excludedModIds = builder
                .comment(
                        "Entries to exclude from Wildex.\n"
                                + "- Use a namespace to exclude an entire mod (e.g. \"alexsmobs\").\n"
                                + "- Use a full entity id to exclude a specific mob (e.g. \"alexsmobs:grizzly_bear\").\n"
                                + "Applies to UI + discovery + completion."
                )
                .defineListAllowEmpty(
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
