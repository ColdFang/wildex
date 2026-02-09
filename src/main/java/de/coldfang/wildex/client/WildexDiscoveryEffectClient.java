package de.coldfang.wildex.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexDiscoveryEffectClient {

    private static final int DURATION_TICKS = 5;

    private static final Map<Integer, EffectState> ACTIVE = new ConcurrentHashMap<>();

    private WildexDiscoveryEffectClient() {
    }

    public static void play(Entity e) {
        if (e == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int id = e.getId();

        long seed = ((long) id * 341873128712L) ^ mc.level.getGameTime();
        ACTIVE.put(id, new EffectState(DURATION_TICKS, RandomSource.create(seed)));

        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.BOOK_PAGE_TURN,
                1.0f,
                1.0f
        ));
    }

    public static void tickClient() {
        if (ACTIVE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }

        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            int id = entry.getKey();
            EffectState st = entry.getValue();

            Entity e = mc.level.getEntity(id);
            if (e != null) {
                spawnTickParticles(mc, e, st.rng);
            }

            st.remainingTicks--;
            if (st.remainingTicks <= 0) it.remove();
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void spawnTickParticles(Minecraft mc, Entity e, RandomSource rng) {
        double x = e.getX();
        double y = e.getY() + (e.getBbHeight() * 0.55);
        double z = e.getZ();

        int n = 4;
        for (int i = 0; i < n; i++) {
            double ox = (rng.nextDouble() - 0.5) * (e.getBbWidth() * 0.9);
            double oy = (rng.nextDouble() - 0.5) * (e.getBbHeight() * 0.6);
            double oz = (rng.nextDouble() - 0.5) * (e.getBbWidth() * 0.9);

            double vx = (rng.nextDouble() - 0.5) * 0.02;
            double vy = 0.02 + rng.nextDouble() * 0.02;
            double vz = (rng.nextDouble() - 0.5) * 0.02;

            mc.level.addParticle(ParticleTypes.ENCHANT, x + ox, y + oy, z + oz, vx, vy, vz);
        }
    }

    private static final class EffectState {
        private int remainingTicks;
        private final RandomSource rng;

        private EffectState(int remainingTicks, RandomSource rng) {
            this.remainingTicks = remainingTicks;
            this.rng = rng;
        }
    }
}
