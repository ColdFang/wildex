package de.coldfang.wildex.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class WildexEntityFactory {

    private WildexEntityFactory() {
    }

    public static Entity tryCreate(EntityType<?> type, Level level) {
        if (type == null || level == null) return null;
        try {
            return type.create(level);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
