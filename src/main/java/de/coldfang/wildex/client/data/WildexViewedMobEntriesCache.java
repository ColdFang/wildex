package de.coldfang.wildex.client.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexViewedMobEntriesCache {

    private static final Set<ResourceLocation> VIEWED = ConcurrentHashMap.newKeySet();

    private WildexViewedMobEntriesCache() {
    }

    public static void clear() {
        VIEWED.clear();
    }

    public static void setAll(Set<ResourceLocation> mobIds) {
        VIEWED.clear();
        if (mobIds == null || mobIds.isEmpty()) return;
        for (ResourceLocation id : mobIds) {
            if (id != null) VIEWED.add(id);
        }
    }

    public static boolean add(ResourceLocation mobId) {
        if (mobId == null) return false;
        return VIEWED.add(mobId);
    }

    public static boolean isViewed(ResourceLocation mobId) {
        if (mobId == null) return false;
        return VIEWED.contains(mobId);
    }
}
