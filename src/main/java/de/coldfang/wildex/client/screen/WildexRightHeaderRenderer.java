package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexKillCache;
import de.coldfang.wildex.client.data.model.WildexDiscoveryDetails;
import de.coldfang.wildex.client.data.model.WildexAggression;
import de.coldfang.wildex.client.data.model.WildexHeaderData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.List;

public final class WildexRightHeaderRenderer {

    private static final ResourceLocation TYPE_ICON =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/type.png");
    private static final ResourceLocation KILLS_ICON =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/kills.png");
    private static final ResourceLocation MOD_ICON =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/mod.png");
    private static final ResourceLocation DISCOVER_ICON =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/discover.png");
    private static final ResourceLocation FAVORITE_ICON =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/fav.png");
    private static final ResourceLocation FAVORITE_ICON_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/fav_modern.png");
    private static final ResourceLocation FAVORITE_ICON_SELECTED =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/fav_selected.png");
    private static final ResourceLocation FAVORITE_ICON_SELECTED_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/fav_selected_modern.png");
    private static final ResourceLocation TYPE_ICON_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/type_modern.png");
    private static final ResourceLocation KILLS_ICON_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/kills_modern.png");
    private static final ResourceLocation MOD_ICON_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/mod_modern.png");
    private static final ResourceLocation DISCOVER_ICON_MODERN =
            ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "textures/gui/discover_modern.png");
    private static final Component TYPE_TOOLTIP = Component.translatable("tooltip.wildex.header.type");
    private static final Component KILLS_TOOLTIP = Component.translatable("tooltip.wildex.header.kills");
    private static final Component MOD_TOOLTIP = Component.translatable("tooltip.wildex.header.mod");
    private static final Component DISCOVERY_TOOLTIP = Component.translatable("tooltip.wildex.header.discovery.details");
    private static final List<Component> DISCOVERY_RESET_TOOLTIP = List.of(
            Component.translatable("tooltip.wildex.header.discovery.reset"),
            Component.translatable("tooltip.wildex.header.discovery.reset.line2")
    );
    private static final Component FAVORITE_ADD_TOOLTIP = Component.translatable("tooltip.wildex.header.favorite.add");
    private static final Component FAVORITE_REMOVE_TOOLTIP = Component.translatable("tooltip.wildex.header.favorite.remove");
    private static final int SUMMARY_ROW_GAP = 5;
    private static final int SUMMARY_PAD_X = 0;
    private static final int SUMMARY_PAD_Y = 2;
    private static final int SUMMARY_VALUE_PAD_X = 10;
    private static final int SUMMARY_TEXT_Y_OFFSET = 1;
    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;
    private static final int DISCOVERY_RESET_GAP = 6;
    private static final float HEADER_ICON_TINT_RED = 0.78f;
    private static final float HEADER_ICON_TINT_GREEN = 0.74f;
    private static final float HEADER_ICON_TINT_BLUE = 0.68f;
    private static final float HEADER_ICON_TINT_ALPHA = 0.92f;
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
            String variantLabel,
            boolean favorite,
            WildexDiscoveryDetails discoveryDetails,
            boolean discoveryLoading
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
        boolean showToggle = WildexDiscoveryCache.isDiscovered(mobRl);
        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        int lh = toLogical(area.h(), s);

        boolean compact = area.h() < COMPACT_THRESHOLD_H;
        boolean vintage = WildexThemes.isVintageLayout();

        int padX = PAD_X;
        int nameX = padX + 4;
        int padY = compact ? (vintage ? 5 : 4) : PAD_Y;
        float nameScale = 1.0f;
        int nameLineGap = compact ? 5 : 7;
        int nameToLowerGap = compact ? 1 : 2;

        WildexScissor.enablePhysical(g, x0, y0, x1, y1);
        try {
            g.pose().pushPose();
            g.pose().translate(x0, y0, 0);
            g.pose().scale(s, s, 1.0f);

            int y = padY;

            int right = lw - padX;
            int maxW = Math.max(1, right - padX);
            int favoriteIconSize = favoriteIconSize(font);
            int favoriteIconGap = 4;
            int favoriteIconX = Math.max(nameX, right - favoriteIconSize - 2);
            int nameMaxW = Math.max(1, favoriteIconX - favoriteIconGap - nameX);
            int contentBottom = Math.max(y + 8, lh);

            int phase = mobRl.toString().hashCode();

            Component baseName = header.name() == null
                    ? Component.empty()
                    : header.name().copy();
            String compactVariant = variantLabel == null ? "" : variantLabel.trim();
            Component emphasizedName = compactVariant.isBlank()
                    ? baseName
                    : Component.literal(baseName.getString() + " (" + compactVariant + ")");
            boolean modern = WildexThemes.isModernLayout();
            int nameColor = modern ? 0xFF000000 : 0xFFFFFFFF;
            int nameScaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * nameScale));
            int bandTop = 0;
            int bandBottom = y + nameScaledLineH + 2;
            if (bandBottom > bandTop) {
                int bx0 = 0;
                int bx1 = Math.max(1, lw);
                int by1 = Math.min(lh, bandBottom);
                if (by1 > bandTop) {
                    int cornerCut = WildexThemes.isVintageLayout() ? 3 : 0;
                    fillTopRoundedBand(g, bx0, bx1, bandTop, by1, cornerCut);
                }
            }
            y = drawStyledValueMarquee(g, font, x0, y0, s, nameX, y, nameMaxW, emphasizedName, nameColor, nameLineGap, phase, nameScale);
            drawFavoriteIcon(g, favoriteIconX, y - Math.max(10, nameScaledLineH + nameLineGap), favoriteIconSize, favorite);
            y += nameToLowerGap;

            int kills = WildexKillCache.getOrRequest(mobRl);
            String modName = resolveModName(mobRl);
            renderSummaryPanel(
                    g,
                    font,
                    x0,
                    y0,
                    s,
                    padX,
                    y,
                    maxW,
                    contentBottom,
                    phase,
                    header.aggression(),
                    kills,
                    modName,
                    showToggle,
                    discoveryDetails,
                    discoveryLoading
            );

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

    private static void renderSummaryPanel(
            GuiGraphics g,
            Font font,
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int maxW,
            int contentBottom,
            int phase,
            WildexAggression aggression,
            int kills,
            String modName,
            boolean showDiscoveryRow,
            WildexDiscoveryDetails discoveryDetails,
            boolean discoveryLoading
    ) {
        if (contentBottom <= y || maxW <= 8) return;

        boolean modern = WildexThemes.isModernLayout();
        int lineH = WildexUiText.lineHeight(font);
        int iconSize = lineH + 4;
        int labelColumnW = iconSize + 8;
        int rowH = Math.max(lineH, iconSize);
        int rowCount = showDiscoveryRow ? 4 : 3;
        int panelH = (SUMMARY_PAD_Y * 2) + (rowH * rowCount) + (SUMMARY_ROW_GAP * Math.max(0, rowCount - 1));
        int availableH = contentBottom - y;
        if (panelH > availableH) {
            panelH = availableH;
        }

        int panelX1 = x + maxW;
        int panelY1 = Math.min(contentBottom, y + panelH);

        int valueColor = modern ? 0xFFF4EFE6 : 0xFF2B1A10;
        int separatorColor = WildexUiTheme.current().rowSeparator();
        int rowX = x + SUMMARY_PAD_X;
        int rowW = maxW;
        int rowY = y + SUMMARY_PAD_Y;
        int separatorX = rowX + labelColumnW + 1;
        int separatorY0 = rowY + Math.max(0, (rowH - lineH) / 2);
        int separatorY1 = rowY + ((rowH * Math.max(0, rowCount - 1)) + (SUMMARY_ROW_GAP * Math.max(0, rowCount - 1))) + Math.max(0, (rowH + lineH) / 2);
        g.fill(separatorX, separatorY0, separatorX + 1, separatorY1, separatorColor);

        rowY = drawSummaryRowInline(
                g, font, baseX, baseY, scale, rowX, rowY, rowW, rowH, iconSize, separatorX,
                modern ? TYPE_ICON_MODERN : TYPE_ICON,
                formatAggression(aggression).getString(),
                resolveAggressionTextColor(aggression, valueColor),
                phase ^ 0x1337,
                0
        );
        if (rowY < panelY1 - SUMMARY_PAD_Y) {
            drawSummaryRowSeparator(g, rowX, panelX1, rowY, separatorColor);
            rowY += SUMMARY_ROW_GAP;
        }
        rowY = drawSummaryRowInline(
                g, font, baseX, baseY, scale, rowX, rowY, rowW, rowH, iconSize, separatorX,
                modern ? KILLS_ICON_MODERN : KILLS_ICON,
                Integer.toString(kills),
                valueColor,
                phase ^ 0x2448,
                0
        );
        if (rowY < panelY1 - SUMMARY_PAD_Y) {
            drawSummaryRowSeparator(g, rowX, panelX1, rowY, separatorColor);
            rowY += SUMMARY_ROW_GAP;
        }
        rowY = drawSummaryRowInline(
                g, font, baseX, baseY, scale, rowX, rowY, rowW, rowH, iconSize, separatorX,
                modern ? MOD_ICON_MODERN : MOD_ICON,
                modName,
                valueColor,
                phase ^ 0x5A5A,
                0
        );
        if (showDiscoveryRow && rowY < panelY1 - SUMMARY_PAD_Y) {
            drawSummaryRowSeparator(g, rowX, panelX1, rowY, separatorColor);
            rowY += SUMMARY_ROW_GAP;
            int resetInset = discoveryDetails != null && discoveryDetails.legacyMissingData()
                    ? discoveryResetButtonWidth(rowH) + DISCOVERY_RESET_GAP
                    : 0;
            drawSummaryRowInline(
                    g, font, baseX, baseY, scale, rowX, rowY, rowW, rowH, iconSize, separatorX,
                    modern ? DISCOVER_ICON_MODERN : DISCOVER_ICON,
                    discoverySummaryLine(discoveryDetails, discoveryLoading).getString(),
                    valueColor,
                    phase ^ 0x6C31,
                    resetInset
            );
            if (resetInset > 0) {
                int buttonW = discoveryResetButtonWidth(rowH);
                int buttonX = panelX1 - buttonW;
                drawDiscoveryResetChip(g, font, buttonX, rowY, rowH);
            }
        }
    }

    private static Component discoverySummaryLine(WildexDiscoveryDetails details, boolean loading) {
        if (loading || details == null) return Component.translatable("gui.wildex.discovery.loading");
        if (details.legacyMissingData()) return Component.translatable("gui.wildex.discovery.legacy.line1");
        if (!details.hasData()) return Component.translatable("gui.wildex.discovery.none");
        return WildexDiscoveryDetailsFormatter.sourceTypeLine(details);
    }

    private static List<Component> discoveryTooltipLines(WildexDiscoveryDetails details, boolean loading) {
        if (loading || details == null) {
            return List.of(Component.translatable("gui.wildex.discovery.loading"));
        }
        if (details.legacyMissingData()) {
            return List.of(
                    Component.translatable("gui.wildex.discovery.legacy.line1"),
                    Component.translatable("gui.wildex.discovery.legacy.line2"),
                    Component.translatable("gui.wildex.discovery.legacy.line3"),
                    Component.translatable("gui.wildex.discovery.legacy.line4")
            );
        }
        if (!details.hasData()) {
            return List.of(Component.translatable("gui.wildex.discovery.none"));
        }
        return List.of(
                WildexDiscoveryDetailsFormatter.whenLine(details),
                WildexDiscoveryDetailsFormatter.dimensionLine(details),
                WildexDiscoveryDetailsFormatter.coordsLine(details)
        );
    }

    private static int drawSummaryRowInline(
            GuiGraphics g,
            Font font,
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int rowW,
            int rowH,
            int labelIconSize,
            int separatorX,
            ResourceLocation iconTexture,
            String value,
            int valueColor,
            int phase,
            int valueRightInset
    ) {
        int iconX = x + 2;
        int iconY = y + Math.max(0, (rowH - labelIconSize) / 2);
        drawHeaderIcon(g, iconTexture, iconX, iconY, labelIconSize, labelIconSize);

        int textLineH = WildexUiText.lineHeight(font);
        int valueX = separatorX + SUMMARY_VALUE_PAD_X;
        int valueMaxW = Math.max(1, rowW - (valueX - x) - Math.max(0, valueRightInset));
        int valueY = y + Math.max(0, (rowH - textLineH) / 2) + SUMMARY_TEXT_Y_OFFSET;
        drawInlineValue(g, font, baseX, baseY, scale, valueX, valueY, valueMaxW, value == null ? "" : value, valueColor, phase);
        return y + rowH;
    }

    private static void drawDiscoveryResetChip(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int rowH
    ) {
        WildexScreenLayout.Area area = new WildexScreenLayout.Area(x, y, discoveryResetButtonWidth(rowH), discoveryResetButtonHeight(rowH));
        int chipY = y + Math.max(0, (rowH - area.h()) / 2);
        area = new WildexScreenLayout.Area(x, chipY, area.w(), area.h());
        drawResetChip(g, area);
        drawResetGlyph(g, font, area);
    }

    public Component favoriteButtonTooltip(
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header,
            boolean favorite,
            int mouseX,
            int mouseY
    ) {
        WildexScreenLayout.Area favoriteArea = favoriteButtonArea(font, area, state, header);
        if (!contains(favoriteArea, mouseX, mouseY)) return null;
        return favorite ? FAVORITE_REMOVE_TOOLTIP : FAVORITE_ADD_TOOLTIP;
    }

    public List<Component> discoveryResetTooltip(
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header,
            WildexDiscoveryDetails discoveryDetails,
            int mouseX,
            int mouseY
    ) {
        WildexScreenLayout.Area resetArea = discoveryResetButtonArea(font, area, state, header, discoveryDetails);
        if (!contains(resetArea, mouseX, mouseY)) return null;
        return DISCOVERY_RESET_TOOLTIP;
    }

    public WildexScreenLayout.Area favoriteButtonArea(
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header
    ) {
        if (font == null || area == null || state == null || header == null) return null;

        String selectedIdStr = state.selectedMobId();
        if (selectedIdStr == null || selectedIdStr.isBlank()) return null;

        ResourceLocation mobRl = ResourceLocation.tryParse(selectedIdStr);
        if (mobRl == null) return null;
        if (WildexClientConfigView.hiddenMode() && !WildexDiscoveryCache.isDiscovered(mobRl)) return null;

        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        boolean compact = area.h() < COMPACT_THRESHOLD_H;
        boolean vintage = WildexThemes.isVintageLayout();
        int padY = compact ? (vintage ? 5 : 4) : PAD_Y;
        int right = lw - PAD_X;
        int iconSize = favoriteIconSize(font);
        int iconX = Math.max(PAD_X + 4, right - iconSize - 2);
        int x0 = toScreenX(area.x(), s, iconX);
        int y0 = toScreenY(area.y(), s, padY);
        int x1 = toScreenX(area.x(), s, iconX + iconSize);
        int y1 = toScreenY(area.y(), s, padY + iconSize);
        return new WildexScreenLayout.Area(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    public WildexScreenLayout.Area discoveryResetButtonArea(
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header,
            WildexDiscoveryDetails discoveryDetails
    ) {
        if (font == null || area == null || state == null || header == null || discoveryDetails == null || !discoveryDetails.legacyMissingData()) {
            return null;
        }

        String selectedIdStr = state.selectedMobId();
        if (selectedIdStr == null || selectedIdStr.isBlank()) return null;

        ResourceLocation mobRl = ResourceLocation.tryParse(selectedIdStr);
        if (!WildexDiscoveryCache.isDiscovered(mobRl)) return null;

        HeaderRowGeometry geometry = headerRowGeometry(font, area);
        int buttonW = discoveryResetButtonWidth(geometry.rowH());
        int buttonH = discoveryResetButtonHeight(geometry.rowH());
        int buttonX = geometry.panelX1() - buttonW;
        int buttonY = geometry.discoveryRowY() + Math.max(0, (geometry.rowH() - buttonH) / 2);
        return logicalAreaToScreen(area.x(), area.y(), geometry.scale(), buttonX, buttonY, buttonW, buttonH);
    }

    private static int favoriteIconSize(Font font) {
        return Math.max(9, WildexUiText.lineHeight(font) - 1);
    }

    private static void drawFavoriteIcon(GuiGraphics g, int x, int y, int size, boolean favorite) {
        if (size <= 0) return;
        ResourceLocation texture = favorite
                ? (WildexThemes.isModernLayout() ? FAVORITE_ICON_SELECTED_MODERN : FAVORITE_ICON_SELECTED)
                : (WildexThemes.isModernLayout() ? FAVORITE_ICON_MODERN : FAVORITE_ICON);
        g.blit(texture, x, y, size, size, 0, 0, 64, 64, 64, 64);
    }

    private static void drawSummaryRowSeparator(GuiGraphics g, int x0, int x1, int y, int color) {
        if (g == null || x1 <= x0) return;
        int separatorY = y + Math.max(1, (SUMMARY_ROW_GAP / 2));
        g.fill(x0, separatorY, x1, separatorY + 1, color);
    }

    private static WildexScreenLayout.Area summaryIconArea(
            int baseX,
            int baseY,
            float scale,
            int rowX,
            int rowY,
            int iconSize,
            int rowH
    ) {
        int iconX = rowX + 2;
        int iconY = rowY + Math.max(0, (rowH - iconSize) / 2);
        int x0 = toScreenX(baseX, scale, iconX);
        int y0 = toScreenY(baseY, scale, iconY);
        int x1 = toScreenX(baseX, scale, iconX + iconSize);
        int y1 = toScreenY(baseY, scale, iconY + iconSize);
        return new WildexScreenLayout.Area(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private static WildexScreenLayout.Area logicalAreaToScreen(
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int w,
            int h
    ) {
        int x0 = toScreenX(baseX, scale, x);
        int y0 = toScreenY(baseY, scale, y);
        int x1 = toScreenX(baseX, scale, x + w);
        int y1 = toScreenY(baseY, scale, y + h);
        return new WildexScreenLayout.Area(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private static boolean contains(WildexScreenLayout.Area area, int mouseX, int mouseY) {
        return area != null
                && mouseX >= area.x()
                && mouseX < area.x() + area.w()
                && mouseY >= area.y()
                && mouseY < area.y() + area.h();
    }

    private static void drawHeaderIcon(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height) {
        if (texture == null || width <= 0 || height <= 0) return;
        g.setColor(
                HEADER_ICON_TINT_RED,
                HEADER_ICON_TINT_GREEN,
                HEADER_ICON_TINT_BLUE,
                HEADER_ICON_TINT_ALPHA
        );
        g.blit(texture, x, y, width, height, 0, 0, 64, 64, 64, 64);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawInlineValue(
            GuiGraphics g,
            Font font,
            int baseX,
            int baseY,
            float scale,
            int x,
            int y,
            int maxW,
            String text,
            int color,
            int phase
    ) {
        int textW = WildexUiText.width(font, text);
        if (textW <= maxW) {
            WildexUiText.draw(g, font, text, x, y, color, false);
            return;
        }
        int clipX0 = toScreenX(baseX, scale, x);
        int clipX1 = toScreenX(baseX, scale, x + maxW);
        int clipY0 = toScreenY(baseY, scale, y);
        int clipY1 = toScreenY(baseY, scale, y + WildexUiText.lineHeight(font) + 1);

        WildexScissor.enablePhysical(g, clipX0, clipY0, clipX1, clipY1);
        try {
            int travel = (textW - maxW) + MARQUEE_GAP_PX;
            float off = marqueeOffset(System.currentTimeMillis(), travel, phase);
            int dx = Math.round(off);
            WildexUiText.draw(g, font, text, x - dx, y, color, false);
        } finally {
            g.disableScissor();
        }
    }

    public List<Component> headerTooltip(
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexHeaderData header,
            WildexDiscoveryDetails discoveryDetails,
            boolean discoveryLoading,
            int mouseX,
            int mouseY
    ) {
        if (font == null || area == null || state == null || header == null) return null;

        String selectedIdStr = state.selectedMobId();
        if (selectedIdStr == null || selectedIdStr.isBlank()) return null;

        ResourceLocation mobRl = ResourceLocation.tryParse(selectedIdStr);
        if (mobRl == null) return null;
        if (WildexClientConfigView.hiddenMode() && !WildexDiscoveryCache.isDiscovered(mobRl)) return null;
        boolean showDiscoveryRow = WildexDiscoveryCache.isDiscovered(mobRl);

        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        int lh = toLogical(area.h(), s);
        boolean compact = area.h() < COMPACT_THRESHOLD_H;
        boolean vintage = WildexThemes.isVintageLayout();
        int padY = compact ? (vintage ? 5 : 4) : PAD_Y;
        int nameLineGap = compact ? 5 : 7;
        int nameToLowerGap = compact ? 1 : 2;
        int footerReserveLogical = 0;

        int maxW = Math.max(1, (lw - PAD_X) - PAD_X);
        int nameScaledLineH = WildexUiText.lineHeight(font);
        int y = padY + Math.max(10, nameScaledLineH + nameLineGap) + nameToLowerGap;
        int contentBottom = Math.max(y + 8, lh - footerReserveLogical);

        int lineH = WildexUiText.lineHeight(font);
        int iconSize = lineH + 4;
        int rowH = Math.max(lineH, iconSize);
        int rowY = y + SUMMARY_PAD_Y;
        int rowX = PAD_X + SUMMARY_PAD_X;
        int rowW = maxW;
        int labelColumnW = iconSize + 8;
        int separatorX = rowX + labelColumnW + 1;
        int discoveryResetInset = discoveryDetails != null && discoveryDetails.legacyMissingData()
                ? discoveryResetButtonWidth(rowH) + DISCOVERY_RESET_GAP
                : 0;

        WildexScreenLayout.Area typeArea = summaryIconArea(area.x(), area.y(), s, rowX, rowY, iconSize, rowH);
        if (contains(typeArea, mouseX, mouseY)) return List.of(TYPE_TOOLTIP);

        rowY += rowH + SUMMARY_ROW_GAP;
        if (rowY < contentBottom) {
            WildexScreenLayout.Area killsArea = summaryIconArea(area.x(), area.y(), s, rowX, rowY, iconSize, rowH);
            if (contains(killsArea, mouseX, mouseY)) return List.of(KILLS_TOOLTIP);
        }

        rowY += rowH + SUMMARY_ROW_GAP;
        if (rowY < contentBottom) {
            WildexScreenLayout.Area modArea = summaryIconArea(area.x(), area.y(), s, rowX, rowY, iconSize, rowH);
            if (contains(modArea, mouseX, mouseY)) return List.of(MOD_TOOLTIP);
        }

        rowY += rowH + SUMMARY_ROW_GAP;
        if (showDiscoveryRow && rowY < contentBottom) {
            WildexScreenLayout.Area discoveryIconArea = summaryIconArea(area.x(), area.y(), s, rowX, rowY, iconSize, rowH);
            if (contains(discoveryIconArea, mouseX, mouseY)) return List.of(DISCOVERY_TOOLTIP);

            int valueX = separatorX + SUMMARY_VALUE_PAD_X;
            int valueY = rowY + Math.max(0, (rowH - lineH) / 2) + SUMMARY_TEXT_Y_OFFSET;
            int valueMaxW = Math.max(1, rowW - (valueX - rowX) - discoveryResetInset);
            WildexScreenLayout.Area discoveryValueArea = valueArea(area.x(), area.y(), s, valueX, valueY, valueMaxW, lineH);
            if (contains(discoveryValueArea, mouseX, mouseY)) {
                return discoveryTooltipLines(discoveryDetails, discoveryLoading);
            }
        }

        return null;
    }

    private static WildexScreenLayout.Area valueArea(
            int baseX,
            int baseY,
            float scale,
            int valueX,
            int valueY,
            int valueW,
            int lineH
    ) {
        int x0 = toScreenX(baseX, scale, valueX);
        int y0 = toScreenY(baseY, scale, valueY);
        int x1 = toScreenX(baseX, scale, valueX + valueW);
        int y1 = toScreenY(baseY, scale, valueY + lineH + 1);
        return new WildexScreenLayout.Area(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private static int discoveryResetButtonWidth(int rowH) {
        return Math.max(12, rowH - 1);
    }

    private static int discoveryResetButtonHeight(int rowH) {
        return Math.max(12, rowH - 2);
    }

    private static HeaderRowGeometry headerRowGeometry(Font font, WildexScreenLayout.Area area) {
        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        boolean compact = area.h() < COMPACT_THRESHOLD_H;
        boolean vintage = WildexThemes.isVintageLayout();
        int padY = compact ? (vintage ? 5 : 4) : PAD_Y;
        int nameLineGap = compact ? 5 : 7;
        int nameToLowerGap = compact ? 1 : 2;
        int maxW = Math.max(1, (lw - PAD_X) - PAD_X);
        int lineH = WildexUiText.lineHeight(font);
        int iconSize = lineH + 4;
        int rowH = Math.max(lineH, iconSize);
        int rowX = PAD_X + SUMMARY_PAD_X;
        int rowY = padY + Math.max(10, lineH + nameLineGap) + nameToLowerGap + SUMMARY_PAD_Y;
        return new HeaderRowGeometry(s, rowY + ((rowH + SUMMARY_ROW_GAP) * 3), rowH, rowX + maxW);
    }

    private static void drawResetChip(
            GuiGraphics graphics,
            WildexScreenLayout.Area area
    ) {
        int outer = 0xFF321710;
        int inner = 0xFFB58B67;
        int fill = 0xFF73402C;
        int topGlow = 0x34FFD3AF;

        graphics.fill(area.x(), area.y(), area.x() + area.w(), area.y() + area.h(), outer);
        graphics.fill(area.x() + 1, area.y() + 1, area.x() + area.w() - 1, area.y() + area.h() - 1, inner);
        graphics.fill(area.x() + 2, area.y() + 2, area.x() + area.w() - 2, area.y() + area.h() - 2, fill);
        graphics.fill(area.x() + 2, area.y() + 2, area.x() + area.w() - 2, area.y() + 4, topGlow);
        graphics.fill(area.x() + 2, area.y() + area.h() - 4, area.x() + area.w() - 2, area.y() + area.h() - 2, 0x44000000);
    }

    private static void drawResetGlyph(
            GuiGraphics graphics,
            Font font,
            WildexScreenLayout.Area area
    ) {
        int color = 0xFFF9F2E8;
        int lineH = WildexUiText.lineHeight(font);
        String text = "X";
        int textW = Math.max(1, WildexUiText.width(font, text));
        float scale = Math.max(0.85f, Math.min(1.0f, Math.min(
                (area.w() - 4) / (float) textW,
                (area.h() - 4) / (float) lineH
        )));
        int scaledW = Math.max(1, Math.round(textW * scale));
        int scaledH = Math.max(1, Math.round(lineH * scale));
        int x = area.x() + Math.max(0, (area.w() - scaledW) / 2);
        int y = area.y() + Math.max(0, (area.h() - scaledH) / 2);

        if (scale >= 0.999f) {
            WildexUiText.draw(graphics, font, text, x, y, color, false);
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        WildexUiText.draw(graphics, font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private record HeaderRowGeometry(
            float scale,
            int discoveryRowY,
            int rowH,
            int panelX1
    ) {
    }
    private static void fillTopRoundedBand(
            GuiGraphics g,
            int x0,
            int x1,
            int y0,
            int y1,
            int cornerCut
    ) {
        int color = WildexThemes.isModernLayout() ? 0xFF29E8F1 : WildexUiTheme.current().selectionBg();
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

    private static int resolveAggressionTextColor(WildexAggression aggression, int fallback) {
        if (WildexThemes.isModernLayout()) return fallback;
        if (aggression == null) return fallback;
        return switch (aggression) {
            case FRIENDLY -> 0xFF365140;
            case NEUTRAL -> 0xFF6A5324;
            case HOSTILE -> 0xFF6D342D;
        };
    }


    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

}




