package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.model.WildexAggression;
import de.coldfang.wildex.client.data.model.WildexHeaderData;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class WildexRightHeaderRenderer {

    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;

    private static final int DIVIDER = 0x22301E14;

    private static final int LINE_GAP = 6;
    private static final int DIVIDER_GAP_TOP = 2;
    private static final int DIVIDER_GAP_BOTTOM = 3;

    private static final int COMPACT_THRESHOLD_H = 72;
    private static final float MIN_SCALE = 0.62f;

    private static final int MARQUEE_GAP_PX = 18;
    private static final float MARQUEE_SPEED_PX_PER_SEC = 22.0f;
    private static final int MARQUEE_PAUSE_MS = 650;

    public void render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header,
            int inkColor
    ) {
        if (g == null || font == null || area == null || state == null) return;

        String selectedIdStr = state.selectedMobId();
        if (selectedIdStr == null || selectedIdStr.isBlank()) return;

        ResourceLocation mobRl = ResourceLocation.tryParse(selectedIdStr);
        if (mobRl == null) return;

        if (CommonConfig.INSTANCE.hiddenMode.get() && !WildexDiscoveryCache.isDiscovered(mobRl)) {
            return;
        }

        if (header == null) return;

        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        int lh = toLogical(area.h(), s);

        boolean compact = area.h() < COMPACT_THRESHOLD_H;

        int padX = PAD_X;
        int padY = compact ? 4 : PAD_Y;
        int lineGap = compact ? 4 : LINE_GAP;
        int dividerGapTop = compact ? 1 : DIVIDER_GAP_TOP;
        int dividerGapBottom = compact ? 2 : DIVIDER_GAP_BOTTOM;

        g.enableScissor(x0, y0, x1, y1);
        try {
            g.pose().pushPose();
            g.pose().translate(x0, y0, 0);
            g.pose().scale(s, s, 1.0f);

            int y = padY;

            int right = lw - padX;
            int maxW = Math.max(1, right - padX);

            int phase = mobRl.toString().hashCode();

            y = drawValueOnlyLineMarquee(g, font, x0, y0, s, padX, y, maxW, header.name(), inkColor, lineGap, phase);
            y = drawDivider(g, padX, right, y, lh, dividerGapTop, dividerGapBottom);

            y = drawValueOnlyLinePlain(g, font, padX, y, maxW, Component.literal(formatAggression(header.aggression())), inkColor, lineGap);
            y = drawDivider(g, padX, right, y, lh, dividerGapTop, dividerGapBottom);

            int kills = WildexKillCache.getOrRequest(mobRl);
            y = drawLinePlain(g, font, padX, y, maxW, "You killed:", Component.literal(Integer.toString(kills)), inkColor, lineGap);
            y = drawDivider(g, padX, right, y, lh, dividerGapTop, dividerGapBottom);

            String modName = resolveModName(mobRl);
            drawLineMarqueeValue(g, font, x0, y0, s, padX, y, maxW, "Mod:", Component.literal(modName), inkColor, lineGap, phase ^ 0x5A5A);

            g.pose().popPose();
        } finally {
            g.disableScissor();
        }
    }

    private static float computeContentScale(int h) {
        if (h <= 0) return MIN_SCALE;
        if (h >= COMPACT_THRESHOLD_H) return 1.0f;
        float s = (float) h / (float) COMPACT_THRESHOLD_H;
        if (s < MIN_SCALE) return MIN_SCALE;
        return s;
    }

    private static int toLogical(int px, float s) {
        if (s <= 0.0f) return px;
        return Math.max(1, (int) Math.floor(px / (double) s));
    }

    private static int toScreenX(int baseX, float scale, int logicalX) {
        return baseX + Math.round(logicalX * scale);
    }

    private static int toScreenY(int baseY, float scale, int logicalY) {
        return baseY + Math.round(logicalY * scale);
    }

    private static float marqueeOffset(long nowMs, int travelPx, int phasePx) {
        if (travelPx <= 0) return 0.0f;

        long cycleMs = (long) Math.ceil((travelPx / MARQUEE_SPEED_PX_PER_SEC) * 1000.0);
        long totalMs = (long) MARQUEE_PAUSE_MS + cycleMs + (long) MARQUEE_PAUSE_MS;

        long t = nowMs + (long) phasePx * 17L;
        long p = t % totalMs;
        if (p < 0) p += totalMs;

        if (p < MARQUEE_PAUSE_MS) return 0.0f;

        long moving = p - MARQUEE_PAUSE_MS;
        if (moving >= cycleMs) return (float) travelPx;

        float seconds = moving / 1000.0f;
        float off = seconds * MARQUEE_SPEED_PX_PER_SEC;
        if (off < 0) return 0.0f;
        if (off > travelPx) return travelPx;
        return off;
    }

    private static int drawLinePlain(GuiGraphics g, Font font, int x, int y, int maxW, String label, Component value, int inkColor, int lineGap) {
        String left = label + " ";
        int leftW = font.width(left);

        String val = value == null ? "" : value.getString();
        int valMaxW = Math.max(1, maxW - leftW);

        g.drawString(font, left, x, y, inkColor, false);
        g.drawString(font, clipToWidth(font, val, valMaxW), x + leftW, y, inkColor, false);

        return y + Math.max(10, font.lineHeight + lineGap);
    }

    private static int drawValueOnlyLinePlain(GuiGraphics g, Font font, int x, int y, int maxW, Component value, int inkColor, int lineGap) {
        String val = value == null ? "" : value.getString();
        g.drawString(font, clipToWidth(font, val, maxW), x, y, inkColor, false);
        return y + Math.max(10, font.lineHeight + lineGap);
    }

    private static int drawValueOnlyLineMarquee(
            GuiGraphics g,
            Font font,
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int maxW,
            Component value,
            int inkColor,
            int lineGap,
            int phase
    ) {
        String val = value == null ? "" : value.getString();
        int w = font.width(val);

        if (w <= maxW) {
            g.drawString(font, val, x, y, inkColor, false);
            return y + Math.max(10, font.lineHeight + lineGap);
        }

        int clipX0 = toScreenX(baseX, scale, x);
        int clipX1 = toScreenX(baseX, scale, x + maxW);
        int clipY0 = toScreenY(baseY, scale, y);
        int clipY1 = toScreenY(baseY, scale, y + font.lineHeight + 1);

        g.enableScissor(clipX0, clipY0, clipX1, clipY1);
        try {
            int travel = (w - maxW) + MARQUEE_GAP_PX;
            float off = marqueeOffset(System.currentTimeMillis(), travel, phase);
            int dx = Math.round(off);
            g.drawString(font, val, x - dx, y, inkColor, false);
        } finally {
            g.disableScissor();
        }

        return y + Math.max(10, font.lineHeight + lineGap);
    }

    private static void drawLineMarqueeValue(
            GuiGraphics g,
            Font font,
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int maxW,
            String label,
            Component value,
            int inkColor,
            int lineGap,
            int phase
    ) {
        String left = label + " ";
        int leftW = font.width(left);

        String val = value == null ? "" : value.getString();
        int valW = font.width(val);
        int valMaxW = Math.max(1, maxW - leftW);

        g.drawString(font, left, x, y, inkColor, false);

        int valX = x + leftW;

        if (valW <= valMaxW) {
            g.drawString(font, val, valX, y, inkColor, false);
            return;
        }

        int clipX0 = toScreenX(baseX, scale, valX);
        int clipX1 = toScreenX(baseX, scale, valX + valMaxW);
        int clipY0 = toScreenY(baseY, scale, y);
        int clipY1 = toScreenY(baseY, scale, y + font.lineHeight + 1);

        g.enableScissor(clipX0, clipY0, clipX1, clipY1);
        try {
            int travel = (valW - valMaxW) + MARQUEE_GAP_PX;
            float off = marqueeOffset(System.currentTimeMillis(), travel, phase);
            int dx = Math.round(off);
            g.drawString(font, val, valX - dx, y, inkColor, false);
        } finally {
            g.disableScissor();
        }
    }

    private static int drawDivider(GuiGraphics g, int x, int right, int yAfterLine, int yMax, int gapTop, int gapBottom) {
        int y = yAfterLine - gapTop;
        if (y + 1 >= yMax) return yAfterLine;
        g.fill(x, y, right, y + 1, DIVIDER);
        return yAfterLine + gapBottom;
    }

    private static String resolveModName(ResourceLocation mobRl) {
        if (mobRl == null) return "Unknown";
        String namespace = mobRl.getNamespace();
        if (namespace.isBlank()) return "Unknown";
        if ("minecraft".equals(namespace)) return "Minecraft";

        return ModList.get()
                .getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .filter(s -> !s.isBlank())
                .orElse(namespace);
    }

    private static String formatAggression(WildexAggression a) {
        if (a == null) return "Friendly";
        return switch (a) {
            case FRIENDLY -> "Friendly";
            case NEUTRAL -> "Neutral";
            case HOSTILE -> "Hostile";
        };
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
}
