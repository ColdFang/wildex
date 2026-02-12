package de.coldfang.wildex.client.data;

import de.coldfang.wildex.util.WildexMobFilters;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class WildexMobIndexModel {

    private final List<EntityType<?>> all;
    private List<EntityType<?>> filtered;

    private String query = "";

    public WildexMobIndexModel() {
        this.all = loadAll();
        this.filtered = this.all;
    }

    public void setQuery(String query) {
        this.query = normalize(query);
        this.filtered = applyFilter(this.all, this.query);
    }

    public int totalCount() {
        return all.size();
    }

    public String query() {
        return query;
    }

    public List<EntityType<?>> filtered() {
        return filtered;
    }

    private static List<EntityType<?>> loadAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }

        Level level = mc.level;

        List<EntityType<?>> list = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!WildexMobFilters.isTrackable(type)) continue;

            if (!isMobType(type, level)) continue;
            list.add(type);
        }

        list.sort(Comparator.comparing(WildexMobIndexModel::idString));
        return List.copyOf(list);
    }

    private static boolean isMobType(EntityType<?> type, Level level) {
        Entity e = type.create(level);
        if (e == null) return false;

        boolean ok = e instanceof Mob;
        e.discard();
        return ok;
    }

    private static List<EntityType<?>> applyFilter(
            List<EntityType<?>> base,
            String query
    ) {
        if (query.isBlank()) return base;

        ParsedQuery pq = parseQuery(query);

        boolean hasText = !pq.textQuery.isBlank();
        boolean hasMods = !pq.modPrefixes.isEmpty();

        if (!hasText && !hasMods) return base;

        List<EntityType<?>> out = new ArrayList<>();

        for (EntityType<?> type : base) {
            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(type);

            if (hasMods) {
                String ns = rl.getNamespace();
                if (!matchesAnyPrefix(ns, pq.modPrefixes)) continue;
            }

            if (hasText) {
                String id = rl.toString();
                String name = normalize(type.getDescription().getString());

                if (!name.contains(pq.textQuery) && !id.contains(pq.textQuery)) continue;
            }

            out.add(type);
        }

        return List.copyOf(out);
    }

    private static boolean matchesAnyPrefix(String namespace, List<String> prefixes) {
        if (namespace == null) return false;
        for (String p : prefixes) {
            if (p == null || p.isBlank()) continue;
            if (namespace.startsWith(p)) return true;
        }
        return false;
    }

    private static String idString(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return Objects.requireNonNull(id, "Missing entity type id").toString();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static ParsedQuery parseQuery(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return new ParsedQuery("", List.of());
        }

        String[] parts = normalizedQuery.split("\\s+");

        List<String> modPrefixes = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (String p : parts) {
            if (p == null || p.isBlank()) continue;

            if (p.charAt(0) == '@') {
                String raw = p.substring(1);
                if (raw.isBlank()) continue;

                int cut = raw.indexOf(':');
                if (cut >= 0) raw = raw.substring(0, cut);

                String prefix = normalize(raw);
                if (!prefix.isBlank()) modPrefixes.add(prefix);
                continue;
            }

            if (!text.isEmpty()) text.append(' ');
            text.append(p);
        }

        return new ParsedQuery(text.toString(), List.copyOf(modPrefixes));
    }

    private record ParsedQuery(String textQuery, List<String> modPrefixes) {
    }
}
