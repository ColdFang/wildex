package de.coldfang.wildex.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WildexEntityFactory {

    private WildexEntityFactory() {
    }

    public static Entity tryCreate(EntityType<?> type, Level level) {
        if (type == null || level == null) return null;
        try {
            Entity entity = type.create(level);
            normalizeDisplayEntity(entity);
            return entity;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void normalizeDisplayEntity(@Nullable Entity entity) {
        if (entity == null) return;

        setBooleanMethod(entity, "setFreezeAnimator", true);
        setBooleanMethod(entity, "setBaby", false);
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

    private static void setBooleanMethod(Object instance, String methodName, boolean value) {
        Method method = findMethod(instance.getClass(), methodName, boolean.class);
        if (method == null) return;
        invoke(method, instance, value);
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

    private static @Nullable Object invoke(@Nullable Method method, Object instance, Object... args) {
        if (method == null || instance == null) return null;
        try {
            return method.invoke(instance, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
