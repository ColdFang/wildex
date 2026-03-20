package de.coldfang.wildex.util;

import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;

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

    private static boolean isTrackableId(ResourceLocation id) {
        if (id == null) return false;
        if (!WildexMobIdCanonicalizer.isCanonical(id)) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return false;

        String ns = id.getNamespace();
        if (EXCLUDED_NAMESPACE_DEFAULT.equals(ns)) return false;

        List<? extends String> cfg = CommonConfig.INSTANCE.excludedModIds.get();
        return !WildexIdFilterMatcher.matches(id, cfg);
    }
}
