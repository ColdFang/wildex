package de.coldfang.wildex.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Objects;

public final class WildexMobPreviewRenderer {

    private static final float BOX_FILL = 0.41f;
    private static final float ROT_SPEED_DEG_PER_TICK = 1.5f;

    private static final float DRAGON_MODEL_FILL = 0.62f;
    private static final float DRAGON_MODEL_EFFECTIVE_DIM = 8.8f;
    private static final float DRAGON_MODEL_PITCH_DEG = 11.0f;
    private static final float DRAGON_MODEL_YAW_OFFSET_DEG = 0.0f;

    private static final float FISH_MODEL_PITCH_DEG = 90.0f;
    private static final float FISH_MODEL_SIDE_YAW_DEG = 90.0f;

    private static final int FRAME_BG = 0x22FFFFFF;
    private static final int FRAME_OUTER = 0x88301E14;
    private static final int FRAME_INNER = 0x55FFFFFF;
    private static final int FRAME_CORNER = 0xAA2B1A10;

    private static final float ZOOM_MIN = 0.55f;
    private static final float ZOOM_MAX = 2.40f;
    private static final float ZOOM_STEP = 1.10f;
    private static final float DRAG_DEG_PER_PX = 0.60f;
    private static final float PITCH_MIN_DEG = -85.0f;
    private static final float PITCH_MAX_DEG = 85.0f;

    private ResourceLocation cachedId;
    private Mob cachedEntity;

    private float zoom = 1.0f;
    private boolean safePreviewMode = true;
    private float smoothedYaw = 0.0f;
    private boolean hasSmoothedYaw = false;
    private boolean dragActive = false;
    private int dragButton = -1;
    private int lastDragX = 0;
    private int lastDragY = 0;
    private float manualYawDeg = 0.0f;
    private float manualPitchDeg = 0.0f;

    public boolean isMouseOverPreview(WildexScreenLayout layout, int mouseX, int mouseY) {
        if (layout == null) return false;
        WildexScreenLayout.Area a = layout.rightPreviewArea();
        if (a == null) return false;
        return mouseX >= a.x() && mouseX < a.x() + a.w() && mouseY >= a.y() && mouseY < a.y() + a.h();
    }

    public void adjustZoom(double scrollDelta) {
        if (scrollDelta == 0.0) return;
        if (scrollDelta > 0.0) zoom *= ZOOM_STEP;
        else zoom /= ZOOM_STEP;
        zoom = clampZoom(zoom);
    }

    public void resetZoom() {
        zoom = 1.0f;
    }

    public void resetPreview() {
        zoom = 1.0f;
        manualYawDeg = 0.0f;
        manualPitchDeg = 0.0f;
        dragActive = false;
        dragButton = -1;
        hasSmoothedYaw = false;
    }

    public boolean isSafePreviewMode() {
        return safePreviewMode;
    }

    public void setSafePreviewMode(boolean enabled) {
        this.safePreviewMode = enabled;
        this.hasSmoothedYaw = false;
    }

    public boolean beginRotationDrag(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        dragActive = true;
        dragButton = button;
        lastDragX = mouseX;
        lastDragY = mouseY;
        return true;
    }

    public boolean updateRotationDrag(int mouseX, int mouseY, int button) {
        if (!dragActive || button != dragButton) return false;

        int dx = mouseX - lastDragX;
        int dy = mouseY - lastDragY;
        lastDragX = mouseX;
        lastDragY = mouseY;

        if (dx == 0 && dy == 0) return true;

        // Orbit-style controls: horizontal drag rotates around the model, vertical drag tilts.
        manualYawDeg = wrapDegrees(manualYawDeg - (dx * DRAG_DEG_PER_PX));
        manualPitchDeg = clampPitch(manualPitchDeg - (dy * DRAG_DEG_PER_PX));
        return true;
    }

    public boolean endRotationDrag(int button) {
        if (!dragActive || button != dragButton) return false;
        dragActive = false;
        dragButton = -1;
        return true;
    }

