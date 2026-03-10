package de.coldfang.wildex.client.data;

public final class WildexPlayerUiStateCache {

    private static String tabId = "STATS";
    private static String mobId = "";
    private static boolean discoveredOnly = false;
    private static boolean friendlyEnabled = false;
    private static boolean neutralEnabled = false;
    private static boolean hostileEnabled = false;
    private static boolean tameableEnabled = false;
    private static boolean hasServerState = false;

    private WildexPlayerUiStateCache() {
    }

    public static synchronized void set(
            String newTabId,
            String newMobId,
            boolean newDiscoveredOnly,
            boolean newFriendlyEnabled,
            boolean newNeutralEnabled,
            boolean newHostileEnabled,
            boolean newTameableEnabled
    ) {
        tabId = newTabId == null || newTabId.isBlank() ? "STATS" : newTabId;
        mobId = newMobId == null ? "" : newMobId;
        discoveredOnly = newDiscoveredOnly;
        friendlyEnabled = newFriendlyEnabled;
        neutralEnabled = newNeutralEnabled;
        hostileEnabled = newHostileEnabled;
        tameableEnabled = newTameableEnabled;
        hasServerState = true;
    }

    public static synchronized String tabId() {
        return tabId;
    }

    public static synchronized String mobId() {
        return mobId;
    }

    public static synchronized boolean discoveredOnly() {
        return discoveredOnly;
    }

    public static synchronized boolean friendlyEnabled() {
        return friendlyEnabled;
    }

    public static synchronized boolean neutralEnabled() {
        return neutralEnabled;
    }

    public static synchronized boolean hostileEnabled() {
        return hostileEnabled;
    }

    public static synchronized boolean tameableEnabled() {
        return tameableEnabled;
    }

    public static synchronized boolean hasServerState() {
        return hasServerState;
    }

    public static synchronized void clear() {
        tabId = "STATS";
        mobId = "";
        discoveredOnly = false;
        friendlyEnabled = false;
        neutralEnabled = false;
        hostileEnabled = false;
        tameableEnabled = false;
        hasServerState = false;
    }
}
