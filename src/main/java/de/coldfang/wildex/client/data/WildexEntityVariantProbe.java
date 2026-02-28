package de.coldfang.wildex.client.data;

import de.coldfang.wildex.integration.cobblemon.WildexCobblemonBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Generic, low-cost client-side probe that nudges preview entities into a non-default visual variant.
 * It only touches entity instances created for UI rendering and never affects server state.
 */
public final class WildexEntityVariantProbe {

    private static final int[] INT_CANDIDATES = {15, 7, 3, 2, 1, 4, 5, 6, 8};
    private static final Map<Class<?>, List<Accessor>> ACCESSOR_CACHE = new ConcurrentHashMap<>();

    private WildexEntityVariantProbe() {
    }

    @SuppressWarnings("unused")
    public static void applyIfSupported(Entity entity) {
        if (!(entity instanceof Mob mob)) return;

        List<Accessor> accessors = resolveAccessors(mob.getClass());
        if (accessors.isEmpty()) return;

        for (Accessor accessor : accessors) {
            if (tryApplyAccessor(mob, accessor)) {
                return;
            }
        }
    }

    /**
     * Applies a conservative fallback variant for preview use-cases.
     * This only touches simple numeric/enum variant accessors and avoids object/string rewrites.
     */
    @SuppressWarnings("unused")
    public static void applySoftFallback(Entity entity) {
        if (!(entity instanceof Mob mob)) return;

        List<Accessor> accessors = resolveAccessors(mob.getClass());
        if (accessors.isEmpty()) return;

        for (Accessor accessor : accessors) {
            if (accessor.kind() != Kind.INT && accessor.kind() != Kind.ENUM) continue;
            if (tryApplyAccessor(mob, accessor)) {
                return;
            }
        }
    }

    public static boolean supportsVariants(Entity entity) {
        return !discoverOptions(entity, 1).isEmpty();
    }

    public static List<VariantOption> discoverOptions(Entity entity, int maxOptions) {
        if (!(entity instanceof Mob mob)) return List.of();
        int cap = Math.max(1, maxOptions);

        List<VariantOption> bridged = WildexCobblemonBridge.discoverVariantOptions(entity, cap);
        if (!bridged.isEmpty()) {
            return bridged;
        }

        List<Accessor> accessors = resolveAccessors(mob.getClass());
        if (accessors.isEmpty()) return List.of();

        Map<String, VariantOption> out = new LinkedHashMap<>();
        for (Accessor accessor : accessors) {
            int remaining = cap - out.size();
            if (remaining <= 0) break;

            List<VariantOption> options = switch (accessor.kind()) {
                case INT -> discoverIntOptions(mob, accessor, remaining);
                case ENUM -> discoverEnumOptions(mob, accessor, remaining);
                case STRING -> discoverStringOptions(mob, accessor, remaining);
                case OBJECT -> discoverObjectOptions(mob, accessor, remaining);
            };

            for (VariantOption option : options) {
                out.putIfAbsent(option.id(), option);
                if (out.size() >= cap) break;
            }
        }

        return out.isEmpty() ? List.of() : List.copyOf(out.values());
    }

    public static boolean applyOption(Entity entity, String optionId) {
        if (!(entity instanceof Mob mob)) return false;
        if (optionId == null || optionId.isBlank()) return false;

        if (WildexCobblemonBridge.applyVariantOption(entity, optionId)) {
            return true;
        }

        int sep = optionId.indexOf('|');
        if (sep <= 0 || sep >= optionId.length() - 1) return false;
        String setterName = optionId.substring(0, sep);
        String rawValue = optionId.substring(sep + 1);

        Accessor accessor = resolveAccessors(mob.getClass()).stream()
                .filter(a -> a.setter().getName().equals(setterName))
                .findFirst()
                .orElse(null);
        if (accessor == null) return false;

        return switch (accessor.kind()) {
            case INT -> applyIntOption(mob, accessor, rawValue);
            case ENUM -> applyEnumOption(mob, accessor, rawValue);
            case STRING -> applyStringOption(mob, accessor, rawValue);
            case OBJECT -> applyObjectOption(mob, accessor, rawValue);
        };
    }

