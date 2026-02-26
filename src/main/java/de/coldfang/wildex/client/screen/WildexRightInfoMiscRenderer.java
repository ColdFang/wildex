package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.extractor.WildexEntityTypeTags;
import de.coldfang.wildex.client.data.model.WildexMiscData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

final class WildexRightInfoMiscRenderer {

    private static final int COLOR_TRUE = 0xFF2ECC71;
    private static final int COLOR_FALSE = 0xFFE74C3C;
    private static final int DIVIDER = 0x22301E14;
    private static final int SECTION_DIVIDER = 0x44301E14;
    private static final int DIVIDER_MODERN = 0x7AE9DCC8;
    private static final int SECTION_DIVIDER_MODERN = 0xB0F3E8D4;
    private static final int SECTION_DIVIDER_THICKNESS = 2;
    private static final int COL_GAP = 6;
    private static final String IS_OWNABLE_ENTITY_LABEL = "gui.wildex.info.is_ownable_entity";
    private static final String BREEDING_ITEMS_LABEL = "gui.wildex.info.breeding_items";
    private static final String TAMING_ITEMS_LABEL = "gui.wildex.info.taming_items";
    private static final String NO_ITEMS = "gui.wildex.info.none";
    private static final String TAMING_NONE_OWNABLE_HINT = "gui.wildex.info.taming_none_ownable_hint";

    private static final int ITEM_ICON = 16;
    private static final int ITEM_GAP_X = 6;
    private static final int ITEM_ROW_H = 18;
    private static final int SCROLLBAR_W = 8;
    private static final int SCROLLBAR_PAD = 2;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;

    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;
    private static int miscScrollPx = 0;
    private static int miscViewportH = 1;
    private static int miscContentH = 1;
    private static boolean miscHasScrollbar = false;
    private static boolean miscDraggingScrollbar = false;
    private static int miscDragOffsetY = 0;
    private static int miscViewportScreenX0 = 0;
    private static int miscViewportScreenY0 = 0;
    private static int miscViewportScreenX1 = 0;
    private static int miscViewportScreenY1 = 0;
    private static int miscBarScreenX0 = 0;
    private static int miscBarScreenY0 = 0;
    private static int miscBarScreenX1 = 0;
    private static int miscBarScreenY1 = 0;
    private static int miscThumbScreenH = 0;
    private static int miscThumbScreenY0 = 0;
    private static int miscThumbScreenY1 = 0;
    private static final int TOGGLE_GAP_X = 4;
    private static final int TOGGLE_BG = 0xCC1A120C;
    private static final int TOGGLE_BORDER = 0xFFECDCC3;
    private static final int TOGGLE_SYMBOL = 0xFFFFF6E8;
    private static final int WRAPPED_EMPTY_LINE_GAP = 2;
    private static final int FULLWIDTH_COLON_CODEPOINT = 0xFF1A;
    private static final String MARK_TICK = Character.toString(0x2714);
    private static final String MARK_CROSS = Character.toString(0x2716);

    private boolean breedingCollapsed = true;
    private boolean tamingCollapsed = true;
    private int breedingToggleScreenX0 = Integer.MIN_VALUE;
    private int breedingToggleScreenY0 = Integer.MIN_VALUE;
    private int breedingToggleScreenX1 = Integer.MIN_VALUE;
    private int breedingToggleScreenY1 = Integer.MIN_VALUE;
    private int tamingToggleScreenX0 = Integer.MIN_VALUE;
    private int tamingToggleScreenY0 = Integer.MIN_VALUE;
    private int tamingToggleScreenX1 = Integer.MIN_VALUE;
    private int tamingToggleScreenY1 = Integer.MIN_VALUE;

    private record TraitLine(String display, List<TagKey<EntityType<?>>> tags, Predicate<EntityType<?>> directCheck) {
    }

