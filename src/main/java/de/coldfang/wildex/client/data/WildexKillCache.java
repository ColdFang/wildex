package de.coldfang.wildex.client.data;

import de.coldfang.wildex.network.WildexNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexKillCache {

    private static final Map<ResourceLocation, Integer> KILLS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> REQUESTED = ConcurrentHashMap.newKeySet();

    private static volatile String SESSION_KEY = "";

    private WildexKillCache() {
    }

    public static int get(ResourceLocation id) {
        ensureSession();
        if (id == null) return 0;
        return Math.max(0, KILLS.getOrDefault(id, 0));
    }

    public static int getOrRequest(ResourceLocation id) {
        ensureSession();
        if (id == null) return 0;

        Integer v = KILLS.get(id);
        if (v != null) return Math.max(0, v);

        if (REQUESTED.add(id)) {
            WildexNetwork.requestKillsForSelected(id.toString());
        }

        return 0;
    }

    public static void set(ResourceLocation id, int kills) {
        ensureSession();
        if (id == null) return;
        KILLS.put(id, Math.max(0, kills));
    }

    public static void clear() {
        KILLS.clear();
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
