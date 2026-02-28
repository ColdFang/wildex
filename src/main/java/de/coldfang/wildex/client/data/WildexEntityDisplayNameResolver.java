package de.coldfang.wildex.client.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class WildexEntityDisplayNameResolver {

    private static final Pattern RAW_KEY_PATTERN = Pattern.compile("^[a-z0-9_.$:/-]+$");
    private static final Map<ResourceLocation, Component> PRETTY_CACHE = new ConcurrentHashMap<>();

    private WildexEntityDisplayNameResolver() {
    }

    public static Component resolve(EntityType<?> type) {
        if (type == null) return Component.empty();

        Component base = type.getDescription();
        String baseText = base.getString();

        if (isReadableDisplayText(baseText)) {
            return base;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return base;

        Component cached = PRETTY_CACHE.get(id);
        if (cached != null) return cached;

        Component resolved = Component.literal(prettyNameFromId(id));
        PRETTY_CACHE.put(id, resolved);
        return resolved;
    }

    public static Component resolveOrFallback(EntityType<?> type, ResourceLocation fallbackId) {
        if (type != null) {
            Component resolved = resolve(type);
            if (isReadableDisplayText(resolved.getString())) {
                return resolved;
            }

            Component base = type.getDescription();
            if (isReadableDisplayText(base.getString())) {
                return base;
            }

            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return Component.literal(prettyNameFromId(typeId));
        }

        if (fallbackId != null) {
            return Component.literal(prettyNameFromId(fallbackId));
        }
        return Component.literal("Unknown");
    }

    public static void clearCache() {
        PRETTY_CACHE.clear();
    }

    private static boolean isReadableDisplayText(String text) {
        if (text == null) return false;
        String s = text.trim();
        if (s.isBlank()) return false;
        if (s.indexOf(' ') >= 0) return false;
        if (s.indexOf('.') < 0) return false;
        return !RAW_KEY_PATTERN.matcher(s).matches();
    }

    private static String prettyNameFromId(ResourceLocation id) {
        String path = id == null ? "" : id.getPath();
        if (path.isBlank()) return "Unknown";

        String normalized = path.replace('.', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) return "Unknown";

        String[] words = normalized.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }

        return out.isEmpty() ? "Unknown" : out.toString();
    }
}
