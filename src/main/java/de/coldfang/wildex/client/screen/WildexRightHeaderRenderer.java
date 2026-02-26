package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.model.WildexAggression;
import de.coldfang.wildex.client.data.model.WildexHeaderData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class WildexRightHeaderRenderer {

    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;

    private static final int COMPACT_THRESHOLD_H = 72;
    private static final float MIN_SCALE = 0.62f;

    private static final int MARQUEE_GAP_PX = 18;
    private static final float MARQUEE_SPEED_PX_PER_SEC = 15.0f;
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

        if (WildexClientConfigView.hiddenMode() && !WildexDiscoveryCache.isDiscovered(mobRl)) {
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
        boolean vintage = WildexThemes.isVintageLayout();

        int padX = PAD_X;
        int padY = compact ? (vintage ? 5 : 4) : PAD_Y;
        float nameScale = 1.0f;
        int nameLineGap = compact ? 5 : 7;
        int dividerGapTop = vintage ? 3 : 2;
        int dividerGapBottom = vintage ? 2 : 1;
        int lowerLineGap = compact ? (vintage ? 4 : 2) : 3;
        int nameToLowerGap = compact ? (vintage ? 6 : 4) : 6;

        WildexScissor.enablePhysical(g, x0, y0, x1, y1);
        try {
            g.pose().pushPose();
            g.pose().translate(x0, y0, 0);
            g.pose().scale(s, s, 1.0f);

            int y = padY;

            int right = lw - padX;
            int maxW = Math.max(1, right - padX);

            int phase = mobRl.toString().hashCode();

            Component emphasizedName = header.name() == null
                    ? Component.empty()
                    : header.name().copy();
            boolean modern = WildexThemes.isModernLayout();
            int nameColor = modern ? 0xFF000000 : 0xFFFFFFFF;
            int detailColor = modern ? 0xFFE6EEF7 : inkColor;
            int nameScaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * nameScale));
            int bandTop = 0;
            int bandBottom = y + nameScaledLineH + 2;
            if (bandBottom > bandTop) {
                int bx0 = 0;
                int bx1 = Math.max(1, lw);
                int by1 = Math.min(lh, bandBottom);
                if (by1 > bandTop) {
                    int cornerCut = WildexThemes.isVintageLayout() ? 3 : 0;
                    int bandColor = modern ? 0xFF29E8F1 : WildexUiTheme.current().selectionBg();
                    fillTopRoundedBand(g, bx0, bx1, bandTop, by1, bandColor, cornerCut);
                }
            }
            y = drawStyledValueMarquee(g, font, x0, y0, s, padX, y, maxW, emphasizedName, nameColor, nameLineGap, phase, nameScale);
            y += nameToLowerGap;

            y = drawValueOnlyLinePlain(g, font, padX, y, maxW, formatAggression(header.aggression()), detailColor, lowerLineGap);
            y = drawDivider(g, padX, right, y, lh, dividerGapTop, dividerGapBottom);

            int kills = WildexKillCache.getOrRequest(mobRl);
            y = drawKillsLine(g, font, padX, y, maxW, Component.literal(Integer.toString(kills)), detailColor, lowerLineGap);
            y = drawDivider(g, padX, right, y, lh, dividerGapTop, dividerGapBottom);

            String modName = resolveModName(mobRl);
            drawModLineMarqueeValue(g, font, x0, y0, s, padX, y, maxW, Component.literal(modName), detailColor, phase ^ 0x5A5A);

            g.pose().popPose();
        } finally {
            g.disableScissor();
        }
    }

    private static float computeContentScale(int h) {
        if (h <= 0) return MIN_SCALE;
        if (h >= COMPACT_THRESHOLD_H) return 1.0f;
        float s = (float) h / (float) COMPACT_THRESHOLD_H;
        return Math.max(s, MIN_SCALE);
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

    private static int drawLinePlainWithLabel(GuiGraphics g, Font font, int x, int y, int maxW, String leftLabel, Component value, int inkColor, int lineGap) {
        String left = leftLabel + " ";
        int leftW = WildexUiText.width(font, left);

        String val = value == null ? "" : value.getString();
        int valMaxW = Math.max(1, maxW - leftW);

        WildexUiText.draw(g, font, left, x, y, inkColor, false);
        WildexUiText.draw(g, font, clipToWidth(font, val, valMaxW), x + leftW, y, inkColor, false);

        return y + Math.max(10, WildexUiText.lineHeight(font) + lineGap);
    }

    private static int drawValueOnlyLinePlain(GuiGraphics g, Font font, int x, int y, int maxW, Component value, int inkColor, int lineGap) {
        String val = value == null ? "" : value.getString();
        WildexUiText.draw(g, font, clipToWidth(font, val, maxW), x, y, inkColor, false);
        return y + Math.max(10, WildexUiText.lineHeight(font) + lineGap);
    }

    private static int drawStyledValueMarquee(
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
            int phase,
            float textScale
    ) {
        float scaleFactor = Math.max(1.0f, textScale);
        int scaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * scaleFactor));
        int lineStep = Math.max(10, scaledLineH + lineGap);
        Component styled = value == null ? Component.empty() : value;
        int maxWLogical = Math.max(1, (int) Math.floor(maxW / scaleFactor));
        int w = WildexUiText.width(font, styled);

        if (w <= maxWLogical) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0f);
            g.pose().scale(scaleFactor, scaleFactor, 1.0f);
            WildexUiText.draw(g, font, styled, 0, 0, inkColor, false);
            g.pose().popPose();
            return y + lineStep;
        }

        int clipX0 = toScreenX(baseX, scale, x);
        int clipX1 = toScreenX(baseX, scale, x + maxW);
        int clipY0 = toScreenY(baseY, scale, y);
        int clipY1 = toScreenY(baseY, scale, y + scaledLineH + 1);

        WildexScissor.enablePhysical(g, clipX0, clipY0, clipX1, clipY1);
        try {
            int travel = (w - maxWLogical) + MARQUEE_GAP_PX;
            float off = marqueeOffset(System.currentTimeMillis(), travel, phase);
            int dx = Math.round(off);
            g.pose().pushPose();
            g.pose().translate(x - dx, y, 0.0f);
            g.pose().scale(scaleFactor, scaleFactor, 1.0f);
            WildexUiText.draw(g, font, styled, 0, 0, inkColor, false);
            g.pose().popPose();
        } finally {
            g.disableScissor();
        }

        return y + lineStep;
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
            Component value,
            int inkColor,
            int phase
    ) {
        String left = tr("gui.wildex.header.mod") + " ";
        int leftW = WildexUiText.width(font, left);

        String val = value == null ? "" : value.getString();
        int valW = WildexUiText.width(font, val);
        int valMaxW = Math.max(1, maxW - leftW);

        WildexUiText.draw(g, font, left, x, y, inkColor, false);

        int valX = x + leftW;

        if (valW <= valMaxW) {
            WildexUiText.draw(g, font, val, valX, y, inkColor, false);
            return;
        }

        int clipX0 = toScreenX(baseX, scale, valX);
        int clipX1 = toScreenX(baseX, scale, valX + valMaxW);
        int clipY0 = toScreenY(baseY, scale, y);
        int clipY1 = toScreenY(baseY, scale, y + WildexUiText.lineHeight(font) + 1);

        WildexScissor.enablePhysical(g, clipX0, clipY0, clipX1, clipY1);
        try {
            int travel = (valW - valMaxW) + MARQUEE_GAP_PX;
            float off = marqueeOffset(System.currentTimeMillis(), travel, phase);
            int dx = Math.round(off);
            WildexUiText.draw(g, font, val, valX - dx, y, inkColor, false);
        } finally {
            g.disableScissor();
        }

    }

    private static int drawKillsLine(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int maxW,
            Component value,
            int inkColor,
            int lineGap
    ) {
        return drawLinePlainWithLabel(g, font, x, y, maxW, tr("gui.wildex.header.you_killed"), value, inkColor, lineGap);
    }

    private static void drawModLineMarqueeValue(
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
            int phase
    ) {
        drawLineMarqueeValue(g, font, baseX, baseY, scale, x, y, maxW, value, inkColor, phase);
    }

    private static int drawDivider(GuiGraphics g, int x, int right, int yAfterLine, int yMax, int gapTop, int gapBottom) {
        int y = yAfterLine - gapTop;
        if (y + 1 >= yMax) return yAfterLine;
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int dividerColor = WildexThemes.isVintageLayout()
                ? theme.rowSeparator()
                : theme.frameInner();
        g.fill(x, y, right, y + 1, dividerColor);
        return yAfterLine + gapBottom;
    }

    private static void fillTopRoundedBand(
            GuiGraphics g,
            int x0,
            int x1,
            int y0,
            int y1,
            int color,
            int cornerCut
    ) {
        int cut = Math.max(0, cornerCut);
        for (int y = y0; y < y1; y++) {
            int relY = y - y0;
            int inset = (relY < cut) ? (cut - relY) : 0;
            int left = x0 + inset;
            int right = x1 - inset;
            if (right > left) {
                g.fill(left, y, right, y + 1, color);
            }
        }
    }

    private static String resolveModName(ResourceLocation mobRl) {
        if (mobRl == null) return tr("gui.wildex.header.unknown_mod");
        String namespace = mobRl.getNamespace();
        if (namespace.isBlank()) return tr("gui.wildex.header.unknown_mod");
        if ("minecraft".equals(namespace)) return tr("gui.wildex.header.minecraft");

        return ModList.get()
                .getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .filter(s -> !s.isBlank())
                .orElse(namespace);
    }

    private static Component formatAggression(WildexAggression a) {
        if (a == null) return Component.translatable("gui.wildex.aggression.friendly");
        return switch (a) {
            case FRIENDLY -> Component.translatable("gui.wildex.aggression.friendly");
            case NEUTRAL -> Component.translatable("gui.wildex.aggression.neutral");
            case HOSTILE -> Component.translatable("gui.wildex.aggression.hostile");
        };
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String clipToWidth(Font font, String s, int maxW) {
        if (s == null || s.isEmpty()) return "";
        if (WildexUiText.width(font, s) <= maxW) return s;

        String ell = "...";
        int ellW = WildexUiText.width(font, ell);
        if (ellW >= maxW) return "";

        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String sub = s.substring(0, mid);
            if (WildexUiText.width(font, sub) + ellW <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ell;
    }
}




