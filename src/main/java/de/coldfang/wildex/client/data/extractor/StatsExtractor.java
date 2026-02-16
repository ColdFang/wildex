package de.coldfang.wildex.client.data.extractor;

import de.coldfang.wildex.client.data.model.WildexStatsData;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.OptionalDouble;

public final class StatsExtractor {

    public WildexStatsData extract(EntityType<?> type) {
        LivingEntity living = createLiving(type);
        if (living == null) return WildexStatsData.empty();

        AttributeMap attrs = living.getAttributes();

        WildexStatsData data = new WildexStatsData(
                read(attrs, Attributes.MAX_HEALTH),
                read(attrs, Attributes.ARMOR),
                read(attrs, Attributes.MOVEMENT_SPEED),
                read(attrs, Attributes.ATTACK_DAMAGE),
                read(attrs, Attributes.FOLLOW_RANGE),
                read(attrs, Attributes.KNOCKBACK_RESISTANCE)
        );

        living.discard();
        return data;
    }

    private static LivingEntity createLiving(EntityType<?> type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;

        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (!(e instanceof LivingEntity living)) return null;

        return living;
    }

    private static OptionalDouble read(AttributeMap attrs, Holder<Attribute> attribute) {
        if (!attrs.hasAttribute(attribute)) return OptionalDouble.empty();
        return OptionalDouble.of(attrs.getValue(attribute));
    }
}
