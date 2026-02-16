package de.coldfang.wildex.client.data;

public final class WildexPlayerUiStateCache {

    private static String tabId = "STATS";
    private static String mobId = "";
    private static boolean hasServerState = false;

    private WildexPlayerUiStateCache() {
    }

    public static synchronized void set(String newTabId, String newMobId) {
        tabId = newTabId == null || newTabId.isBlank() ? "STATS" : newTabId;
        mobId = newMobId == null ? "" : newMobId;
        hasServerState = true;
    }

    public static synchronized String tabId() {
        return tabId;
    }

    public static synchronized String mobId() {
        return mobId;
    }

    public static synchronized boolean hasServerState() {
        return hasServerState;
    }

    public static synchronized void clear() {
        tabId = "STATS";
        mobId = "";
        hasServerState = false;
    }
}
