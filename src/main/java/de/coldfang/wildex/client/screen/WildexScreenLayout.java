package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;

public final class WildexScreenLayout {

    public static final int TEX_W = 1024;
    public static final int TEX_H = 704;

    private static final float DESIGN_SCALE = 0.5f;
    private static final int SAFE_TOP_PADDING = 8;
    private static final int SAFE_BOTTOM_PADDING = 8;
    private static final int HOTBAR_SAFE_HEIGHT = 28;
    private static final int PAGE_INSET_ALL_SIDES = 2;
    private static final int MODERN_PREVIEW_DECOUPLED_NUDGE_X = 4;
    private static final int MODERN_PREVIEW_DECOUPLED_NUDGE_Y = 1;

    private static final Metrics MODERN = new Metrics(
            102, 79, 50, 9, 38,
            6, 4, 14, 15,
            7, 6, 11, 35,
            35, 38, 0,
            0,
            true,
            16, 22, 6, 0, 0,
            16, -4, 0,
            150, 0.95f, 79 + 25, 16, -34,
            14, 4, 3, 9, 26,
            13, 24, 14, 19,
            14, 1, 87, 0, 10, 8
    );

    private static final Metrics VINTAGE = new Metrics(
            102, 79, 50, 9, 38,
            6, 4, 14, 15,
            8, 7, -7, 7,
            30, 2, 5,
            -15,
            false,
            16, 22, 6, 0, 0,
            -15, 0, 0,
            150, 0.95f, 79 + 25, -2, 2,
            14, 4, 10, 0, 0,
            10, 24, 0, 0,
            0, 0, 61, 2, 0, 0
    );

    private final float scale;
    private final float x;
    private final float y;

    private final Area leftContentArea;
    private final Area leftSearchArea;
    private final Area leftActionArea;

    private final Area rightHeaderArea;
    private final Area rightTabsArea;
    private final Area rightInfoArea;

    private final Area leftDiscoveryCounterArea;
    private final Area leftEntriesCounterArea;

    private final Area rightPreviewArea;
    private final Area previewResetButtonArea;

    private WildexScreenLayout(
            float scale,
            float x,
            float y,
            Area leftContentArea,
            Area leftSearchArea,
            Area leftActionArea,
            Area rightHeaderArea,
            Area rightTabsArea,
            Area rightInfoArea,
            Area leftDiscoveryCounterArea,
            Area leftEntriesCounterArea,
            Area rightPreviewArea,
            Area previewResetButtonArea
    ) {
        this.scale = scale;
        this.x = x;
        this.y = y;

        this.leftContentArea = leftContentArea;
        this.leftSearchArea = leftSearchArea;
        this.leftActionArea = leftActionArea;

        this.rightHeaderArea = rightHeaderArea;
        this.rightTabsArea = rightTabsArea;
        this.rightInfoArea = rightInfoArea;

        this.leftDiscoveryCounterArea = leftDiscoveryCounterArea;
        this.leftEntriesCounterArea = leftEntriesCounterArea;

        this.rightPreviewArea = rightPreviewArea;
        this.previewResetButtonArea = previewResetButtonArea;
    }

