package de.coldfang.wildex.client.data.extractor;

import de.coldfang.wildex.client.data.WildexEntityVariantProbe;
import de.coldfang.wildex.client.data.model.WildexStatsData;
import de.coldfang.wildex.integration.cobblemon.WildexCobblemonBridge;
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

        try {
            return readFromLiving(living);
        } finally {
            living.discard();
        }
    }

    public Result extractVariant(EntityType<?> type, String variantOptionId) {
        LivingEntity living = createLiving(type);
        if (living == null) return Result.unsupported();

        try {
            boolean applied = variantOptionId != null
                    && !variantOptionId.isBlank()
                    && WildexEntityVariantProbe.applyOption(living, variantOptionId);
            if (!applied) {
                return Result.unsupported();
            }
            return Result.ready(readFromLiving(living));
        } finally {
            living.discard();
        }
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

    private static WildexStatsData readFromLiving(LivingEntity living) {
        WildexStatsData cobblemonStats = WildexCobblemonBridge.readPokemonStats(living);
        if (cobblemonStats != null) {
            return cobblemonStats;
        }

        AttributeMap attrs = living.getAttributes();
        return new WildexStatsData(
                read(attrs, Attributes.MAX_HEALTH),
                read(attrs, Attributes.ARMOR),
                read(attrs, Attributes.MOVEMENT_SPEED),
                read(attrs, Attributes.ATTACK_DAMAGE),
                read(attrs, Attributes.FOLLOW_RANGE),
                read(attrs, Attributes.KNOCKBACK_RESISTANCE)
        );
    }

    public record Result(boolean supported, WildexStatsData stats) {
        public static Result ready(WildexStatsData stats) {
            return new Result(true, stats == null ? WildexStatsData.empty() : stats);
        }

        public static Result unsupported() {
            return new Result(false, WildexStatsData.empty());
        }
    }
}
