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

    private WildexBreedingExtractor() {
    }

    public static Result extract(ServerLevel level, EntityType<?> type) {
        if (level == null || type == null) return Result.empty();

        Entity probe = WildexEntityFactory.tryCreate(type, level);
        if (probe == null) return Result.empty();

        boolean ownable = probe instanceof OwnableEntity;
        if (!(probe instanceof Animal animalProbe)) {
            probe.discard();
            return new Result(ownable, List.of());
        }

        Set<Item> candidates = findFoodCandidates(animalProbe);
        probe.discard();
        if (candidates.isEmpty()) return new Result(ownable, List.of());

        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);

        Set<ResourceLocation> valid = new LinkedHashSet<>();
        for (Item item : candidates) {
            Animal entity = createAnimal(type, level);
            if (entity == null) continue;

            try {
                prepareForBreedCheck(entity);
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                entity.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
                if (entity.isInLove()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    valid.add(id);
                }
            } catch (Throwable ignored) {
                // Defensive against entity-specific interaction quirks.
            } finally {
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                entity.discard();
            }
        }

        ArrayList<ResourceLocation> sorted = new ArrayList<>(valid);
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        return new Result(ownable, List.copyOf(sorted));
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

    public record Result(boolean ownable, List<ResourceLocation> breedingItemIds) {
        public static Result empty() {
            return new Result(false, List.of());
        }
    }
}
