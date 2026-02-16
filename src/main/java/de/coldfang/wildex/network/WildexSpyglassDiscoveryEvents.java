package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexCompletionHelper;
import de.coldfang.wildex.server.WildexProgressHooks;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexSpyglassDiscoveryEvents {

    private static final double RANGE = 98.0;
    private static final double HIT_INFLATE = 1.25;

    private static final int CHARGE_PARTICLE_EVERY = 2;

    private static final Map<UUID, ChargeState> CHARGE = new ConcurrentHashMap<>();

    private WildexSpyglassDiscoveryEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        ServerLevel level = sp.serverLevel();

        if (!CommonConfig.INSTANCE.hiddenMode.get()) {
            CHARGE.remove(sp.getUUID());
            return;
        }

        if (!sp.isUsingItem() || !sp.getUseItem().is(Items.SPYGLASS)) {
            CHARGE.remove(sp.getUUID());
            return;
        }

        Entity target = findLookTarget(level, sp);
        if (!(target instanceof Mob)) {
            CHARGE.remove(sp.getUUID());
            return;
        }

        if (sp.distanceToSqr(target) > (RANGE * RANGE)) {
            CHARGE.remove(sp.getUUID());
            return;
        }

        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (!WildexMobFilters.isTrackable(mobId)) {
            CHARGE.remove(sp.getUUID());
            return;
        }

        WildexWorldPlayerDiscoveryData disc = WildexWorldPlayerDiscoveryData.get(level);

        UUID playerId = sp.getUUID();
        if (disc.isDiscovered(playerId, mobId)) {
            CHARGE.remove(playerId);
            return;
        }

        int entityId = target.getId();

        ChargeState st = CHARGE.get(playerId);
        if (st == null || st.entityId != entityId || !mobId.equals(st.mobId)) {
            st = new ChargeState(entityId, mobId, 0);
            CHARGE.put(playerId, st);
        } else {
            st.ticks++;
        }

        if ((st.ticks % CHARGE_PARTICLE_EVERY) == 0) {
            spawnChargeParticlesForPlayer(level, sp, target);
        }

        int requiredChargeTicks = CommonConfig.INSTANCE.spyglassDiscoveryChargeTicks.get();
        if (st.ticks < requiredChargeTicks) return;

        boolean newlyDiscovered = disc.markDiscovered(playerId, mobId);

        CHARGE.remove(playerId);

        if (!newlyDiscovered) return;

        PacketDistributor.sendToPlayer(sp, new S2CDiscoveredMobPayload(mobId));

        boolean newlyCompleted = WildexCompletionHelper.markCompleteIfEligible(level, sp);
        WildexProgressHooks.onDiscoveryChanged(sp, mobId);

        spawnDiscoveryParticlesForPlayer(level, sp, target);

        PacketDistributor.sendToPlayer(sp, new S2CSpyglassDiscoveryEffectPayload(entityId, mobId));

        if (newlyCompleted) {
            WildexCompletionHelper.notifyCompleted(sp);
        }
    }

    private static void spawnChargeParticlesForPlayer(ServerLevel level, ServerPlayer sp, Entity target) {
        double w = Math.max(0.2, target.getBbWidth());
        double h = Math.max(0.2, target.getBbHeight());

        double cx = target.getX();
        double cy = target.getY() + (h * 0.55);
        double cz = target.getZ();

        level.sendParticles(
                sp,
                ParticleTypes.END_ROD,
                true,
                cx, cy, cz,
                3,
                w * 0.45, h * 0.35, w * 0.45,
                0.005
        );
    }

    private static void spawnDiscoveryParticlesForPlayer(ServerLevel level, ServerPlayer sp, Entity target) {
        double w = Math.max(0.2, target.getBbWidth());
        double h = Math.max(0.2, target.getBbHeight());

        double cx = target.getX();
        double cy = target.getY() + (h * 0.55);
        double cz = target.getZ();

        level.sendParticles(
                sp,
                ParticleTypes.GLOW,
                true,
                cx, cy, cz,
                70,
                w * 0.7, h * 0.55, w * 0.7,
                0.02
        );

        level.sendParticles(
                sp,
                ParticleTypes.WAX_ON,
                true,
                cx, target.getY() + (h * 0.4), cz,
                18,
                w * 0.6, h * 0.5, w * 0.6,
                0.01
        );
    }

    private static Entity findLookTarget(ServerLevel level, ServerPlayer sp) {
        Vec3 from = sp.getEyePosition();
        Vec3 view = sp.getViewVector(1.0f);
        Vec3 to = from.add(view.x * RANGE, view.y * RANGE, view.z * RANGE);

        HitResult blockHit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.of(sp)
        ));

        double maxDist = RANGE;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDist = blockHit.getLocation().distanceTo(from);
            to = from.add(view.x * maxDist, view.y * maxDist, view.z * maxDist);
        }

        AABB box = sp.getBoundingBox()
                .expandTowards(view.scale(maxDist))
                .inflate(HIT_INFLATE);

        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level,
                sp,
                from,
                to,
                box,
                e -> e != null && e.isAlive() && e != sp
        );

        return hit == null ? null : hit.getEntity();
    }

    private static final class ChargeState {
        private final int entityId;
        private final ResourceLocation mobId;
        private int ticks;

        private ChargeState(int entityId, ResourceLocation mobId, int ticks) {
            this.entityId = entityId;
            this.mobId = mobId;
            this.ticks = ticks;
        }
    }
}
