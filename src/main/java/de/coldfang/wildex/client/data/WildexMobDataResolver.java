package de.coldfang.wildex.client.data;

import de.coldfang.wildex.client.data.extractor.HeaderExtractor;
import de.coldfang.wildex.client.data.extractor.StatsExtractor;
import de.coldfang.wildex.client.data.model.WildexHeaderData;
import de.coldfang.wildex.client.data.model.WildexMobData;
import de.coldfang.wildex.client.data.model.WildexStatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public final class WildexMobDataResolver {

    private final Map<String, WildexMobData> cache = new HashMap<>();
    private final StatsExtractor statsExtractor = new StatsExtractor();
    private final HeaderExtractor headerExtractor = new HeaderExtractor();

    public WildexMobData resolve(String mobId) {
        String key = mobId == null ? "" : mobId;
        if (key.isBlank()) return WildexMobData.empty();

        WildexMobData cached = cache.get(key);
        if (cached != null) return cached;

        EntityType<?> type = resolveType(key);
        if (type == null) {
            WildexMobData data = WildexMobData.empty();
            cache.put(key, data);
            return data;
        }

        WildexStatsData stats = statsExtractor.extract(type);

        Level level = Minecraft.getInstance().level;
        WildexHeaderData header = headerExtractor.extract(type, level);

        WildexMobData data = new WildexMobData(stats, header);
        cache.put(key, data);
        return data;
    }

    public void clearCache() {
        cache.clear();
    }

    private static EntityType<?> resolveType(String mobId) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId);
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }
}
