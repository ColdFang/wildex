package de.coldfang.wildex.client.data;

public final class WildexCompletionCache {

    private static boolean complete = false;
    private static boolean justCompleted = false;

    private WildexCompletionCache() {
    }

    public static boolean isComplete() {
        return complete;
    }

    public static boolean consumeJustCompleted() {
        boolean v = justCompleted;
        justCompleted = false;
        return v;
    }

    public static void clear() {
        complete = false;
        justCompleted = false;
    }

    // One-shot completion event (trigger FX, sound, overlay once).
    // Called ONLY from S2CWildexCompletePayload handler.
    public static void markCompleteFromServer() {
        if (!complete) {
            complete = true;
            justCompleted = true;
        }
    }

    // Persistent status sync (NO FX!). Used on join/rejoin.
    // Called ONLY from S2CWildexCompleteStatusPayload handler.
    public static void setCompleteStatusFromServer(boolean isComplete) {
        complete = isComplete;
        justCompleted = false;
    }
}
