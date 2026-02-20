package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexSpawnCache;
import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

final class WildexRightInfoSpawnsRenderer {

    private static final int SPAWN_LINE_GAP = 2;
    private static final int SPAWN_GROUP_GAP = 4;
    private static final int SPAWN_FILTER_GAP = 2;
    private static final int SPAWN_FILTER_PAD_X = 5;
    private static final int SPAWN_FILTER_BORDER_ON = 0xCC3A2618;
    private static final int SPAWN_FILTER_BORDER_OFF = 0x55301E14;
    private static final int SPAWN_FILTER_ON_BG = 0xB83A2618;
    private static final int SPAWN_FILTER_OFF_BG = 0x18FFFFFF;
    private static final int SPAWN_FILTER_TEXT_ON = 0xFFF7E7CC;
    private static final int SPAWN_FILTER_TEXT_OFF = 0x7A2B1A10;
    private static final int SPAWN_FILTER_OFF_CROSS = 0xFFE74C3C;
    private static final int SPAWN_HEADING_COLOR = 0xCC2B1A10;
    private static final int SPAWN_HEADING_RULE = 0x66301E14;
    private static final int SPAWN_SUBHEADING_COLOR = 0xB8301E14;
    private static final int SPAWN_SUBHEADING_RULE = 0x33301E14;

    private static final int SCROLLBAR_W = 8;
    private static final int SCROLLBAR_PAD = 2;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;
    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;

    private static int spawnScrollPx = 0;
    private static int spawnViewportH = 1;
    private static int spawnContentH = 1;
    private static boolean spawnHasScrollbar = false;
    private static boolean spawnDraggingScrollbar = false;
    private static int spawnDragOffsetY = 0;
    private static int spawnBarScreenX0 = 0;
    private static int spawnBarScreenY0 = 0;
    private static int spawnBarScreenX1 = 0;
    private static int spawnBarScreenY1 = 0;
    private static int spawnThumbScreenH = 0;
    private static int spawnThumbScreenY0 = 0;
    private static int spawnThumbScreenY1 = 0;
    private static int spawnFilterBiomeX0 = 0;
    private static int spawnFilterBiomeY0 = 0;
    private static int spawnFilterBiomeX1 = 0;
    private static int spawnFilterBiomeY1 = 0;
    private static int spawnFilterStructureX0 = 0;
    private static int spawnFilterStructureY0 = 0;
    private static int spawnFilterStructureX1 = 0;
    private static int spawnFilterStructureY1 = 0;

    void resetScroll() {
        spawnScrollPx = 0;
        spawnViewportH = 1;
        spawnContentH = 1;
        spawnHasScrollbar = false;
        spawnDraggingScrollbar = false;
    }

    void scroll(double scrollY) {
        int max = Math.max(0, spawnContentH - spawnViewportH);
        int step = 10;
        int next = spawnScrollPx - (int) Math.round(scrollY * (double) step);
        if (next < 0) next = 0;
        if (next > max) next = max;
        spawnScrollPx = next;
    }

