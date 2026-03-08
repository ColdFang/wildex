package de.coldfang.wildex.util;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class WildexIdFilterMatcher {

    private WildexIdFilterMatcher() {
    }

    public static boolean matches(ResourceLocation id, List<? extends String> filters) {
        if (id == null) return false;
        if (filters == null || filters.isEmpty()) return false;

        String namespace = id.getNamespace();
        String fullId = id.toString();

        for (String raw : filters) {
            String normalized = normalize(raw);
            if (normalized.isBlank()) continue;

            if (isFullId(normalized)) {
                if (fullId.equals(normalized)) return true;
                continue;
            }

            if (namespace.equals(normalized)) return true;
        }

        return false;
    }

    public static List<String> normalizeEntries(List<? extends String> filters) {
        if (filters == null || filters.isEmpty()) return List.of();

        ArrayList<String> out = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        for (String raw : filters) {
            String normalized = normalize(raw);
            if (normalized.isBlank() || !seen.add(normalized)) continue;
            out.add(normalized);
        }

        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    public static boolean isFullId(String value) {
        if (value == null) return false;
        int colon = value.indexOf(':');
        if (colon <= 0) return false;
        return colon == value.lastIndexOf(':') && (colon + 1) < value.length();
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
