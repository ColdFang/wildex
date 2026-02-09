package de.coldfang.wildex.client.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexDiscoveryCache {

    private static final Set<ResourceLocation> DISCOVERED = ConcurrentHashMap.newKeySet();

    private WildexDiscoveryCache() {
    }

    public static boolean isDiscovered(ResourceLocation mobId) {
        if (mobId == null) return false;
        return DISCOVERED.contains(mobId);
    }

    public static int count() {
        return DISCOVERED.size();
    }

    public static void setAll(Collection<ResourceLocation> mobIds) {
        DISCOVERED.clear();
        if (mobIds == null || mobIds.isEmpty()) return;
        DISCOVERED.addAll(mobIds);
    }

    public static void add(ResourceLocation mobId) {
        if (mobId == null) return;
        DISCOVERED.add(mobId);
    }

    public static void clear() {
        DISCOVERED.clear();
    }
}
