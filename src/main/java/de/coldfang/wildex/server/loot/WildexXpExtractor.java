package de.coldfang.wildex.server.loot;

import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WildexXpExtractor {

    private static final int DRAGON_FIRST_KILL_XP = 12_000;
    private static final int DRAGON_REPEAT_KILL_XP = 500;
    private static final Method SET_LAST_HURT_BY_PLAYER = resolveSetLastHurtByPlayerMethod();

    private WildexXpExtractor() {
    }

    public record XpSummary(boolean known, int minXp, int maxXp) {
        public static XpSummary unknown() {
            return new XpSummary(false, 0, 0);
        }

        public static XpSummary known(int minXp, int maxXp) {
            int min = Math.max(0, minXp);
            int max = Math.max(min, Math.max(0, maxXp));
            return new XpSummary(true, min, max);
        }
    }

    public static XpSummary samplePlayerKillXp(ServerLevel level, EntityType<?> type, int samplesPerPlan) {
        if (level == null || type == null) return XpSummary.unknown();
        if (type == EntityType.ENDER_DRAGON) {
            return resolveEnderDragonXp(level);
        }
        int samples = Math.max(1, samplesPerPlan);

        ServerPlayer fakePlayer = FakePlayerFactory.getMinecraft(level);

        List<XpSamplingPlan> plans = buildPlayerKillPlans(type, samples);
        if (plans.isEmpty()) return XpSummary.unknown();

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        boolean seenAny = false;

        for (XpSamplingPlan plan : plans) {
            if (plan == null) continue;

            Entity entity = WildexEntityFactory.tryCreate(type, level);
            if (!(entity instanceof LivingEntity living)) {
                if (entity != null) entity.discard();
                continue;
            }

            try {
                if (plan.entityConfigurator() != null) {
                    try {
                        plan.entityConfigurator().accept(living);
                    } catch (Throwable ignored) {
                        // State hooks must never break sampling.
                    }
                }

                int runSamples = Math.max(1, plan.samples());
                for (int i = 0; i < runSamples; i++) {
                    int xp = readPlayerKillXp(level, living, fakePlayer);
                    min = Math.min(min, xp);
                    max = Math.max(max, xp);
                    seenAny = true;
                }
            } finally {
                living.discard();
            }
        }

        if (!seenAny) return XpSummary.unknown();
        return XpSummary.known(min == Integer.MAX_VALUE ? 0 : min, max == Integer.MIN_VALUE ? 0 : max);
    }

    private static int readPlayerKillXp(ServerLevel level, LivingEntity living, ServerPlayer killer) {
        if (level == null || living == null) return 0;
        try {
            applyPlayerKillContext(living, killer);
            return Math.max(0, living.getExperienceReward(level, killer));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static Method resolveSetLastHurtByPlayerMethod() {
        try {
            return LivingEntity.class.getMethod("setLastHurtByPlayer", Player.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyPlayerKillContext(LivingEntity living, ServerPlayer killer) {
        if (living == null || killer == null) return;
        if (SET_LAST_HURT_BY_PLAYER == null) return;
        try {
            SET_LAST_HURT_BY_PLAYER.invoke(living, killer);
        } catch (Throwable ignored) {
        }
    }

    private static XpSummary resolveEnderDragonXp(ServerLevel level) {
        XpSummary fallback = XpSummary.known(DRAGON_REPEAT_KILL_XP, DRAGON_FIRST_KILL_XP);
        if (level == null) return fallback;

        try {
            MinecraftServer server = level.getServer();
            ServerLevel endLevel = server.getLevel(Level.END);
            if (endLevel == null) return fallback;

            Object dragonFight = endLevel.getDragonFight();
            if (dragonFight == null) return fallback;

            Boolean previouslyKilled = invokeDragonPreviouslyKilled(dragonFight);
            if (previouslyKilled == null) return fallback;

            return previouslyKilled
                    ? XpSummary.known(DRAGON_REPEAT_KILL_XP, DRAGON_REPEAT_KILL_XP)
                    : XpSummary.known(DRAGON_FIRST_KILL_XP, DRAGON_FIRST_KILL_XP);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Boolean invokeDragonPreviouslyKilled(Object target) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod("hasPreviouslyKilledDragon");
            Object out = method.invoke(target);
            if (out instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static List<XpSamplingPlan> buildPlayerKillPlans(EntityType<?> type, int baseSamples) {
        if (type == null) return List.of();
        int samples = Math.max(1, baseSamples);

        List<XpSamplingPlan> plans = new ArrayList<>();
        plans.add(new XpSamplingPlan(samples, null));

        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            int variantSamples = Math.max(16, samples / 2);
            plans.add(new XpSamplingPlan(variantSamples, e -> configureSlimeSize(e, 1)));
            plans.add(new XpSamplingPlan(variantSamples, e -> configureSlimeSize(e, 2)));
        }

        return List.copyOf(plans);
    }

    private static void configureSlimeSize(Entity entity, int size) {
        if (!(entity instanceof Slime slime)) return;
        try {
            slime.setSize(Math.max(1, size), true);
        } catch (Throwable ignored) {
        }
    }

    private record XpSamplingPlan(
            int samples,
            Consumer<Entity> entityConfigurator
    ) {
    }
}