    private static final List<TraitLine> INFO_TRAITS = List.of(
            new TraitLine("gui.wildex.trait.immune_fire", List.of(), EntityType::fireImmune),
            new TraitLine("gui.wildex.trait.immune_drowning", List.of(WildexEntityTypeTags.CAN_BREATHE_UNDER_WATER), null),
            new TraitLine("gui.wildex.trait.immune_fall_damage", List.of(WildexEntityTypeTags.FALL_DAMAGE_IMMUNE), null),
            new TraitLine("gui.wildex.trait.weak_bane_of_arthropods", List.of(WildexEntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS), null),
            new TraitLine("gui.wildex.trait.weak_impaling", List.of(WildexEntityTypeTags.SENSITIVE_TO_IMPALING), null),
            new TraitLine("gui.wildex.trait.weak_smite", List.of(WildexEntityTypeTags.SENSITIVE_TO_SMITE), null)
    );

    void resetScroll() {
        miscScrollPx = 0;
        miscViewportH = 1;
        miscContentH = 1;
        miscHasScrollbar = false;
        miscDraggingScrollbar = false;
    }

    boolean scroll(int mouseX, int mouseY, double scrollY) {
        if (!miscHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, miscViewportScreenX0, miscViewportScreenY0, miscViewportScreenX1, miscViewportScreenY1)) {
            return false;
        }
        int max = Math.max(0, miscContentH - miscViewportH);
        int step = 10;
        int next = miscScrollPx - (int) Math.round(scrollY * (double) step);
        if (next < 0) next = 0;
        if (next > max) next = max;
        miscScrollPx = next;
        return true;
    }

    boolean handleMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, breedingToggleScreenX0, breedingToggleScreenY0, breedingToggleScreenX1, breedingToggleScreenY1)) {
            breedingCollapsed = !breedingCollapsed;
            return true;
        }
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, tamingToggleScreenX0, tamingToggleScreenY0, tamingToggleScreenX1, tamingToggleScreenY1)) {
            tamingCollapsed = !tamingCollapsed;
            return true;
        }

        if (!miscHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, miscBarScreenX0, miscBarScreenY0, miscBarScreenX1, miscBarScreenY1)) return false;
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, miscBarScreenX0, miscThumbScreenY0, miscBarScreenX1, miscThumbScreenY1)) {
            miscDraggingScrollbar = true;
            miscDragOffsetY = mouseY - miscThumbScreenY0;
            return true;
        }
        setScrollFromThumbTop(mouseY - (miscThumbScreenH / 2));
        return true;
    }

    boolean handleMouseDragged(int mouseY, int button) {
        if (button != 0 || !miscDraggingScrollbar) return false;
        setScrollFromThumbTop(mouseY - miscDragOffsetY);
        return true;
    }

    boolean handleMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = miscDraggingScrollbar;
        miscDraggingScrollbar = false;
        return wasDragging;
    }

    void render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexMiscData miscData,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        EntityType<?> type = resolveSelectedType(state.selectedMobId());
        if (type == null) return;
        clearToggleHitboxes();

        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int yTop = area.y() + WildexRightInfoRenderer.PAD_Y;
        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;
        int viewportH = Math.max(1, maxY - yTop);

        int line = Math.max(10, WildexUiText.lineHeight(font) + 3);
        int textH = WildexUiText.lineHeight(font);
        int toggleSize = Math.max(10, textH + 1);

        int markW = Math.max(WildexUiText.width(font, MARK_TICK), WildexUiText.width(font, MARK_CROSS));
        int markColW = Math.max(10, markW + 2);
        int usableRightWithBar = rightLimitX - (SCROLLBAR_W + SCROLLBAR_PAD);
        int dividerWithoutBar = (rightLimitX - markColW) - Math.max(2, COL_GAP / 2);
        int dividerWithBar = (usableRightWithBar - markColW) - Math.max(2, COL_GAP / 2);
        int labelMaxWithoutBar = Math.max(1, (dividerWithoutBar - 2) - x);
        int labelMaxWithBar = Math.max(1, (dividerWithBar - 2) - x);

        List<ResourceLocation> breedingItems = miscData == null ? List.of() : miscData.breedingItemIds();
        List<ResourceLocation> tamingItems = miscData == null ? List.of() : miscData.tamingItemIds();
        boolean ownable = miscData != null && miscData.ownable();
        boolean wrapTamingEmpty = ownable && tamingItems.isEmpty();
        String tamingEmptyText = wrapTamingEmpty
                ? WildexRightInfoTabUtil.tr(TAMING_NONE_OWNABLE_HINT)
                : WildexRightInfoTabUtil.tr(NO_ITEMS);

        miscViewportH = viewportH;
        int estimatedNoBar = estimateContentHeight(
                font,
                labelMaxWithoutBar,
                breedingItems,
                WildexRightInfoTabUtil.tr(NO_ITEMS),
                tamingItems,
                tamingEmptyText,
                wrapTamingEmpty
        );
        boolean hasBar = (estimatedNoBar - viewportH) > SCROLLBAR_SHOW_THRESHOLD_PX;
        int labelMaxW = hasBar ? labelMaxWithBar : labelMaxWithoutBar;
        miscContentH = estimateContentHeight(
                font,
                labelMaxW,
                breedingItems,
                WildexRightInfoTabUtil.tr(NO_ITEMS),
                tamingItems,
                tamingEmptyText,
                wrapTamingEmpty
        );
        boolean hasBarAfter = (miscContentH - viewportH) > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (hasBarAfter != hasBar) {
            hasBar = hasBarAfter;
            labelMaxW = hasBar ? labelMaxWithBar : labelMaxWithoutBar;
            miscContentH = estimateContentHeight(
                    font,
                    labelMaxW,
                    breedingItems,
                    WildexRightInfoTabUtil.tr(NO_ITEMS),
                    tamingItems,
                    tamingEmptyText,
                    wrapTamingEmpty
            );
        }
        miscHasScrollbar = hasBar;
        int maxScroll = Math.max(0, miscContentH - viewportH);
        if (!miscHasScrollbar) miscScrollPx = 0;
        else if (miscScrollPx > maxScroll) miscScrollPx = maxScroll;

        miscViewportScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x());
        miscViewportScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop);
        miscViewportScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x() + area.w());
        miscViewportScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop + viewportH);

        int usableRightX = rightLimitX - (miscHasScrollbar ? (SCROLLBAR_W + SCROLLBAR_PAD) : 0);
        int markColX0 = usableRightX - markColW;
        int dividerX = markColX0 - Math.max(2, COL_GAP / 2);
        int headerLabelMaxW = Math.max(1, usableRightX - toggleSize - TOGGLE_GAP_X - x);

        int yCursor = yTop - miscScrollPx;

        if (dividerX > x + 8) {
            g.fill(dividerX, yTop, dividerX + 1, maxY, dividerColor());
        }

        for (int i = 0; i < INFO_TRAITS.size(); i++) {
            if (yCursor + textH > yTop && yCursor < maxY) {
                TraitLine t = INFO_TRAITS.get(i);
                boolean has = evalTrait(type, t);
                String mark = has ? MARK_TICK : MARK_CROSS;
                int markColor = has ? COLOR_TRUE : COLOR_FALSE;

                WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                        g, font, WildexRightInfoTabUtil.tr(t.display()), x, yCursor, labelMaxW, inkColor, screenOriginX, screenOriginY, scale
                );
                int markX = markColX0 + Math.max(0, (markColW - WildexUiText.width(font, mark)) / 2);
                WildexUiText.draw(g, font, mark, markX, yCursor, markColor, false);
                int dividerY = yCursor + textH + 1;
                if (i < INFO_TRAITS.size() - 1 && dividerY + 1 < maxY && dividerY >= yTop) {
                    g.fill(x, dividerY, rightLimitX, dividerY + 1, dividerColor());
                }
            }
            yCursor += line;
        }

        int ownableGap = Math.max(1, line / 4);
        int ownableDividerY = yCursor + Math.max(1, ownableGap / 2);
        if (ownableDividerY + 1 < maxY && ownableDividerY >= yTop) {
            g.fill(x, ownableDividerY, usableRightX, ownableDividerY + 1, dividerColor());
        }

        int ownableY = yCursor + ownableGap;
        String mark = ownable ? MARK_TICK : MARK_CROSS;
        int markColor = ownable ? COLOR_TRUE : COLOR_FALSE;
        if (ownableY + textH > yTop && ownableY < maxY) {
            WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                    g, font, WildexRightInfoTabUtil.tr(IS_OWNABLE_ENTITY_LABEL), x, ownableY, labelMaxW, inkColor, screenOriginX, screenOriginY, scale
            );
            int markX = markColX0 + Math.max(0, (markColW - WildexUiText.width(font, mark)) / 2);
            WildexUiText.draw(g, font, mark, markX, ownableY, markColor, false);
        }

        int breedingGap = Math.max(2, line / 3);
        int betweenDividerY = ownableY + line + Math.max(1, breedingGap / 2);
        if (betweenDividerY + SECTION_DIVIDER_THICKNESS <= maxY && betweenDividerY >= yTop) {
            g.fill(x, betweenDividerY, usableRightX, betweenDividerY + SECTION_DIVIDER_THICKNESS, sectionDividerColor());
        }
        int breedingHeaderY = ownableY + line + breedingGap;
        String breedingRawLabel = stripTrailingColon(WildexRightInfoTabUtil.tr(BREEDING_ITEMS_LABEL));
        String breedingLabel = WildexRightInfoTabUtil.clipToWidth(font, breedingRawLabel, headerLabelMaxW);
        int breedingLabelW = WildexUiText.width(font, breedingLabel);
        int breedingToggleX0 = Math.max(x, Math.min(usableRightX - toggleSize, x + breedingLabelW + TOGGLE_GAP_X));
        int breedingToggleY0 = breedingHeaderY + Math.max(0, (textH - toggleSize) / 2);
        if (breedingHeaderY + textH > yTop && breedingHeaderY < maxY) {
            WildexUiText.draw(g, font, breedingLabel, x, breedingHeaderY, inkColor, false);
            renderToggle(g, breedingToggleX0, breedingToggleY0, toggleSize, breedingCollapsed);
            cacheBreedingToggleHitbox(screenOriginX, screenOriginY, scale, breedingToggleX0, breedingToggleY0, toggleSize);
        }

        int breedingItemsY = breedingHeaderY + textH + 2;
        if (!breedingCollapsed) {
            renderItemList(
                    g, font, x, breedingItemsY, yTop, maxY, labelMaxW,
                    breedingItems,
                    WildexRightInfoTabUtil.tr(NO_ITEMS),
                    false,
                    inkColor, screenOriginX, screenOriginY, scale
            );
        }

        int rowH = Math.max(Math.max(8, Math.round(ITEM_ICON * WildexUiScale.get())) + 2, Math.round(ITEM_ROW_H * WildexUiScale.get()));
        int breedingBlockRows = breedingCollapsed ? 0 : Math.max(1, breedingItems == null ? 0 : breedingItems.size());
        int breedingBlockH = breedingBlockRows * rowH;

        int tamingGap = Math.max(2, line / 3);
        int tamingDividerY = breedingItemsY + breedingBlockH + Math.max(1, tamingGap / 2);
        if (tamingDividerY + 1 < maxY && tamingDividerY >= yTop) {
            g.fill(x, tamingDividerY, usableRightX, tamingDividerY + 1, dividerColor());
        }

        int tamingHeaderY = breedingItemsY + breedingBlockH + tamingGap;
        String tamingRawLabel = stripTrailingColon(WildexRightInfoTabUtil.tr(TAMING_ITEMS_LABEL));
        String tamingLabel = WildexRightInfoTabUtil.clipToWidth(font, tamingRawLabel, headerLabelMaxW);
        int tamingLabelW = WildexUiText.width(font, tamingLabel);
        int tamingToggleX0 = Math.max(x, Math.min(usableRightX - toggleSize, x + tamingLabelW + TOGGLE_GAP_X));
        int tamingToggleY0 = tamingHeaderY + Math.max(0, (textH - toggleSize) / 2);
        if (tamingHeaderY + textH > yTop && tamingHeaderY < maxY) {
            WildexUiText.draw(g, font, tamingLabel, x, tamingHeaderY, inkColor, false);
            renderToggle(g, tamingToggleX0, tamingToggleY0, toggleSize, tamingCollapsed);
            cacheTamingToggleHitbox(screenOriginX, screenOriginY, scale, tamingToggleX0, tamingToggleY0, toggleSize);
        }

        int tamingItemsY = tamingHeaderY + textH + 2;
        if (!tamingCollapsed) {
            renderItemList(
                    g, font, x, tamingItemsY, yTop, maxY, labelMaxW,
                    tamingItems,
                    tamingEmptyText,
                    wrapTamingEmpty,
                    inkColor, screenOriginX, screenOriginY, scale
            );
        }

        if (miscHasScrollbar) {
            int barX0 = rightLimitX - SCROLLBAR_W;
            int barY1 = yTop + viewportH;
            g.fill(barX0, yTop, rightLimitX, barY1, SCROLLBAR_BG);

            int thumbH = Math.max(8, (int) Math.floor(viewportH * (viewportH / (float) miscContentH)));
            if (thumbH > viewportH) thumbH = viewportH;
            int denom = Math.max(1, miscContentH - viewportH);
            float t = miscScrollPx / (float) denom;
            int travel = viewportH - thumbH;
            int thumbY0 = yTop + Math.round(travel * t);
            int thumbY1 = thumbY0 + thumbH;
            g.fill(barX0, thumbY0, rightLimitX, thumbY1, SCROLLBAR_THUMB);
            cacheScrollbarRect(screenOriginX, screenOriginY, scale, barX0, yTop, rightLimitX, barY1, thumbY0, thumbY1);
        } else {
            miscDraggingScrollbar = false;
        }
    }

    private int estimateContentHeight(
            Font font,
            int labelMaxW,
            List<ResourceLocation> breedingItems,
            String breedingEmptyText,
            List<ResourceLocation> tamingItems,
            String tamingEmptyText,
            boolean wrapTamingEmpty
    ) {
        int line = Math.max(10, WildexUiText.lineHeight(font) + 3);
        int textH = WildexUiText.lineHeight(font);
        int itemRow = Math.max(textH + 2, Math.round(ITEM_ROW_H * WildexUiScale.get()));
        int breedingBlockH = breedingCollapsed
                ? 0
                : computeItemBlockHeight(font, labelMaxW, itemRow, breedingItems, breedingEmptyText, false);
        int tamingBlockH = tamingCollapsed
                ? 0
                : computeItemBlockHeight(font, labelMaxW, itemRow, tamingItems, tamingEmptyText, wrapTamingEmpty);

        int h = 0;
        h += INFO_TRAITS.size() * line;
        h += Math.max(1, line / 4); // ownable offset after traits
        h += line; // ownable row block
        h += Math.max(2, line / 3); // spacing before breeding header
        h += SECTION_DIVIDER_THICKNESS; // emphasized divider between ownable and breeding block
        h += textH + 2; // breeding header
        h += breedingBlockH;
        h += Math.max(2, line / 3); // spacing before taming header
        h += textH + 2; // taming header
        h += tamingBlockH;
        return h;
    }

    private static int computeItemBlockHeight(
            Font font,
            int labelMaxW,
            int itemRow,
            List<ResourceLocation> itemIds,
            String emptyText,
            boolean wrapEmptyText
    ) {
        if (itemIds != null && !itemIds.isEmpty()) {
            return itemIds.size() * itemRow;
        }
        if (!wrapEmptyText) {
            return itemRow;
        }

        List<FormattedCharSequence> lines = wrapText(font, emptyText, labelMaxW);
        int count = Math.max(1, lines.size());
        int lineH = WildexUiText.lineHeight(font);
        return (count * lineH) + Math.max(0, (count - 1) * WRAPPED_EMPTY_LINE_GAP);
    }

    private static void renderToggle(GuiGraphics g, int x, int y, int size, boolean collapsed) {
        int x1 = x + size;
        int y1 = y + size;
        g.fill(x, y, x1, y1, TOGGLE_BG);
        g.fill(x, y, x1, y + 1, TOGGLE_BORDER);
        g.fill(x, y1 - 1, x1, y1, TOGGLE_BORDER);
        g.fill(x, y, x + 1, y1, TOGGLE_BORDER);
        g.fill(x1 - 1, y, x1, y1, TOGGLE_BORDER);

        int cx = x + (size / 2);
        int cy = y + (size / 2);
        int arm = Math.max(1, (size - 9) / 2);
        int thick = size >= 12 ? 2 : 1;

        g.fill(cx - arm, cy - (thick / 2), cx + arm + 1, cy - (thick / 2) + thick, TOGGLE_SYMBOL);
        if (collapsed) {
            g.fill(cx - (thick / 2), cy - arm, cx - (thick / 2) + thick, cy + arm + 1, TOGGLE_SYMBOL);
        }
    }

    private static String stripTrailingColon(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        while (!s.isEmpty()) {
            char c = s.charAt(s.length() - 1);
            if (c == ':' || c == FULLWIDTH_COLON_CODEPOINT) {
                s = s.substring(0, s.length() - 1).trim();
                continue;
            }
            break;
        }
        return s;
    }

    private static int dividerColor() {
        return WildexThemes.isModernLayout() ? DIVIDER_MODERN : DIVIDER;
    }

    private static int sectionDividerColor() {
        return WildexThemes.isModernLayout() ? SECTION_DIVIDER_MODERN : SECTION_DIVIDER;
    }

    private void clearToggleHitboxes() {
        breedingToggleScreenX0 = Integer.MIN_VALUE;
        breedingToggleScreenY0 = Integer.MIN_VALUE;
        breedingToggleScreenX1 = Integer.MIN_VALUE;
        breedingToggleScreenY1 = Integer.MIN_VALUE;
        tamingToggleScreenX0 = Integer.MIN_VALUE;
        tamingToggleScreenY0 = Integer.MIN_VALUE;
        tamingToggleScreenX1 = Integer.MIN_VALUE;
        tamingToggleScreenY1 = Integer.MIN_VALUE;
    }

    private void cacheBreedingToggleHitbox(int screenOriginX, int screenOriginY, float scale, int x, int y, int size) {
        breedingToggleScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, x);
        breedingToggleScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, y);
        breedingToggleScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, x + size);
        breedingToggleScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, y + size);
    }

    private void cacheTamingToggleHitbox(int screenOriginX, int screenOriginY, float scale, int x, int y, int size) {
        tamingToggleScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, x);
        tamingToggleScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, y);
        tamingToggleScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, x + size);
        tamingToggleScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, y + size);
    }

    private static void renderItemList(
            GuiGraphics g,
            Font font,
            int x,
            int startY,
            int viewportTop,
            int viewportBottom,
            int labelMaxW,
            List<ResourceLocation> itemIds,
            String emptyText,
            boolean wrapEmptyText,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int y = startY;
        int iconPx = Math.max(8, Math.round(ITEM_ICON * WildexUiScale.get()));
        int rowH = Math.max(iconPx + 2, Math.round(ITEM_ROW_H * WildexUiScale.get()));
        int gapX = Math.max(2, Math.round(ITEM_GAP_X * WildexUiScale.get()));

        if (itemIds == null || itemIds.isEmpty()) {
            String text = (emptyText == null || emptyText.isBlank()) ? WildexRightInfoTabUtil.tr(NO_ITEMS) : emptyText;
            if (!wrapEmptyText) {
                if (y + WildexUiText.lineHeight(font) > viewportTop && y < viewportBottom) {
                    WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                            g,
                            font,
                            text,
                            x,
                            y,
                            labelMaxW,
                            inkColor,
                            screenOriginX,
                            screenOriginY,
                            scale
                    );
                }
            } else {
                List<FormattedCharSequence> wrapped = wrapText(font, text, labelMaxW);
                int lineH = WildexUiText.lineHeight(font);
                int yy = y;
                if (wrapped.isEmpty()) {
                    if (yy + lineH > viewportTop && yy < viewportBottom) {
                        WildexUiText.draw(g, font, text, x, yy, inkColor, false);
                    }
                } else {
                    for (FormattedCharSequence seq : wrapped) {
                        if (yy + lineH > viewportTop && yy < viewportBottom) {
                            WildexUiText.draw(g, font, seq, x, yy, inkColor, false);
                        }
                        yy += lineH + WRAPPED_EMPTY_LINE_GAP;
                    }
                }
            }
            return;
        }

        for (ResourceLocation id : itemIds) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) {
                y += rowH;
                continue;
            }
            if (y + iconPx > viewportTop && y < viewportBottom) {
                ItemStack stack = new ItemStack(item);
                drawScaledItem(g, stack, x, y, iconPx);
                int textX = x + iconPx + gapX;
                int textY = y + Math.max(0, (iconPx - WildexUiText.lineHeight(font)) / 2);
                int textW = Math.max(1, labelMaxW - iconPx - gapX);
                WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                        g, font, stack.getHoverName().getString(), textX, textY, textW, inkColor, screenOriginX, screenOriginY, scale
                );
            }
            y += rowH;
        }
    }

    private static List<FormattedCharSequence> wrapText(Font font, String text, int maxWidthPx) {
        if (font == null || text == null || text.isBlank()) return List.of();
        if (maxWidthPx <= 0) return List.of();

        float s = WildexUiText.scale();
        int logicalWidth = Math.max(1, Math.round(maxWidthPx / Math.max(0.001f, s)));
        return font.split(Component.literal(text), logicalWidth);
    }

    private static void drawScaledItem(GuiGraphics g, ItemStack stack, int x, int y, int iconPx) {
        if (iconPx == ITEM_ICON) {
            g.renderItem(stack, x, y);
            return;
        }
        float s = iconPx / (float) ITEM_ICON;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0f);
        g.pose().scale(s, s, 1.0f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private static void cacheScrollbarRect(
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
        miscBarScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX0);
        miscBarScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY0);
        miscBarScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX1);
        miscBarScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY1);

        miscThumbScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY0);
        miscThumbScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY1);
        miscThumbScreenH = Math.max(1, miscThumbScreenY1 - miscThumbScreenY0);
    }

    private static void setScrollFromThumbTop(int desiredThumbTop) {
        if (!miscHasScrollbar) return;

        int barTop = miscBarScreenY0;
        int barBottom = miscBarScreenY1;
        int travel = Math.max(1, (barBottom - barTop) - miscThumbScreenH);
        int clamped = Math.max(barTop, Math.min(desiredThumbTop, barBottom - miscThumbScreenH));

        float t = (clamped - barTop) / (float) travel;
        int maxScroll = Math.max(0, miscContentH - miscViewportH);
        miscScrollPx = Math.round(maxScroll * t);
    }

    private static boolean evalTrait(EntityType<?> type, TraitLine t) {
        if (t.directCheck() != null) {
            return t.directCheck().test(type);
        }
        if (t.tags() == null || t.tags().isEmpty()) return false;
        return WildexEntityTypeTags.isAny(type, t.tags());
    }

    private static EntityType<?> resolveSelectedType(String selectedMobId) {
        if (selectedMobId == null || selectedMobId.isBlank()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(selectedMobId);
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }
}
