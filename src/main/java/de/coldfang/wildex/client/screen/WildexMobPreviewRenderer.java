package de.coldfang.wildex.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import java.util.Objects;

public final class WildexMobPreviewRenderer {

    private static final float BOX_FILL = 0.41f;
    private static final float ROT_SPEED_DEG_PER_TICK = 1.5f;

    private static final float DRAGON_HEAD_FILL = 1.0f;
    private static final float DRAGON_HEAD_TILT_DEG = 12.0f;
    private static final float DRAGON_HEAD_SCALE_BOOST = 3.5f;

    private static final float FISH_MODEL_PITCH_DEG = 90.0f;
    private static final float FISH_MODEL_SIDE_YAW_DEG = 90.0f;

    private static final int FRAME_BG = 0x22FFFFFF;
    private static final int FRAME_OUTER = 0x88301E14;
    private static final int FRAME_INNER = 0x55FFFFFF;
    private static final int FRAME_CORNER = 0xAA2B1A10;

    private static final float ZOOM_MIN = 0.55f;
    private static final float ZOOM_MAX = 2.40f;
    private static final float ZOOM_STEP = 1.10f;

    private ResourceLocation cachedId;
    private Mob cachedEntity;

    private float zoom = 1.0f;

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

        if (isEnderDragon(id)) {
            if (hiddenUndiscovered) RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
            try {
                renderDragonHead(graphics, area, partialTick, zoom);
            } finally {
                if (hiddenUndiscovered) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            drawFrameOverlay(graphics, area);
            return;
        }

        Mob mob = getOrCreateEntity(level, id);
        if (mob == null) {
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

        float yaw = computeYaw(mc, partialTick);

        boolean fish = isUprightFish(mob.getType());

        float prevYRot = mob.getYRot();
        float prevXRot = mob.getXRot();
        float prevBodyRot = mob.yBodyRot;
        float prevBodyRotO = mob.yBodyRotO;
        float prevHeadRot = mob.yHeadRot;
        float prevHeadRotO = mob.yHeadRotO;

        if (!fish) {
            mob.setYRot(yaw);
            mob.setXRot(0.0f);
            mob.yBodyRot = yaw;
            mob.yBodyRotO = yaw;
            mob.yHeadRot = yaw;
            mob.yHeadRotO = yaw;
        } else {
            mob.setYRot(0.0f);
            mob.setXRot(0.0f);
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
            graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(FISH_MODEL_PITCH_DEG)));
            graphics.pose().mulPose(new Quaternionf().rotateY((float) Math.toRadians(FISH_MODEL_SIDE_YAW_DEG)));
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
                    partialTick,
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
        mob.yBodyRot = prevBodyRot;
        mob.yBodyRotO = prevBodyRotO;
        mob.yHeadRot = prevHeadRot;
        mob.yHeadRotO = prevHeadRotO;

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

    private static void renderDragonHead(GuiGraphics graphics, WildexScreenLayout.Area area, float partialTick, float zoom) {
        Minecraft mc = Minecraft.getInstance();

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

        float yaw = computeYaw(mc, partialTick);

        ItemStack stack = new ItemStack(Items.DRAGON_HEAD);

        float sizePx = Math.min(area.w(), area.h()) * DRAGON_HEAD_FILL;
        float scale = ((sizePx / 16.0f) * DRAGON_HEAD_SCALE_BOOST) * zoom;

        graphics.enableScissor(innerX0, innerY0, innerX1, innerY1);

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 1050.0f);
        graphics.pose().scale(scale, scale, scale);

        graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI));
        graphics.pose().mulPose(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(DRAGON_HEAD_TILT_DEG)));

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                graphics.pose(),
                graphics.bufferSource(),
                mc.level,
                0
        );

        graphics.flush();
        graphics.pose().popPose();

        graphics.disableScissor();
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

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);

        Entity e = type.create(level);
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
    }

    private static float clampZoom(float v) {
        return Math.max(ZOOM_MIN, Math.min(v, ZOOM_MAX));
    }
}
