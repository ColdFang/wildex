package de.coldfang.wildex.client.data;

import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WildexMobIndexModel {

    private static final ConcurrentMap<String, String> MOD_DISPLAY_NAME_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ResourceLocation, String> NORMALIZED_NAME_CACHE = new ConcurrentHashMap<>();
    private static volatile String NAME_CACHE_LANGUAGE = "";

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

    public static void clearCaches() {
        MOD_DISPLAY_NAME_CACHE.clear();
        NORMALIZED_NAME_CACHE.clear();
        NAME_CACHE_LANGUAGE = "";
    }

    private static List<EntityType<?>> loadAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }

        Level level = mc.level;
        ensureNameCacheLanguage(mc);

        List<EntityType<?>> list = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!WildexMobFilters.isTrackable(type)) continue;

            if (!isMobType(type, level)) continue;
            list.add(type);
        }

        list.sort(
                Comparator
                        .comparing(WildexMobIndexModel::localizedNameForSort)
                        .thenComparing(WildexMobIndexModel::idString)
        );
        return List.copyOf(list);
    }

    private static boolean isMobType(EntityType<?> type, Level level) {
        Entity e = WildexEntityFactory.tryCreate(type, level);
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
                if (!matchesAnyPrefix(rl, pq.modPrefixes)) continue;
            }

            if (hasText) {
                String id = rl.toString();
                String name = normalizedDisplayName(type);

                if (!name.contains(pq.textQuery) && !id.contains(pq.textQuery)) continue;
            }

            out.add(type);
        }

        return List.copyOf(out);
    }

    private static boolean matchesAnyPrefix(ResourceLocation id, List<String> prefixes) {
        String namespace = id.getNamespace();
        String modDisplay = modDisplayNameLower(namespace);
        for (String p : prefixes) {
            if (p == null || p.isBlank()) continue;
            if (namespace.startsWith(p)) return true;
            if (modDisplay != null && modDisplay.contains(p)) return true;
        }
        return false;
    }

    private static String modDisplayNameLower(String namespace) {
        if (namespace == null || namespace.isBlank()) return null;
        return MOD_DISPLAY_NAME_CACHE.computeIfAbsent(namespace, ns ->
                ModList.get()
                        .getModContainerById(ns)
                        .map(c -> c.getModInfo().getDisplayName())
                        .filter(s -> !s.isBlank())
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .orElse(ns)
        );
    }

    private static String idString(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return Objects.requireNonNull(id, "Missing entity type id").toString();
    }

    private static String localizedNameForSort(EntityType<?> type) {
        return normalizedDisplayName(type);
    }

    private static String normalizedDisplayName(EntityType<?> type) {
        if (type == null) return "";
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type), "Missing entity type id");

        Minecraft mc = Minecraft.getInstance();
        ensureNameCacheLanguage(mc);

        String cached = NORMALIZED_NAME_CACHE.get(id);
        if (cached != null) return cached;

        String name = WildexEntityDisplayNameResolver.resolve(type).getString();
        String normalized = normalize(name);
        String previous = NORMALIZED_NAME_CACHE.putIfAbsent(id, normalized);
        return previous == null ? normalized : previous;
    }

    private static void ensureNameCacheLanguage(Minecraft mc) {
        if (mc == null) return;
        String selected = "";
        try {
            selected = normalize(mc.getLanguageManager().getSelected());
        } catch (Throwable ignored) {
        }

        String current = NAME_CACHE_LANGUAGE;
        if (!Objects.equals(current, selected)) {
            NORMALIZED_NAME_CACHE.clear();
            NAME_CACHE_LANGUAGE = selected;
        }
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
