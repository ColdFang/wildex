package de.coldfang.wildex.integration.kubejs;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.integration.WildexOptionalIntegrations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class WildexKubeJsBridge {

    private static final WildexKubeJsEventEmitter NOOP = (eventId, payload) -> false;

    private static volatile WildexKubeJsEventEmitter externalEmitter = NOOP;
    private static volatile WildexKubeJsEventEmitter activeEmitter = NOOP;
    private static volatile boolean initialized = false;
    private static volatile boolean warnedUnavailable = false;

    private WildexKubeJsBridge() {
    }

    public static void registerEmitter(@Nullable WildexKubeJsEventEmitter customEmitter) {
        externalEmitter = customEmitter == null ? NOOP : customEmitter;
        if (initialized) {
            activeEmitter = createEmitter();
        }
    }

    public static boolean emit(@NotNull String eventId, @NotNull Map<String, Object> payload) {
        if (!WildexOptionalIntegrations.isKubeJsLoaded()) return false;
        ensureInitialized();

        try {
            return activeEmitter.emit(eventId, payload);
        } catch (Throwable t) {
            Wildex.LOGGER.warn("Wildex KubeJS bridge emit failed for event '{}'", eventId, t);
            return false;
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (WildexKubeJsBridge.class) {
            if (initialized) return;
            activeEmitter = createEmitter();
            initialized = true;
        }
    }

    @NotNull
    private static WildexKubeJsEventEmitter createEmitter() {
        if (!warnedUnavailable) {
            warnedUnavailable = true;
            Wildex.LOGGER.info("Wildex KubeJS bridge default script emitter is active.");
        }
        WildexKubeJsEventEmitter ext = externalEmitter;
        return (eventId, payload) -> {
            if (ext != NOOP) return ext.emit(eventId, payload);
            return WildexKubeJsScriptBridge.emitInternal(eventId, payload);
        };
    }
}