    public static WildexScreenLayout compute(int screenW, int screenH) {
        DesignStyle style = ClientConfig.INSTANCE.designStyle.get();
        Metrics m = metricsFor(style);
        int safeW = Math.max(1, screenW);
        int safeH = Math.max(1, screenH - HOTBAR_SAFE_HEIGHT);

        float maxScaleW = (float) (safeW - (SAFE_TOP_PADDING + SAFE_BOTTOM_PADDING)) / (float) TEX_W;
        float maxScaleH = (float) (safeH - (SAFE_TOP_PADDING + SAFE_BOTTOM_PADDING)) / (float) TEX_H;
        float maxScale = Math.max(0.01f, Math.min(maxScaleW, maxScaleH));

        float scale = Math.min(DESIGN_SCALE, maxScale);

        int scaledW = Math.round(TEX_W * scale);
        int scaledH = Math.round(TEX_H * scale);

        float x = (safeW - scaledW) / 2.0f;
        float y = (safeH - scaledH) / 2.0f;

        int contentTop = m.topMargin();
        int contentBottom = TEX_H - m.bottomMargin() - m.pageBottomCut();
        int centerX = TEX_W / 2;

        int leftX0 = m.outerMarginX() + m.leftPageLeftCut();
        int leftX1 = centerX - m.spineGap() - m.leftPageRightCut() - m.leftPageRightExtraCut();
        int leftTop = contentTop + m.leftPageTopCut();

        int rightX0 = centerX + m.spineGap() + m.rightPageShiftX();
        int rightX1 = TEX_W - m.outerMarginX() + m.rightPageShiftX() - m.rightPageRightCut() - m.rightPageRightExtraCut();

        int inset = PAGE_INSET_ALL_SIDES;
        int leftY0 = leftTop + inset;
        int rightY0 = contentTop + inset;
        int bottomY = contentBottom - inset;

        int leftPageX0 = leftX0 + inset;
        int leftPageX1 = leftX1 - inset;
        int rightPageX0 = rightX0 + inset;
        int rightPageX1 = rightX1 - inset;

        Area leftPage = areaFromTex(x, y, scale, leftPageX0, leftY0, leftPageX1 - leftPageX0, bottomY - leftY0);
        Area rightPage = areaFromTex(x, y, scale, rightPageX0, rightY0, rightPageX1 - rightPageX0, bottomY - rightY0);

        int padX = Math.round(m.contentPadX() * scale);
        int padY = Math.round(m.contentPadY() * scale);
        int spinePad = Math.round(m.spineSafePad() * scale);

        int leftContentX = leftPage.x() + padX + Math.round(m.leftContentShiftX() * scale);
        int leftContentY = leftPage.y() + padY;
        int leftContentW = Math.max(1, leftPage.w() - padX - spinePad);
        int leftContentH = Math.max(1, leftPage.h() - (padY * 2));
        Area leftContent = new Area(leftContentX, leftContentY, leftContentW, leftContentH);

        int searchH = Math.max(12, Math.round(m.searchHeight() * scale));
        Area searchArea = new Area(leftContentX, leftContentY + 2, leftContentW, searchH);

        int actionGap = Math.max(0, Math.round(m.actionRowGap() * scale));
        int actionH = Math.max(1, Math.round(m.actionRowHeight() * scale));

        int ax = leftContentX + Math.round(m.actionRowShiftX() * scale);
        int ay = (leftContentY + searchH + actionGap) + Math.round(m.actionRowShiftY() * scale);
        Area actionArea = new Area(ax, ay, leftContentW, actionH);

        int counterTexY = (contentTop + m.contentPadY()) + m.counterOffsetY() + m.counterShiftY();
        int counterAnchorY = texToScreenY(y, scale, counterTexY);

        int lineH = Math.max(9, Math.round(10 * scale));
        int counterX = leftContentX + Math.round(m.counterShiftX() * scale);
        Area entriesCounterArea = new Area(counterX, counterAnchorY, leftContentW, lineH);

        int minGapEntriesToSearch = Math.max(1, Math.round(2 * scale));
        int entriesBottom = entriesCounterArea.y() + entriesCounterArea.h();
        int maxAllowedEntriesBottom = searchArea.y() - minGapEntriesToSearch;
        if (entriesBottom > maxAllowedEntriesBottom) {
            int pushDown = entriesBottom - maxAllowedEntriesBottom;
            searchArea = new Area(searchArea.x(), searchArea.y() + pushDown, searchArea.w(), searchArea.h());
            actionArea = new Area(actionArea.x(), actionArea.y() + pushDown, actionArea.w(), actionArea.h());
        }

        int minGapSearchToAction = Math.max(1, Math.round(2 * scale));
        int searchBottom = searchArea.y() + searchArea.h();
        int currentGap = actionArea.y() - searchBottom;
        if (currentGap < minGapSearchToAction) {
            int delta = minGapSearchToAction - currentGap;
            actionArea = new Area(actionArea.x(), actionArea.y() + delta, actionArea.w(), actionArea.h());
        }

        // Vintage uses extra top trim on the left page, so anchor entries counter to the search box.
        if (!m.alignRightToLeftSearch()) {
            int entriesY = Math.max(leftContentY, searchArea.y() - lineH - 2);
            entriesCounterArea = new Area(leftContentX, entriesY, leftContentW, lineH);
        }

        int discoveredGap = Math.max(0, Math.round(4 * scale));
        int discoveredY = searchArea.y() + searchArea.h() + discoveredGap;
        Area discoveredCounterArea = new Area(leftContentX, discoveredY, leftContentW, lineH);

        Area previewBase = computeRightPreviewArea(x, y, scale, rightX0, rightX1 - rightX0, contentTop, contentBottom, m);
        int previewShiftYPx = Math.round(m.previewShiftY() * scale);
        int alignRightBy = m.alignRightToLeftSearch() ? (searchArea.y() - previewBase.y() + previewShiftYPx) : 0;
        Area preview = new Area(previewBase.x(), previewBase.y() + alignRightBy, previewBase.w(), previewBase.h());

        int resetSize = Math.max(10, Math.round(m.previewResetBtnSize() * scale));
        int resetMargin = Math.max(2, Math.round(m.previewResetBtnMargin() * scale));
        int resetBelowGap = Math.max(1, Math.round(2 * scale));
        int resetY = (style == DesignStyle.MODERN)
                ? (preview.y() + preview.h()) + resetBelowGap - 2
                : (preview.y() + preview.h()) - resetSize - resetMargin;
        Area previewReset = new Area(
                (preview.x() + preview.w()) - resetSize - resetMargin,
                resetY,
                resetSize,
                resetSize
        );

        int rightContentX = rightPage.x() + spinePad;
        int rightContentY = rightPage.y() + padY + alignRightBy;
        int rightContentH = Math.max(1, rightPage.h() - (padY * 2));

        int gapToPreview = Math.max(0, Math.round(m.rightGapToPreview() * scale));

        int headerX = rightContentX + Math.round(m.rightHeaderShiftX() * scale);
        int headerY = preview.y() + Math.round(m.rightHeaderShiftY() * scale);
        int headerW = Math.max(1, (preview.x() - gapToPreview) - headerX);
        Area headerArea = new Area(headerX, headerY, headerW, preview.h());

        int sectionGapY = Math.max(0, Math.round(m.rightSectionGapY() * scale));
        int tabsH = Math.max(1, Math.round(m.rightTabsHeight() * scale));
        int tabsX = rightContentX + Math.round(m.rightTabsShiftX() * scale);
        int tabsY = (preview.y() + preview.h() + sectionGapY) + Math.round(m.rightTabsShiftY() * scale);

        int previewRight = preview.x() + preview.w();
        int tabsW = Math.max(1, previewRight - tabsX - Math.max(0, Math.round(m.rightTabsRightCut() * scale)));
        Area tabsArea = new Area(tabsX, tabsY, tabsW, tabsH);

        int tabsToInfoGap = Math.max(0, Math.round(m.rightTabsToInfoGapY() * scale));
        int infoX = rightContentX + Math.round(m.rightInfoShiftX() * scale);
        int infoY = (tabsY + tabsH + tabsToInfoGap) + Math.round(m.rightInfoShiftY() * scale);

        int infoW = Math.max(1, previewRight - infoX - Math.max(0, Math.round(m.rightInfoRightCut() * scale)));
        int bottomCut = Math.max(0, Math.round(m.rightInfoBottomCut() * scale));
        int infoH = Math.max(1, (rightContentY + rightContentH) - infoY - bottomCut);
        Area infoArea = new Area(infoX, infoY, infoW, infoH);

        int rightShiftY = Math.round(m.rightPageShiftY() * scale);
        preview = shiftY(preview, rightShiftY);
        previewReset = shiftY(previewReset, rightShiftY);
        headerArea = shiftY(headerArea, rightShiftY);
        tabsArea = shiftY(tabsArea, rightShiftY);
        infoArea = shiftY(infoArea, rightShiftY);

        if (style == DesignStyle.MODERN) {
            preview = new Area(
                    preview.x() + MODERN_PREVIEW_DECOUPLED_NUDGE_X,
                    preview.y() + MODERN_PREVIEW_DECOUPLED_NUDGE_Y,
                    preview.w(),
                    preview.h()
            );
        }

        return new WildexScreenLayout(
                scale, x, y,
                leftContent, searchArea, actionArea,
                headerArea, tabsArea, infoArea,
                discoveredCounterArea, entriesCounterArea,
                preview, previewReset
        );
    }

