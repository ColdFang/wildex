package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.extractor.WildexEntityTypeTags;
import de.coldfang.wildex.client.data.model.WildexMiscData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
    private static final int SECTION_DIVIDER_THICKNESS = 2;
    private static final int COL_GAP = 6;
    private static final String IS_OWNABLE_ENTITY_LABEL = "gui.wildex.info.is_ownable_entity";
    private static final String BREEDING_ITEMS_LABEL = "gui.wildex.info.breeding_items";
    private static final String NO_ITEMS = "gui.wildex.info.none";

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
        if (button != 0 || !miscHasScrollbar) return false;
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

        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int yTop = area.y() + WildexRightInfoRenderer.PAD_Y;
        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;
        int viewportH = Math.max(1, maxY - yTop);

        miscViewportH = viewportH;
        miscContentH = estimateContentHeight(font, miscData);
        int maxScroll = Math.max(0, miscContentH - viewportH);
        miscHasScrollbar = maxScroll > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (!miscHasScrollbar) miscScrollPx = 0;
        else if (miscScrollPx > maxScroll) miscScrollPx = maxScroll;

        miscViewportScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x());
        miscViewportScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop);
        miscViewportScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, area.x() + area.w());
        miscViewportScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop + viewportH);

        String tick = "\u2714";
        String cross = "\u2716";
        int line = Math.max(10, WildexUiText.lineHeight(font) + 3);
        int textH = WildexUiText.lineHeight(font);

        int markW = Math.max(WildexUiText.width(font, tick), WildexUiText.width(font, cross));
        int markColW = Math.max(10, markW + 2);
        int usableRightX = rightLimitX - (miscHasScrollbar ? (SCROLLBAR_W + SCROLLBAR_PAD) : 0);
        int markColX0 = usableRightX - markColW;
        int dividerX = markColX0 - Math.max(2, COL_GAP / 2);
        int labelMaxW = Math.max(1, (dividerX - 2) - x);

        int yCursor = yTop - miscScrollPx;

        if (dividerX > x + 8) {
            g.fill(dividerX, yTop, dividerX + 1, maxY, DIVIDER);
        }

        for (int i = 0; i < INFO_TRAITS.size(); i++) {
            if (yCursor + textH > yTop && yCursor < maxY) {
                TraitLine t = INFO_TRAITS.get(i);
                boolean has = evalTrait(type, t);
                String mark = has ? tick : cross;
                int markColor = has ? COLOR_TRUE : COLOR_FALSE;

                WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                        g, font, WildexRightInfoTabUtil.tr(t.display()), x, yCursor, labelMaxW, inkColor, screenOriginX, screenOriginY, scale
                );
                int markX = markColX0 + Math.max(0, (markColW - WildexUiText.width(font, mark)) / 2);
                WildexUiText.draw(g, font, mark, markX, yCursor, markColor, false);
                int dividerY = yCursor + textH + 1;
                if (i < INFO_TRAITS.size() - 1 && dividerY + 1 < maxY && dividerY >= yTop) {
                    g.fill(x, dividerY, rightLimitX, dividerY + 1, DIVIDER);
                }
            }
            yCursor += line;
        }

        int ownableGap = Math.max(1, line / 4);
        int ownableDividerY = yCursor + Math.max(1, ownableGap / 2);
        if (ownableDividerY + 1 < maxY && ownableDividerY >= yTop) {
            g.fill(x, ownableDividerY, usableRightX, ownableDividerY + 1, DIVIDER);
        }

        int ownableY = yCursor + ownableGap;
        boolean ownable = miscData != null && miscData.ownable();
        String mark = ownable ? tick : cross;
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
            g.fill(x, betweenDividerY, usableRightX, betweenDividerY + SECTION_DIVIDER_THICKNESS, SECTION_DIVIDER);
        }
        int breedingHeaderY = ownableY + line + breedingGap;
        if (breedingHeaderY + textH > yTop && breedingHeaderY < maxY) {
            WildexUiText.draw(g, font, WildexRightInfoTabUtil.tr(BREEDING_ITEMS_LABEL), x, breedingHeaderY, inkColor, false);
        }

        int breedingItemsY = breedingHeaderY + textH + 2;
        renderItemList(
                g, font, x, breedingItemsY, yTop, maxY, labelMaxW,
                miscData == null ? List.of() : miscData.breedingItemIds(),
                inkColor, screenOriginX, screenOriginY, scale
        );

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

    private static int estimateContentHeight(Font font, WildexMiscData miscData) {
        int line = Math.max(10, WildexUiText.lineHeight(font) + 3);
        int textH = WildexUiText.lineHeight(font);
        int itemRow = Math.max(textH + 2, Math.round(ITEM_ROW_H * WildexUiScale.get()));
        int breedingCount = Math.max(1, miscData == null ? 0 : miscData.breedingItemIds().size());

        int h = 0;
        h += INFO_TRAITS.size() * line;
        h += Math.max(1, line / 4); // ownable offset after traits
        h += line; // ownable row block
        h += Math.max(2, line / 3); // spacing before breeding header
        h += SECTION_DIVIDER_THICKNESS; // emphasized divider between ownable and breeding block
        h += textH + 2; // breeding header
        h += breedingCount * itemRow;
        return h;
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
            if (y + WildexUiText.lineHeight(font) > viewportTop && y < viewportBottom) {
                WildexUiText.draw(g, font, WildexRightInfoTabUtil.tr(NO_ITEMS), x, y, inkColor, false);
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
