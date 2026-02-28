package de.coldfang.wildex.server.breeding;

import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexBreedingExtractor {

    // Keep retries bounded to reduce server load while still handling RNG-based taming.
    private static final int TAMING_CONFIRM_ATTEMPTS = 24;
    private static final Map<Class<?>, BabyAccess> BABY_ACCESS_BY_CLASS = new ConcurrentHashMap<>();
    private static final BabyAccess NO_BABY_ACCESS = new BabyAccess(null, null);

    private WildexBreedingExtractor() {
    }

    public static Result extract(ServerLevel level, EntityType<?> type) {
        if (level == null || type == null) return Result.empty();

        Entity probe = WildexEntityFactory.tryCreate(type, level);
        if (probe == null) return Result.empty();

        boolean ownable = probe instanceof OwnableEntity;
        if (!(probe instanceof Animal animalProbe)) {
            probe.discard();
            return new Result(ownable, List.of(), List.of());
        }

        Set<Item> breedingCandidates = findFoodCandidates(animalProbe);
        boolean supportsTaming = animalProbe instanceof TamableAnimal;
        boolean supportsBabyVariant = supportsBabyVariant(animalProbe);
        boolean supportsBabyTamingFallback = supportsTaming && supportsBabyVariant;
        probe.discard();

        Set<ResourceLocation> validBreeding = new LinkedHashSet<>();
        if (!breedingCandidates.isEmpty()) {
            FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
            for (Item item : breedingCandidates) {
                Animal entity = createAnimal(type, level);
                if (entity == null) continue;

                try {
                    prepareForBreedCheck(entity);
                    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                    entity.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
                    if (entity.isInLove()) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                        validBreeding.add(id);
                    }
                } catch (Throwable ignored) {
                    // Defensive against entity-specific interaction quirks.
                } finally {
                    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    entity.discard();
                }
            }
        }

        List<ResourceLocation> breedingItems = sortResourceIds(validBreeding);
        List<ResourceLocation> tamingItems = List.of();
        if (supportsTaming) {
            tamingItems = extractTamingItems(level, type, false, false);
            if (supportsBabyTamingFallback && tamingItems.isEmpty()) {
                tamingItems = extractTamingItems(level, type, false, true);
            }
            if (tamingItems.isEmpty()) {
                tamingItems = extractTamingItems(level, type, true, false);
            }
            if (supportsBabyTamingFallback && tamingItems.isEmpty()) {
                tamingItems = extractTamingItems(level, type, true, true);
            }
        }
        if (ownable && !supportsTaming) {
            tamingItems = extractOwnableTamingItems(level, type, false, false);
            if (supportsBabyVariant && tamingItems.isEmpty()) {
                tamingItems = extractOwnableTamingItems(level, type, false, true);
            }
            if (tamingItems.isEmpty()) {
                tamingItems = extractOwnableTamingItems(level, type, true, false);
            }
            if (supportsBabyVariant && tamingItems.isEmpty()) {
                tamingItems = extractOwnableTamingItems(level, type, true, true);
            }
        }
        return new Result(ownable, breedingItems, tamingItems);
    }

    private static List<ResourceLocation> extractTamingItems(
            ServerLevel level,
            EntityType<?> type,
            boolean sneaking,
            boolean babyMode
    ) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        Set<ResourceLocation> valid = new LinkedHashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

            // Phase 1 (cheap): if an untamed probe does not even consume the item, it cannot be a taming item.
            TamingProbeResult first = probeUntamedInteraction(type, level, fakePlayer, item, sneaking, babyMode);
            if (first == null || !first.consumed() || first.enteredLove()) continue;
            if (first.tamed()) {
                valid.add(itemId);
                continue;
            }

            // Phase 2 (strict): only accept item if taming can actually happen at least once.
            if (canTameInRetries(type, level, fakePlayer, item, sneaking, babyMode)) {
                valid.add(itemId);
            }
        }

        return sortResourceIds(valid);
    }

    private static boolean canTameInRetries(
            EntityType<?> type,
            ServerLevel level,
            FakePlayer fakePlayer,
            Item item,
            boolean sneaking,
            boolean babyMode
    ) {
        int safeAttempts = Math.max(0, TAMING_CONFIRM_ATTEMPTS - 1);
        for (int i = 0; i < safeAttempts; i++) {
            TamingProbeResult r = probeUntamedInteraction(type, level, fakePlayer, item, sneaking, babyMode);
            if (r != null && r.tamed()) return true;
        }
        return false;
    }

    private static List<ResourceLocation> extractOwnableTamingItems(
            ServerLevel level,
            EntityType<?> type,
            boolean sneaking,
            boolean babyMode
    ) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        Set<ResourceLocation> valid = new LinkedHashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

            OwnableProbeResult first = probeOwnableInteraction(type, level, fakePlayer, item, sneaking, babyMode);
            if (first == null || !first.consumed() || first.enteredLove()) continue;
            if (first.ownedAfterInteract()) {
                valid.add(itemId);
                continue;
            }

            if (canOwnInRetries(type, level, fakePlayer, item, sneaking, babyMode)) {
                valid.add(itemId);
            }
        }

        return sortResourceIds(valid);
    }

    private static boolean canOwnInRetries(
            EntityType<?> type,
            ServerLevel level,
            FakePlayer fakePlayer,
            Item item,
            boolean sneaking,
            boolean babyMode
    ) {
        int safeAttempts = Math.max(0, TAMING_CONFIRM_ATTEMPTS - 1);
        for (int i = 0; i < safeAttempts; i++) {
            OwnableProbeResult r = probeOwnableInteraction(type, level, fakePlayer, item, sneaking, babyMode);
            if (r != null && r.ownedAfterInteract()) return true;
        }
        return false;
    }

    private static TamingProbeResult probeUntamedInteraction(
            EntityType<?> type,
            ServerLevel level,
            FakePlayer fakePlayer,
            Item item,
            boolean sneaking,
            boolean babyMode
    ) {
        TamableAnimal entity = createTamable(type, level);
        if (entity == null) return null;

        try {
            prepareForTamingCheck(entity, babyMode);
            fakePlayer.setShiftKeyDown(sneaking);
            ItemStack hand = new ItemStack(item);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, hand);

            int beforeCount = hand.getCount();
            boolean beforeTame = entity.isTame();
            entity.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
            ItemStack after = fakePlayer.getMainHandItem();

            boolean consumed = wasItemConsumed(item, beforeCount, after);
            boolean tamed = entity.isTame() && !beforeTame;
            boolean enteredLove = entity.isInLove();
            return new TamingProbeResult(consumed, tamed, enteredLove);
        } catch (Throwable ignored) {
            // Defensive against entity-specific interaction quirks.
            return null;
        } finally {
            fakePlayer.setShiftKeyDown(false);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            entity.discard();
        }
    }

    private static OwnableProbeResult probeOwnableInteraction(
            EntityType<?> type,
            ServerLevel level,
            FakePlayer fakePlayer,
            Item item,
            boolean sneaking,
            boolean babyMode
    ) {
        OwnableProbe probe = createOwnable(type, level);
        if (probe == null) return null;

        Entity entity = probe.entity();
        OwnableEntity ownable = probe.ownable();
        try {
            prepareForOwnableTamingCheck(entity, babyMode);
            fakePlayer.setShiftKeyDown(sneaking);
            ItemStack hand = new ItemStack(item);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, hand);

            int beforeCount = hand.getCount();
            java.util.UUID beforeOwner = ownable.getOwnerUUID();
            if (beforeOwner != null) return null;

            entity.interact(fakePlayer, InteractionHand.MAIN_HAND);
            ItemStack after = fakePlayer.getMainHandItem();
            java.util.UUID afterOwner = ownable.getOwnerUUID();

            boolean consumed = wasItemConsumed(item, beforeCount, after);
            boolean ownedAfter = afterOwner != null;
            boolean enteredLove = entity instanceof Animal animal && animal.isInLove();
            return new OwnableProbeResult(consumed, ownedAfter, enteredLove);
        } catch (Throwable ignored) {
            return null;
        } finally {
            fakePlayer.setShiftKeyDown(false);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            entity.discard();
        }
    }

    private static Set<Item> findFoodCandidates(Animal entity) {
        Set<Item> out = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;
            try {
                if (entity.isFood(new ItemStack(item))) {
                    out.add(item);
                }
            } catch (Throwable ignored) {
                // Defensive for third-party entities with strict assumptions.
            }
        }
        return out;
    }

    private static Animal createAnimal(EntityType<?> type, ServerLevel level) {
        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (e instanceof Animal animal) return animal;
        if (e != null) e.discard();
        return null;
    }

    private static TamableAnimal createTamable(EntityType<?> type, ServerLevel level) {
        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (e instanceof TamableAnimal tamable) return tamable;
        if (e != null) e.discard();
        return null;
    }

    private static OwnableProbe createOwnable(EntityType<?> type, ServerLevel level) {
        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (e instanceof OwnableEntity ownable) return new OwnableProbe(e, ownable);
        if (e != null) e.discard();
        return null;
    }

    private static void prepareForBreedCheck(Animal entity) {
        entity.setAge(0);
        entity.resetLove();
        entity.setHealth(entity.getMaxHealth());

        if (entity instanceof TamableAnimal tamable) {
            tamable.setTame(true, true);
        }
        if (entity instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setTemper(horse.getMaxTemper());
        }
    }

    private static void prepareForTamingCheck(TamableAnimal entity, boolean babyMode) {
        entity.setTame(false, true);
        entity.setOrderedToSit(false);
        entity.setAge(0);
        setBabyState(entity, babyMode);
        entity.resetLove();
        entity.setHealth(entity.getMaxHealth());
    }

    private static void prepareForOwnableTamingCheck(Entity entity, boolean babyMode) {
        switch (entity) {
            case TamableAnimal tamable -> {
                tamable.setTame(false, true);
                tamable.setOrderedToSit(false);
                tamable.setAge(0);
                tamable.resetLove();
                tamable.setHealth(tamable.getMaxHealth());
            }
            case Animal animal -> {
                animal.setAge(0);
                animal.setHealth(animal.getMaxHealth());
                animal.resetLove();
            }
            default -> {
            }
        }
        setBabyState(entity, babyMode);
    }

    private static boolean supportsBabyVariant(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof AgeableMob ageable) {
            boolean before = ageable.isBaby();
            ageable.setBaby(!before);
            boolean changed = ageable.isBaby() != before;
            ageable.setBaby(before);
            return changed;
        }

        BabyAccess access = resolveBabyAccess(entity.getClass());
        if (access == NO_BABY_ACCESS) return false;

        Boolean before = readBabyState(entity, access);
        if (before == null) return false;

        boolean target = !before;
        if (!writeBabyState(entity, access, target)) return false;
        Boolean after = readBabyState(entity, access);
        writeBabyState(entity, access, before);
        return after != null && after == target;
    }

    private static void setBabyState(Entity entity, boolean baby) {
        if (entity == null) return;
        if (entity instanceof AgeableMob ageable) {
            ageable.setBaby(baby);
            return;
        }

        BabyAccess access = resolveBabyAccess(entity.getClass());
        if (access == NO_BABY_ACCESS) return;
        writeBabyState(entity, access, baby);
    }

    private static BabyAccess resolveBabyAccess(Class<?> entityClass) {
        if (entityClass == null) return NO_BABY_ACCESS;
        return BABY_ACCESS_BY_CLASS.computeIfAbsent(entityClass, WildexBreedingExtractor::findBabyAccess);
    }

    private static BabyAccess findBabyAccess(Class<?> entityClass) {
        Method getter = findMethod(entityClass, "isBaby");
        Method setter = findMethod(entityClass, "setBaby", boolean.class);
        if (getter == null || setter == null) return NO_BABY_ACCESS;

        Class<?> returnType = getter.getReturnType();
        if (returnType != boolean.class && returnType != Boolean.class) return NO_BABY_ACCESS;
        return new BabyAccess(getter, setter);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null || name == null || name.isBlank()) return null;
        try {
            return type.getMethod(name, parameterTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Boolean readBabyState(Entity entity, BabyAccess access) {
        if (entity == null || access == null || access.getter() == null) return null;
        try {
            Object value = access.getter().invoke(entity);
            if (value instanceof Boolean boolValue) return boolValue;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean writeBabyState(Entity entity, BabyAccess access, boolean baby) {
        if (entity == null || access == null || access.setter() == null) return false;
        try {
            access.setter().invoke(entity, baby);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean wasItemConsumed(Item expected, int beforeCount, ItemStack after) {
        if (beforeCount <= 0) return false;
        if (after.isEmpty()) return true;
        if (after.getItem() != expected) return false;
        return after.getCount() < beforeCount;
    }

    private static List<ResourceLocation> sortResourceIds(Set<ResourceLocation> ids) {
        ArrayList<ResourceLocation> sorted = new ArrayList<>(ids);
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        return List.copyOf(sorted);
    }

    public record Result(boolean ownable, List<ResourceLocation> breedingItemIds, List<ResourceLocation> tamingItemIds) {
        public static Result empty() {
            return new Result(false, List.of(), List.of());
        }
    }

    private record TamingProbeResult(boolean consumed, boolean tamed, boolean enteredLove) {
    }

    private record OwnableProbe(Entity entity, OwnableEntity ownable) {
    }

    private record OwnableProbeResult(boolean consumed, boolean ownedAfterInteract, boolean enteredLove) {
    }

    private record BabyAccess(Method getter, Method setter) {
    }
}
