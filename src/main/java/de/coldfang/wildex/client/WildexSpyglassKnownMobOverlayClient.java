package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class WildexSpyglassKnownMobOverlayClient {

    private static final double RANGE = 98.0;
    private static final double HIT_INFLATE = 1.25;
    private static final float BASE_SCALE = 0.025f;
    private static final int AIM_GRACE_TICKS = 1;

    private static Component text = Component.empty();
    private static float anim = 0.0f;
    private static int lastEntityId = -1;
    private static int lostAimTicks = 0;
    private static boolean targetVisible = false;

    private WildexSpyglassKnownMobOverlayClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        targetVisible = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        if (!ClientConfig.INSTANCE.showDiscoveredSpyglassOverlay.get()) {
            fadeOutAndForget();
            return;
        }

        if (!mc.player.isUsingItem() || !mc.player.getUseItem().is(Items.SPYGLASS)) {
            fadeOutAndForget();
            return;
        }

        Entity target = findLookTarget(mc);
        if (!(target instanceof Mob mob) || !mob.isAlive()) {
            if (lastEntityId != -1 && ++lostAimTicks <= AIM_GRACE_TICKS) {
                targetVisible = true;
                anim = Mth.clamp(anim + 0.18f, 0.0f, 1.0f);
            } else {
                fadeOutAndForget();
            }
            return;
        }

        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        boolean hiddenMode = CommonConfig.INSTANCE.hiddenMode.get();
        if (hiddenMode && !WildexDiscoveryCache.isDiscovered(mobId)) {
            fadeOutAndForget();
            return;
        }

        Component nextText = mob.getType().getDescription();
        if (mob.getId() != lastEntityId || !equalsText(text, nextText)) {
            text = nextText;
            lastEntityId = mob.getId();
        }

        lostAimTicks = 0;
        targetVisible = true;
        anim = Mth.clamp(anim + 0.23f, 0.0f, 1.0f);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        if (!targetVisible) {
            fadeOutAndForget();
        }
        if (anim <= 0.01f || text == null || text.getString().isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity target = mc.level.getEntity(lastEntityId);
        if (!(target instanceof Mob mob) || !mob.isAlive()) {
            fadeOutAndForget();
            return;
        }

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        double x = Mth.lerp(partial, mob.xo, mob.getX());
        double y = Mth.lerp(partial, mob.yo, mob.getY()) + mob.getBbHeight() + 0.55;
        double z = Mth.lerp(partial, mob.zo, mob.getZ());

        float eased = easeOutCubic(anim);
        float pop = 0.92f + (eased * 0.13f);
        float scale = BASE_SCALE * pop;
        int alpha = Mth.clamp((int) (anim * 255.0f), 0, 255);
        int textColor = (alpha << 24) | 0xFFFFFF;

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        DebugRenderer.renderFloatingText(
                event.getPoseStack(),
                buffer,
                text.getString(),
                x,
                y,
                z,
                textColor,
                scale
        );
        buffer.endBatch();
    }

    public static void clear() {
        text = Component.empty();
        anim = 0.0f;
        lastEntityId = -1;
        lostAimTicks = 0;
        targetVisible = false;
    }

    private static void fadeOutAndForget() {
        targetVisible = false;
        anim = Mth.clamp(anim - 0.32f, 0.0f, 1.0f);
        if (anim <= 0.01f) {
            text = Component.empty();
            lastEntityId = -1;
            lostAimTicks = 0;
        }
    }

    private static Entity findLookTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 from = mc.player.getEyePosition();
        Vec3 view = mc.player.getViewVector(1.0f);
        Vec3 to = from.add(view.x * RANGE, view.y * RANGE, view.z * RANGE);

        HitResult blockHit = mc.level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.of(mc.player)
        ));

        double maxDist = RANGE;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDist = blockHit.getLocation().distanceTo(from);
            to = from.add(view.x * maxDist, view.y * maxDist, view.z * maxDist);
        }

        AABB box = mc.player.getBoundingBox()
                .expandTowards(view.scale(maxDist))
                .inflate(HIT_INFLATE);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level,
                mc.player,
                from,
                to,
                box,
                e -> e != null && e.isAlive() && e != mc.player
        );

        return hit == null ? null : hit.getEntity();
    }

    private static float easeOutCubic(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - c;
        return 1.0f - inv * inv * inv;
    }

    private static boolean equalsText(Component a, Component b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getString().equals(b.getString());
    }
}
