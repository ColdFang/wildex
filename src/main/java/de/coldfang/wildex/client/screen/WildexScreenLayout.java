package de.coldfang.wildex.client.screen;

public final class WildexScreenLayout {

    public static final int TEX_W = 1024;
    public static final int TEX_H = 704;

    private static final float DESIGN_SCALE = 0.5f;
    private static final int SAFE_TOP_PADDING = 8;
    private static final int SAFE_BOTTOM_PADDING = 8;
    private static final int HOTBAR_SAFE_HEIGHT = 28;

    private static final int OUTER_MARGIN_X = 94;
    private static final int TOP_MARGIN = 79;
    private static final int BOTTOM_MARGIN = 50;
    private static final int SPINE_GAP = 32;

    private static final int CONTENT_PAD_X = 6;
    private static final int CONTENT_PAD_Y = 4;
    private static final int SPINE_SAFE_PAD = 14;

    private static final int SEARCH_HEIGHT = 16;

    private static final int ACTION_ROW_HEIGHT = 22;
    private static final int ACTION_ROW_GAP = 6;
    private static final int ACTION_ROW_SHIFT_X = 0;
    private static final int ACTION_ROW_SHIFT_Y = 0;

    private static final int COUNTER_OFFSET_Y = -15;
    private static final int COUNTER_SHIFT_X = 0;
    private static final int COUNTER_SHIFT_Y = 0;

    private static final int PREVIEW_SIZE = 150;
    private static final float PREVIEW_ANCHOR_X = 0.95f;
    private static final int PREVIEW_BASE_Y = TOP_MARGIN + 25;
    private static final int PREVIEW_SHIFT_X = 0;
    private static final int PREVIEW_SHIFT_Y = 0;

    private static final int PREVIEW_RESET_BTN_SIZE = 14;
    private static final int PREVIEW_RESET_BTN_MARGIN = 4;

    private static final int RIGHT_GAP_TO_PREVIEW = 10;
    private static final int RIGHT_HEADER_SHIFT_X = 0;
    private static final int RIGHT_HEADER_SHIFT_Y = 0;

    private static final int RIGHT_SECTION_GAP_Y = 10;

    private static final int RIGHT_TABS_HEIGHT = 24;
    private static final int RIGHT_TABS_SHIFT_X = 0;
    private static final int RIGHT_TABS_SHIFT_Y = 0;

    private static final int RIGHT_INFO_SHIFT_X = 0;
    private static final int RIGHT_INFO_SHIFT_Y = 0;
    private static final int RIGHT_INFO_BOTTOM_CUT = 6;

    private static final int RIGHT_TABS_TO_INFO_GAP_Y = 2;

    private static final int RIGHT_TABS_RIGHT_CUT = 6;
    private static final int RIGHT_INFO_RIGHT_CUT = 6;

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

        int contentTop = TOP_MARGIN;
        int contentBottom = TEX_H - BOTTOM_MARGIN;
        int centerX = TEX_W / 2;

        int leftX0 = OUTER_MARGIN_X;
        int leftX1 = centerX - SPINE_GAP;

        int rightX0 = centerX + SPINE_GAP;
        int rightX1 = TEX_W - OUTER_MARGIN_X;

        Area leftPage = areaFromTex(x, y, scale, leftX0, contentTop, leftX1 - leftX0, contentBottom - contentTop);
        Area rightPage = areaFromTex(x, y, scale, rightX0, contentTop, rightX1 - rightX0, contentBottom - contentTop);

        int padX = Math.round(CONTENT_PAD_X * scale);
        int padY = Math.round(CONTENT_PAD_Y * scale);
        int spinePad = Math.round(SPINE_SAFE_PAD * scale);

        int leftContentX = leftPage.x() + padX;
        int leftContentY = leftPage.y() + padY;
        int leftContentW = Math.max(1, leftPage.w() - padX - spinePad);
        int leftContentH = Math.max(1, leftPage.h() - (padY * 2));
        Area leftContent = new Area(leftContentX, leftContentY, leftContentW, leftContentH);

        int searchH = Math.max(12, Math.round(SEARCH_HEIGHT * scale));
        Area searchArea = new Area(leftContentX, leftContentY + 2, leftContentW, searchH);

        int actionGap = Math.max(0, Math.round(ACTION_ROW_GAP * scale));
        int actionH = Math.max(1, Math.round(ACTION_ROW_HEIGHT * scale));

        int ax = leftContentX + Math.round(ACTION_ROW_SHIFT_X * scale);
        int ay = (leftContentY + searchH + actionGap) + Math.round(ACTION_ROW_SHIFT_Y * scale);
        Area actionArea = new Area(ax, ay, leftContentW, actionH);

        int counterTexX = (leftX0 + CONTENT_PAD_X + (leftX1 - leftX0 - CONTENT_PAD_X - SPINE_SAFE_PAD)) + COUNTER_SHIFT_X;
        int counterTexY = (contentTop + CONTENT_PAD_Y) + COUNTER_OFFSET_Y + COUNTER_SHIFT_Y;

