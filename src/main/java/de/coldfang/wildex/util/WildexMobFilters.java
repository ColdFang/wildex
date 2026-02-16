package de.coldfang.wildex.util;

import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Locale;

public final class WildexMobFilters {

    private static final String EXCLUDED_NAMESPACE_DEFAULT = "cobblehelper";

    private WildexMobFilters() {
    }

    public static boolean isTrackable(ResourceLocation id) {
        return isTrackableId(id);
    }

    public static boolean isTrackable(EntityType<?> type) {
        if (type == null) return false;
        if (type == EntityType.PLAYER) return false;
        ResourceLocation id = EntityType.getKey(type);
        return isTrackableId(id);
    }

    public static boolean isTrackableMobType(EntityType<?> type) {
        if (!isTrackable(type)) return false;
        return Mob.class.isAssignableFrom(type.getBaseClass());
    }

    public static boolean isTrackableMobId(ResourceLocation id) {
        if (id == null) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return false;
        return isTrackableMobType(type);
    }

    private static boolean isTrackableId(ResourceLocation id) {
        if (id == null) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return false;

        String ns = id.getNamespace();
        if (EXCLUDED_NAMESPACE_DEFAULT.equals(ns)) return false;

        List<? extends String> cfg = CommonConfig.INSTANCE.excludedModIds.get();
        if (cfg.isEmpty()) return true;

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
