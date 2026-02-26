package de.coldfang.wildex.server.breeding;

import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WildexBreedingExtractor {

    // Higher retry count to make RNG taming (e.g. wolf/parrot) robust without manual lists.
    private static final int TAMING_CONFIRM_ATTEMPTS = 96;

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
        List<ResourceLocation> tamingItems = supportsTaming ? extractTamingItems(level, type) : List.of();
        return new Result(ownable, breedingItems, tamingItems);
    }

    private static List<ResourceLocation> extractTamingItems(ServerLevel level, EntityType<?> type) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        Set<ResourceLocation> valid = new LinkedHashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) continue;

            // Phase 1 (cheap): if an untamed probe does not even consume the item, it cannot be a taming item.
            TamingProbeResult first = probeUntamedInteraction(type, level, fakePlayer, item);
            if (first == null || !first.consumed() || first.enteredLove()) continue;
            if (first.tamed()) {
                valid.add(itemId);
                continue;
            }

            // Phase 2 (strict): only accept item if taming can actually happen at least once.
            if (canTameInRetries(type, level, fakePlayer, item, TAMING_CONFIRM_ATTEMPTS - 1)) {
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
            int attempts
    ) {
        int safeAttempts = Math.max(0, attempts);
        for (int i = 0; i < safeAttempts; i++) {
            TamingProbeResult r = probeUntamedInteraction(type, level, fakePlayer, item);
            if (r != null && r.tamed()) return true;
        }
        return false;
    }

    private static TamingProbeResult probeUntamedInteraction(
            EntityType<?> type,
            ServerLevel level,
            FakePlayer fakePlayer,
            Item item
    ) {
        TamableAnimal entity = createTamable(type, level);
        if (entity == null) return null;

        try {
            prepareForTamingCheck(entity);
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

    private static void prepareForTamingCheck(TamableAnimal entity) {
        entity.setTame(false, true);
        entity.setOrderedToSit(false);
        entity.setAge(0);
        entity.resetLove();
        entity.setHealth(entity.getMaxHealth());
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
}