    private static Area computeRightPreviewArea(
            float baseX,
            float baseY,
            float scale,
            int rightX,
            int rightW,
            int contentTop,
            int contentBottom,
            Metrics m
    ) {
        int size = m.previewSize();
        int px = rightX + Math.round(Math.max(0, rightW - size) * m.previewAnchorX()) + m.previewShiftX();
        int py = m.previewBaseY() + m.previewShiftY();

        px = clamp(px, rightX, rightX + Math.max(0, rightW - size));
        py = clamp(py, contentTop, contentBottom - size);
        return areaFromTex(baseX, baseY, scale, px, py, size, size);
    }

    private static Metrics metricsFor(DesignStyle style) {
        return style == DesignStyle.VINTAGE ? VINTAGE : MODERN;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }

    private static Area areaFromTex(float baseX, float baseY, float scale, int texX, int texY, int texW, int texH) {
        int sx = texToScreenX(baseX, scale, texX);
        int sy = texToScreenY(baseY, scale, texY);
        int sw = Math.max(1, Math.round(texW * scale));
        int sh = Math.max(1, Math.round(texH * scale));
        return new Area(sx, sy, sw, sh);
    }

    private static Area shiftY(Area area, int dy) {
        return new Area(area.x(), area.y() + dy, area.w(), area.h());
    }