    private static List<VariantOption> discoverIntOptions(Mob entity, Accessor accessor, int cap) {
        Integer original = readInt(accessor.getter(), entity);
        if (original == null) return List.of();

        String baseName = safeEntityName(entity);
        Map<String, VariantOption> out = new LinkedHashMap<>();
        int ordinal = 1;

        for (int candidate : INT_CANDIDATES) {
            if (candidate == original) continue;
            if (!writeAndVerifyInt(entity, accessor, candidate)) continue;

            String label = optionLabelFromState(entity, baseName, accessor.suffix(), ordinal, candidate);
            String optionId = accessor.setter().getName() + "|" + candidate;
            out.putIfAbsent(optionId, new VariantOption(optionId, label));
            ordinal++;
            if (out.size() >= cap) break;
        }

        writeInt(entity, accessor, original);
        return new ArrayList<>(out.values());
    }

    private static List<VariantOption> discoverEnumOptions(Mob entity, Accessor accessor, int cap) {
        Object original = read(accessor.getter(), entity);
        if (original == null) return List.of();

        Object[] constants = accessor.valueType().getEnumConstants();
        if (constants == null || constants.length <= 1) return List.of();

        String baseName = safeEntityName(entity);
        List<VariantOption> out = new ArrayList<>();
        int ordinal = 1;

        for (Object candidate : constants) {
            if (candidate == null || candidate.equals(original)) continue;
            if (!writeAndVerifyEnum(entity, accessor, candidate)) continue;

            String raw = ((Enum<?>) candidate).name();
            String label = optionLabelFromState(entity, baseName, accessor.suffix(), ordinal, raw);
            String optionId = accessor.setter().getName() + "|" + raw;
            out.add(new VariantOption(optionId, label));
            ordinal++;
            if (out.size() >= cap) break;
        }

        write(accessor.setter(), entity, original);
        return out;
    }

    private static List<VariantOption> discoverStringOptions(Mob entity, Accessor accessor, int cap) {
        Object current = read(accessor.getter(), entity);
        if (!(current instanceof String original)) return List.of();

        String baseName = safeEntityName(entity);
        Map<String, VariantOption> out = new LinkedHashMap<>();
        int ordinal = 1;

        List<String> candidates = discoverStringCandidates(entity, accessor, original);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank() || Objects.equals(candidate, original)) continue;
            if (!writeAndVerifyString(entity, accessor, candidate)) continue;

