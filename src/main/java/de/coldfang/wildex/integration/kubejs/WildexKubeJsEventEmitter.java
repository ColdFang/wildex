package de.coldfang.wildex.integration.kubejs;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

@FunctionalInterface
public interface WildexKubeJsEventEmitter {
    boolean emit(@NotNull String eventId, @NotNull Map<String, Object> payload);
}

