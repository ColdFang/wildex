package de.coldfang.wildex.integration.accessorify;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.integration.WildexOptionalIntegrations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class WildexAccessorifyIntegration {

    private static final String ACCESSORIES_CAPABILITY_CLASS = "io.wispforest.accessories.api.AccessoriesCapability";
    private static final String ACCESSORIFY_CLIENT_UTIL_CLASS = "me.pajic.accessorify.util.ClientUtil";
    private static final String ACCESSORIFY_MOD_UTIL_CLASS = "me.pajic.accessorify.util.ModUtil";
    private static final String ACCESSORIFY_ACCESSORY_UTIL_CLASS = "me.pajic.accessorify.util.AccessoryUtil";
    private static final String ACCESSORIFY_KEYBINDS_CLASS = "me.pajic.accessorify.keybind.ModKeybinds";

    private static volatile Method accessoriesGetOptionallyMethod;
    private static volatile Method accessoriesIsEquippedMethod;
    private static volatile Method accessorifyAccessoryEquippedMethod;
    private static volatile boolean accessoriesLookupFailed;

    private static volatile Field accessorifyShouldScopeField;
    private static volatile Method accessorifyShouldScopeMethod;
    private static volatile Field accessorifySpyglassKeybindField;
    private static volatile boolean clientUtilLookupFailed;

    private WildexAccessorifyIntegration() {
    }

    public static boolean isAccessorySpyglassEquipped(LivingEntity entity) {
        if (entity == null || !WildexOptionalIntegrations.isAccessorifyLoaded()) return false;

        try {
            Optional<?> capability = getAccessoriesCapability(entity);
            if (capability.isEmpty()) return false;
            return Boolean.TRUE.equals(getAccessoriesIsEquippedMethod().invoke(capability.get(), Items.SPYGLASS));
        } catch (Throwable t) {
            try {
                return Boolean.TRUE.equals(getAccessorifyAccessoryEquippedMethod().invoke(null, entity, Items.SPYGLASS));
            } catch (Throwable fallbackError) {
                if (!accessoriesLookupFailed) {
                    accessoriesLookupFailed = true;
                    Wildex.LOGGER.warn("Wildex failed to query Accessorify/Accessories spyglass state. Continuing without Accessorify spyglass support.", fallbackError);
                }
                return false;
            }
        }
    }

    public static boolean isClientAccessorySpyglassActive(Player player) {
        if (player == null || !WildexOptionalIntegrations.isAccessorifyLoaded()) return false;
        boolean equipped = isAccessorySpyglassEquipped(player);
        return equipped && readAccessorifyShouldScope(equipped);
    }

    private static Optional<?> getAccessoriesCapability(LivingEntity entity) throws ReflectiveOperationException {
        Object value = getAccessoriesGetOptionallyMethod().invoke(null, entity);
        if (value instanceof Optional<?> optional) {
            return optional;
        }
        return Optional.empty();
    }

    private static Method getAccessoriesGetOptionallyMethod() throws ReflectiveOperationException {
        Method cached = accessoriesGetOptionallyMethod;
        if (cached != null) return cached;

        Class<?> capabilityClass = Class.forName(ACCESSORIES_CAPABILITY_CLASS);
        Method resolved = capabilityClass.getMethod("getOptionally", LivingEntity.class);
        accessoriesGetOptionallyMethod = resolved;
        return resolved;
    }

    private static Method getAccessoriesIsEquippedMethod() throws ReflectiveOperationException {
        Method cached = accessoriesIsEquippedMethod;
        if (cached != null) return cached;

        Class<?> capabilityClass = Class.forName(ACCESSORIES_CAPABILITY_CLASS);
        Method resolved = capabilityClass.getMethod("isEquipped", net.minecraft.world.item.Item.class);
        accessoriesIsEquippedMethod = resolved;
        return resolved;
    }

    private static Method getAccessorifyAccessoryEquippedMethod() throws ReflectiveOperationException {
        Method cached = accessorifyAccessoryEquippedMethod;
        if (cached != null) return cached;

        ReflectiveOperationException lastError = new NoSuchMethodException("No Accessorify accessory-equipped helper found");
        for (String className : new String[]{ACCESSORIFY_ACCESSORY_UTIL_CLASS, ACCESSORIFY_MOD_UTIL_CLASS}) {
            for (String methodName : new String[]{"isAccessoryEquipped", "accessoryEquipped"}) {
                try {
                    Class<?> utilClass = Class.forName(className);
                    Method resolved = utilClass.getMethod(methodName, LivingEntity.class, net.minecraft.world.item.Item.class);
                    accessorifyAccessoryEquippedMethod = resolved;
                    return resolved;
                } catch (ReflectiveOperationException e) {
                    lastError = e;
                }
            }
        }

        throw lastError;
    }

    private static boolean readAccessorifyShouldScope(boolean accessorySpyglassEquipped) {
        try {
            try {
                Field scopeField = getAccessorifyShouldScopeField();
                return scopeField.getBoolean(null);
            } catch (ReflectiveOperationException ignored) {
            }

            try {
                Method scopeMethod = getAccessorifyShouldScopeMethod();
                return Boolean.TRUE.equals(scopeMethod.invoke(null));
            } catch (ReflectiveOperationException ignored) {
            }

            return accessorySpyglassEquipped && isAccessorifySpyglassKeybindDown();
        } catch (Throwable t) {
            if (!clientUtilLookupFailed) {
                clientUtilLookupFailed = true;
                Wildex.LOGGER.warn("Wildex failed to read Accessorify client spyglass state. Continuing without Accessorify client overlay support.", t);
            }
            return false;
        }
    }

    private static Field getAccessorifyShouldScopeField() throws ReflectiveOperationException {
        Field cached = accessorifyShouldScopeField;
        if (cached != null) return cached;

        ReflectiveOperationException lastError = new NoSuchFieldException("No Accessorify shouldScope field found");
        for (String className : new String[]{ACCESSORIFY_CLIENT_UTIL_CLASS, ACCESSORIFY_MOD_UTIL_CLASS}) {
            try {
                Class<?> utilClass = Class.forName(className);
                Field resolved = utilClass.getField("shouldScope");
                accessorifyShouldScopeField = resolved;
                return resolved;
            } catch (ReflectiveOperationException e) {
                lastError = e;
            }
        }

        throw lastError;
    }

    private static Method getAccessorifyShouldScopeMethod() throws ReflectiveOperationException {
        Method cached = accessorifyShouldScopeMethod;
        if (cached != null) return cached;

        ReflectiveOperationException lastError = new NoSuchMethodException("No Accessorify shouldScope method found");
        for (String className : new String[]{ACCESSORIFY_CLIENT_UTIL_CLASS, ACCESSORIFY_MOD_UTIL_CLASS}) {
            for (String methodName : new String[]{"shouldScope", "isScoping", "isSpyglassActive"}) {
                try {
                    Class<?> utilClass = Class.forName(className);
                    Method resolved = utilClass.getMethod(methodName);
                    if (resolved.getReturnType() == boolean.class || resolved.getReturnType() == Boolean.class) {
                        accessorifyShouldScopeMethod = resolved;
                        return resolved;
                    }
                } catch (ReflectiveOperationException e) {
                    lastError = e;
                }
            }
        }

        throw lastError;
    }

    private static boolean isAccessorifySpyglassKeybindDown() throws ReflectiveOperationException {
        Object keybindHolder = getAccessorifySpyglassKeybindField().get(null);
        if (keybindHolder == null) return false;

        Object keybind = resolveKeybindInstance(keybindHolder);
        if (keybind == null) return false;

        Method isDown = keybind.getClass().getMethod("isDown");
        return Boolean.TRUE.equals(isDown.invoke(keybind));
    }

    private static Field getAccessorifySpyglassKeybindField() throws ReflectiveOperationException {
        Field cached = accessorifySpyglassKeybindField;
        if (cached != null) return cached;

        Class<?> keybindsClass = Class.forName(ACCESSORIFY_KEYBINDS_CLASS);
        Field resolved = keybindsClass.getField("USE_SPYGLASS");
        accessorifySpyglassKeybindField = resolved;
        return resolved;
    }

    private static Object resolveKeybindInstance(Object keybindHolder) throws ReflectiveOperationException {
        try {
            Method isDown = keybindHolder.getClass().getMethod("isDown");
            if (isDown.getReturnType() == boolean.class || isDown.getReturnType() == Boolean.class) {
                return keybindHolder;
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method get = keybindHolder.getClass().getMethod("get");
            return get.invoke(keybindHolder);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
