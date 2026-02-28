package de.coldfang.wildex.integration.cobblemon;

import de.coldfang.wildex.client.data.WildexEntityVariantProbe;
import de.coldfang.wildex.client.data.model.WildexStatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexCobblemonBridge {

    private static final String COBBLEMON_NAMESPACE = "cobblemon";
    private static final String COBBLEMON_POKEMON_PATH = "pokemon";

    private static final String COBBLEMON_ENTITY_CLASS = "com.cobblemon.mod.common.entity.pokemon.PokemonEntity";
    private static final String COBBLEMON_SPECIES_CLASS = "com.cobblemon.mod.common.api.pokemon.PokemonSpecies";
    private static final String COBBLEMON_OPTION_PREFIX = "bridge:cobblemon";

    private static volatile int cobblemonPresence = -1; // -1 unknown, 0 absent, 1 present
    private static volatile List<WildexEntityVariantProbe.VariantOption> cachedCobblemonOptions = List.of();
    private static volatile boolean cobblemonOptionsReady = false;
    private static volatile String cachedLanguage = "";

    private static final Map<Class<?>, MethodHolder> ENTITY_GET_POKEMON_METHOD = new ConcurrentHashMap<>();
    private static final Map<Class<?>, PokemonStatAccessors> POKEMON_STAT_ACCESSORS = new ConcurrentHashMap<>();

    private WildexCobblemonBridge() {
    }

    public static boolean isCobblemonPokemon(EntityType<?> type) {
        ResourceLocation id = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return isCobblemonPokemon(id);
    }

    public static boolean isCobblemonPokemon(String entityId) {
        if (entityId == null || entityId.isBlank()) return false;
        return isCobblemonPokemon(ResourceLocation.tryParse(entityId));
    }

    public static boolean isCobblemonPokemon(ResourceLocation id) {
        return id != null
                && COBBLEMON_NAMESPACE.equals(id.getNamespace())
                && COBBLEMON_POKEMON_PATH.equals(id.getPath());
    }

    public static boolean isCobblemonPokemonEntity(Entity entity) {
        return entity != null && isCobblemonPokemon(entity.getType()) && isCobblemonPresent();
    }

    public static WildexStatsData readPokemonStats(LivingEntity living) {
        if (living == null || !isCobblemonPokemonEntity(living)) return null;

        MethodHolder methodHolder = ENTITY_GET_POKEMON_METHOD.computeIfAbsent(
                living.getClass(),
                clazz -> new MethodHolder(resolveMethod(clazz, "getPokemon"))
        );
        Method getPokemon = methodHolder.method();
        if (getPokemon == null) return null;

        Object pokemon = invokeNoArgs(living, getPokemon);
        if (pokemon == null) return null;

        PokemonStatAccessors accessors = POKEMON_STAT_ACCESSORS.computeIfAbsent(
                pokemon.getClass(),
                PokemonStatAccessors::resolve
        );
        if (!accessors.hasAnyStat()) return null;

        // Map Pokemon core stats to Wildex's six-slot model:
        // HP, DEF, SPEED, ATK, SP.ATK, SP.DEF
        return new WildexStatsData(
                readNumber(pokemon, accessors.maxHealth()),
                readNumber(pokemon, accessors.defense()),
                readNumber(pokemon, accessors.speed()),
                readNumber(pokemon, accessors.attack()),
                readNumber(pokemon, accessors.specialAttack()),
                readNumber(pokemon, accessors.specialDefense())
        );
    }

    public static List<WildexEntityVariantProbe.VariantOption> discoverVariantOptions(Entity entity, int maxOptions) {
        if (!isCobblemonPokemonEntity(entity)) return List.of();
        int cap = Math.max(1, maxOptions);
        ensureLanguageCache();

        List<WildexEntityVariantProbe.VariantOption> options = cachedCobblemonOptions;
        if (!cobblemonOptionsReady) {
            options = buildCobblemonOptions();
            cachedCobblemonOptions = options;
            cobblemonOptionsReady = true;
        }

        if (options.isEmpty()) return List.of();
        if (options.size() <= cap) return options;
        return List.copyOf(options.subList(0, cap));
    }

    public static boolean applyVariantOption(Entity entity, String optionId) {
        if (optionId == null || optionId.isBlank()) return false;
        if (!optionId.startsWith(COBBLEMON_OPTION_PREFIX + "|")) return false;
        if (!isCobblemonPokemonEntity(entity)) return false;

        String payload = optionId.substring((COBBLEMON_OPTION_PREFIX + "|").length());
        int sep = payload.indexOf('|');
        if (sep <= 0 || sep >= payload.length() - 1) return false;

        String speciesId = payload.substring(0, sep);
        String formName = payload.substring(sep + 1);
        ResourceLocation speciesRl = ResourceLocation.tryParse(speciesId);
        if (speciesRl == null) return false;

        try {
            Object pokemon = invokeNoArgs(entity, "getPokemon");
            if (pokemon == null) return false;

            Object speciesRegistry = getKotlinObject(COBBLEMON_SPECIES_CLASS, entity.getClass().getClassLoader());
            if (speciesRegistry == null) return false;

            Object species = callOneArg(speciesRegistry, "getByIdentifier", speciesRl);
            if (species == null) return false;

            if (!invokeOneArg(pokemon, "setSpecies", species)) return false;

            Object form = resolveCobblemonForm(species, formName);
            if (form != null) {
                invokeOneArg(pokemon, "setForm", form);
            }
            invokeNoArgs(pokemon, "updateAspects");
            invokeNoArgs(entity, "refreshDimensions");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clearCache() {
        cachedCobblemonOptions = List.of();
        cobblemonOptionsReady = false;
        cachedLanguage = "";
    }

    private static boolean isCobblemonPresent() {
        int state = cobblemonPresence;
        if (state >= 0) return state == 1;

        try {
            Class.forName(COBBLEMON_ENTITY_CLASS, false, Thread.currentThread().getContextClassLoader());
            cobblemonPresence = 1;
            return true;
        } catch (Throwable ignored) {
            cobblemonPresence = 0;
            return false;
        }
    }

    private static List<WildexEntityVariantProbe.VariantOption> buildCobblemonOptions() {
        if (!isCobblemonPresent()) return List.of();
        try {
            Object speciesRegistry = getKotlinObject(COBBLEMON_SPECIES_CLASS, Thread.currentThread().getContextClassLoader());
            if (speciesRegistry == null) return List.of();

            Collection<?> implemented = asCollection(invokeNoArgs(speciesRegistry, "getImplemented"));
            if (implemented == null || implemented.isEmpty()) {
                implemented = asCollection(invokeNoArgs(speciesRegistry, "getSpecies"));
            }
            if (implemented == null || implemented.isEmpty()) return List.of();

            Map<String, WildexEntityVariantProbe.VariantOption> out = new LinkedHashMap<>();
            for (Object species : implemented) {
                ResourceLocation speciesId = asResourceLocation(invokeNoArgs(species, "getResourceIdentifier"));
                if (speciesId == null) continue;

                String speciesLabel = resolveSpeciesLabel(species, speciesId);
                Object standardForm = invokeNoArgs(species, "getStandardForm");
                String standardName = safeLower(formName(standardForm));
                Collection<?> forms = asCollection(invokeNoArgs(species, "getForms"));

                if (forms == null || forms.isEmpty()) {
                    String optionId = COBBLEMON_OPTION_PREFIX + "|" + speciesId + "|" + "standard";
                    out.putIfAbsent(optionId, new WildexEntityVariantProbe.VariantOption(optionId, speciesLabel));
                    continue;
                }

                for (Object form : forms) {
                    String name = formName(form);
                    if (name.isBlank()) name = "standard";
                    String normalized = safeLower(name);

                    String label = normalized.equals(standardName) || looksStandardForm(normalized)
                            ? speciesLabel
                            : (speciesLabel + " [" + name + "]");

                    String optionId = COBBLEMON_OPTION_PREFIX + "|" + speciesId + "|" + normalized;
                    out.putIfAbsent(optionId, new WildexEntityVariantProbe.VariantOption(optionId, label));
                }
            }

            return out.isEmpty() ? List.of() : List.copyOf(out.values());
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static Object resolveCobblemonForm(Object species, String formName) {
        if (species == null) return null;
        String normalized = safeLower(formName);

        Collection<?> forms = asCollection(invokeNoArgs(species, "getForms"));
        if (forms != null) {
            for (Object form : forms) {
                String candidate = safeLower(formName(form));
                if (candidate.equals(normalized)) return form;
            }
        }
        return invokeNoArgs(species, "getStandardForm");
    }

    private static String resolveSpeciesLabel(Object species, ResourceLocation speciesId) {
        Object translated = invokeNoArgs(species, "getTranslatedName");
        if (translated instanceof Component component) {
            String text = component.getString();
            if (text != null && !text.isBlank() && !looksLikeRawTranslationKey(text)) return text;
        }

        String showdownId = asString(invokeNoArgs(species, "unformattedShowdownId"));
        if (showdownId == null || showdownId.isBlank()) {
            showdownId = speciesId.getPath();
        }
        if (showdownId != null && !showdownId.isBlank()) {
            String key = speciesId.getNamespace() + ".species." + showdownId.toLowerCase(Locale.ROOT) + ".name";
            String localized = Component.translatable(key).getString();
            if (localized != null && !localized.isBlank() && !looksLikeRawTranslationKey(localized)) {
                return localized;
            }
        }

        String path = speciesId.getPath();
        if (path == null || path.isBlank()) return speciesId.toString();
        String[] words = path.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w == null || w.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) out.append(w.substring(1));
        }
        return out.isEmpty() ? speciesId.toString() : out.toString();
    }

    private static void ensureLanguageCache() {
        Minecraft mc = Minecraft.getInstance();
        String selected = "";
        try {
            selected = mc.getLanguageManager() == null ? "" : mc.getLanguageManager().getSelected();
        } catch (Throwable ignored) {
        }
        selected = selected == null ? "" : selected.toLowerCase(Locale.ROOT);
        if (!Objects.equals(cachedLanguage, selected)) {
            clearCache();
            cachedLanguage = selected;
        }
    }

    private static boolean looksLikeRawTranslationKey(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.indexOf(' ') >= 0) return false;
        return text.indexOf('.') > 0;
    }

    private static boolean looksStandardForm(String name) {
        if (name == null || name.isBlank()) return true;
        return "standard".equals(name) || "default".equals(name) || "normal".equals(name) || "base".equals(name);
    }

    private static String formName(Object form) {
        Object value = invokeNoArgs(form, "getName");
        if (value instanceof String s && !s.isBlank()) return s;
        return "";
    }

    private static String safeLower(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Method resolveMethod(Class<?> owner, String... names) {
        if (owner == null || names == null) return null;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                return owner.getMethod(name);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object getKotlinObject(String className, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            Field instance = type.getField("INSTANCE");
            return instance.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ResourceLocation asResourceLocation(Object value) {
        return value instanceof ResourceLocation rl ? rl : null;
    }

    private static Collection<?> asCollection(Object value) {
        return value instanceof Collection<?> c ? c : null;
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    private static Object invokeNoArgs(Object target, Method method) {
        if (target == null || method == null) return null;
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callOneArg(Object target, String methodName, Object argValue) {
        if (target == null || methodName == null || methodName.isBlank()) return null;
        try {
            Method m = resolveOneArgMethod(target.getClass(), methodName, argValue);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(target, argValue);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeOneArg(Object target, String methodName, Object argValue) {
        if (target == null || methodName == null || methodName.isBlank()) return false;
        try {
            Method m = resolveOneArgMethod(target.getClass(), methodName, argValue);
            if (m == null) return false;
            m.setAccessible(true);
            m.invoke(target, argValue);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method resolveOneArgMethod(Class<?> owner, String name, Object argValue) {
        if (owner == null || name == null || name.isBlank() || argValue == null) return null;
        Class<?> argClass = argValue.getClass();

        Method exact = null;
        Method assignable = null;

        for (Method m : owner.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterCount() != 1) continue;
            Class<?> p = m.getParameterTypes()[0];
            if (p == argClass) {
                exact = m;
                break;
            }
            if (isParameterAssignable(p, argClass) && assignable == null) {
                assignable = m;
            }
        }
        if (exact != null) return exact;
        if (assignable != null) return assignable;

        Class<?> c = owner;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!name.equals(m.getName()) || m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p == argClass || isParameterAssignable(p, argClass)) {
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static boolean isParameterAssignable(Class<?> parameterType, Class<?> argClass) {
        if (parameterType == null || argClass == null) return false;
        if (parameterType.isAssignableFrom(argClass)) return true;

        if (!parameterType.isPrimitive()) return false;
        return switch (parameterType.getName()) {
            case "boolean" -> argClass == Boolean.class;
            case "byte" -> argClass == Byte.class;
            case "short" -> argClass == Short.class;
            case "int" -> argClass == Integer.class;
            case "long" -> argClass == Long.class;
            case "float" -> argClass == Float.class;
            case "double" -> argClass == Double.class;
            case "char" -> argClass == Character.class;
            default -> false;
        };
    }

    private static OptionalDouble readNumber(Object target, Method getter) {
        if (target == null || getter == null) return OptionalDouble.empty();
        try {
            Object value = getter.invoke(target);
            if (value instanceof Number n) {
                return OptionalDouble.of(n.doubleValue());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return OptionalDouble.empty();
    }

    private record MethodHolder(Method method) {
    }

    private record PokemonStatAccessors(
            Method maxHealth,
            Method attack,
            Method defense,
            Method specialAttack,
            Method specialDefense,
            Method speed
    ) {
        static PokemonStatAccessors resolve(Class<?> pokemonClass) {
            return new PokemonStatAccessors(
                    resolveMethod(pokemonClass, "getMaxHealth", "getHp"),
                    resolveMethod(pokemonClass, "getAttack"),
                    resolveMethod(pokemonClass, "getDefence", "getDefense"),
                    resolveMethod(pokemonClass, "getSpecialAttack"),
                    resolveMethod(pokemonClass, "getSpecialDefence", "getSpecialDefense"),
                    resolveMethod(pokemonClass, "getSpeed")
            );
        }

        boolean hasAnyStat() {
            return maxHealth != null
                    || attack != null
                    || defense != null
                    || specialAttack != null
                    || specialDefense != null
                    || speed != null;
        }
    }
}
