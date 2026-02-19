package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.network.S2CMobLootPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class WildexRightInfoLootRenderer {

    private static final int ITEM_ICON = 16;
    private static final int ITEM_GAP_X = 6;
    private static final int LOOT_ROW_H = 18;
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_PAD = 2;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;
    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;

    private static int lootScrollPx = 0;
    private static int lootViewportH = 1;
    private static int lootContentH = 1;
    private static boolean lootHasScrollbar = false;
    private static boolean lootDraggingScrollbar = false;
    private static int lootDragOffsetY = 0;
    private static int lootViewportScreenX0 = 0;
    private static int lootViewportScreenY0 = 0;
    private static int lootViewportScreenX1 = 0;
    private static int lootViewportScreenY1 = 0;
    private static int lootBarScreenX0 = 0;
    private static int lootBarScreenY0 = 0;
    private static int lootBarScreenX1 = 0;
    private static int lootBarScreenY1 = 0;
    private static int lootThumbScreenH = 0;
    private static int lootThumbScreenY0 = 0;
    private static int lootThumbScreenY1 = 0;

    void resetScroll() {
        lootScrollPx = 0;
        lootViewportH = 1;
        lootContentH = 1;
        lootHasScrollbar = false;
        lootDraggingScrollbar = false;
    }

    boolean scroll(int mouseX, int mouseY, double scrollY) {
        if (!lootHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, lootViewportScreenX0, lootViewportScreenY0, lootViewportScreenX1, lootViewportScreenY1)) {
            return false;
        }
        int max = Math.max(0, lootContentH - lootViewportH);
        int step = 10;
        int next = lootScrollPx - (int) Math.round(scrollY * (double) step);
        if (next < 0) next = 0;
        if (next > max) next = max;
        lootScrollPx = next;
        return true;
    }

    boolean handleMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !lootHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, lootBarScreenX0, lootBarScreenY0, lootBarScreenX1, lootBarScreenY1)) return false;
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, lootBarScreenX0, lootThumbScreenY0, lootBarScreenX1, lootThumbScreenY1)) {
            lootDraggingScrollbar = true;
            lootDragOffsetY = mouseY - lootThumbScreenY0;
            return true;
        }
        setScrollFromThumbTop(mouseY - (lootThumbScreenH / 2));
        return true;
    }

    boolean handleMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0 || !lootDraggingScrollbar) return false;
        setScrollFromThumbTop(mouseY - lootDragOffsetY);
        return true;
    }

    boolean handleMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = lootDraggingScrollbar;
        lootDraggingScrollbar = false;
        return wasDragging;
    }

    void render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            ResourceLocation mobId,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int yTop = area.y() + WildexRightInfoRenderer.PAD_Y;
        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;

        int viewportX = area.x();
        int viewportY = yTop;
        int viewportW = area.w();
        int viewportH = Math.max(1, maxY - yTop);

        List<S2CMobLootPayload.LootLine> lines = WildexLootCache.get(mobId);
        if (lines.isEmpty()) {
            g.drawString(font, WildexRightInfoTabUtil.tr("gui.wildex.loot.none"), x, yTop, inkColor, false);
            lootHasScrollbar = false;
            lootDraggingScrollbar = false;
            return;
        }

        int totalRows = Math.min(64, lines.size());
        int contentH = Math.max(LOOT_ROW_H, totalRows * LOOT_ROW_H);
        lootViewportH = viewportH;
        lootContentH = contentH;
        int maxScroll = Math.max(0, contentH - viewportH);
        boolean showLootScrollbar = maxScroll > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (!showLootScrollbar) {
            lootScrollPx = 0;
        } else if (lootScrollPx > maxScroll) {
            lootScrollPx = maxScroll;
        }

        int sx0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, viewportX);
        int sy0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, viewportY);
        int sx1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, viewportX + viewportW);
        int sy1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, viewportY + viewportH);
        lootViewportScreenX0 = sx0;
        lootViewportScreenY0 = sy0;
        lootViewportScreenX1 = sx1;
        lootViewportScreenY1 = sy1;

        int y = yTop - lootScrollPx;
        int textX = x + ITEM_ICON + ITEM_GAP_X;
        int textW = Math.max(1, (rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD) - textX);

        g.enableScissor(sx0, sy0, sx1, sy1);
        try {
            int shown = 0;
            for (S2CMobLootPayload.LootLine l : lines) {
                if (shown >= 64) break;
                ResourceLocation itemId = l.itemId();
                Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                if (item == null) continue;

                if (y + ITEM_ICON >= yTop && y < maxY) {
                    ItemStack stack = new ItemStack(item);
                    g.renderItem(stack, x, y);
                    String name = stack.getHoverName().getString();
                    String count = formatCount(l.minCount(), l.maxCount());
                    String line = count.isEmpty() ? name : (name + " " + count);
                    WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, line, textX, y + 4, textW, inkColor, screenOriginX, screenOriginY, scale);
                }

                y += LOOT_ROW_H;
                shown++;
            }

            if (showLootScrollbar) {
                int barX0 = rightLimitX - SCROLLBAR_W;
                int barY0 = yTop;
                int barY1 = yTop + viewportH;
                g.fill(barX0, barY0, rightLimitX, barY1, SCROLLBAR_BG);

                int thumbH = Math.max(8, (int) Math.floor(viewportH * (viewportH / (float) contentH)));
                if (thumbH > viewportH) thumbH = viewportH;

                int denom = Math.max(1, contentH - viewportH);
                float t = lootScrollPx / (float) denom;
                int travel = viewportH - thumbH;
                int thumbY0 = yTop + Math.round(travel * t);
                int thumbY1 = thumbY0 + thumbH;
                g.fill(barX0, thumbY0, rightLimitX, thumbY1, SCROLLBAR_THUMB);

                lootHasScrollbar = true;
                cacheScrollbarRect(screenOriginX, screenOriginY, scale, barX0, barY0, rightLimitX, barY1, thumbY0, thumbY1);
            } else {
                lootHasScrollbar = false;
                lootDraggingScrollbar = false;
            }
        } finally {
            g.disableScissor();
        }
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
        lootBarScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX0);
        lootBarScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY0);
        lootBarScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX1);
        lootBarScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY1);

        lootThumbScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY0);
        lootThumbScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY1);
        lootThumbScreenH = Math.max(1, lootThumbScreenY1 - lootThumbScreenY0);
    }

    private static void setScrollFromThumbTop(int desiredThumbTop) {
        if (!lootHasScrollbar) return;

        int barTop = lootBarScreenY0;
        int barBottom = lootBarScreenY1;
        int travel = Math.max(1, (barBottom - barTop) - lootThumbScreenH);
        int clamped = Math.max(barTop, Math.min(desiredThumbTop, barBottom - lootThumbScreenH));

        float t = (clamped - barTop) / (float) travel;
        int maxScroll = Math.max(0, lootContentH - lootViewportH);
        lootScrollPx = Math.round(maxScroll * t);
    }

    private static String formatCount(int min, int max) {
        int a = Math.max(0, min);
        int b = Math.max(0, max);
        if (a <= 0 && b <= 0) return "";
        if (a == b) return "x" + a;
        return "x" + a + "-" + b;
    }
}
