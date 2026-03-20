package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.client.data.model.WildexDiscoveryDetails;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexDiscoveryDetailsCache {

    private static final Map<ResourceLocation, WildexDiscoveryDetails> DETAILS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> REQUESTED = ConcurrentHashMap.newKeySet();

    private static volatile String SESSION_KEY = "";

    private WildexDiscoveryDetailsCache() {
    }

    public static WildexDiscoveryDetails get(ResourceLocation mobId) {
        ensureSession();
        if (mobId == null) return null;
        return DETAILS.get(mobId);
    }

    public static WildexDiscoveryDetails getOrRequest(ResourceLocation mobId) {
        ensureSession();
        if (mobId == null) return null;

        WildexDiscoveryDetails cached = DETAILS.get(mobId);
        if (cached != null) return cached;

        request(mobId);
        return null;
    }

    public static void request(ResourceLocation mobId) {
        ensureSession();
        if (mobId == null) return;
        if (!REQUESTED.add(mobId)) return;
        WildexNetworkClient.requestDiscoveryDetails(mobId);
    }

    public static void set(ResourceLocation mobId, WildexDiscoveryDetails details) {
        ensureSession();
        if (mobId == null || details == null) return;
        DETAILS.put(mobId, details);
        REQUESTED.add(mobId);
    }

    public static void invalidate(ResourceLocation mobId) {
        ensureSession();
        if (mobId == null) return;
        DETAILS.remove(mobId);
        REQUESTED.remove(mobId);
    }

    public static void clear() {
        DETAILS.clear();
        REQUESTED.clear();
    }

    private static void ensureSession() {
        Minecraft mc = Minecraft.getInstance();
        String levelName = mc.level == null ? "" : mc.level.dimension().location().toString();
        String server = mc.getCurrentServer() == null ? "" : mc.getCurrentServer().ip;
        String key = server + "|" + levelName;

        if (!key.equals(SESSION_KEY)) {
            SESSION_KEY = key;
            clear();
        }
    }
}
