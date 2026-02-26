package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.network.S2CMobLootPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

final class WildexRightInfoLootRenderer {

    private static final int ITEM_ICON = 16;
    private static final int ITEM_GAP_X = 6;
    private static final int LOOT_ROW_H = 18;
    private static final int SCROLLBAR_W = 8;
    private static final int SCROLLBAR_PAD = 2;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;
    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;
    private static final String CONDITIONAL_MARKER = "[!]";
    private static final ResourceLocation WHITE_BANNER_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "white_banner");

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

    boolean handleMouseDragged(int mouseY, int button) {
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

    WildexRightInfoRenderer.TooltipRequest render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            ResourceLocation mobId,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale,
            int mouseX,
            int mouseY
    ) {
        WildexRightInfoRenderer.TooltipRequest tooltip = null;

        int itemIcon = scaledLootIconPx();
        int itemGapX = scaledLootIconGapPx();
        int lootRowH = scaledLootRowHeightPx(itemIcon);

        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int yTop = area.y() + WildexRightInfoRenderer.PAD_Y;
        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;

        int viewportX = area.x();
        int viewportW = area.w();
        int viewportH = Math.max(1, maxY - yTop);

        List<S2CMobLootPayload.LootLine> lines = WildexLootCache.get(mobId);
        if (lines.isEmpty()) {
            WildexUiText.draw(g, font, WildexRightInfoTabUtil.tr("gui.wildex.loot.none"), x, yTop, inkColor, false);
            lootHasScrollbar = false;
            lootDraggingScrollbar = false;
            return null;
        }

        int totalRows = Math.min(64, lines.size());
        int contentH = Math.max(lootRowH, totalRows * lootRowH);
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
        int sy0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop);
        int sx1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, viewportX + viewportW);
        int sy1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, yTop + viewportH);
        lootViewportScreenX0 = sx0;
        lootViewportScreenY0 = sy0;
        lootViewportScreenX1 = sx1;
        lootViewportScreenY1 = sy1;

        int y = yTop - lootScrollPx;
        int textX = x + itemIcon + itemGapX;
        int textW = Math.max(1, (rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD) - textX);

        WildexScissor.enablePhysical(g, sx0, sy0, sx1, sy1);
        try {
            int shown = 0;
            for (S2CMobLootPayload.LootLine l : lines) {
                if (shown >= 64) break;
                ResourceLocation itemId = l.itemId();
                Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                if (item == null) continue;

                int rowTop = y;
                int rowBottom = y + lootRowH;
                if (y + itemIcon >= yTop && y < maxY) {
                    ItemStack stack = new ItemStack(item);
                    drawScaledItem(g, stack, x, y, itemIcon);
                    String name = resolveDisplayName(l, stack);
                    String count = formatCount(l.minCount(), l.maxCount());

                    boolean conditional = !extractConditionProfiles(l).isEmpty();
                    String marker = conditional ? (" " + CONDITIONAL_MARKER) : "";
                    String line = count.isEmpty() ? (name + marker) : (name + " " + count + marker);

                    int textY = y + Math.max(0, (itemIcon - WildexUiText.lineHeight(font)) / 2);
                    WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, line, textX, textY, textW, inkColor, screenOriginX, screenOriginY, scale);

                    if (conditional && mouseX >= 0 && mouseY >= 0 && tooltip == null) {
                        int rowScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, x);
                        int rowScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, rightLimitX);
                        int rowScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, rowTop);
                        int rowScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, rowBottom);

                        if (mouseX >= rowScreenX0 && mouseX < rowScreenX1 && mouseY >= rowScreenY0 && mouseY < rowScreenY1) {
                            tooltip = tooltipForConditionProfiles(extractConditionProfiles(l));
                        }
                    }
                }

                y += lootRowH;
                shown++;
            }

            if (showLootScrollbar) {
                int barX0 = rightLimitX - SCROLLBAR_W;
                int barY1 = yTop + viewportH;
                g.fill(barX0, yTop, rightLimitX, barY1, SCROLLBAR_BG);

                int thumbH = Math.max(8, (int) Math.floor(viewportH * (viewportH / (float) contentH)));
                if (thumbH > viewportH) thumbH = viewportH;

                int denom = Math.max(1, contentH - viewportH);
                float t = lootScrollPx / (float) denom;
                int travel = viewportH - thumbH;
                int thumbY0 = yTop + Math.round(travel * t);
                int thumbY1 = thumbY0 + thumbH;
                g.fill(barX0, thumbY0, rightLimitX, thumbY1, SCROLLBAR_THUMB);

                lootHasScrollbar = true;
                cacheScrollbarRect(screenOriginX, screenOriginY, scale, barX0, yTop, rightLimitX, barY1, thumbY0, thumbY1);
            } else {
                lootHasScrollbar = false;
                lootDraggingScrollbar = false;
            }
        } finally {
            g.disableScissor();
        }

        return tooltip;
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
        if (a == 0 && b == 0) return "";
        if (a == b) return "x" + a;
        return "x" + a + "-" + b;
    }

    private static WildexRightInfoRenderer.TooltipRequest tooltipForConditionProfiles(List<Integer> profiles) {
        if (profiles == null || profiles.isEmpty()) return null;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.wildex.loot.condition.title"));

        for (int i = 0; i < profiles.size(); i++) {
            String expression = formatProfileMask(profiles.get(i));
            lines.add(Component.literal(capitalizeFirst(expression)));
            if (i < profiles.size() - 1) {
                lines.add(Component.translatable("tooltip.wildex.loot.condition.logic.or"));
            }
        }

        return lines.isEmpty() ? null : new WildexRightInfoRenderer.TooltipRequest(lines);
    }

    private static List<Integer> extractConditionProfiles(S2CMobLootPayload.LootLine line) {
        if (line == null) return List.of();
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();

        List<Integer> rawProfiles = line.conditionProfiles();
        if (rawProfiles != null) {
            for (int mask : rawProfiles) {
                if (mask != S2CMobLootPayload.COND_NONE) {
                    unique.add(mask);
                }
            }
        }
        if (unique.isEmpty() && line.conditionMask() != S2CMobLootPayload.COND_NONE) {
            unique.add(line.conditionMask());
        }
        if (unique.isEmpty()) return List.of();

        List<Integer> out = new ArrayList<>(unique);
        out.sort(Comparator
                .comparingInt(Integer::bitCount)
                .thenComparingInt(Integer::intValue));
        return List.copyOf(out);
    }

    private static String formatProfileMask(int mask) {
        List<String> labels = conditionLabels(mask);
        if (labels.isEmpty()) {
            return WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.unknown");
        }
        if (labels.size() == 1) return labels.getFirst();
        String andJoin = " " + WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.logic.and_join") + " ";
        return String.join(andJoin, labels);
    }

    private static String capitalizeFirst(String value) {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String resolveDisplayName(S2CMobLootPayload.LootLine line, ItemStack stack) {
        if (line != null && WHITE_BANNER_ID.equals(line.itemId()) && hasCaptainCondition(line)) {
            return Component.translatable("block.minecraft.ominous_banner").getString();
        }
        return stack.getHoverName().getString();
    }

    private static boolean hasCaptainCondition(S2CMobLootPayload.LootLine line) {
        if (line == null) return false;
        if ((line.conditionMask() & S2CMobLootPayload.COND_CAPTAIN) != 0) return true;

        List<Integer> profiles = line.conditionProfiles();
        if (profiles == null || profiles.isEmpty()) return false;
        for (int mask : profiles) {
            if ((mask & S2CMobLootPayload.COND_CAPTAIN) != 0) {
                return true;
            }
        }
        return false;
    }

    private static List<String> conditionLabels(int mask) {
        List<String> out = new ArrayList<>(6);
        if ((mask & S2CMobLootPayload.COND_PLAYER_KILL) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.player_kill"));
        }
        if ((mask & S2CMobLootPayload.COND_ON_FIRE) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.on_fire"));
        }
        if ((mask & S2CMobLootPayload.COND_SLIME_SIZE) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.size_variant"));
        }
        if ((mask & S2CMobLootPayload.COND_CAPTAIN) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.captain"));
        }
        if ((mask & S2CMobLootPayload.COND_FROG_KILL) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.frog_kill"));
        }
        if ((mask & S2CMobLootPayload.COND_SHEEP_COLOR) != 0) {
            out.add(WildexRightInfoTabUtil.tr("tooltip.wildex.loot.condition.label.sheep_color"));
        }
        return out;
    }

    private static int scaledLootIconPx() {
        return Math.max(8, Math.round(ITEM_ICON * WildexUiScale.get()));
    }

    private static int scaledLootIconGapPx() {
        return Math.max(2, Math.round(ITEM_GAP_X * WildexUiScale.get()));
    }

    private static int scaledLootRowHeightPx(int iconPx) {
        return Math.max(iconPx + 2, Math.round(LOOT_ROW_H * WildexUiScale.get()));
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
}