    private static int texToScreenX(float baseX, float scale, int texX) {
        return Math.round(baseX + (texX * scale));
    }

    private static int texToScreenY(float baseY, float scale, int texY) {
        return Math.round(baseY + (texY * scale));
    }

    public float scale() {
        return scale;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public Area leftContentArea() {
        return leftContentArea;
    }

    public Area leftSearchArea() {
        return leftSearchArea;
    }

    public Area leftActionArea() {
        return leftActionArea;
    }

    public Area rightHeaderArea() {
        return rightHeaderArea;
    }

    public Area rightTabsArea() {
        return rightTabsArea;
    }

    public Area rightInfoArea() {
        return rightInfoArea;
    }

    public Area leftDiscoveryCounterArea() {
        return leftDiscoveryCounterArea;
    }

    public Area leftEntriesCounterArea() {
        return leftEntriesCounterArea;
    }

    public Area rightPreviewArea() {
        return rightPreviewArea;
    }

    public Area previewResetButtonArea() {
        return previewResetButtonArea;
    }

    public record Area(int x, int y, int w, int h) {
    }

    private record Metrics(
            int outerMarginX,
            int topMargin,
            int bottomMargin,
            int pageBottomCut,
            int spineGap,
            int contentPadX,
            int contentPadY,
            int spineSafePad,
            int leftContentShiftX,
            int leftPageLeftCut,
            int leftPageRightCut,
            int rightPageShiftX,
            int rightPageRightCut,
            int leftPageTopCut,
            int leftPageRightExtraCut,
            int rightPageRightExtraCut,
            int rightPageShiftY,
            boolean alignRightToLeftSearch,
            int searchHeight,
            int actionRowHeight,
            int actionRowGap,
            int actionRowShiftX,
            int actionRowShiftY,
            int counterOffsetY,
            int counterShiftX,
            int counterShiftY,
            int previewSize,
            float previewAnchorX,
            int previewBaseY,
            int previewShiftX,
            int previewShiftY,
            int previewResetBtnSize,
            int previewResetBtnMargin,
            int rightGapToPreview,
            int rightHeaderShiftX,
            int rightHeaderShiftY,
            int rightSectionGapY,
            int rightTabsHeight,
            int rightTabsShiftX,
            int rightTabsShiftY,
            int rightInfoShiftX,
            int rightInfoShiftY,
            int rightInfoBottomCut,
            int rightTabsToInfoGapY,
            int rightTabsRightCut,
            int rightInfoRightCut
    ) {
    }
}
