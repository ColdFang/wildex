package de.coldfang.wildex.client.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WildexFavoriteMobEntriesCache {

    private static final Set<ResourceLocation> FAVORITES = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean REQUESTED = new AtomicBoolean(false);

    private WildexFavoriteMobEntriesCache() {
    }

    public static void clear() {
        FAVORITES.clear();
        REQUESTED.set(false);
    }

    public static void setAll(Set<ResourceLocation> mobIds) {
        FAVORITES.clear();
        REQUESTED.set(true);
        if (mobIds == null || mobIds.isEmpty()) return;
        for (ResourceLocation id : mobIds) {
            if (id != null) FAVORITES.add(id);
        }
    }

    public static void setFavorite(ResourceLocation mobId, boolean favorite) {
        if (mobId == null) return;
        REQUESTED.set(true);
        if (favorite) {
            FAVORITES.add(mobId);
        } else {
            FAVORITES.remove(mobId);
        }
    }

    public static boolean isFavorite(ResourceLocation mobId) {
        if (mobId == null) return false;
        return FAVORITES.contains(mobId);
    }

    public static boolean markRequested() {
        return REQUESTED.compareAndSet(false, true);
    }
}
