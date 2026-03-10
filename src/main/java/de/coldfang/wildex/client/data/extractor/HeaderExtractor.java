package de.coldfang.wildex.client.data.extractor;

import de.coldfang.wildex.client.data.WildexEntityDisplayNameResolver;
import de.coldfang.wildex.client.data.model.WildexAggression;
import de.coldfang.wildex.client.data.model.WildexHeaderData;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.level.Level;

public final class HeaderExtractor {

    public WildexHeaderData extract(EntityType<?> type, Level level) {
        if (type == null) return WildexHeaderData.empty();

        Component name = WildexEntityDisplayNameResolver.resolve(type);
        WildexAggression aggression = classify(type, level);

        return new WildexHeaderData(name, aggression);
    }

    public static WildexAggression classify(EntityType<?> type, Level level) {
        if (type == null) return WildexAggression.FRIENDLY;

        WildexAggression base = classifyInternal(type, null);
        if (level == null || base != WildexAggression.FRIENDLY) {
            return base;
        }

        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (e == null) return base;
        try {
            return classifyInternal(type, e);
        } finally {
            WildexEntityFactory.discardQuietly(e);
        }
    }

    private static WildexAggression classifyInternal(EntityType<?> type, Entity e) {
        if (type.getCategory() == MobCategory.MONSTER) return WildexAggression.HOSTILE;
        if (e instanceof NeutralMob) return WildexAggression.NEUTRAL;
        return WildexAggression.FRIENDLY;
    }
}
