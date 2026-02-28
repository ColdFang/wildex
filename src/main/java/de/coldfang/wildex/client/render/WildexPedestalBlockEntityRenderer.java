package de.coldfang.wildex.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WildexPedestalBlockEntityRenderer implements BlockEntityRenderer<WildexPedestalBlockEntity> {

    private static final double MAX_RENDER_DISTANCE = 32.0;
    private static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    private static final double MID_DETAIL_DISTANCE_SQ = 24.0 * 24.0;
    private static final double NEAR_DETAIL_DISTANCE_SQ = 14.0 * 14.0;
    private static final float BASE_HEIGHT = 2.10f;
    private static final float BOB_AMPLITUDE = 0.07f;
    private static final float ROTATION_DEG_PER_TICK = 2.1f;
    private static final float MIN_SCALE = 0.22f;
    private static final float MAX_SCALE = 0.60f;
    private static final float CONE_APEX_Y = 1.06f;
    private static final int CONE_SEGMENTS_NEAR = 12;
    private static final int CONE_SEGMENTS_MID = 8;
    private static final int CONE_SEGMENTS_FAR = 6;

    public WildexPedestalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(WildexPedestalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return (int) MAX_RENDER_DISTANCE;
    }

    @Override
    public boolean shouldRender(WildexPedestalBlockEntity blockEntity, Vec3 cameraPos) {
        if (blockEntity == null || cameraPos == null) return false;
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, MAX_RENDER_DISTANCE);
    }

    @Override
    public AABB getRenderBoundingBox(WildexPedestalBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.75, 5.25, 1.75);
    }

    @Override
    public void render(
            WildexPedestalBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity == null) return;
        if (blockEntity.getDisplayMobId() == null) return;

        Entity entity = blockEntity.getOrCreateClientRenderEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (living.isRemoved()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 centerPos = Vec3.atCenterOf(blockEntity.getBlockPos());
        double distSq = cameraPos.distanceToSqr(centerPos);
        if (distSq > MAX_RENDER_DISTANCE_SQ) return;

        float time = mc.level.getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.16f) * BOB_AMPLITUDE;
        float yaw = (time * ROTATION_DEG_PER_TICK) % 360.0f;

        float bbW = Math.max(0.01f, living.getBbWidth());
        float bbH = Math.max(0.01f, living.getBbHeight());
        float dim = Math.max(bbW, bbH);
        float scale = clamp(0.70f / dim, MIN_SCALE, MAX_SCALE);
        float hologramY = BASE_HEIGHT + bob;
        float coneTopY = hologramY - Math.max(0.14f, Math.min(0.34f, bbH * scale * 0.38f));
        float coneRadius = Math.max(0.18f, Math.min(0.48f, bbW * scale * 0.95f + 0.12f));
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.21f);
        int coneSegments = distSq <= MID_DETAIL_DISTANCE_SQ ? (distSq <= NEAR_DETAIL_DISTANCE_SQ ? CONE_SEGMENTS_NEAR : CONE_SEGMENTS_MID) : CONE_SEGMENTS_FAR;
        boolean renderShellPass = distSq <= MID_DETAIL_DISTANCE_SQ;
        boolean renderGhostPass = distSq <= NEAR_DETAIL_DISTANCE_SQ;

        float prevYRot = living.getYRot();
        float prevXRot = living.getXRot();
        float prevYBodyRot = living.yBodyRot;
        float prevYHeadRot = living.yHeadRot;
        float prevYBodyRotO = living.yBodyRotO;
        float prevYHeadRotO = living.yHeadRotO;
        float prevXRotO = living.xRotO;
        boolean prevNoGravity = living.isNoGravity();
        Vec3 prevDelta = living.getDeltaMovement();
        int prevTickCount = living.tickCount;

        living.setNoGravity(true);
        living.setDeltaMovement(Vec3.ZERO);
        living.tickCount = 0;
        living.setYRot(yaw);
        living.setXRot(0.0f);
        living.xRotO = 0.0f;
        living.yBodyRot = yaw;
        living.yHeadRot = yaw;
        living.yBodyRotO = yaw;
        living.yHeadRotO = yaw;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        boolean rootPosePushed = false;
        try {
            renderProjectionCone(poseStack, buffer, yaw, coneTopY, coneRadius, pulse, coneSegments);

            poseStack.pushPose();
            rootPosePushed = true;
            poseStack.translate(0.5f, hologramY, 0.5f);

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            // Base hologram pass.
            renderMobPass(
                    living,
                    dispatcher,
                    poseStack,
                    new TintingBufferSource(buffer, 0.10f, 0.92f, 1.0f, 0.88f),
                    partialTick,
                    scale,
                    0.0f
            );
            if (renderShellPass) {
                // Slightly larger shell pass for holographic glow/ghosting.
                renderMobPass(
                        living,
                        dispatcher,
                        poseStack,
                        new TintingBufferSource(buffer, 0.18f, 1.0f, 1.0f, 0.62f + 0.14f * pulse),
                        partialTick,
                        scale * 1.03f,
                        0.006f
                );
            }
            if (renderGhostPass) {
                // Small offset pass to imitate scan ghosting.
                float ghostShift = 0.006f + 0.006f * pulse;
                renderMobPass(
                        living,
                        dispatcher,
                        poseStack,
                        new TintingBufferSource(buffer, 0.04f, 0.66f, 1.0f, 0.45f),
                        partialTick,
                        scale,
                        ghostShift
                );
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (rootPosePushed) poseStack.popPose();
            dispatcher.setRenderShadow(true);

            living.setYRot(prevYRot);
            living.setXRot(prevXRot);
            living.yBodyRot = prevYBodyRot;
            living.yHeadRot = prevYHeadRot;
            living.yBodyRotO = prevYBodyRotO;
            living.yHeadRotO = prevYHeadRotO;
            living.xRotO = prevXRotO;
            living.setNoGravity(prevNoGravity);
            living.setDeltaMovement(prevDelta);
            living.tickCount = prevTickCount;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void renderProjectionCone(
            PoseStack poseStack,
            MultiBufferSource buffer,
            float yawDeg,
            float topY,
            float topRadius,
            float pulse,
            int segments
    ) {
        float apexY = CONE_APEX_Y;
        int clampedSegments = Math.max(3, segments);
        if (topY <= apexY + 0.03f || topRadius <= 0.0f) return;

        VertexConsumer vertices = buffer.getBuffer(RenderType.debugStructureQuads());
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);

        Matrix4f mat = poseStack.last().pose();
        float rot = yawDeg * Mth.DEG_TO_RAD * 0.55f;
        int apexAlpha = (int) (28 + 12 * pulse);
        int rimAlpha = (int) (5 + 4 * pulse);

        for (int i = 0; i < clampedSegments; i++) {
            float a0 = rot + (Mth.TWO_PI * i / clampedSegments);
            float a1 = rot + (Mth.TWO_PI * (i + 1) / clampedSegments);

            float x0 = Mth.cos(a0) * topRadius;
            float z0 = Mth.sin(a0) * topRadius;
            float x1 = Mth.cos(a1) * topRadius;
            float z1 = Mth.sin(a1) * topRadius;

            // Degenerated quad = translucent triangle segment (apex -> rim -> rim).
            vertices.addVertex(mat, 0.0f, apexY, 0.0f).setColor(64, 232, 255, apexAlpha);
            vertices.addVertex(mat, x0, topY, z0).setColor(82, 250, 255, rimAlpha);
            vertices.addVertex(mat, x1, topY, z1).setColor(82, 250, 255, rimAlpha);
            vertices.addVertex(mat, 0.0f, apexY, 0.0f).setColor(64, 232, 255, apexAlpha);
        }

        poseStack.popPose();
    }

    private static void renderMobPass(
            LivingEntity living,
            EntityRenderDispatcher dispatcher,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float partialTick,
            float scale,
            float yOffset
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0f, yOffset, 0.0f);
        poseStack.scale(scale, scale, scale);

        dispatcher.render(
                living,
                0.0,
                0.0,
                0.0,
                0.0f,
                partialTick,
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT
        );

        poseStack.popPose();
    }

    private static int tint(int value, float mul) {
        return Mth.clamp((int) (value * mul), 0, 255);
    }

    private record TintingBufferSource(MultiBufferSource delegate, float rMul, float gMul, float bMul, float aMul) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return new TintingVertexConsumer(delegate.getBuffer(type), rMul, gMul, bMul, aMul);
        }
    }

    private static final class TintingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float rMul;
        private final float gMul;
        private final float bMul;
        private final float aMul;

        private TintingVertexConsumer(VertexConsumer delegate, float rMul, float gMul, float bMul, float aMul) {
            this.delegate = delegate;
            this.rMul = rMul;
            this.gMul = gMul;
            this.bMul = bMul;
            this.aMul = aMul;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(tint(red, rMul), tint(green, gMul), tint(blue, bMul), tint(alpha, aMul));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