    public void render(
            GuiGraphics graphics,
            WildexScreenLayout layout,
            WildexScreenState state,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (layout == null || state == null) return;

        Objects.hash(mouseX, mouseY);

        WildexScreenLayout.Area area = layout.rightPreviewArea();
        if (area == null) return;

        drawFrame(graphics, area);

        String idStr = state.selectedMobId();
        if (idStr == null || idStr.isBlank()) {
            clearCachedEntity();
            drawFrameOverlay(graphics, area);
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) {
            clearCachedEntity();
            drawFrameOverlay(graphics, area);
            return;
        }

        boolean hiddenUndiscovered = CommonConfig.INSTANCE.hiddenMode.get() && !WildexDiscoveryCache.isDiscovered(id);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            drawFrameOverlay(graphics, area);
            return;
        }

        Level level = mc.level;

        Mob mob = getOrCreateEntity(level, id);
        if (mob == null) {
            drawFrameOverlay(graphics, area);
            return;
        }

        if (isEnderDragon(id)) {
            if (hiddenUndiscovered) RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
            try {
                renderDragonEntity(graphics, area, mob, partialTick, zoom);
            } finally {
                if (hiddenUndiscovered) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            drawFrameOverlay(graphics, area);
            return;
        }

        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        int innerX0 = x0 + 2;
        int innerY0 = y0 + 2;
        int innerX1 = x1 - 2;
        int innerY1 = y1 - 2;

        int cx = area.x() + Math.round(area.w() * 0.5f);
        int cy = area.y() + Math.round(area.h() * 0.5f);

        float bbW = mob.getBbWidth();
        float bbH = mob.getBbHeight();
        float dim = Math.max(bbW, bbH);
        if (dim < 0.01f) dim = 1.0f;

        float maxSizePx = Math.min(area.w(), area.h()) * BOX_FILL;
        float scale = (maxSizePx / dim) * zoom;

        float yaw = resolvePreviewYaw(mc, partialTick) + manualYawDeg;
        float pitch = manualPitchDeg;
        float renderPartialTick = safePreviewMode ? 0.0f : partialTick;

        boolean fish = isUprightFish(mob.getType());

        float prevYRot = mob.getYRot();
        float prevXRot = mob.getXRot();
        float prevXRotO = mob.xRotO;
        float prevBodyRot = mob.yBodyRot;
        float prevBodyRotO = mob.yBodyRotO;
        float prevHeadRot = mob.yHeadRot;
        float prevHeadRotO = mob.yHeadRotO;
        Vec3 prevDeltaMovement = mob.getDeltaMovement();
        boolean prevNoGravity = mob.isNoGravity();
        int prevTickCount = mob.tickCount;

        if (safePreviewMode) {
            mob.setDeltaMovement(Vec3.ZERO);
            mob.setNoGravity(true);
            mob.tickCount = 0;
        }

        if (!fish) {
            mob.setYRot(yaw);
            mob.setXRot(0.0f);
            mob.xRotO = 0.0f;
            mob.yBodyRot = yaw;
            mob.yBodyRotO = yaw;
            mob.yHeadRot = yaw;
            mob.yHeadRotO = yaw;
        } else {
            mob.setYRot(0.0f);
            mob.setXRot(0.0f);
            mob.xRotO = 0.0f;
            mob.yBodyRot = 0.0f;
            mob.yBodyRotO = 0.0f;
            mob.yHeadRot = 0.0f;
            mob.yHeadRotO = 0.0f;
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        graphics.enableScissor(innerX0, innerY0, innerX1, innerY1);

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 1050.0f);
        graphics.pose().scale(-scale, scale, scale);

        Quaternionf base = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(180.0f));
        graphics.pose().mulPose(base);

