package de.coldfang.wildex.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WildexDiscoveryToast implements Toast {

    private static final long DURATION_MS = 5000L;

    private static final int W = 160;
    private static final int H = 32;

    private static final int FRAME_BG = 0xCC151515;
    private static final int FRAME_OUTER = 0xFF2B1A10;
    private static final int FRAME_INNER = 0x66FFFFFF;

    private static final int ICON_PAD_L = 6;
    private static final int ICON_CENTER_X = 22;
    private static final int ICON_BOTTOM_Y = 26;

    private static final int TEXT_X = 42;
    private static final int TEXT_Y = 7;
    private static final int TEXT_LINE_GAP = 10;

    private static final float DISPLAY_PITCH = -10.0f;
    private static final float ROT_SPEED_DEG_PER_SEC = 45.0f;
    private static final float DRAGON_TOAST_EFFECTIVE_DIM = 6.4f;
    private static final float DRAGON_TOAST_Y_RENDER_OFFSET_FACTOR = -0.20f;
    private static final float DRAGON_TOAST_PITCH_DEG = 11.0f;

    private final ResourceLocation mobId;
    private final Component title;

    private Entity cachedEntity;

    public WildexDiscoveryToast(ResourceLocation mobId, Component title) {
        this.mobId = mobId;
        this.title = title;
    }

    @Override
    public @NotNull Visibility render(@NotNull GuiGraphics g, @NotNull ToastComponent toastComponent, long timeSinceLastVisible) {
        drawFrame(g);

        Minecraft mc = Minecraft.getInstance();

        if (cachedEntity == null && mc.level != null) {
            mc.level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                    .getOptional(mobId)
                    .ifPresent(type -> cachedEntity = WildexEntityFactory.tryCreate(type, mc.level));
        }

        if (cachedEntity != null) {
            renderEntityOnToast(g, cachedEntity, computeYaw(timeSinceLastVisible));
        }

        renderWrappedTitle(g, mc.font, title);

        return timeSinceLastVisible >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    private static float computeYaw(long timeSinceLastVisibleMs) {
        return ((float) timeSinceLastVisibleMs / 1000.0f * ROT_SPEED_DEG_PER_SEC) % 360.0f;
    }

    private static void renderWrappedTitle(GuiGraphics g, Font font, Component title) {
        int x = TEXT_X;
        int y = TEXT_Y;
        int maxW = W - TEXT_X - 6;
        String s = title == null ? "" : title.getString();
        if (s.isBlank()) return;

        if (font.width(s) <= maxW) {
            g.drawString(font, s, x, y + 5, 0xFFFFFF, false);
            return;
        }

        List<String> lines = wrapToTwoLines(font, s, maxW);
        if (lines.isEmpty()) return;

        if (lines.size() == 1) {
            g.drawString(font, lines.getFirst(), x, y + 5, 0xFFFFFF, false);
            return;
        }

        g.drawString(font, lines.getFirst(), x, y, 0xFFFFFF, false);
        g.drawString(font, lines.getLast(), x, y + TEXT_LINE_GAP, 0xFFFFFF, false);
    }

    private static List<String> wrapToTwoLines(Font font, String s, int maxW) {
        int cut = lastFitIndex(font, s, maxW);
        if (cut <= 0) {
            return List.of(clipToWidth(font, s, maxW));
        }

        int split = cut;
        for (int i = cut; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ' ') {
                split = i;
                break;
            }
        }

        String first = s.substring(0, split).trim();
        String rest = s.substring(split).trim();

        if (first.isEmpty()) {
            first = clipToWidth(font, s, maxW);
            rest = "";
        }

        if (rest.isEmpty()) {
            return List.of(first);
        }

        String second = clipToWidth(font, rest, maxW);
        return List.of(first, second);
    }

    private static int lastFitIndex(Font font, String s, int maxW) {
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String sub = s.substring(0, mid);
            if (font.width(sub) <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private static String clipToWidth(Font font, String s, int maxW) {
        if (s == null || s.isEmpty()) return "";
        if (font.width(s) <= maxW) return s;

        String ell = "...";
        int ellW = font.width(ell);
        if (ellW >= maxW) return "";

        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String sub = s.substring(0, mid);
            if (font.width(sub) + ellW <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ell;
    }

    private static void drawFrame(GuiGraphics g) {
        g.fill(0, 0, W, H, FRAME_BG);

        g.fill(0, 0, W, 1, FRAME_OUTER);
        g.fill(0, H - 1, W, H, FRAME_OUTER);
        g.fill(0, 0, 1, H, FRAME_OUTER);
        g.fill(W - 1, 0, W, H, FRAME_OUTER);

        g.fill(1, 1, W - 1, 2, FRAME_INNER);
        g.fill(1, H - 2, W - 1, H - 1, FRAME_INNER);
        g.fill(1, 1, 2, H - 1, FRAME_INNER);
        g.fill(W - 2, 1, W - 1, H - 1, FRAME_INNER);
    }

    private static void renderEntityOnToast(GuiGraphics g, Entity entity, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        boolean dragon = entity.getType() == EntityType.ENDER_DRAGON;

        float bbH = Math.max(0.01f, entity.getBbHeight());
        float bbW = Math.max(0.01f, entity.getBbWidth());
        float max = dragon ? DRAGON_TOAST_EFFECTIVE_DIM : Math.max(bbH, bbW);

        float scale = 18.0f / max;
        scale = Math.min(scale, 22.0f);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        float prevYRot = entity.getYRot();
        float prevXRot = entity.getXRot();

        float prevHead = 0.0f;
        float prevBody = 0.0f;

        if (entity instanceof LivingEntity le) {
            prevHead = le.getYHeadRot();
            prevBody = le.yBodyRot;
            le.setYHeadRot(yaw);
            le.yBodyRot = yaw;
        }

        entity.setYRot(yaw);
        entity.setXRot(DISPLAY_PITCH);

        g.pose().pushPose();
        int iconBottomY = dragon ? ICON_BOTTOM_Y - 6 : ICON_BOTTOM_Y;
        g.pose().translate(ICON_PAD_L + ICON_CENTER_X, iconBottomY, 50.0f);
        g.pose().scale(scale, scale, scale);
        g.pose().mulPose(Axis.ZP.rotationDegrees(180.0f));
        g.pose().mulPose(Axis.YP.rotationDegrees(180.0f));
        if (dragon) {
            g.pose().mulPose(Axis.YP.rotationDegrees(yaw));
            g.pose().mulPose(Axis.XP.rotationDegrees(DRAGON_TOAST_PITCH_DEG));
        }

        RenderSystem.enableDepthTest();

        dispatcher.setRenderShadow(false);
        var buf = g.bufferSource();
        double yRenderOffset = dragon ? entity.getBbHeight() * DRAGON_TOAST_Y_RENDER_OFFSET_FACTOR : 0.0;
        dispatcher.render(entity, 0.0, yRenderOffset, 0.0, 0.0f, 1.0f, g.pose(), buf, LightTexture.FULL_BRIGHT);
        buf.endBatch();
        dispatcher.setRenderShadow(true);

        RenderSystem.disableDepthTest();
        g.pose().popPose();

        entity.setYRot(prevYRot);
        entity.setXRot(prevXRot);

        if (entity instanceof LivingEntity le) {
            le.setYHeadRot(prevHead);
            le.yBodyRot = prevBody;
        }
    }
}
