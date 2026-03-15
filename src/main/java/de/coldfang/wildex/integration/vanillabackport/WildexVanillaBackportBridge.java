package de.coldfang.wildex.integration.vanillabackport;

import de.coldfang.wildex.client.data.WildexEntityVariantProbe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexVanillaBackportBridge {

    private static final String MOD_ID = "vanillabackport";
    private static final String OPTION_PREFIX = "bridge:vanillabackport";
    private static final String HOLDER_CLASS_NAME = "com.blackgear.vanillabackport.common.api.variant.VariantDataHolder";
    private static final BridgeBinding NO_BINDING = new BridgeBinding(null, null, List.of(), Map.of());
    private static final Map<String, String> FARM_ANIMAL_VARIANT_CLASSES = new HashMap<>();

    private static volatile int modPresence = -1; // -1 unknown, 0 absent, 1 present
    private static volatile Class<?> holderClass;
    private static final Map<Class<?>, BridgeBinding> BINDINGS = new ConcurrentHashMap<>();

    static {
        FARM_ANIMAL_VARIANT_CLASSES.put("pig", "com.blackgear.vanillabackport.common.level.entities.animal.PigVariants");
        FARM_ANIMAL_VARIANT_CLASSES.put("cow", "com.blackgear.vanillabackport.common.level.entities.animal.CowVariants");
        FARM_ANIMAL_VARIANT_CLASSES.put("chicken", "com.blackgear.vanillabackport.common.level.entities.animal.ChickenVariants");
    }

    private WildexVanillaBackportBridge() {
    }

    public static List<WildexEntityVariantProbe.VariantOption> discoverVariantOptions(Entity entity, int maxOptions) {
        BridgeBinding binding = resolveBinding(entity);
        if (binding == null || binding.entries().isEmpty()) return List.of();

        int cap = Math.max(1, maxOptions);
        String currentKey = currentVariantKey(binding, entity);
        List<WildexEntityVariantProbe.VariantOption> out = new ArrayList<>();

        for (VariantEntry entry : binding.entries()) {
            if (entry.key().equals(currentKey)) continue;
            out.add(new WildexEntityVariantProbe.VariantOption(entry.optionId(), entry.label()));
            if (out.size() >= cap) break;
        }

        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    public static boolean supportsVariantOptions(Entity entity) {
        BridgeBinding binding = resolveBinding(entity);
        return binding != null && !binding.entries().isEmpty();
    }

    public static boolean applyVariantOption(Entity entity, String optionId) {
        if (optionId == null || optionId.isBlank()) return false;
        if (!optionId.startsWith(OPTION_PREFIX + "|")) return false;

        BridgeBinding binding = resolveBinding(entity);
        if (binding == null) return false;

        String key = optionId.substring((OPTION_PREFIX + "|").length());
        VariantEntry entry = binding.entriesByKey().get(key);
        if (entry == null) return false;

        Object value = resolveVariantValue(binding, entry);
        if (value == null) return false;
        if (!invokeOneArg(entity, binding.setter(), value)) return false;
        invokeRefreshDimensions(entity);
        return true;
    }

    public static void clearCache() {
        BINDINGS.clear();
    }

    private static BridgeBinding resolveBinding(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        if (!isVanillaBackportLoaded()) return null;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        String exactVariantClass = FARM_ANIMAL_VARIANT_CLASSES.get(entityId.getPath());
        if (exactVariantClass != null) {
            BridgeBinding exactBinding = BINDINGS.computeIfAbsent(
                    living.getClass(),
                    clazz -> buildBinding(clazz, exactVariantClass)
            );
            if (exactBinding != NO_BINDING) {
                return exactBinding;
            }
        }

        Class<?> variantHolder = resolveHolderClass(living.getClass().getClassLoader());
        if (variantHolder == null || !variantHolder.isInstance(living)) return null;

        BridgeBinding binding = BINDINGS.computeIfAbsent(living.getClass(), clazz -> buildBinding(clazz, null));
        return binding == NO_BINDING ? null : binding;
    }

    private static BridgeBinding buildBinding(Class<?> entityClass, String exactRegistryHolderClassName) {
        if (entityClass == null) return NO_BINDING;

        Method getter = resolveVariantGetter(entityClass);
        Method setter = resolveVariantSetter(entityClass);
        if (getter == null || setter == null) return NO_BINDING;
        if (!Optional.class.isAssignableFrom(getter.getReturnType())) return NO_BINDING;

        Class<?> variantType = setter.getParameterTypes()[0];
        Class<?> registryHolderClass = exactRegistryHolderClassName == null
                ? null
                : tryLoadClass(entityClass.getClassLoader(), exactRegistryHolderClassName);
        if (registryHolderClass == null) {
            registryHolderClass = tryLoadClass(
                    entityClass.getClassLoader(),
                    variantType.getPackageName() + "." + variantType.getSimpleName() + "s"
            );
        }
        if (registryHolderClass == null) return NO_BINDING;

        Object registry = readRegistryField(registryHolderClass);
        if (registry == null) return NO_BINDING;

        List<VariantEntry> entries = buildEntries(registryHolderClass);
        if (entries.isEmpty()) return NO_BINDING;

        Map<String, VariantEntry> byKey = new LinkedHashMap<>();
        for (VariantEntry entry : entries) {
            byKey.putIfAbsent(entry.key(), entry);
        }
        return new BridgeBinding(getter, setter, List.copyOf(entries), Map.copyOf(byKey));
    }

    private static List<VariantEntry> buildEntries(Class<?> registryHolderClass) {
        Map<String, VariantEntry> out = new LinkedHashMap<>();

        for (Field field : registryHolderClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;

            Object keyObj = readStaticField(field);
            if (!(keyObj instanceof ResourceKey<?> resourceKey)) continue;

            ResourceLocation location = resourceKey.location();
            String key = location.toString();
            String optionId = OPTION_PREFIX + "|" + key;
            String label = formatLabel(field.getName(), location);
            out.putIfAbsent(optionId, new VariantEntry(optionId, key, label, resourceKey));
        }

        return out.isEmpty() ? List.of() : new ArrayList<>(out.values());
    }

    private static String currentVariantKey(BridgeBinding binding, Entity entity) {
        if (binding == null) return null;

        Object variant = currentVariantValue(binding, entity);
        if (variant == null) return null;

        Object keyObj = invokeGetKey(resolveRegistry(binding), variant);
        if (keyObj instanceof ResourceLocation location) {
            return location.toString();
        }
        if (keyObj instanceof CharSequence text) {
            return text.toString().trim();
        }
        return null;
    }

    private static Object currentVariantValue(BridgeBinding binding, Entity entity) {
        if (binding == null || entity == null) return null;
        Object raw = invokeNoArgs(entity, binding.getter());
        return unwrapOptional(raw);
    }

    private static Object resolveVariantValue(BridgeBinding binding, VariantEntry entry) {
        if (binding == null || entry == null) return null;
        Object registry = binding.registry();
        if (registry == null) return null;

        Object resourceKey = entry.resourceKey();
        if (resourceKey != null) {
            Object value = invokeOneArgWithResult(registry, "getOrThrow", resourceKey);
            if (value != null) return value;
            value = invokeOneArgWithResult(registry, "get", resourceKey);
            if (value != null) return value;
        }

        ResourceLocation location = ResourceLocation.tryParse(entry.key());

        Object value = invokeOneArgWithResult(registry, "get", location);
        if (value != null) return value;

        Object keyObj = invokeOneArgWithResult(registry, "getResourceKey", location);
        Object unwrapped = unwrapOptional(keyObj);
        if (unwrapped != null) {
            value = invokeOneArgWithResult(registry, "getOrThrow", unwrapped);
            if (value != null) return value;
            value = invokeOneArgWithResult(registry, "get", unwrapped);
            if (value != null) return value;
        }
        return null;
    }

    private static Object resolveRegistry(BridgeBinding binding) {
        return binding == null ? null : binding.registry();
    }

    private static boolean isVanillaBackportLoaded() {
        int state = modPresence;
        if (state >= 0) return state == 1;

        try {
            boolean loaded = ModList.get().isLoaded(MOD_ID);
            modPresence = loaded ? 1 : 0;
            return loaded;
        } catch (Throwable ignored) {
            modPresence = 0;
            return false;
        }
    }

    private static Class<?> resolveHolderClass(ClassLoader loader) {
        Class<?> cached = holderClass;
        if (cached != null) return cached;

        try {
            cached = Class.forName(HOLDER_CLASS_NAME, false, loader);
            holderClass = cached;
            return cached;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method resolveVariantGetter(Class<?> owner) {
        if (owner == null) return null;
        String methodName = "getVariantData";
        try {
            Method method = owner.getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
        }

        Class<?> current = owner;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method resolveVariantSetter(Class<?> owner) {
        if (owner == null) return null;
        String methodName = "setVariantData";
        for (Method method : owner.getMethods()) {
            if (!methodName.equals(method.getName())) continue;
            if (method.getParameterCount() != 1) continue;
            try {
                method.setAccessible(true);
            } catch (Throwable ignored) {
            }
            return method;
        }

        Class<?> current = owner;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!methodName.equals(method.getName())) continue;
                if (method.getParameterCount() != 1) continue;
                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                }
                return method;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Object readRegistryField(Class<?> owner) {
        if (owner == null) return null;
        try {
            Field field = owner.getField("REGISTRY");
            return readStaticField(field);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readStaticField(Field field) {
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> tryLoadClass(ClassLoader loader, String className) {
        if (loader == null || className == null || className.isBlank()) return null;
        try {
            return Class.forName(className, false, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, Method method) {
        if (target == null || method == null) return null;
        try {
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void invokeRefreshDimensions(Object target) {
        if (target == null) return;
        try {
            Method method = target.getClass().getMethod("refreshDimensions");
            method.setAccessible(true);
            method.invoke(target);
        } catch (Throwable ignored) {
        }
    }

    private static boolean invokeOneArg(Object target, Method method, Object arg) {
        if (target == null || method == null) return false;
        try {
            method.invoke(target, arg);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeGetKey(Object target, Object arg) {
        return invokeOneArgWithResult(target, "getKey", arg);
    }

    private static Object invokeOneArgWithResult(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) return null;

        for (Method method : target.getClass().getMethods()) {
            if (!methodName.equals(method.getName())) continue;
            if (method.getParameterCount() != 1) continue;
            if (!isArgumentCompatible(method.getParameterTypes()[0], arg)) continue;

            try {
                method.setAccessible(true);
                return method.invoke(target, arg);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean isArgumentCompatible(Class<?> parameterType, Object arg) {
        if (parameterType == null) return false;
        if (arg == null) return !parameterType.isPrimitive();
        return parameterType.isInstance(arg);
    }

    private static Object unwrapOptional(Object raw) {
        if (raw instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return raw;
    }

    private static String formatLabel(String fieldName, ResourceLocation location) {
        String base = location == null ? fieldName : location.getPath();
        if (base == null || base.isBlank()) base = fieldName;
        if (base == null || base.isBlank()) return "Variant";

        String normalized = base.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        String[] words = normalized.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.isEmpty() ? "Variant" : out.toString();
    }

    private record BridgeBinding(
            Method getter,
            Method setter,
            List<VariantEntry> entries,
            Map<String, VariantEntry> entriesByKey
    ) {
        private Object registry() {
            if (this.entries().isEmpty()) {
                return null;
            }
            try {
                Class<?> variantType = this.setter().getParameterTypes()[0];
                Class<?> registryHolderClass = Class.forName(
                        variantType.getPackageName() + "." + variantType.getSimpleName() + "s",
                        false,
                        variantType.getClassLoader()
                );
                return readRegistryField(registryHolderClass);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private record VariantEntry(String optionId, String key, String label, Object resourceKey) {
    }
}