        int counterAnchorY = texToScreenY(y, scale, counterTexY);

        int lineH = Math.max(9, Math.round(10 * scale));
        Area entriesCounterArea = new Area(leftContentX, counterAnchorY, leftContentW, lineH);

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

        int discoveredGap = Math.max(0, Math.round(4 * scale));
        int discoveredY = searchArea.y() + searchArea.h() + discoveredGap;
        Area discoveredCounterArea = new Area(leftContentX, discoveredY, leftContentW, lineH);

        Area previewBase = computeRightPreviewArea(x, y, scale, rightX0, rightX1 - rightX0, contentTop, contentBottom);
        int alignRightBy = searchArea.y() - previewBase.y();
        Area preview = new Area(previewBase.x(), previewBase.y() + alignRightBy, previewBase.w(), previewBase.h());

        int resetSize = Math.max(10, Math.round(PREVIEW_RESET_BTN_SIZE * scale));
        int resetMargin = Math.max(2, Math.round(PREVIEW_RESET_BTN_MARGIN * scale));
        Area previewReset = new Area(
                (preview.x() + preview.w()) - resetSize - resetMargin,
                (preview.y() + preview.h()) - resetSize - resetMargin,
                resetSize,
                resetSize
        );

        int rightContentX = rightPage.x() + spinePad;
        int rightContentY = rightPage.y() + padY + alignRightBy;
        int rightContentW = Math.max(1, rightPage.w() - spinePad - padX);
        int rightContentH = Math.max(1, rightPage.h() - (padY * 2));

        int gapToPreview = Math.max(0, Math.round(RIGHT_GAP_TO_PREVIEW * scale));

        int headerX = rightContentX + Math.round(RIGHT_HEADER_SHIFT_X * scale);
        int headerY = preview.y() + Math.round(RIGHT_HEADER_SHIFT_Y * scale);
        int headerW = Math.max(1, (preview.x() - gapToPreview) - headerX);
        Area headerArea = new Area(headerX, headerY, headerW, preview.h());

        int sectionGapY = Math.max(0, Math.round(RIGHT_SECTION_GAP_Y * scale));

        int tabsH = Math.max(1, Math.round(RIGHT_TABS_HEIGHT * scale));
        int tabsX = rightContentX + Math.round(RIGHT_TABS_SHIFT_X * scale);
        int tabsY = (preview.y() + preview.h() + sectionGapY) + Math.round(RIGHT_TABS_SHIFT_Y * scale);

        int tabsW = Math.max(1, rightContentW - Math.max(0, Math.round(RIGHT_TABS_RIGHT_CUT * scale)));
        Area tabsArea = new Area(tabsX, tabsY, tabsW, tabsH);

        int tabsToInfoGap = Math.max(0, Math.round(RIGHT_TABS_TO_INFO_GAP_Y * scale));

        int infoX = rightContentX + Math.round(RIGHT_INFO_SHIFT_X * scale);
        int infoY = (tabsY + tabsH + tabsToInfoGap) + Math.round(RIGHT_INFO_SHIFT_Y * scale);

        int infoW = Math.max(1, rightContentW - Math.max(0, Math.round(RIGHT_INFO_RIGHT_CUT * scale)));
        int bottomCut = Math.max(0, Math.round(RIGHT_INFO_BOTTOM_CUT * scale));
        int infoH = Math.max(1, (rightContentY + rightContentH) - infoY - bottomCut);
        Area infoArea = new Area(infoX, infoY, infoW, infoH);

        return new WildexScreenLayout(
                scale,
                x,
                y,
                leftContent,
                searchArea,
                actionArea,
                headerArea,
                tabsArea,
                infoArea,
                discoveredCounterArea,
                entriesCounterArea,
                preview,
                previewReset
        );
    }

    private static Area computeRightPreviewArea(
            float baseX,
            float baseY,
            float scale,
            int rightX,
            int rightW,
            int contentTop,
            int contentBottom
    ) {
        int size = PREVIEW_SIZE;

        int px = rightX + Math.round(Math.max(0, rightW - size) * PREVIEW_ANCHOR_X) + PREVIEW_SHIFT_X;
        int py = PREVIEW_BASE_Y + PREVIEW_SHIFT_Y;

        px = clamp(px, rightX, rightX + Math.max(0, rightW - size));
        py = clamp(py, contentTop, contentBottom - size);

        return areaFromTex(baseX, baseY, scale, px, py, size, size);
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static Area areaFromTex(float baseX, float baseY, float scale, int texX, int texY, int texW, int texH) {
        int sx = texToScreenX(baseX, scale, texX);
        int sy = texToScreenY(baseY, scale, texY);
        int sw = Math.max(1, Math.round(texW * scale));
        int sh = Math.max(1, Math.round(texH * scale));
        return new Area(sx, sy, sw, sh);
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
}
