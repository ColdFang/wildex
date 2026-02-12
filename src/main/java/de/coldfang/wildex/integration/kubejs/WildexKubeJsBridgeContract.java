package de.coldfang.wildex.integration.kubejs;

import de.coldfang.wildex.server.WildexProgressService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WildexKubeJsBridgeContract {

    public static final int API_VERSION = 1;

    public static final String EVENT_DISCOVERY_CHANGED = "wildex.discovery_changed";
    public static final String EVENT_COMPLETED = "wildex.completed";

    private WildexKubeJsBridgeContract() {
    }

    @NotNull
    public static Payload discoveryChanged(
            @NotNull ServerPlayer player,
            @NotNull ResourceLocation mobId,
            @NotNull WildexProgressService.ProgressSnapshot snapshot
    ) {
        return new Payload(
                API_VERSION,
                player.getUUID().toString(),
                player.getGameProfile().getName(),
                mobId.toString(),
                snapshot.discoveredCount(),
                snapshot.totalCount(),
                snapshot.completionPercentScaled(),
                snapshot.complete()
        );
    }

    @NotNull
    public static Payload completed(
            @NotNull ServerPlayer player,
            @NotNull WildexProgressService.ProgressSnapshot snapshot
    ) {
        return new Payload(
                API_VERSION,
                player.getUUID().toString(),
                player.getGameProfile().getName(),
                null,
                snapshot.discoveredCount(),
                snapshot.totalCount(),
                snapshot.completionPercentScaled(),
                snapshot.complete()
        );
    }

    public record Payload(
            int apiVersion,
            @NotNull String playerUuid,
            @NotNull String playerName,
            @Nullable String mobId,
            int discovered,
            int total,
            int percentScaled,
            boolean isComplete
    ) {
        @NotNull
        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("apiVersion", apiVersion);
            out.put("playerUuid", playerUuid);
            out.put("playerName", playerName);
            out.put("mobId", mobId);
            out.put("discovered", discovered);
            out.put("total", total);
            out.put("percentScaled", percentScaled);
            out.put("isComplete", isComplete);
            return out;
        }
    }
}