            String label = optionLabelFromState(entity, baseName, accessor.suffix(), ordinal, candidate);
            String optionId = accessor.setter().getName() + "|" + candidate;
            out.putIfAbsent(optionId, new VariantOption(optionId, label));
            ordinal++;
            if (out.size() >= cap) break;
        }

        write(accessor.setter(), entity, original);
        return new ArrayList<>(out.values());
    }

    private static List<VariantOption> discoverObjectOptions(Mob entity, Accessor accessor, int cap) {
        Object original = read(accessor.getter(), entity);
        String originalToken = stableToken(original);
        String baseName = safeEntityName(entity);

        Map<String, VariantOption> out = new LinkedHashMap<>();
        int ordinal = 1;

        List<Object> candidates = discoverObjectCandidates(entity, accessor);
        candidates.sort(WildexEntityVariantProbe::compareObjectCandidates);
        for (Object candidate : candidates) {
            if (candidate == null) continue;
            String token = stableToken(candidate);
            if (token == null || token.isBlank()) continue;
            if (originalToken != null && originalToken.equals(token)) continue;
            if (!writeAndVerifyObjectIsolated(entity, accessor, candidate)) continue;

            String label = optionLabelFromState(entity, baseName, accessor.suffix(), ordinal, token);
            String optionId = accessor.setter().getName() + "|" + token;
            out.putIfAbsent(optionId, new VariantOption(optionId, label));
            ordinal++;
            if (out.size() >= cap) break;
        }

        return new ArrayList<>(out.values());
    }

    private static int compareObjectCandidates(Object a, Object b) {
        String ta = stableToken(a);
        String tb = stableToken(b);
        int ra = objectTokenRank(ta);
        int rb = objectTokenRank(tb);
        if (ra != rb) return Integer.compare(ra, rb);
        if (ta == null && tb == null) return 0;
        if (ta == null) return 1;
        if (tb == null) return -1;
        return ta.compareToIgnoreCase(tb);
    }

    private static int objectTokenRank(String token) {
        if (token == null || token.isBlank()) return 99;
        String s = token.toLowerCase(Locale.ROOT);
        if (s.startsWith("hybrid_") || s.contains("_hybrid_")) return 40;
        int separators = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ':' || c == '$' || c == '.') separators++;
        }
        if (separators == 0) return 0;
        if (separators == 1) return 8;
        if (separators == 2) return 16;
        return 24;
    }

    private static boolean applyIntOption(Mob entity, Accessor accessor, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            return writeAndVerifyInt(entity, accessor, value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean applyEnumOption(Mob entity, Accessor accessor, String rawValue) {
        Object[] constants = accessor.valueType().getEnumConstants();

        for (Object constant : constants) {
            if (!(constant instanceof Enum<?> e)) continue;
            if (!e.name().equalsIgnoreCase(rawValue)) continue;
            return writeAndVerifyEnum(entity, accessor, constant);
        }
        return false;
    }

    private static boolean applyStringOption(Mob entity, Accessor accessor, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return false;
        return writeAndVerifyString(entity, accessor, rawValue);
    }

    private static boolean applyObjectOption(Mob entity, Accessor accessor, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return false;

        List<Object> candidates = discoverObjectCandidates(entity, accessor);
        for (Object candidate : candidates) {
            String token = stableToken(candidate);
            if (token == null || token.isBlank()) continue;
            if (!token.equalsIgnoreCase(rawValue)) continue;
            return writeAndVerifyObject(entity, accessor, candidate);
        }
        return false;
    }

    private static String optionLabelFromState(
            Mob entity,
            String baseName,
            String suffix,
            int ordinal,
            Object rawValue
    ) {
        String dynamic = safeEntityName(entity);
        if (!dynamic.isBlank() && !dynamic.equalsIgnoreCase(baseName) && !looksLikeRawTranslationKey(dynamic)) {
            return dynamic;
        }
        String titleSuffix = toTitleCase(suffix);
        return titleSuffix + " " + ordinal + " (" + rawValue + ")";
    }

    private static String safeEntityName(Mob entity) {
        try {
            return entity.getName().getString().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean looksLikeRawTranslationKey(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.indexOf(' ') >= 0) return false;
        return text.indexOf('.') > 0;
    }

    private static String toTitleCase(String suffix) {
        if (suffix == null || suffix.isBlank()) return "Variant";
        String normalized = suffix.replace('_', ' ').trim();
        if (normalized.isBlank()) return "Variant";
        String[] words = normalized.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w == null || w.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) out.append(w.substring(1));
        }
        return out.isEmpty() ? "Variant" : out.toString();
    }

    private static boolean tryApplyAccessor(Mob entity, Accessor accessor) {
        return switch (accessor.kind()) {
            case INT -> tryApplyIntAccessor(entity, accessor);
            case ENUM -> tryApplyEnumAccessor(entity, accessor);
            case STRING -> tryApplyStringAccessor(entity, accessor);
            case OBJECT -> tryApplyObjectAccessor(entity, accessor);
        };
    }

    private static boolean tryApplyIntAccessor(Mob entity, Accessor accessor) {
        Integer original = readInt(accessor.getter(), entity);
        if (original == null) return false;

        for (int candidate : INT_CANDIDATES) {
            if (candidate == original) continue;
            if (writeAndVerifyInt(entity, accessor, candidate)) return true;
        }

        int fallback = original + 1;
        if (writeAndVerifyInt(entity, accessor, fallback)) return true;

        writeInt(entity, accessor, original);
        return false;
    }

    private static boolean tryApplyEnumAccessor(Mob entity, Accessor accessor) {
        Object original = read(accessor.getter(), entity);
        if (original == null) return false;

        Object[] constants = accessor.valueType().getEnumConstants();
        if (constants == null || constants.length <= 1) return false;

        for (Object candidate : constants) {
            if (candidate == null || candidate.equals(original)) continue;
            if (writeAndVerifyEnum(entity, accessor, candidate)) return true;
        }

        write(accessor.setter(), entity, original);
        return false;
    }

    private static boolean tryApplyStringAccessor(Mob entity, Accessor accessor) {
        Object current = read(accessor.getter(), entity);
        if (!(current instanceof String original)) return false;

        List<String> candidates = discoverStringCandidates(entity, accessor, original);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank() || Objects.equals(candidate, original)) continue;
            if (writeAndVerifyString(entity, accessor, candidate)) return true;
        }

        write(accessor.setter(), entity, original);
        return false;
    }

    private static boolean tryApplyObjectAccessor(Mob entity, Accessor accessor) {
        Object original = read(accessor.getter(), entity);
        List<Object> candidates = discoverObjectCandidates(entity, accessor);

        for (Object candidate : candidates) {
            if (candidate == null) continue;
            if (original != null && Objects.equals(stableToken(candidate), stableToken(original))) continue;
            if (writeAndVerifyObject(entity, accessor, candidate)) return true;
        }

        if (original != null) {
            write(accessor.setter(), entity, original);
        }
        return false;
    }

    private static boolean writeAndVerifyInt(Mob entity, Accessor accessor, int candidate) {
        if (!writeInt(entity, accessor, candidate)) return false;
        Integer after = readInt(accessor.getter(), entity);
        return after != null && after == candidate;
    }

    private static boolean writeAndVerifyEnum(Mob entity, Accessor accessor, Object candidate) {
        if (!write(accessor.setter(), entity, candidate)) return false;
        Object after = read(accessor.getter(), entity);
        return candidate.equals(after);
    }

    private static boolean writeAndVerifyString(Mob entity, Accessor accessor, String candidate) {
        if (!write(accessor.setter(), entity, candidate)) return false;
        Object after = read(accessor.getter(), entity);
        return after instanceof String out && Objects.equals(out, candidate);
    }

    private static boolean writeAndVerifyObject(Mob entity, Accessor accessor, Object candidate) {
        if (!write(accessor.setter(), entity, candidate)) return false;
        Object after = read(accessor.getter(), entity);
        String wanted = stableToken(candidate);
        String actual = stableToken(after);
        return wanted != null && !wanted.isBlank() && wanted.equals(actual);
    }

    private static boolean writeAndVerifyObjectIsolated(Mob source, Accessor accessor, Object candidate) {
        Mob probe = freshProbe(source);
        if (probe == null) return false;
        try {
            Accessor probeAccessor = resolveAccessors(probe.getClass()).stream()
                    .filter(a -> a.kind() == Kind.OBJECT)
                    .filter(a -> a.setter().getName().equals(accessor.setter().getName()))
                    .findFirst()
                    .orElse(accessor);
            return writeAndVerifyObject(probe, probeAccessor, candidate);
        } finally {
            probe.discard();
        }
    }

    private static Mob freshProbe(Mob source) {
        if (source == null) return null;
        Level level = source.level();
        try {
            Entity fresh = source.getType().create(level);
            if (fresh instanceof Mob mob) return mob;
            if (fresh != null) fresh.discard();
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean writeInt(Mob entity, Accessor accessor, int value) {
        Class<?> paramType = accessor.setter().getParameterTypes()[0];
        Object converted = convertInt(paramType, value);
        if (converted == null) return false;
        return write(accessor.setter(), entity, converted);
    }

    private static Object convertInt(Class<?> paramType, int value) {
        if (paramType == int.class || paramType == Integer.class) return value;
        if (paramType == byte.class || paramType == Byte.class) return (byte) value;
        if (paramType == short.class || paramType == Short.class) return (short) value;
        if (paramType == long.class || paramType == Long.class) return (long) value;
        return null;
    }

    private static Integer readInt(Method getter, Object target) {
        Object out = read(getter, target);
        if (out instanceof Integer i) return i;
        if (out instanceof Byte b) return (int) b;
        if (out instanceof Short s) return (int) s;
        if (out instanceof Long l) return (int) (long) l;
        return null;
    }

    private static Object read(Method getter, Object target) {
        try {
            return getter.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean write(Method setter, Object target, Object value) {
        try {
            setter.invoke(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<Accessor> resolveAccessors(Class<?> entityClass) {
        if (entityClass == null) return List.of();
        return ACCESSOR_CACHE.computeIfAbsent(entityClass, WildexEntityVariantProbe::scanAccessors);
    }

    private static List<Accessor> scanAccessors(Class<?> entityClass) {
        Method[] methods = entityClass.getMethods();
        List<Accessor> candidates = new ArrayList<>();

        for (Method getter : methods) {
            if (getter.getParameterCount() != 0) continue;

            String suffix = extractSuffix(getter.getName());
            if (suffix == null || suffix.isBlank()) continue;

            int score = variantTokenScore(suffix);
            if (score <= 0) continue;

            Kind kind = resolveKind(getter.getReturnType());
            if (kind == null) continue;

            Method setter = findSetter(entityClass, suffix, kind, getter.getReturnType());
            if (setter == null) continue;

            trySetAccessible(getter);
            trySetAccessible(setter);
            candidates.add(new Accessor(getter, setter, getter.getReturnType(), suffix, kind, score));
        }

        candidates.sort(Comparator.comparingInt(Accessor::score).reversed());

        Map<String, Accessor> unique = new LinkedHashMap<>();
        for (Accessor accessor : candidates) {
            String key = accessor.setter().getName() + "#" + accessor.setter().getParameterTypes()[0].getName();
            unique.putIfAbsent(key, accessor);
        }

        return List.copyOf(unique.values());
    }

    private static void trySetAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    private static String extractSuffix(String methodName) {
        if (methodName == null || methodName.isBlank()) return null;
        if (methodName.startsWith("get") && methodName.length() > 3) return methodName.substring(3);
        if (methodName.startsWith("is") && methodName.length() > 2) return methodName.substring(2);
        return null;
    }

    private static int variantTokenScore(String suffix) {
        String s = suffix.toLowerCase(Locale.ROOT);

        if ("type".equals(s) || "id".equals(s) || "uuid".equals(s)) return 0;
        if (s.contains("platform")) return 0;
        if (s.contains("baby") || s.contains("age") || s.contains("health")) return 0;
        if (s.contains("owner") || s.contains("target") || s.contains("vehicle")) return 0;

        if ("variant".equals(s)) return 200;
        if (s.endsWith("variant")) return 150;
        if (s.contains("breed")) return 140;
        if (s.contains("variant")) return 100;
        if (s.contains("skin")) return 90;
        if (s.contains("pattern")) return 80;
        if (s.contains("species")) return 75;
        if (s.contains("morph")) return 70;
        if (s.contains("form")) return 65;
        if (s.contains("overlay")) return 60;
        if (s.contains("colour") || s.contains("color")) return 45;
        if (s.contains("type")) return 40;
        return 0;
    }

    private static Kind resolveKind(Class<?> type) {
        if (type == null) return null;
        if (type == int.class || type == Integer.class || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class || type == long.class || type == Long.class) {
            return Kind.INT;
        }
        if (type.isEnum()) return Kind.ENUM;
        if (type == String.class) return Kind.STRING;
        if (!type.isPrimitive() && !type.getName().startsWith("java.")) return Kind.OBJECT;
        return null;
    }

    private static Method findSetter(Class<?> owner, String suffix, Kind kind, Class<?> getterType) {
        String setterName = "set" + suffix;

        for (Method method : owner.getMethods()) {
            if (!setterName.equals(method.getName())) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (kind == Kind.INT && isIntLike(paramType)) {
                return method;
            }
            if (kind == Kind.ENUM && paramType.isAssignableFrom(getterType)) {
                return method;
            }
            if (kind == Kind.STRING && paramType == String.class) {
                return method;
            }
            if (kind == Kind.OBJECT && (paramType.isAssignableFrom(getterType) || getterType.isAssignableFrom(paramType))) {
                return method;
            }
        }
        return null;
    }

    private static boolean isIntLike(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class
                || type == long.class || type == Long.class;
    }

    private static List<String> discoverStringCandidates(Mob entity, Accessor accessor, String original) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        String suffix = accessor.suffix().toLowerCase(Locale.ROOT);

        if (suffix.contains("variant")) {
            Object breed = tryCallZeroArg(entity, "getBreed");
            if (breed != null) {
                for (Object variant : extractElements(tryCallZeroArg(breed, "getVariants"))) {
                    String id = stableToken(variant);
                    if (id != null && !id.isBlank()) out.putIfAbsent(id, true);
                }
            }
        }

        if (original != null && !original.isBlank()) {
            out.putIfAbsent(original, true);
        }
        return new ArrayList<>(out.keySet());
    }

    private static List<Object> discoverObjectCandidates(Mob entity, Accessor accessor) {
        Class<?> setterType = accessor.setter().getParameterTypes()[0];
        Class<?> entityClass = entity.getClass();
        Map<String, Object> out = new LinkedHashMap<>();

        Object current = read(accessor.getter(), entity);
        addCandidate(out, current);

        for (String className : registryClassNameCandidates(entityClass, setterType)) {
            Class<?> registryClass = tryLoadClass(entityClass, className);
            if (registryClass == null) continue;

            for (Method method : registryClass.getMethods()) {
                if (!isLikelyVariantRegistryMethod(method, setterType)) continue;

                Object result = read(method, null);
                if (result == null) continue;

                if (setterType.isInstance(result)) {
                    addCandidate(out, result);
                    continue;
                }

                for (Object element : extractElements(result)) {
                    if (setterType.isInstance(element)) {
                        addCandidate(out, element);
                    }
                }
            }
        }

        return new ArrayList<>(out.values());
    }

    private static boolean isLikelyVariantRegistryMethod(Method method, Class<?> setterType) {
        if (method == null || setterType == null) return false;
        if (!Modifier.isStatic(method.getModifiers())) return false;
        if (method.getParameterCount() != 0) return false;

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) return false;
        if (!isCandidateContainerType(returnType, setterType)) return false;

        return hasVariantNameHint(method.getName());
    }

    private static boolean isCandidateContainerType(Class<?> returnType, Class<?> setterType) {
        if (returnType == null || setterType == null) return false;
        return setterType.isAssignableFrom(returnType)
                || Collection.class.isAssignableFrom(returnType)
                || Iterable.class.isAssignableFrom(returnType)
                || Map.class.isAssignableFrom(returnType)
                || Stream.class.isAssignableFrom(returnType)
                || returnType.isArray();
    }

    private static boolean hasVariantNameHint(String methodName) {
        if (methodName == null || methodName.isBlank()) return false;
        String name = methodName.toLowerCase(Locale.ROOT);
        return "values".equals(name)
                || name.contains("variant")
                || name.contains("breed")
                || name.contains("type")
                || name.contains("form")
                || name.contains("skin")
                || name.contains("pattern")
                || name.contains("species")
                || name.contains("morph")
                || name.contains("entry")
                || name.contains("value")
                || name.contains("list")
                || name.contains("all");
    }

    private static void addCandidate(Map<String, Object> out, Object candidate) {
        if (candidate == null) return;
        String token = stableToken(candidate);
        if (token == null || token.isBlank()) return;
        out.putIfAbsent(token, candidate);
    }

    private static List<String> registryClassNameCandidates(Class<?> entityClass, Class<?> setterType) {
        List<String> roots = new ArrayList<>();
        String pkg = entityClass.getPackageName();
        while (!pkg.isBlank()) {
            roots.add(pkg);
            int cut = pkg.lastIndexOf('.');
            if (cut <= 0) break;
            pkg = pkg.substring(0, cut);
        }

        String base = normalizedTypeName(setterType.getSimpleName());
        String plural = base.endsWith("s") ? base : base + "s";

        Map<String, Boolean> names = new LinkedHashMap<>();
        for (String root : roots) {
            names.putIfAbsent(root + ".registry." + base + "Registry", true);
            names.putIfAbsent(root + ".registry." + plural + "Registry", true);
        }

        return new ArrayList<>(names.keySet());
    }

    private static String normalizedTypeName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) return "Type";
        String s = simpleName;
        if (s.startsWith("I") && s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
            s = s.substring(1);
        }
        return s;
    }

    private static Class<?> tryLoadClass(Class<?> contextClass, String className) {
        try {
            return Class.forName(className, false, contextClass.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryCallZeroArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            trySetAccessible(method);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Object> extractElements(Object source) {
        if (source == null) return List.of();
        List<Object> out = new ArrayList<>();

        switch (source) {
            case Collection<?> collection -> {
                out.addAll(collection);
                return out;
            }
            case Iterable<?> iterable -> {
                iterable.forEach(out::add);
                return out;
            }
            case Map<?, ?> map -> {
                out.addAll(map.values());
                return out;
            }
            case Stream<?> stream -> {
                try (stream) {
                    stream.forEach(out::add);
                }
                return out;
            }
            default -> {
                if (source.getClass().isArray()) {
                    int len = Array.getLength(source);
                    for (int i = 0; i < len; i++) {
                        out.add(Array.get(source, i));
                    }
                    return out;
                }
            }
        }
        return List.of();
    }

    private static String stableToken(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Enum<?> e -> {
                return e.name();
            }
            case CharSequence s -> {
                return s.toString().trim();
            }
            default -> {
            }
        }

        for (String methodName : List.of("id", "getId", "name", "getName")) {
            Object out = tryCallZeroArg(value, methodName);
            if (out instanceof CharSequence s && !s.toString().isBlank()) {
                return s.toString().trim();
            }
        }

        return null;
    }

    private enum Kind {
        INT,
        ENUM,
        STRING,
        OBJECT
    }

    public record VariantOption(String id, String label) {
    }

    private record Accessor(Method getter, Method setter, Class<?> valueType, String suffix, Kind kind, int score) {
    }
}
