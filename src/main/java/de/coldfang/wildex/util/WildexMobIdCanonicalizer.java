package de.coldfang.wildex.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class WildexMobIdCanonicalizer {

    private static final String MOWZIES_NAMESPACE = "mowziesmobs";
    private static final String TRADE_SUFFIX = ".trade";
    private static final String FOLLOWER_PLAYER_SUFFIX = "_follower_player";
    private static final String FOLLOWER_RAPTOR_SUFFIX = "_follower_raptor";
    private static final String FOLLOWER_HOWLER_SUFFIX = "_follower_howler";
    private static final String CRANE_PLAYER_SUFFIX = "_crane_player";
    private static final String PLAYER_SUFFIX = "_player";

    private WildexMobIdCanonicalizer() {
    }

    public static @Nullable ResourceLocation canonicalize(@Nullable ResourceLocation id) {
        if (id == null) return null;
        if (!MOWZIES_NAMESPACE.equals(id.getNamespace())) return id;

        String path = id.getPath();
        ResourceLocation normalized = null;

        if (path.endsWith(TRADE_SUFFIX)) {
            normalized = withPath(id, path.substring(0, path.length() - TRADE_SUFFIX.length()));
        } else if (path.endsWith(FOLLOWER_PLAYER_SUFFIX)) {
            normalized = withPath(id, path.substring(0, path.length() - FOLLOWER_PLAYER_SUFFIX.length()));
        } else if (path.endsWith(FOLLOWER_RAPTOR_SUFFIX)) {
            normalized = withPath(id, path.substring(0, path.length() - FOLLOWER_RAPTOR_SUFFIX.length()));
        } else if (path.endsWith(FOLLOWER_HOWLER_SUFFIX)) {
            normalized = withPath(id, path.substring(0, path.length() - FOLLOWER_HOWLER_SUFFIX.length()));
        } else if (path.endsWith(CRANE_PLAYER_SUFFIX)) {
            normalized = withPath(id, path.substring(0, path.length() - PLAYER_SUFFIX.length()));
        }

        if (normalized == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(normalized)) {
            return id;
        }
        return normalized;
    }

    public static boolean isCanonical(@Nullable ResourceLocation id) {
        return Objects.equals(id, canonicalize(id));
    }

    private static ResourceLocation withPath(ResourceLocation base, String path) {
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), path);
    }
}