    boolean handleMouseClicked(int mouseX, int mouseY, int button, WildexScreenState state) {
        if (button != 0 || state == null) return false;

        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnFilterBiomeX0, spawnFilterBiomeY0, spawnFilterBiomeX1, spawnFilterBiomeY1)) {
            state.toggleSpawnFilterNatural();
            return true;
        }
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnFilterStructureX0, spawnFilterStructureY0, spawnFilterStructureX1, spawnFilterStructureY1)) {
            state.toggleSpawnFilterStructures();
            return true;
        }

        if (!spawnHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnBarScreenX0, spawnBarScreenY0, spawnBarScreenX1, spawnBarScreenY1)) return false;

        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnBarScreenX0, spawnThumbScreenY0, spawnBarScreenX1, spawnThumbScreenY1)) {
            spawnDraggingScrollbar = true;
            spawnDragOffsetY = mouseY - spawnThumbScreenY0;
            return true;
        }

        setScrollFromThumbTop(mouseY - (spawnThumbScreenH / 2));
        return true;
    }

    boolean handleMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0 || !spawnDraggingScrollbar) return false;
        setScrollFromThumbTop(mouseY - spawnDragOffsetY);
        return true;
    }

    boolean handleMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = spawnDraggingScrollbar;
        spawnDraggingScrollbar = false;
        return wasDragging;
    }

    WildexRightInfoRenderer.TooltipRequest render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            ResourceLocation mobId,
            WildexScreenState state,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale,
            int mouseX,
            int mouseY
    ) {
        WildexRightInfoRenderer.TooltipRequest hoverTip = null;
        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int yTop = area.y() + 2;

        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;

        int filterH = Math.max(10, WildexUiText.lineHeight(font) + 4);
        int filterY = yTop;
        int filterTextY = filterY + Math.max(0, (filterH - WildexUiText.lineHeight(font)) / 2) + 1;

        String biomeLabel = WildexRightInfoTabUtil.tr("gui.wildex.spawn.filter.biomes.short");
        String structureLabel = WildexRightInfoTabUtil.tr("gui.wildex.spawn.filter.structures.short");

        int biomeW = WildexUiText.width(font, biomeLabel) + (SPAWN_FILTER_PAD_X * 2);
        int structureW = WildexUiText.width(font, structureLabel) + (SPAWN_FILTER_PAD_X * 2);

        int filterX = area.x() + 2;
        int biomeX0 = filterX;
        int biomeX1 = biomeX0 + biomeW;
        int structureX0 = biomeX1 + SPAWN_FILTER_GAP;
        int structureX1 = structureX0 + structureW;

        boolean naturalEnabled = state != null && state.spawnFilterNatural();
        boolean structuresEnabled = state != null && state.spawnFilterStructures();

        drawSpawnFilterChip(g, font, biomeX0, biomeX1, filterY, filterH, filterTextY, biomeLabel, naturalEnabled);
        drawSpawnFilterChip(g, font, structureX0, structureX1, filterY, filterH, filterTextY, structureLabel, structuresEnabled);

        cacheSpawnFilterRects(screenOriginX, screenOriginY, scale, biomeX0, biomeX1, filterY, filterY + filterH, structureX0, structureX1);

        if (mouseX >= 0 && mouseY >= 0) {
            if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnFilterBiomeX0, spawnFilterBiomeY0, spawnFilterBiomeX1, spawnFilterBiomeY1)) {
                hoverTip = tooltipLines("tooltip.wildex.spawn.filter.biomes");
            } else if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, spawnFilterStructureX0, spawnFilterStructureY0, spawnFilterStructureX1, spawnFilterStructureY1)) {
                hoverTip = tooltipLines("tooltip.wildex.spawn.filter.structures");
            }
        }

        yTop += filterH + SPAWN_FILTER_GAP + 2;

        WildexSpawnCache.SpawnData data = WildexSpawnCache.get(mobId);
        List<S2CMobSpawnsPayload.DimSection> naturalSections = data.naturalSections();
        List<S2CMobSpawnsPayload.StructureSection> structureSections = data.structureSections();

        if (!naturalEnabled) naturalSections = List.of();
        if (!structuresEnabled) structureSections = List.of();

        if (naturalSections.isEmpty() && structureSections.isEmpty()) {
            WildexUiText.draw(g, font, WildexRightInfoTabUtil.tr("gui.wildex.spawn.none"), x, yTop, inkColor, false);
            spawnHasScrollbar = false;
            spawnDraggingScrollbar = false;
            return hoverTip;
        }

        int lineH = Math.max(10, WildexUiText.lineHeight(font) + SPAWN_LINE_GAP);
        int titleH = Math.max(10, WildexUiText.lineHeight(font) + 3);

        int contentH = 0;
        if (!naturalSections.isEmpty()) {
            contentH += titleH;
            for (S2CMobSpawnsPayload.DimSection s : naturalSections) {
                contentH += titleH;
                contentH += (s.biomeIds() == null ? 0 : s.biomeIds().size()) * lineH;
            }
        }
        if (!naturalSections.isEmpty() && !structureSections.isEmpty()) {
            contentH += SPAWN_GROUP_GAP;
        }
        if (!structureSections.isEmpty()) {
            contentH += titleH;
            for (S2CMobSpawnsPayload.StructureSection ignored : structureSections) {
                contentH += titleH;
            }
        }

        int viewportH = Math.max(1, (maxY - yTop));
        spawnViewportH = viewportH;
        spawnContentH = Math.max(1, contentH);
        int maxScroll = Math.max(0, contentH - viewportH);
        boolean showSpawnScrollbar = maxScroll > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (!showSpawnScrollbar) {
            spawnScrollPx = 0;
        } else if (spawnScrollPx > maxScroll) {
            spawnScrollPx = maxScroll;
        }

        int y = yTop - spawnScrollPx;
        int textMaxW = Math.max(1, (rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD) - x);

        int contentSx0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x());
        int contentSy0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop);
        int contentSx1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x() + area.w());
        int contentSy1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, maxY);

        WildexScissor.enablePhysical(g, contentSx0, contentSy0, contentSx1, contentSy1);
        try {
            if (!naturalSections.isEmpty()) {
                if (y + WildexUiText.lineHeight(font) > yTop - lineH && y < maxY) {
                    drawSpawnHeading(g, font, x, y, rightLimitX, WildexRightInfoTabUtil.tr("gui.wildex.spawn.heading.natural"));
                }
                y += titleH;

                for (S2CMobSpawnsPayload.DimSection s : naturalSections) {
                    if (y + WildexUiText.lineHeight(font) > yTop - lineH && y < maxY) {
                        String title = formatDimensionTitle(s.dimensionId());
                        drawSpawnSubheading(g, font, title, x, y, rightLimitX, screenOriginX, screenOriginY, scale);
                    }
                    y += titleH;

                    List<ResourceLocation> biomes = s.biomeIds() == null ? List.of() : s.biomeIds();
                    for (ResourceLocation b : biomes) {
                        if (y + WildexUiText.lineHeight(font) >= yTop - lineH && y < maxY) {
                            WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, b.toString(), x, y, textMaxW, inkColor, screenOriginX, screenOriginY, scale);
                        }
                        y += lineH;
                    }
                }
            }

            if (!structureSections.isEmpty()) {
                if (!naturalSections.isEmpty()) y += SPAWN_GROUP_GAP;

                if (y + WildexUiText.lineHeight(font) > yTop - lineH && y < maxY) {
                    drawSpawnHeading(g, font, x, y, rightLimitX, WildexRightInfoTabUtil.tr("gui.wildex.spawn.heading.structures"));
                }
                y += titleH;

                for (S2CMobSpawnsPayload.StructureSection s : structureSections) {
                    if (y + WildexUiText.lineHeight(font) > yTop - lineH && y < maxY) {
                        WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                                g, font, formatStructureTitle(s.structureId()), x, y, textMaxW, inkColor, screenOriginX, screenOriginY, scale
                        );
                    }
                    y += titleH;
                }
            }

            if (showSpawnScrollbar) {
                int barX0 = rightLimitX - SCROLLBAR_W;
                int barY1 = yTop + viewportH;
                g.fill(barX0, yTop, rightLimitX, barY1, SCROLLBAR_BG);

                float ratio = viewportH / (float) contentH;
                int thumbH = Math.max(8, (int) Math.floor(viewportH * ratio));
                if (thumbH > viewportH) thumbH = viewportH;

                int denom = Math.max(1, contentH - viewportH);
                float t = spawnScrollPx / (float) denom;
                int travel = viewportH - thumbH;
                int thumbY0 = yTop + Math.round(travel * t);
                int thumbY1 = thumbY0 + thumbH;
                g.fill(barX0, thumbY0, rightLimitX, thumbY1, SCROLLBAR_THUMB);

                spawnHasScrollbar = true;
                cacheSpawnScrollbarRect(screenOriginX, screenOriginY, scale, barX0, yTop, rightLimitX, barY1, thumbY0, thumbY1);
            } else {
                spawnHasScrollbar = false;
                spawnDraggingScrollbar = false;
            }
        } finally {
            g.disableScissor();
        }

        return hoverTip;
    }

    private static void drawSpawnFilterChip(
            GuiGraphics g,
            Font font,
            int x0,
            int x1,
            int y0,
            int h,
            int textY,
            String label,
            boolean active
    ) {
        boolean modern = WildexThemes.isModernLayout();
        int onBg = SPAWN_FILTER_ON_BG;
        int onFg = SPAWN_FILTER_TEXT_ON;
        int onBorder = SPAWN_FILTER_BORDER_ON;
        int offBg = modern ? 0x3342C7F5 : SPAWN_FILTER_OFF_BG;
        int offFg = modern ? 0xFFEAF7FF : SPAWN_FILTER_TEXT_OFF;
        int offBorder = modern ? 0xCC4FCBF3 : SPAWN_FILTER_BORDER_OFF;
        int bg = active ? offBg : onBg;
        int fg = active ? offFg : onFg;
        int border = active ? offBorder : onBorder;
        int offCross = modern ? 0xFFFF6A7A : SPAWN_FILTER_OFF_CROSS;
        int y1 = y0 + h;

        g.fill(x0, y0, x1, y1, bg);
        g.fill(x0, y0, x1, y0 + 1, border);
        g.fill(x0, y1 - 1, x1, y1, border);
        g.fill(x0, y0, x0 + 1, y1, border);
        g.fill(x1 - 1, y0, x1, y1, border);

        WildexUiText.draw(g, font, label, x0 + SPAWN_FILTER_PAD_X, textY, fg, false);

        if (!active) {
            int size = 5;
            int ix1 = x1 - 3;
            int ix0 = ix1 - (size - 1);
            int iy0 = y0 + 2;
            for (int i = 0; i < size; i++) {
                g.fill(ix0 + i, iy0 + i, ix0 + i + 1, iy0 + i + 1, offCross);
                g.fill(ix1 - i, iy0 + i, ix1 - i + 1, iy0 + i + 1, offCross);
            }
        }
    }

    private static void drawSpawnHeading(GuiGraphics g, Font font, int x, int y, int rightLimitX, String text) {
        if (text == null || text.isBlank()) return;
        boolean modern = WildexThemes.isModernLayout();
        int headingColor = modern ? 0xFFEAF7FF : SPAWN_HEADING_COLOR;
        int ruleColor = modern ? 0x664FCBF3 : SPAWN_HEADING_RULE;
        int ruleX1 = rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD;
        WildexUiText.draw(g, font, text, x, y, headingColor, false);
        int ruleY = y + WildexUiText.lineHeight(font) + 1;
        if (ruleX1 > x + 8) g.fill(x, ruleY, ruleX1, ruleY + 1, ruleColor);
    }

    private static void drawSpawnSubheading(
            GuiGraphics g,
            Font font,
            String text,
            int x,
            int y,
            int rightLimitX,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        if (text == null || text.isBlank()) return;
        boolean modern = WildexThemes.isModernLayout();
        int subColor = modern ? 0xFFD5ECFA : SPAWN_SUBHEADING_COLOR;
        int ruleColor = modern ? 0x554FCBF3 : SPAWN_SUBHEADING_RULE;
        int maxW = Math.max(1, (rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD) - x);
        WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, text, x, y, maxW, subColor, screenOriginX, screenOriginY, scale);
        int ruleY = y + WildexUiText.lineHeight(font) + 1;
        int ruleX1 = rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD;
        if (ruleX1 > x + 8) g.fill(x, ruleY, ruleX1, ruleY + 1, ruleColor);
    }

    private static void cacheSpawnFilterRects(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int biomeLocalX0,
            int biomeLocalX1,
            int localY0,
            int localY1,
            int structLocalX0,
            int structLocalX1
    ) {
        spawnFilterBiomeX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, biomeLocalX0);
        spawnFilterBiomeX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, biomeLocalX1);
        spawnFilterBiomeY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, localY0);
        spawnFilterBiomeY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, localY1);

        spawnFilterStructureX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, structLocalX0);
        spawnFilterStructureX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, structLocalX1);
        spawnFilterStructureY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, localY0);
        spawnFilterStructureY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, localY1);
    }

    private static void cacheSpawnScrollbarRect(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int barLocalX0,
            int barLocalY0,
            int barLocalX1,
            int barLocalY1,
            int thumbLocalY0,
            int thumbLocalY1
    ) {
        spawnBarScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX0);
        spawnBarScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY0);
        spawnBarScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX1);
        spawnBarScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY1);

        spawnThumbScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY0);
        spawnThumbScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY1);
        spawnThumbScreenH = Math.max(1, spawnThumbScreenY1 - spawnThumbScreenY0);
    }

    private static void setScrollFromThumbTop(int desiredThumbTop) {
        if (!spawnHasScrollbar) return;

        int barTop = spawnBarScreenY0;
        int barBottom = spawnBarScreenY1;
        int travel = Math.max(1, (barBottom - barTop) - spawnThumbScreenH);
        int clamped = Math.max(barTop, Math.min(desiredThumbTop, barBottom - spawnThumbScreenH));

        float t = (clamped - barTop) / (float) travel;
        int maxScroll = Math.max(0, spawnContentH - spawnViewportH);
        spawnScrollPx = Math.round(maxScroll * t);
    }

    private static WildexRightInfoRenderer.TooltipRequest tooltipLines(String... keys) {
        ArrayList<Component> out = new ArrayList<>();
        for (String key : keys) {
            if (key != null && !key.isBlank()) out.add(Component.translatable(key));
        }
        return out.isEmpty() ? null : new WildexRightInfoRenderer.TooltipRequest(out);
    }

    private static String formatDimensionTitle(ResourceLocation dimId) {
        if (dimId == null) return WildexRightInfoTabUtil.tr("gui.wildex.spawn.dimension.unknown");
        String path = dimId.getPath();
        String nice = path.replace('_', ' ').replace('/', ' ').trim();
        if (nice.isEmpty()) nice = path;
        return dimId.getNamespace() + ": " + nice;
    }

    private static String formatStructureTitle(ResourceLocation structureId) {
        if (structureId == null) return WildexRightInfoTabUtil.tr("gui.wildex.spawn.structure.unknown");
        return structureId.toString();
    }
}




