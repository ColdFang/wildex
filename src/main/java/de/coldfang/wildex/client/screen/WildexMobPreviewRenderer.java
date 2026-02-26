package de.coldfang.wildex.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexMobPreviewRenderer {

    private static final float BOX_FILL = 0.41f;
    private static final float ROT_SPEED_DEG_PER_TICK = 1.5f;

    private static final float DRAGON_MODEL_FILL = 0.62f;
    private static final float DRAGON_MODEL_EFFECTIVE_DIM = 8.8f;
    private static final float DRAGON_MODEL_PITCH_DEG = 11.0f;
    private static final float DRAGON_MODEL_YAW_OFFSET_DEG = 0.0f;

    private static final float FISH_MODEL_PITCH_DEG = 90.0f;
    private static final float FISH_MODEL_SIDE_YAW_DEG = 90.0f;
    private static final int PREVIEW_CORNER_CUT = 3;
    private static final int MODERN_PREVIEW_CLIP_CUT = 9;
    private static final int MODERN_PREVIEW_CLIP_TOP_TRIM = 10;
    private static final int MODERN_PREVIEW_CLIP_BOTTOM_TRIM = 2;
    // Fast rollback switch for the undiscovered mystery overlay experiment.
    private static final boolean ENABLE_UNDISCOVERED_RUNE_OVERLAY = true;
    private static final ResourceLocation SGA_FONT = ResourceLocation.fromNamespaceAndPath("minecraft", "alt");
    private static final String RUNE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final long RUNE_ANIMATION_START_MS = System.currentTimeMillis();
    // Keep in sync with WildexScreen.PREVIEW_HINT_SCALE (used by the Controls hint text).
    private static final float PREVIEW_HINT_SCALE_FOR_CLIP = 0.62f;
    private static final Component PREVIEW_CONTROLS_LABEL = Component.translatable("gui.wildex.preview_controls_hint");

    private static final float ZOOM_MIN = 0.55f;
    private static final float ZOOM_MAX = 2.40f;
    private static final float ZOOM_STEP = 1.10f;
    private static final float DRAG_DEG_PER_PX = 0.60f;
    private static final float PITCH_MIN_DEG = -85.0f;
    private static final float PITCH_MAX_DEG = 85.0f;
    private static final Map<Class<?>, BabyAccess> BABY_ACCESS_BY_CLASS = new ConcurrentHashMap<>();
    private static final BabyAccess NO_BABY_ACCESS = new BabyAccess(null, null);

    private ResourceLocation cachedId;
    private Mob cachedEntity;
    private Boolean cachedSupportsBabyVariant;

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
    private boolean babyPreviewEnabled = false;

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
        resetZoom();
        manualYawDeg = 0.0f;
        manualPitchDeg = 0.0f;
        dragActive = false;
        dragButton = -1;
        hasSmoothedYaw = false;
    }

    @SuppressWarnings("unused")
    public boolean isSafePreviewMode() {
        return safePreviewMode;
    }

    @SuppressWarnings("unused")
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
        carryForwardYawAfterDrag();
        dragActive = false;
        dragButton = -1;
        return true;
    }

    public void setBabyPreviewEnabled(boolean enabled) {
        this.babyPreviewEnabled = enabled;
    }

    public boolean isBabyPreviewEnabled() {
        return babyPreviewEnabled;
    }

    public boolean toggleBabyPreview(String mobId) {
        if (!canToggleBabyPreview(mobId)) {
            babyPreviewEnabled = false;
            return false;
        }
        babyPreviewEnabled = !babyPreviewEnabled;
        return true;
    }

    public boolean canToggleBabyPreview(String mobId) {
        ResourceLocation id = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (id == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Mob mob = getOrCreateEntity(mc.level, id);
        if (mob == null) return false;
        return supportsBabyVariant(mob);
    }

    @SuppressWarnings("unused")
    public void render(
            GuiGraphics graphics,
            WildexScreenLayout layout,
            WildexScreenState state,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (layout == null || state == null) return;

        WildexScreenLayout.Area area = layout.rightPreviewArea();
        if (area == null) return;

        boolean vintage = WildexThemes.isVintageLayout();
        if (vintage) {
            drawFrame(graphics, area);
        }

        String idStr = state.selectedMobId();
        if (idStr == null || idStr.isBlank()) {
            clearCachedEntity();
            drawFrameOverlay();
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) {
            clearCachedEntity();
            drawFrameOverlay();
            return;
        }

        boolean hiddenUndiscovered = WildexClientConfigView.hiddenMode() && !WildexDiscoveryCache.isDiscovered(id);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            drawFrameOverlay();
            return;
        }

        Level level = mc.level;

        Mob mob = getOrCreateEntity(level, id);
        if (mob == null) {
            drawFrameOverlay();
            return;
        }

        applyConfiguredBabyState(mob);

        if (isEnderDragon(id)) {
            renderDragonEntity(graphics, area, mob, partialTick, zoom, layout, hiddenUndiscovered);
            drawFrameOverlay();
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
        if (!WildexThemes.isVintageLayout()) {
            int topTrim = Math.max(0, Math.round(MODERN_PREVIEW_CLIP_TOP_TRIM * layout.scale()));
            int bottomTrim = Math.max(0, Math.round(MODERN_PREVIEW_CLIP_BOTTOM_TRIM * layout.scale()));
            int maxTrim = Math.max(0, (innerY1 - innerY0) - 1);
            if (topTrim + bottomTrim > maxTrim) {
                float total = Math.max(1.0f, topTrim + bottomTrim);
                topTrim = Math.round((topTrim / total) * maxTrim);
                bottomTrim = maxTrim - topTrim;
            }
            innerY0 += topTrim;
            innerY1 -= bottomTrim;
        }
        int clipCut = WildexThemes.isVintageLayout() ? 0 : MODERN_PREVIEW_CLIP_CUT;
        drawModernPreviewBackdrop(graphics, innerX0, innerY0, innerX1, innerY1, clipCut, hiddenUndiscovered);

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
            renderWithRoundedScissorBands(graphics, innerX0, innerY0, innerX1, innerY1, clipCut, () -> {
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
            });
        } finally {
            if (hiddenUndiscovered) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            dispatcher.setRenderShadow(true);
            graphics.pose().popPose();
        }

        if (hiddenUndiscovered && ENABLE_UNDISCOVERED_RUNE_OVERLAY) {
            ExclusionRect controlsExclusion = computeControlsExclusionRect(layout, innerX0, innerY0, innerX1, innerY1);
            drawUndiscoveredRuneParticles(graphics, innerX0, innerY0, innerX1, innerY1, controlsExclusion);
        }

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

        drawFrameOverlay();
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

    private void carryForwardYawAfterDrag() {
        if (!hasSmoothedYaw) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float rawYawNow = computeYaw(mc, 0.0f);
        float visibleYawNow = wrapDegrees(smoothedYaw + manualYawDeg);
        // Preserve current visible heading, then continue with normal auto-rotation from there.
        manualYawDeg = wrapDegrees(visibleYawNow - rawYawNow);
        hasSmoothedYaw = false;
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
            float zoom,
            WildexScreenLayout layout,
            boolean hiddenUndiscovered
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || layout == null) return;

        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        int innerX0 = x0 + 2;
        int innerY0 = y0 + 2;
        int innerX1 = x1 - 2;
        int innerY1 = y1 - 2;
        if (!WildexThemes.isVintageLayout()) {
            int topTrim = Math.max(0, Math.round(MODERN_PREVIEW_CLIP_TOP_TRIM * layout.scale()));
            int bottomTrim = Math.max(0, Math.round(MODERN_PREVIEW_CLIP_BOTTOM_TRIM * layout.scale()));
            int maxTrim = Math.max(0, (innerY1 - innerY0) - 1);
            if (topTrim + bottomTrim > maxTrim) {
                float total = Math.max(1.0f, topTrim + bottomTrim);
                topTrim = Math.round((topTrim / total) * maxTrim);
                bottomTrim = maxTrim - topTrim;
            }
            innerY0 += topTrim;
            innerY1 -= bottomTrim;
        }
        int clipCut = WildexThemes.isVintageLayout() ? 0 : MODERN_PREVIEW_CLIP_CUT;
        drawModernPreviewBackdrop(graphics, innerX0, innerY0, innerX1, innerY1, clipCut, hiddenUndiscovered);

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

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 1050.0f);
        graphics.pose().scale(-scale, scale, scale);

        graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(180.0f + yaw)));
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(DRAGON_MODEL_PITCH_DEG + pitch)));

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        if (hiddenUndiscovered) RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
        try {
            renderWithRoundedScissorBands(graphics, innerX0, innerY0, innerX1, innerY1, clipCut, () -> {
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
            });
        } finally {
            if (hiddenUndiscovered) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            graphics.pose().popPose();
            dispatcher.setRenderShadow(true);
        }

        if (hiddenUndiscovered && ENABLE_UNDISCOVERED_RUNE_OVERLAY) {
            ExclusionRect controlsExclusion = computeControlsExclusionRect(layout, innerX0, innerY0, innerX1, innerY1);
            drawUndiscoveredRuneParticles(graphics, innerX0, innerY0, innerX1, innerY1, controlsExclusion);
        }

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
        WildexUiRenderUtil.drawRoundedPanelFrame(graphics, a, WildexUiTheme.current(), PREVIEW_CORNER_CUT, 3, 1);
    }

    private static void drawFrameOverlay() {
        // No overlay mask: modern uses rounded clipping directly while rendering.
    }

    private static void drawModernPreviewBackdrop(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int cornerCut,
            boolean hiddenUndiscovered
    ) {
        if (graphics == null || WildexThemes.isVintageLayout()) return;

        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) return;

        int c = Math.max(0, Math.min(cornerCut, Math.max(0, (Math.min(w, h) / 2) - 1)));

        int top = hiddenUndiscovered ? 0xFF0A1016 : 0xFF102632;
        int mid = hiddenUndiscovered ? 0xFF101A23 : 0xFF184154;
        int bottom = hiddenUndiscovered ? 0xFF141D27 : 0xFF1E5A72;
        int glowRgb = hiddenUndiscovered ? 0x2A394A : 0x4EDFF6;
        int floorRgb = hiddenUndiscovered ? 0x111922 : 0x1D3648;

        int glowCenterY = y0 + Math.round(h * 0.42f);
        int glowRange = Math.max(1, Math.round(h * 0.52f));
        int floorStartY = y0 + Math.round(h * 0.72f);
        int sideInset = Math.max(2, w / 10);

        for (int y = y0; y < y1; y++) {
            int inset = rowInsetForRoundedRect(y, y0, y1, c);
            int left = x0 + inset;
            int right = x1 - inset;
            if (right <= left) continue;

            float t = (h <= 1) ? 0.0f : (float) (y - y0) / (float) (h - 1);
            int base;
            if (t <= 0.55f) {
                base = lerpColor(top, mid, t / 0.55f);
            } else {
                base = lerpColor(mid, bottom, (t - 0.55f) / 0.45f);
            }
            graphics.fill(left, y, right, y + 1, base);

            float glowDist = Math.abs(y - glowCenterY) / (float) glowRange;
            if (glowDist < 1.0f) {
                float glowPower = 1.0f - glowDist;
                int glowAlphaMax = hiddenUndiscovered ? 26 : 58;
                int glowAlpha = Math.max(0, Math.min(255, Math.round(glowAlphaMax * glowPower * glowPower)));
                int glowLeft = left + sideInset;
                int glowRight = right - sideInset;
                if (glowRight > glowLeft && glowAlpha > 0) {
                    graphics.fill(glowLeft, y, glowRight, y + 1, (glowAlpha << 24) | glowRgb);
                }
            }

            if (y >= floorStartY) {
                float floorT = (float) (y - floorStartY) / Math.max(1.0f, (float) (y1 - floorStartY));
                int floorAlphaMax = hiddenUndiscovered ? 62 : 78;
                int floorAlpha = Math.max(0, Math.min(255, Math.round(floorAlphaMax * floorT)));
                if (floorAlpha > 0) {
                    graphics.fill(left, y, right, y + 1, (floorAlpha << 24) | floorRgb);
                }
            }
        }
    }

    private static int rowInsetForRoundedRect(int y, int y0, int y1, int cornerCut) {
        if (cornerCut <= 0) return 0;
        int topRel = y - y0;
        int bottomRel = (y1 - 1) - y;
        int topInset = topRel < cornerCut ? (cornerCut - topRel) : 0;
        int bottomInset = bottomRel < cornerCut ? (cornerCut - bottomRel) : 0;
        return Math.max(topInset, bottomInset);
    }

    private static int lerpColor(int a, int b, float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int oa = Math.round(aa + ((ba - aa) * clamped));
        int or = Math.round(ar + ((br - ar) * clamped));
        int og = Math.round(ag + ((bg - ag) * clamped));
        int ob = Math.round(ab + ((bb - ab) * clamped));
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    private static void drawUndiscoveredRuneParticles(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            ExclusionRect exclusion
    ) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) return;

        int clipY1 = Math.max(y0 + 1, y1 - 1);
        int clipH = clipY1 - y0;
        if (clipH <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        int glyphH = Math.max(1, WildexUiText.lineHeight(mc.font));
        long nowMs = System.currentTimeMillis();
        double elapsedSeconds = Math.max(0.0, (nowMs - RUNE_ANIMATION_START_MS) / 1000.0);
        double cycleHeight = clipH + 24.0;
        int baseColumnCount = Math.max(18, Math.min(48, w / 8));
        int columnCount = Math.max(1, baseColumnCount - 2);
        int particlesPerColumn = 2;
        int baseColor = runeBaseColorForCurrentTheme();

        WildexScissor.enablePhysical(graphics, x0, y0, x1, clipY1);
        try {
            for (int col = 0; col < columnCount; col++) {
                int seed = hashInt(col * 1103515245 + 12345);

                double speed = 16.0 + ((seed & 7) * 2.1);
                double phase = (seed >>> 16) & 0xFF;
                double spacing = (double) w / columnCount;
                double baseX = x0 + ((col + 0.5) * spacing);

                for (int p = 0; p < particlesPerColumn; p++) {
                    double layerOffset = (cycleHeight / particlesPerColumn) * p;
                    double y = y0 - 12.0 + ((elapsedSeconds * speed + phase + layerOffset) % cycleHeight);

                    double wiggle = Math.sin((elapsedSeconds * 2.7) + col * 0.91 + p * 0.6) * 1.8;
                    int drawX = (int) Math.round(baseX + wiggle);
                    int drawY = (int) Math.round(y);

                    int runeIndex = Math.floorMod((int) (elapsedSeconds * 8.0 + col * 7 + p * 11), RUNE_CHARS.length());
                    char rune = RUNE_CHARS.charAt(runeIndex);

                    double progress = (y - (y0 - 12.0)) / cycleHeight;
                    float alphaF = 0.12f + (float) Math.sin(progress * Math.PI) * 0.64f;
                    alphaF = Math.max(0.0f, Math.min(1.0f, alphaF));
                    int alpha = Math.max(0, Math.min(255, Math.round(alphaF * 255.0f)));
                    int color = (alpha << 24) | (baseColor & 0x00FFFFFF);

                    if (exclusion != null) {
                        int glyphW = Math.max(3, WildexUiText.width(mc.font, String.valueOf(rune)));
                        if (intersects(drawX, drawY, drawX + glyphW, drawY + glyphH, exclusion.x0(), exclusion.y0(), exclusion.x1(), exclusion.y1())) {
                            continue;
                        }
                    }

                    Component glyph = Component.literal(String.valueOf(rune)).withStyle(style -> style.withFont(SGA_FONT));
                    WildexUiText.draw(graphics, mc.font, glyph, drawX, drawY, color, false);
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private static ExclusionRect computeControlsExclusionRect(
            WildexScreenLayout layout,
            int innerX0,
            int innerY0,
            int innerX1,
            int innerY1
    ) {
        if (layout == null || WildexThemes.isModernLayout()) return null;

        WildexScreenLayout.PreviewControlsHintAnchor anchor = layout.previewControlsHintAnchor();
        if (anchor == null) return null;

        Minecraft mc = Minecraft.getInstance();
        int textW = WildexUiText.width(mc.font, PREVIEW_CONTROLS_LABEL);
        int scaledTextW = Math.max(1, Math.round(textW * PREVIEW_HINT_SCALE_FOR_CLIP));
        int scaledTextH = Math.max(1, Math.round(mc.font.lineHeight * PREVIEW_HINT_SCALE_FOR_CLIP));

        int hintBaseX = anchor.x();
        int hintBaseY = anchor.bottomY() - scaledTextH;
        int availableW = Math.max(0, anchor.rightBoundX() - hintBaseX);
        int hintW = Math.min(scaledTextW, availableW);
        if (hintW == 0) return null;

        int pad = Math.max(2, Math.round(2 * layout.scale()));
        int padX = pad;
        int padY = pad;

        int exX0 = Math.max(innerX0, hintBaseX - padX);
        int exY0 = Math.max(innerY0, hintBaseY - padY);
        int exX1 = Math.min(innerX1, hintBaseX + hintW + padX);
        int exY1 = Math.min(innerY1, hintBaseY + scaledTextH + padY);

        if (exX1 <= exX0 || exY1 <= exY0) return null;
        return new ExclusionRect(exX0, exY0, exX1, exY1);
    }

    private static boolean intersects(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
        return ax0 < bx1 && ax1 > bx0 && ay0 < by1 && ay1 > by0;
    }

    private static int runeBaseColorForCurrentTheme() {
        DesignStyle style = WildexThemes.current().layoutProfile();
        return switch (style) {
            case VINTAGE -> 0xFF111111;
            case MODERN -> 0xFF29E8F1;
            case JUNGLE -> 0xFF1F5B2D;
            case RUNES -> 0xFF4A2A6E;
            case STEAMPUNK -> 0xFF6A4325;
        };
    }

    private static int hashInt(int x) {
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x;
    }

    private static void renderWithRoundedScissorBands(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int cornerCut,
            Runnable drawCall
    ) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0 || drawCall == null) return;

        int maxCut = Math.max(0, (Math.min(w, h) / 2) - 1);
        int c = Math.max(0, Math.min(cornerCut, maxCut));

        if (c == 0) {
            WildexScissor.enablePhysical(graphics, x0, y0, x1, y1);
            try {
                drawCall.run();
            } finally {
                graphics.disableScissor();
            }
            return;
        }

        for (int i = 0; i < c; i++) {
            int inset = c - i;
            int sy0 = y0 + i;
            int sx0 = x0 + inset;
            int sx1 = x1 - inset;
            if (sx1 <= sx0) continue;
            WildexScissor.enablePhysical(graphics, sx0, sy0, sx1, sy0 + 1);
            try {
                drawCall.run();
            } finally {
                graphics.disableScissor();
            }
        }

        int cy0 = y0 + c;
        int cy1 = y1 - c;
        if (cy1 > cy0) {
            WildexScissor.enablePhysical(graphics, x0, cy0, x1, cy1);
            try {
                drawCall.run();
            } finally {
                graphics.disableScissor();
            }
        }

        for (int i = 0; i < c; i++) {
            int inset = i + 1;
            int sy0 = (y1 - c) + i;
            int sx0 = x0 + inset;
            int sx1 = x1 - inset;
            if (sx1 <= sx0) continue;
            WildexScissor.enablePhysical(graphics, sx0, sy0, sx1, sy0 + 1);
            try {
                drawCall.run();
            } finally {
                graphics.disableScissor();
            }
        }
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
        cachedSupportsBabyVariant = null;
        return mob;
    }

    private void clearCachedEntity() {
        if (cachedEntity != null) cachedEntity.discard();
        cachedEntity = null;
        cachedId = null;
        cachedSupportsBabyVariant = null;
    }

    public void clear() {
        clearCachedEntity();
        hasSmoothedYaw = false;
        dragActive = false;
        dragButton = -1;
        babyPreviewEnabled = false;
    }

    public boolean isDraggingPreview() {
        return dragActive;
    }

    private static float clampZoom(float v) {
        return Math.max(ZOOM_MIN, Math.min(v, ZOOM_MAX));
    }

    private void applyConfiguredBabyState(Mob mob) {
        if (mob == null) return;

        boolean supports = supportsBabyVariant(mob);
        if (!supports) {
            babyPreviewEnabled = false;
            return;
        }
        setMobBabyState(mob, babyPreviewEnabled);
    }

    private boolean supportsBabyVariant(Mob mob) {
        if (mob == null) return false;
        if (cachedEntity == mob && cachedSupportsBabyVariant != null) {
            return cachedSupportsBabyVariant;
        }

        boolean supported = detectBabyVariantSupport(mob);
        if (cachedEntity == mob) cachedSupportsBabyVariant = supported;
        return supported;
    }

    private static boolean detectBabyVariantSupport(Mob mob) {
        if (mob == null) return false;
        if (mob instanceof AgeableMob) return true;

        BabyAccess access = resolveBabyAccess(mob.getClass());
        if (access == NO_BABY_ACCESS) return false;

        Boolean before = readBabyState(mob, access);
        if (before == null) return false;

        boolean target = !before;
        if (!writeBabyState(mob, access, target)) return false;

        Boolean after = readBabyState(mob, access);
        writeBabyState(mob, access, before);
        return after != null && after == target;
    }

    private static void setMobBabyState(Mob mob, boolean baby) {
        if (mob == null) return;

        if (mob instanceof AgeableMob ageable) {
            ageable.setBaby(baby);
            return;
        }

        BabyAccess access = resolveBabyAccess(mob.getClass());
        if (access == NO_BABY_ACCESS) return;
        writeBabyState(mob, access, baby);
    }

    private static BabyAccess resolveBabyAccess(Class<?> entityClass) {
        if (entityClass == null) return NO_BABY_ACCESS;
        return BABY_ACCESS_BY_CLASS.computeIfAbsent(entityClass, WildexMobPreviewRenderer::findBabyAccess);
    }

    private static BabyAccess findBabyAccess(Class<?> entityClass) {
        Method getter = findMethod(entityClass, "isBaby");
        Method setter = findMethod(entityClass, "setBaby", boolean.class);

        if (getter == null || setter == null) return NO_BABY_ACCESS;
        Class<?> rt = getter.getReturnType();
        if (rt != boolean.class && rt != Boolean.class) return NO_BABY_ACCESS;
        return new BabyAccess(getter, setter);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        if (type == null || name == null || name.isBlank()) return null;

        try {
            return type.getMethod(name, params);
        } catch (Throwable ignored) {
        }

        Class<?> c = type;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                try {
                    m.setAccessible(true);
                } catch (Throwable ignored) {
                }
                return m;
            } catch (Throwable ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static Boolean readBabyState(Mob mob, BabyAccess access) {
        if (mob == null || access == null || access.getter() == null) return null;
        try {
            Object out = access.getter().invoke(mob);
            if (out instanceof Boolean b) return b;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean writeBabyState(Mob mob, BabyAccess access, boolean baby) {
        if (mob == null || access == null || access.setter() == null) return false;
        try {
            access.setter().invoke(mob, baby);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private record ExclusionRect(int x0, int y0, int x1, int y1) {
    }

    private record BabyAccess(Method getter, Method setter) {
    }
}