        if (fish) {
            graphics.pose().mulPose(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
            graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(pitch)));
            graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(FISH_MODEL_PITCH_DEG)));
            graphics.pose().mulPose(new Quaternionf().rotateY((float) Math.toRadians(FISH_MODEL_SIDE_YAW_DEG)));
        } else if (Math.abs(pitch) > 0.01f) {
            // Apply pitch as a view-space orbit tilt so it is consistently visible for vanilla and modded mobs.
            graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(pitch)));
        }

        dispatcher.setRenderShadow(false);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        if (hiddenUndiscovered) RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
        try {
            dispatcher.render(
                    mob,
                    0.0,
                    -mob.getBbHeight() * 0.5,
                    0.0,
                    0.0f,
                    renderPartialTick,
                    graphics.pose(),
                    graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT
            );

            graphics.flush();
        } finally {
            if (hiddenUndiscovered) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();

        graphics.disableScissor();

        mob.setYRot(prevYRot);
        mob.setXRot(prevXRot);
        mob.xRotO = prevXRotO;
        mob.yBodyRot = prevBodyRot;
        mob.yBodyRotO = prevBodyRotO;
        mob.yHeadRot = prevHeadRot;
        mob.yHeadRotO = prevHeadRotO;
        mob.setDeltaMovement(prevDeltaMovement);
        mob.setNoGravity(prevNoGravity);
        mob.tickCount = prevTickCount;

        drawFrameOverlay(graphics, area);
    }

    private static boolean isEnderDragon(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type == EntityType.ENDER_DRAGON;
    }

    private static boolean isUprightFish(EntityType<?> type) {
        return type == EntityType.COD
                || type == EntityType.SALMON
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.PUFFERFISH;
    }

    private static float computeYaw(Minecraft mc, float partialTick) {
        long gameTime = mc.level == null ? 0L : mc.level.getGameTime();
        float t = gameTime + partialTick;
        return (t * ROT_SPEED_DEG_PER_TICK) % 360.0f;
    }

    private float resolvePreviewYaw(Minecraft mc, float partialTick) {
        float rawYaw = computeYaw(mc, partialTick);
        if (dragActive) {
            if (!hasSmoothedYaw) {
                smoothedYaw = rawYaw;
                hasSmoothedYaw = true;
            }
            return smoothedYaw;
        }

        if (!safePreviewMode) return rawYaw;

        if (!hasSmoothedYaw) {
            smoothedYaw = rawYaw;
            hasSmoothedYaw = true;
            return smoothedYaw;
        }

        float delta = wrapDegrees(rawYaw - smoothedYaw);
        float maxStep = ROT_SPEED_DEG_PER_TICK * 0.55f;
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        smoothedYaw = wrapDegrees(smoothedYaw + delta);
        return smoothedYaw;
    }

    private static float wrapDegrees(float degrees) {
        float out = degrees % 360.0f;
        if (out >= 180.0f) out -= 360.0f;
        if (out < -180.0f) out += 360.0f;
        return out;
    }

    private static float clampPitch(float pitch) {
        return Math.max(PITCH_MIN_DEG, Math.min(PITCH_MAX_DEG, pitch));
    }

    private void renderDragonEntity(
            GuiGraphics graphics,
            WildexScreenLayout.Area area,
            Mob dragon,
            float partialTick,
            float zoom
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        int innerX0 = x0 + 2;
        int innerY0 = y0 + 2;
        int innerX1 = x1 - 2;
        int innerY1 = y1 - 2;

        int cx = area.x() + Math.round(area.w() * 0.5f);
        int cy = area.y() + Math.round(area.h() * 0.5f);

        float yaw = resolvePreviewYaw(mc, partialTick) + manualYawDeg + DRAGON_MODEL_YAW_OFFSET_DEG;
        float pitch = manualPitchDeg;
        float sizePx = Math.min(area.w(), area.h()) * DRAGON_MODEL_FILL;
        float scale = (sizePx / DRAGON_MODEL_EFFECTIVE_DIM) * zoom;
        float renderPartialTick = safePreviewMode ? 0.0f : partialTick;

        float prevYRot = dragon.getYRot();
        float prevXRot = dragon.getXRot();
        float prevXRotO = dragon.xRotO;
        float prevBodyRot = dragon.yBodyRot;
        float prevBodyRotO = dragon.yBodyRotO;
        float prevHeadRot = dragon.yHeadRot;
        float prevHeadRotO = dragon.yHeadRotO;
        Vec3 prevDeltaMovement = dragon.getDeltaMovement();
        boolean prevNoGravity = dragon.isNoGravity();
        int prevTickCount = dragon.tickCount;

        if (safePreviewMode) {
            dragon.setDeltaMovement(Vec3.ZERO);
            dragon.setNoGravity(true);
            dragon.tickCount = 0;
        }

        dragon.setYRot(yaw);
        dragon.setXRot(0.0f);
        dragon.xRotO = 0.0f;
        dragon.yBodyRot = yaw;
        dragon.yBodyRotO = yaw;
        dragon.yHeadRot = yaw;
        dragon.yHeadRotO = yaw;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        graphics.enableScissor(innerX0, innerY0, innerX1, innerY1);

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 1050.0f);
        graphics.pose().scale(-scale, scale, scale);

        graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(180.0f + yaw)));
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(DRAGON_MODEL_PITCH_DEG + pitch)));

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        dispatcher.render(
                dragon,
                0.0,
                -dragon.getBbHeight() * 0.38,
                0.0,
                0.0f,
                renderPartialTick,
                graphics.pose(),
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT
        );

        graphics.flush();
        graphics.pose().popPose();

        graphics.disableScissor();

        dispatcher.setRenderShadow(true);

        dragon.setYRot(prevYRot);
        dragon.setXRot(prevXRot);
        dragon.xRotO = prevXRotO;
        dragon.yBodyRot = prevBodyRot;
        dragon.yBodyRotO = prevBodyRotO;
        dragon.yHeadRot = prevHeadRot;
        dragon.yHeadRotO = prevHeadRotO;
        dragon.setDeltaMovement(prevDeltaMovement);
        dragon.setNoGravity(prevNoGravity);
        dragon.tickCount = prevTickCount;
    }

    private static void drawFrame(GuiGraphics graphics, WildexScreenLayout.Area a) {
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, FRAME_BG);

        graphics.fill(x0, y0, x1, y0 + 1, FRAME_OUTER);
        graphics.fill(x0, y1 - 1, x1, y1, FRAME_OUTER);
        graphics.fill(x0, y0, x0 + 1, y1, FRAME_OUTER);
        graphics.fill(x1 - 1, y0, x1, y1, FRAME_OUTER);

        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, FRAME_INNER);
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, FRAME_INNER);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, FRAME_INNER);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, FRAME_INNER);
    }

    private static void drawFrameOverlay(GuiGraphics graphics, WildexScreenLayout.Area a) {
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();

        int s = 6;

        graphics.fill(x0, y0, x0 + s, y0 + 1, FRAME_CORNER);
        graphics.fill(x0, y0, x0 + 1, y0 + s, FRAME_CORNER);

        graphics.fill(x1 - s, y0, x1, y0 + 1, FRAME_CORNER);
        graphics.fill(x1 - 1, y0, x1, y0 + s, FRAME_CORNER);

        graphics.fill(x0, y1 - 1, x0 + s, y1, FRAME_CORNER);
        graphics.fill(x0, y1 - s, x0 + 1, y1, FRAME_CORNER);

        graphics.fill(x1 - s, y1 - 1, x1, y1, FRAME_CORNER);
        graphics.fill(x1 - 1, y1 - s, x1, y1, FRAME_CORNER);
    }

    private Mob getOrCreateEntity(Level level, ResourceLocation id) {
        if (cachedEntity != null && Objects.equals(cachedId, id) && !cachedEntity.isRemoved()) {
            return cachedEntity;
        }

        clearCachedEntity();

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        Entity e = WildexEntityFactory.tryCreate(type, level);
        if (!(e instanceof Mob mob)) {
            if (e != null) e.discard();
            return null;
        }

        cachedId = id;
        cachedEntity = mob;
        return mob;
    }

    private void clearCachedEntity() {
        if (cachedEntity != null) cachedEntity.discard();
        cachedEntity = null;
        cachedId = null;
    }

    public void clear() {
        clearCachedEntity();
        hasSmoothedYaw = false;
        dragActive = false;
        dragButton = -1;
    }

    public boolean isDraggingPreview() {
        return dragActive;
    }

    private static float clampZoom(float v) {
        return Math.max(ZOOM_MIN, Math.min(v, ZOOM_MAX));
    }
}
