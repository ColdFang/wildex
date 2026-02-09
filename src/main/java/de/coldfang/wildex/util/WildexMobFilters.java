package de.coldfang.wildex.util;

import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Locale;

public final class WildexMobFilters {

    private static final String EXCLUDED_NAMESPACE_DEFAULT = "cobblehelper";

    private static final ResourceLocation GIANT = ResourceLocation.fromNamespaceAndPath("minecraft", "giant");
    private static final ResourceLocation ILLUSIONER = ResourceLocation.fromNamespaceAndPath("minecraft", "illusioner");

    private WildexMobFilters() {
    }

    public static boolean isTrackable(ResourceLocation id) {
        if (id == null) return false;

        if (GIANT.equals(id)) return false;
        if (ILLUSIONER.equals(id)) return false;

        String ns = id.getNamespace();
        if (EXCLUDED_NAMESPACE_DEFAULT.equals(ns)) return false;

        List<? extends String> cfg = CommonConfig.INSTANCE.excludedModIds.get();
        if (cfg == null || cfg.isEmpty()) return true;

        String full = id.toString();

        for (String raw : cfg) {
            String ex = normalize(raw);
            if (ex.isBlank()) continue;

            if (isFullId(ex)) {
                if (full.equals(ex)) return false;
                continue;
            }

            if (ns.equals(ex)) return false;
        }

        return true;
    }

    public static boolean isTrackable(EntityType<?> type) {
        if (type == null) return false;
        if (type == EntityType.PLAYER) return false;
        ResourceLocation id = EntityType.getKey(type);
        return isTrackable(id);
    }

    private static boolean isFullId(String s) {
        int c = s.indexOf(':');
        if (c <= 0) return false;
        return c == s.lastIndexOf(':') && (c + 1) < s.length();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
