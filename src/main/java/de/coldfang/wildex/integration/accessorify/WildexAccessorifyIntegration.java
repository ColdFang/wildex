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

    private static volatile Method accessoriesGetOptionallyMethod;
    private static volatile Method accessoriesIsEquippedMethod;
    private static volatile boolean accessoriesLookupFailed;

    private static volatile Field accessorifyShouldScopeField;
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
            if (!accessoriesLookupFailed) {
                accessoriesLookupFailed = true;
                Wildex.LOGGER.warn("Wildex failed to query Accessorify/Accessories spyglass state. Continuing without Accessorify spyglass support.", t);
            }
            return false;
        }
    }

    public static boolean isClientAccessorySpyglassActive(Player player) {
        if (player == null || !WildexOptionalIntegrations.isAccessorifyLoaded()) return false;
        return isAccessorySpyglassEquipped(player) && readAccessorifyShouldScope();
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

    private static boolean readAccessorifyShouldScope() {
        try {
            return getAccessorifyShouldScopeField().getBoolean(null);
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

        Class<?> clientUtilClass = Class.forName(ACCESSORIFY_CLIENT_UTIL_CLASS);
        Field resolved = clientUtilClass.getField("shouldScope");
        accessorifyShouldScopeField = resolved;
        return resolved;
    }
}
