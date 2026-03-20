package de.coldfang.wildex.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WildexEntityFactory {

    private WildexEntityFactory() {
    }

    public static Entity tryCreate(EntityType<?> type, Level level) {
        return tryCreate(type, level, true);
    }

    public static Entity tryCreate(EntityType<?> type, Level level, boolean normalizeDisplayState) {
        if (type == null || level == null) return null;
        try {
            Entity entity = type.create(level);
            if (normalizeDisplayState) {
                normalizeDisplayEntity(entity);
            }
            return entity;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static @Nullable Mob tryCreateMob(EntityType<?> type, Level level) {
        Entity entity = tryCreate(type, level);
        if (entity instanceof Mob mob) {
            return mob;
        }
        discardQuietly(entity);
        return null;
    }

    public static void discardQuietly(@Nullable Entity entity) {
        if (entity == null) return;
        try {
            entity.discard();
        } catch (Throwable ignored) {
            // Some third-party mods hook entity discard in ways that can fail on probe entities.
        }
    }

    private static void normalizeDisplayEntity(@Nullable Entity entity) {
        if (entity == null) return;

        setBooleanMethod(entity, "setFreezeAnimator", true);
        setBooleanMethod(entity, "setBaby", false);
        normalizeMowziesDisplayEntity(entity);
        if (entity instanceof LivingEntity living) {
            living.refreshDimensions();
        }

        Object modules = invoke(findMethod(entity.getClass(), "getModules"), entity);
        if (modules == null) {
            invoke(findMethod(entity.getClass(), "createModuleHolder"), entity);
            modules = invoke(findMethod(entity.getClass(), "getModules"), entity);
        }
        if (modules == null) return;

        Object growthModule = invoke(findMethod(modules.getClass(), "getGrowthStageModule"), modules);
        if (growthModule == null) return;

        Method setMax = findMethod(growthModule.getClass(), "setMax");
        if (setMax != null) {
            invoke(setMax, growthModule);
        } else {
            Method setGrowthStage = findMethod(growthModule.getClass(), "setGrowthStage", resolveAnimalGrowthStageClass());
            Object adultStage = resolveAnimalGrowthStageAdult();
            if (setGrowthStage != null && adultStage != null) {
                invoke(setGrowthStage, growthModule, adultStage);
            }
        }

        if (entity instanceof LivingEntity living) {
            living.refreshDimensions();
        }
    }

    private static void normalizeMowziesDisplayEntity(Entity entity) {
        Class<?> type = entity.getClass();
        while (type != null) {
            if ("com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana".equals(type.getName())) {
                // Mowzie's Umvuthana model reads the raw "active" field directly for its live pose.
                setBooleanMethod(entity, "setActive", true);
                setActiveField(entity);
                break;
            }
            type = type.getSuperclass();
        }
    }

    private static void setBooleanMethod(Object instance, String methodName, boolean value) {
        Method method = findMethod(instance.getClass(), methodName, boolean.class);
        if (method == null) return;
        invoke(method, instance, value);
    }

    private static void setActiveField(Object instance) {
        Field field = findActiveField(instance.getClass());
        if (field == null) return;
        try {
            field.setBoolean(instance, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static @Nullable Class<?> resolveAnimalGrowthStageClass() {
        try {
            return Class.forName("jp.jurassicsaga.server.base.animal.entity.obj.info.AnimalGrowthStage");
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static @Nullable Object resolveAnimalGrowthStageAdult() {
        Class<?> growthStageClass = resolveAnimalGrowthStageClass();
        if (growthStageClass == null) return null;
        try {
            Field adultField = growthStageClass.getField("ADULT");
            return adultField.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                try {
                    Method method = current.getMethod(name, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignoredAgain) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }

    private static @Nullable Field findActiveField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("active");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static @Nullable Object invoke(@Nullable Method method, Object instance, Object... args) {
        if (method == null || instance == null) return null;
        try {
            return method.invoke(instance, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
