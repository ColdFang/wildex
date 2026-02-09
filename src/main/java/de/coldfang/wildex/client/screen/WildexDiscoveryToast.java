package de.coldfang.wildex.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class WildexDiscoveryToast implements Toast {

    private static final long DURATION_MS = 5000L;

    private static final int W = 160;
    private static final int H = 32;

    private static final int FRAME_BG = 0xCC151515;
    private static final int FRAME_OUTER = 0xFF2B1A10;
    private static final int FRAME_INNER = 0x66FFFFFF;

    private static final int TEXT_X = 42;
    private static final int TEXT_Y = 7;
    private static final int TEXT_LINE_GAP = 10;

    private static final int MODEL_PAD = 3;
    private static final int MODEL_SIZE_MIN = 6;

    private final ResourceLocation mobId;
    private final Component title;

    private Visibility wantedVisibility = Visibility.SHOW;
    private LivingEntity cachedEntity;

    public WildexDiscoveryToast(ResourceLocation mobId, Component title) {
        this.mobId = mobId;
        this.title = title;
    }

    @Override
    public Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long timeSinceLastVisible) {
        wantedVisibility = timeSinceLastVisible >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;

        if (wantedVisibility == Visibility.HIDE) {
            clearCachedEntity();
            return;
        }

        if (cachedEntity != null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
        if (type == null) return;

        Entity e = type.create(mc.level, EntitySpawnReason.COMMAND);
        if (e instanceof LivingEntity le) {
            cachedEntity = le;
        } else if (e != null) {
            e.discard();
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, long timeSinceLastVisible) {
        drawFrame(g);
        renderMobModel(g);
        renderWrappedTitle(g, font, title, TEXT_X, TEXT_Y, W - TEXT_X - 6);
    }

    private void renderMobModel(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity le = cachedEntity;
        if (le == null) return;

        int boxX0 = MODEL_PAD;
        int boxX1 = TEXT_X - MODEL_PAD;
        int boxY0 = MODEL_PAD;
        int boxY1 = H - MODEL_PAD;

        int boxW = boxX1 - boxX0;
        int boxH = boxY1 - boxY0;
        if (boxW <= 0 || boxH <= 0) return;

        int cx = boxX0 + boxW / 2;
        int cy = boxY0 + boxH / 2 + 6;

        float bw = Math.max(0.1f, le.getBbWidth());
        float bh = Math.max(0.1f, le.getBbHeight());
        float dim = Math.max(bw, bh);

        float fit = (Math.min(boxW, boxH) - 2) * 0.85f;
        int size = Math.max(MODEL_SIZE_MIN, (int) (fit / dim));

        float yaw = (float) ((Util.getMillis() * 0.08) % 360.0);

        Quaternionf bodyRot = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(180.0f + yaw));

        Matrix4f pose = g.pose().last().pose();
        int ox = Math.round(pose.m30());
        int oy = Math.round(pose.m31());

        int sx0 = ox + boxX0;
        int sy0 = oy + boxY0;
        int sx1 = ox + boxX1;
        int sy1 = oy + boxY1;

        g.enableScissor(sx0, sy0, sx1, sy1);
        try {
            InventoryScreen.renderEntityInInventory(
                    g,
                    cx,
                    cy,
                    size,
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    bodyRot,
                    new Quaternionf(),
                    le
            );
        } finally {
            g.disableScissor();
        }
    }

    private void clearCachedEntity() {
        if (cachedEntity != null) cachedEntity.discard();
        cachedEntity = null;
    }

    private static void renderWrappedTitle(GuiGraphics g, Font font, Component title, int x, int y, int maxW) {
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
        if (cut <= 0) return List.of(clipToWidth(font, s, maxW));

        int split = cut;
        for (int i = cut; i >= 0; i--) {
            if (s.charAt(i) == ' ') {
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

        if (rest.isEmpty()) return List.of(first);

        String second = clipToWidth(font, rest, maxW);
        return List.of(first, second);
    }

    private static int lastFitIndex(Font font, String s, int maxW) {
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (font.width(s.substring(0, mid)) <= maxW) lo = mid;
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
            if (font.width(s.substring(0, mid)) + ellW <= maxW) lo = mid;
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
}
