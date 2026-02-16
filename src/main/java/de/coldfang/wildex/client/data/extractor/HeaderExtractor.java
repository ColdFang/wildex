package de.coldfang.wildex.client.data.extractor;

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

        Component name = type.getDescription();
        WildexAggression aggression = classify(type, null);

        if (level != null) {
            Entity e = WildexEntityFactory.tryCreate(type, level);
            if (e != null) {
                aggression = classify(type, e);
                e.discard();
            }
        }

        return new WildexHeaderData(name, aggression);
    }

    private static WildexAggression classify(EntityType<?> type, Entity e) {
        if (type.getCategory() == MobCategory.MONSTER) return WildexAggression.HOSTILE;
        if (e instanceof NeutralMob) return WildexAggression.NEUTRAL;
        return WildexAggression.FRIENDLY;
    }
}
