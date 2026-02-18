package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexLootCache;
import de.coldfang.wildex.client.data.WildexSpawnCache;
import de.coldfang.wildex.client.data.extractor.WildexEntityTypeTags;
import de.coldfang.wildex.client.data.model.WildexMobData;
import de.coldfang.wildex.client.data.model.WildexStatsData;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import de.coldfang.wildex.util.WildexEntityFactory;
import de.coldfang.wildex.network.S2CMobLootPayload;
import de.coldfang.wildex.network.S2CMobSpawnsPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Predicate;

public final class WildexRightInfoRenderer {

    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;
    private static final int PAD_RIGHT = 2;

    private static final int ICON = 9;
    private static final int ICON_GAP = 1;
    private static final int COL_GAP = 6;
    private static final float STATS_LABEL_COL_RATIO = 0.56f;
    private static final int STATS_LABEL_COL_MIN = 56;
    private static final int STATS_LABEL_COL_MAX = 240;
    private static final int STATS_MIN_VALUE_COL_W = 48;

    private static final int ITEM_ICON = 16;
    private static final int ITEM_GAP_X = 6;
    private static final int LOOT_ROW_H = 18;

    private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");

    private static final ResourceLocation ARMOR_FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation ARMOR_HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");

    private static final int MAX_HEART_ICONS = 10;
    private static final int MAX_HEART_HP = MAX_HEART_ICONS * 2;

    private static final int COLOR_TRUE = 0xFF2ECC71;
    private static final int COLOR_FALSE = 0xFFE74C3C;

    private static final int DIVIDER = 0x22301E14;

    private static final int TARGET_MIN_H = 124;
    private static final float MIN_SCALE = 0.78f;


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

    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_PAD = 2;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;
    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;

    private static final int MARQUEE_GAP_PX = 16;
    private static final double MARQUEE_SPEED_PX_PER_SEC = 15.0;

    private static final int HINT_TEXT_COLOR = 0x7A2B1A10;
    private static final int HINT_LINE_GAP = 3;

    private static final int TIP_BG = 0xE61A120C;
    private static final int TIP_BORDER = 0xAA301E14;
    private static final int TIP_TEXT = 0xF2E8D5;
    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

    private static final Component SHIFT_HINT_LINE_1 =
            Component.translatable("gui.wildex.hint.shift_details");

    private static final Component SHIFT_HINT_LINE_2 =
            Component.translatable("gui.wildex.hint.shift_hover");
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

    private record Dims(OptionalDouble hitboxWidth, OptionalDouble hitboxHeight, OptionalDouble eyeHeight) {
    }

    private record TooltipRequest(List<Component> lines) {
    }

    private static final Map<ResourceLocation, Dims> DIMS_CACHE = new HashMap<>();
    private static final Dims EMPTY_DIMS = new Dims(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
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
    private static int statsScrollPx = 0;
    private static int statsViewportH = 1;
    private static int statsContentH = 1;
    private static boolean statsHasScrollbar = false;
    private static boolean statsDraggingScrollbar = false;
    private static int statsDragOffsetY = 0;
    private static int statsViewportScreenX0 = 0;
    private static int statsViewportScreenY0 = 0;
    private static int statsViewportScreenX1 = 0;
    private static int statsViewportScreenY1 = 0;
    private static int statsBarScreenX0 = 0;
    private static int statsBarScreenY0 = 0;
    private static int statsBarScreenX1 = 0;
    private static int statsBarScreenY1 = 0;
    private static int statsThumbScreenH = 0;
    private static int statsThumbScreenY0 = 0;
    private static int statsThumbScreenY1 = 0;
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

    public void resetSpawnScroll() {
        spawnScrollPx = 0;
        spawnViewportH = 1;
        spawnContentH = 1;
        spawnHasScrollbar = false;
        spawnDraggingScrollbar = false;
    }

    public void resetStatsScroll() {
        statsScrollPx = 0;
        statsViewportH = 1;
        statsContentH = 1;
        statsHasScrollbar = false;
        statsDraggingScrollbar = false;
    }

    public void resetLootScroll() {
        lootScrollPx = 0;
        lootViewportH = 1;
        lootContentH = 1;
        lootHasScrollbar = false;
        lootDraggingScrollbar = false;
    }

    public void scrollSpawn(double scrollY) {
        int max = Math.max(0, spawnContentH - spawnViewportH);
        int step = 10;
        int next = spawnScrollPx - (int) Math.round(scrollY * (double) step);
        if (next < 0) next = 0;
        if (next > max) next = max;
        spawnScrollPx = next;
    }

    public boolean handleSpawnMouseClicked(int mouseX, int mouseY, int button, WildexScreenState state) {
        if (button != 0) return false;
        if (state == null) return false;

        if (isInside(mouseX, mouseY, spawnFilterBiomeX0, spawnFilterBiomeY0, spawnFilterBiomeX1, spawnFilterBiomeY1)) {
            state.toggleSpawnFilterNatural();
            return true;
        }
        if (isInside(mouseX, mouseY, spawnFilterStructureX0, spawnFilterStructureY0, spawnFilterStructureX1, spawnFilterStructureY1)) {
            state.toggleSpawnFilterStructures();
            return true;
        }

        if (!spawnHasScrollbar) return false;
        if (!isInside(mouseX, mouseY, spawnBarScreenX0, spawnBarScreenY0, spawnBarScreenX1, spawnBarScreenY1)) return false;

        if (isInside(mouseX, mouseY, spawnBarScreenX0, spawnThumbScreenY0, spawnBarScreenX1, spawnThumbScreenY1)) {
            spawnDraggingScrollbar = true;
            spawnDragOffsetY = mouseY - spawnThumbScreenY0;
            return true;
        }

        setSpawnScrollFromThumbTop(mouseY - (spawnThumbScreenH / 2));
        return true;
    }

    @SuppressWarnings("unused")
    public boolean handleSpawnMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        if (!spawnDraggingScrollbar) return false;
        setSpawnScrollFromThumbTop(mouseY - spawnDragOffsetY);
        return true;
    }

    public boolean handleSpawnMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = spawnDraggingScrollbar;
        spawnDraggingScrollbar = false;
        return wasDragging;
    }

    public boolean scrollStats(int mouseX, int mouseY, double scrollY) {
        if (!statsHasScrollbar) return false;
        if (!isInside(mouseX, mouseY, statsViewportScreenX0, statsViewportScreenY0, statsViewportScreenX1, statsViewportScreenY1)) {
            return false;
        }
        int max = Math.max(0, statsContentH - statsViewportH);
        int step = 10;
        int next = statsScrollPx - (int) Math.round(scrollY * (double) step);
        if (next < 0) next = 0;
        if (next > max) next = max;
        statsScrollPx = next;
        return true;
    }

    public boolean handleStatsMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !statsHasScrollbar) return false;
        if (!isInside(mouseX, mouseY, statsBarScreenX0, statsBarScreenY0, statsBarScreenX1, statsBarScreenY1)) return false;
        if (isInside(mouseX, mouseY, statsBarScreenX0, statsThumbScreenY0, statsBarScreenX1, statsThumbScreenY1)) {
            statsDraggingScrollbar = true;
            statsDragOffsetY = mouseY - statsThumbScreenY0;
            return true;
        }
        setStatsScrollFromThumbTop(mouseY - (statsThumbScreenH / 2));
        return true;
    }

    @SuppressWarnings("unused")
    public boolean handleStatsMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0 || !statsDraggingScrollbar) return false;
        setStatsScrollFromThumbTop(mouseY - statsDragOffsetY);
        return true;
    }

    public boolean handleStatsMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = statsDraggingScrollbar;
        statsDraggingScrollbar = false;
        return wasDragging;
    }

    public boolean scrollLoot(int mouseX, int mouseY, double scrollY) {
        if (!lootHasScrollbar) return false;
        if (!isInside(mouseX, mouseY, lootViewportScreenX0, lootViewportScreenY0, lootViewportScreenX1, lootViewportScreenY1)) {
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

    public boolean handleLootMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !lootHasScrollbar) return false;
        if (!isInside(mouseX, mouseY, lootBarScreenX0, lootBarScreenY0, lootBarScreenX1, lootBarScreenY1)) return false;
        if (isInside(mouseX, mouseY, lootBarScreenX0, lootThumbScreenY0, lootBarScreenX1, lootThumbScreenY1)) {
            lootDraggingScrollbar = true;
            lootDragOffsetY = mouseY - lootThumbScreenY0;
            return true;
        }
        setLootScrollFromThumbTop(mouseY - (lootThumbScreenH / 2));
        return true;
    }

    @SuppressWarnings("unused")
    public boolean handleLootMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0 || !lootDraggingScrollbar) return false;
        setLootScrollFromThumbTop(mouseY - lootDragOffsetY);
        return true;
    }

    public boolean handleLootMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = lootDraggingScrollbar;
        lootDraggingScrollbar = false;
        return wasDragging;
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexMobData data,
            int inkColor
    ) {
        render(graphics, font, area, state, data, inkColor, -1, -1);
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            WildexMobData data,
            int inkColor,
            int mouseX,
            int mouseY
    ) {
        if (graphics == null || font == null || area == null || state == null || data == null) return;

        String selectedIdStr = state.selectedMobId();
        if (selectedIdStr == null || selectedIdStr.isBlank()) return;

        ResourceLocation mobRl = ResourceLocation.tryParse(selectedIdStr);
        if (mobRl == null) return;

        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        float s = computeContentScale(area.h());
        int lw = toLogical(area.w(), s);
        int lh = toLogical(area.h(), s);

        graphics.enableScissor(x0, y0, x1, y1);
        TooltipRequest tooltip = null;
        try {
            graphics.pose().pushPose();
            graphics.pose().translate(x0, y0, 0);
            graphics.pose().scale(s, s, 1.0f);

            WildexScreenLayout.Area local = new WildexScreenLayout.Area(0, 0, lw, lh);

            if (WildexClientConfigView.hiddenMode() && !WildexDiscoveryCache.isDiscovered(mobRl)) {
                renderLockedHint(graphics, font, local);
                graphics.pose().popPose();
                return;
            }

            boolean shiftDown = Screen.hasShiftDown();

            switch (state.selectedTab()) {
                case STATS -> {
                    tooltip = renderStatsWithScroll(
                            graphics, font, local, state.selectedMobId(), data.stats(), inkColor,
                            mouseX, mouseY, x0, y0, s, shiftDown
                    );
                }
                case LOOT -> renderLoot(graphics, font, local, mobRl, inkColor, x0, y0, s);
                case SPAWNS -> tooltip = renderSpawns(graphics, font, local, mobRl, state, inkColor, x0, y0, s, mouseX, mouseY);
                case MISC -> renderInfoTraits(graphics, font, local, state, inkColor, x0, y0, s);
            }

            graphics.pose().popPose();

            if (tooltip != null && mouseX >= 0 && mouseY >= 0 && !tooltip.lines().isEmpty()) {
                renderPanelTooltip(graphics, font, tooltip.lines(), mouseX, mouseY, x0, y0, area.w(), area.h());
            }
        } finally {
            graphics.disableScissor();
        }
    }

    @SuppressWarnings("unused")
    private static void renderShiftHint(GuiGraphics g, Font font, WildexScreenLayout.Area area) {
        int hintColor = WildexUiTheme.current().inkMuted();
        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.x() + area.w();
        int y1 = area.y() + area.h();

        int maxW = Math.max(1, area.w() - (PAD_X * 2));

        List<FormattedCharSequence> line1 = font.split(SHIFT_HINT_LINE_1, maxW);
        FormattedCharSequence line2 = SHIFT_HINT_LINE_2.getVisualOrderText();

        int lineH = font.lineHeight + 1;
        int totalH = (line1.size() + 1) * lineH;

        int y = y1 - PAD_Y - totalH;
        if (y < y0 + PAD_Y) y = y0 + PAD_Y;

        for (FormattedCharSequence l : line1) {
            int w = font.width(l);
            int x = x1 - PAD_X - w;
            if (x < x0 + PAD_X) x = x0 + PAD_X;

            g.drawString(font, l, x, y, hintColor, false);
            y += lineH;
        }

        int w2 = font.width(line2);
        int x2 = x1 - PAD_X - w2;
        if (x2 < x0 + PAD_X) x2 = x0 + PAD_X;

        g.drawString(font, line2, x2, y, hintColor, false);
    }

    private static TooltipRequest renderStatsWithScroll(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            String selectedMobId,
            WildexStatsData s,
            int inkColor,
            int mouseX,
            int mouseY,
            int screenOriginX,
            int screenOriginY,
            float scale,
            boolean shiftDown
    ) {
        int x = area.x();
        int y = area.y();
        int w = area.w();
        int h = area.h();
        int rightLimitX = x + w - PAD_RIGHT;

        int maxW = Math.max(1, w - (PAD_X * 2));
        List<FormattedCharSequence> hintL1 = font.split(SHIFT_HINT_LINE_1, maxW);
        int hintLineH = font.lineHeight + 1;
        int hintTotalH = (hintL1.size() + 1) * hintLineH;
        int hintBlockH = hintTotalH + PAD_Y + 2;

        int dividerShiftDown = 5;
        int dividerY = Math.min((y + h) - 2, (y + h) - hintBlockH - 2 + dividerShiftDown);
        int viewportX = x;
        int viewportY = y;
        int viewportW = w;
        int viewportH = Math.max(24, dividerY - viewportY - 1);

        int line = Math.max(10, font.lineHeight + 2);
        int estimatedContentH = Math.max(viewportH, PAD_Y + (line * 9) + 2);
        statsViewportH = viewportH;
        statsContentH = estimatedContentH;
        int maxScroll = Math.max(0, statsContentH - statsViewportH);
        boolean showStatsScrollbar = maxScroll > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (!showStatsScrollbar) {
            statsScrollPx = 0;
        } else if (statsScrollPx > maxScroll) {
            statsScrollPx = maxScroll;
        }

        int sx0 = toScreenX(screenOriginX, scale, viewportX);
        int sy0 = toScreenY(screenOriginY, scale, viewportY);
        int sx1 = toScreenX(screenOriginX, scale, viewportX + viewportW);
        int sy1 = toScreenY(screenOriginY, scale, viewportY + viewportH);
        statsViewportScreenX0 = sx0;
        statsViewportScreenY0 = sy0;
        statsViewportScreenX1 = sx1;
        statsViewportScreenY1 = sy1;

        g.enableScissor(sx0, sy0, sx1, sy1);
        TooltipRequest tooltip;
        try {
            WildexScreenLayout.Area statsArea = new WildexScreenLayout.Area(viewportX, viewportY - statsScrollPx, viewportW, statsContentH);
            tooltip = shiftDown
                    ? renderStats(g, font, statsArea, selectedMobId, s, inkColor, mouseX, mouseY, screenOriginX, screenOriginY, scale)
                    : renderStats(g, font, statsArea, selectedMobId, s, inkColor, -1, -1, screenOriginX, screenOriginY, scale);
        } finally {
            g.disableScissor();
        }

        int lineY = viewportY + viewportH + 1;
        if (lineY < y + h - 1) {
            g.fill(x + PAD_X, lineY, rightLimitX, lineY + 1, DIVIDER);
        }

        int hintY = lineY + 3;
        renderShiftHintAt(g, font, area, hintY, y + h - PAD_Y - hintTotalH);

        if (showStatsScrollbar) {
            int barX0 = rightLimitX - SCROLLBAR_W;
            int barY0 = viewportY;
            int barY1 = viewportY + viewportH;
            g.fill(barX0, barY0, rightLimitX, barY1, SCROLLBAR_BG);

            int thumbH = Math.max(12, Math.round((statsViewportH / (float) statsContentH) * statsViewportH));
            if (thumbH > statsViewportH) thumbH = statsViewportH;
            int denom = Math.max(1, statsContentH - statsViewportH);
            float t = statsScrollPx / (float) denom;
            int travel = statsViewportH - thumbH;
            int thumbY0 = viewportY + Math.round(travel * t);
            int thumbY1 = thumbY0 + thumbH;
            g.fill(barX0, thumbY0, rightLimitX, thumbY1, SCROLLBAR_THUMB);

            statsHasScrollbar = true;
            cacheStatsScrollbarRect(screenOriginX, screenOriginY, scale, barX0, barY0, rightLimitX, barY1, thumbY0, thumbY1);
        } else {
            statsHasScrollbar = false;
            statsDraggingScrollbar = false;
        }

        return tooltip;
    }

    private static void renderShiftHintAt(GuiGraphics g, Font font, WildexScreenLayout.Area area, int preferredY, int minY) {
        int hintColor = WildexUiTheme.current().inkMuted();
        int x0 = area.x();
        int x1 = area.x() + area.w();
        int maxW = Math.max(1, area.w() - (PAD_X * 2));
        List<FormattedCharSequence> line1 = font.split(SHIFT_HINT_LINE_1, maxW);
        FormattedCharSequence line2 = SHIFT_HINT_LINE_2.getVisualOrderText();
        int lineH = font.lineHeight + 1;
        int y = Math.max(minY, preferredY);
        for (FormattedCharSequence l : line1) {
            int w = font.width(l);
            int x = x1 - PAD_X - w;
            if (x < x0 + PAD_X) x = x0 + PAD_X;
            g.drawString(font, l, x, y, hintColor, false);
            y += lineH;
        }
        int w2 = font.width(line2);
        int x2 = x1 - PAD_X - w2;
        if (x2 < x0 + PAD_X) x2 = x0 + PAD_X;
        g.drawString(font, line2, x2, y, hintColor, false);
    }

    private static void renderLockedHint(GuiGraphics g, Font font, WildexScreenLayout.Area area) {
        int x = area.x() + PAD_X;
        int yTop = area.y() + PAD_Y;
        boolean modern = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN;
        int lockedColor = modern ? 0xFFD9F6FF : HINT_TEXT_COLOR;

        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int maxW = Math.max(1, rightLimitX - x);

        int lineH = Math.max(10, font.lineHeight + HINT_LINE_GAP);

        String[] lines = {
                tr("gui.wildex.locked.line1"),
                tr("gui.wildex.locked.line2"),
                tr("gui.wildex.locked.line3")
        };

        int totalH = lines.length * lineH;
        int startY = yTop + Math.max(0, (area.h() - (PAD_Y * 2) - totalH) / 2);

        for (String line : lines) {
            String clipped = clipToWidth(font, line, maxW);
            g.drawString(font, clipped, x, startY, lockedColor, false);
            startY += lineH;
        }
    }

    private static float computeContentScale(int h) {
        if (h <= 0) return MIN_SCALE;
        float fitScale = (float) h / (float) TARGET_MIN_H;
        if (fitScale > 1.0f) return 1.0f;
        if (fitScale < MIN_SCALE) return MIN_SCALE;
        return fitScale;
    }

    private static int toLogical(int px, float s) {
        if (s <= 0.0f) return px;
        return Math.max(1, (int) Math.floor(px / (double) s));
    }

    private static void renderLoot(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            ResourceLocation mobId,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int x = area.x() + PAD_X;
        int yTop = area.y() + PAD_Y;
        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int maxY = area.y() + area.h() - PAD_Y;

        int viewportX = area.x();
        int viewportY = yTop;
        int viewportW = area.w();
        int viewportH = Math.max(1, maxY - yTop);

        List<S2CMobLootPayload.LootLine> lines = WildexLootCache.get(mobId);
        if (lines.isEmpty()) {
            g.drawString(font, tr("gui.wildex.loot.none"), x, yTop, inkColor, false);
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

        int sx0 = toScreenX(screenOriginX, scale, viewportX);
        int sy0 = toScreenY(screenOriginY, scale, viewportY);
        int sx1 = toScreenX(screenOriginX, scale, viewportX + viewportW);
        int sy1 = toScreenY(screenOriginY, scale, viewportY + viewportH);
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
                    drawMarqueeIfNeeded(g, font, line, textX, y + 4, textW, inkColor, screenOriginX, screenOriginY, scale);
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
                cacheLootScrollbarRect(screenOriginX, screenOriginY, scale, barX0, barY0, rightLimitX, barY1, thumbY0, thumbY1);
            } else {
                lootHasScrollbar = false;
                lootDraggingScrollbar = false;
            }
        } finally {
            g.disableScissor();
        }
    }

    private static TooltipRequest renderSpawns(
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
        TooltipRequest hoverTip = null;
        int x = area.x() + PAD_X;
        int yTop = area.y() + 2;

        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int maxY = area.y() + area.h() - PAD_Y;

        int filterH = Math.max(10, font.lineHeight + 4);
        int filterY = yTop;
        int filterTextY = filterY + Math.max(0, (filterH - font.lineHeight) / 2) + 1;

        String biomeLabel = tr("gui.wildex.spawn.filter.biomes.short");
        String structureLabel = tr("gui.wildex.spawn.filter.structures.short");

        int biomeW = font.width(biomeLabel) + (SPAWN_FILTER_PAD_X * 2);
        int structureW = font.width(structureLabel) + (SPAWN_FILTER_PAD_X * 2);

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
            if (isInside(mouseX, mouseY, spawnFilterBiomeX0, spawnFilterBiomeY0, spawnFilterBiomeX1, spawnFilterBiomeY1)) {
                hoverTip = tooltipLines("tooltip.wildex.spawn.filter.biomes");
            } else if (isInside(mouseX, mouseY, spawnFilterStructureX0, spawnFilterStructureY0, spawnFilterStructureX1, spawnFilterStructureY1)) {
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
            g.drawString(font, tr("gui.wildex.spawn.none"), x, yTop, inkColor, false);
            spawnHasScrollbar = false;
            spawnDraggingScrollbar = false;
            return hoverTip;
        }

        int lineH = Math.max(10, font.lineHeight + SPAWN_LINE_GAP);
        int titleH = Math.max(10, font.lineHeight + 3);

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
            for (S2CMobSpawnsPayload.StructureSection s : structureSections) {
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

        int contentSx0 = toScreenX(screenOriginX, scale, area.x());
        int contentSy0 = toScreenY(screenOriginY, scale, yTop);
        int contentSx1 = toScreenX(screenOriginX, scale, area.x() + area.w());
        int contentSy1 = toScreenY(screenOriginY, scale, maxY);

        g.enableScissor(contentSx0, contentSy0, contentSx1, contentSy1);
        try {
            if (!naturalSections.isEmpty()) {
                if (y + font.lineHeight > yTop - lineH && y < maxY) {
            drawSpawnHeading(g, font, x, y, rightLimitX, tr("gui.wildex.spawn.heading.natural"));
                }
                y += titleH;

                for (S2CMobSpawnsPayload.DimSection s : naturalSections) {
                    if (y + font.lineHeight > yTop - lineH && y < maxY) {
                        String title = formatDimensionTitle(s.dimensionId());
                        drawSpawnSubheading(g, font, title, x, y, rightLimitX, screenOriginX, screenOriginY, scale);
                    }
                    y += titleH;

                    List<ResourceLocation> biomes = s.biomeIds() == null ? List.of() : s.biomeIds();
                    for (ResourceLocation b : biomes) {
                        if (y + font.lineHeight >= yTop - lineH && y < maxY) {
                            String line = b.toString();
                            drawMarqueeIfNeeded(g, font, line, x, y, textMaxW, inkColor, screenOriginX, screenOriginY, scale);
                        }
                        y += lineH;
                    }
                }
            }

            if (!structureSections.isEmpty()) {
                if (!naturalSections.isEmpty()) {
                    y += SPAWN_GROUP_GAP;
                }

                if (y + font.lineHeight > yTop - lineH && y < maxY) {
            drawSpawnHeading(g, font, x, y, rightLimitX, tr("gui.wildex.spawn.heading.structures"));
                }
                y += titleH;

                for (S2CMobSpawnsPayload.StructureSection s : structureSections) {
                    if (y + font.lineHeight > yTop - lineH && y < maxY) {
                        String title = formatStructureTitle(s.structureId());
                        drawMarqueeIfNeeded(g, font, title, x, y, textMaxW, inkColor, screenOriginX, screenOriginY, scale);
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
        boolean modern = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN;
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

        g.drawString(font, label, x0 + SPAWN_FILTER_PAD_X, textY, fg, false);

        if (!active) {
            // Small top-right "off" badge to keep the symbol readable.
            int size = 5;
            int ix1 = x1 - 3;
            int ix0 = ix1 - (size - 1);
            int iy0 = y0 + 2;
            int iy1 = iy0 + (size - 1);

            for (int i = 0; i < size; i++) {
                g.fill(ix0 + i, iy0 + i, ix0 + i + 1, iy0 + i + 1, offCross);
                g.fill(ix1 - i, iy0 + i, ix1 - i + 1, iy0 + i + 1, offCross);
            }
        }
    }

    private static void drawSpawnHeading(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int rightLimitX,
            String text
    ) {
        if (text == null || text.isBlank()) return;
        boolean modern = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN;
        int headingColor = modern ? 0xFFB7EEFF : SPAWN_HEADING_COLOR;
        int headingRule = modern ? 0x6658D7FF : SPAWN_HEADING_RULE;
        g.drawString(font, text, x, y, headingColor, false);
        int ruleY = y + font.lineHeight + 1;
        int ruleX1 = rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD;
        if (ruleX1 > x) {
            g.fill(x, ruleY, ruleX1, ruleY + 1, headingRule);
        }
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
        boolean modern = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN;
        int subheadingColor = modern ? 0xFFD9F6FF : SPAWN_SUBHEADING_COLOR;
        int subheadingRule = modern ? 0x554FCBF3 : SPAWN_SUBHEADING_RULE;
        int maxW = Math.max(1, (rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD) - x);
        drawMarqueeIfNeeded(g, font, text, x, y, maxW, subheadingColor, screenOriginX, screenOriginY, scale);
        int ruleY = y + font.lineHeight + 1;
        int ruleX1 = rightLimitX - SCROLLBAR_W - SCROLLBAR_PAD;
        if (ruleX1 > x) {
            g.fill(x, ruleY, ruleX1, ruleY + 1, subheadingRule);
        }
    }

    private static void cacheSpawnFilterRects(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int biomeX0,
            int biomeX1,
            int y0,
            int y1,
            int structureX0,
            int structureX1
    ) {
        spawnFilterBiomeX0 = toScreenX(screenOriginX, scale, biomeX0);
        spawnFilterBiomeY0 = toScreenY(screenOriginY, scale, y0);
        spawnFilterBiomeX1 = toScreenX(screenOriginX, scale, biomeX1);
        spawnFilterBiomeY1 = toScreenY(screenOriginY, scale, y1);

        spawnFilterStructureX0 = toScreenX(screenOriginX, scale, structureX0);
        spawnFilterStructureY0 = toScreenY(screenOriginY, scale, y0);
        spawnFilterStructureX1 = toScreenX(screenOriginX, scale, structureX1);
        spawnFilterStructureY1 = toScreenY(screenOriginY, scale, y1);
    }

    private static void cacheSpawnScrollbarRect(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int barX0,
            int barY0,
            int barX1,
            int barY1,
            int thumbY0,
            int thumbY1
    ) {
        spawnBarScreenX0 = toScreenX(screenOriginX, scale, barX0);
        spawnBarScreenY0 = toScreenY(screenOriginY, scale, barY0);
        spawnBarScreenX1 = toScreenX(screenOriginX, scale, barX1);
        spawnBarScreenY1 = toScreenY(screenOriginY, scale, barY1);

        spawnThumbScreenY0 = toScreenY(screenOriginY, scale, thumbY0);
        spawnThumbScreenY1 = toScreenY(screenOriginY, scale, thumbY1);
        spawnThumbScreenH = Math.max(1, spawnThumbScreenY1 - spawnThumbScreenY0);
    }

    private static void cacheStatsScrollbarRect(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int barX0,
            int barY0,
            int barX1,
            int barY1,
            int thumbY0,
            int thumbY1
    ) {
        statsBarScreenX0 = toScreenX(screenOriginX, scale, barX0);
        statsBarScreenY0 = toScreenY(screenOriginY, scale, barY0);
        statsBarScreenX1 = toScreenX(screenOriginX, scale, barX1);
        statsBarScreenY1 = toScreenY(screenOriginY, scale, barY1);

        statsThumbScreenY0 = toScreenY(screenOriginY, scale, thumbY0);
        statsThumbScreenY1 = toScreenY(screenOriginY, scale, thumbY1);
        statsThumbScreenH = Math.max(1, statsThumbScreenY1 - statsThumbScreenY0);
    }

    private static void cacheLootScrollbarRect(
            int screenOriginX,
            int screenOriginY,
            float scale,
            int barX0,
            int barY0,
            int barX1,
            int barY1,
            int thumbY0,
            int thumbY1
    ) {
        lootBarScreenX0 = toScreenX(screenOriginX, scale, barX0);
        lootBarScreenY0 = toScreenY(screenOriginY, scale, barY0);
        lootBarScreenX1 = toScreenX(screenOriginX, scale, barX1);
        lootBarScreenY1 = toScreenY(screenOriginY, scale, barY1);

        lootThumbScreenY0 = toScreenY(screenOriginY, scale, thumbY0);
        lootThumbScreenY1 = toScreenY(screenOriginY, scale, thumbY1);
        lootThumbScreenH = Math.max(1, lootThumbScreenY1 - lootThumbScreenY0);
    }

    private static void setSpawnScrollFromThumbTop(int desiredThumbTop) {
        if (!spawnHasScrollbar) return;
        int barTravel = Math.max(1, (spawnBarScreenY1 - spawnBarScreenY0) - spawnThumbScreenH);
        int clampedTop = desiredThumbTop;
        int minTop = spawnBarScreenY0;
        int maxTop = spawnBarScreenY0 + barTravel;
        if (clampedTop < minTop) clampedTop = minTop;
        if (clampedTop > maxTop) clampedTop = maxTop;

        float t = (clampedTop - spawnBarScreenY0) / (float) barTravel;
        int maxScroll = Math.max(0, spawnContentH - spawnViewportH);
        spawnScrollPx = Math.round(maxScroll * t);
    }

    private static void setStatsScrollFromThumbTop(int desiredThumbTop) {
        if (!statsHasScrollbar) return;
        int barTravel = Math.max(1, (statsBarScreenY1 - statsBarScreenY0) - statsThumbScreenH);
        int minTop = statsBarScreenY0;
        int maxTop = statsBarScreenY0 + barTravel;
        int clampedTop = Math.max(minTop, Math.min(maxTop, desiredThumbTop));
        float t = (clampedTop - statsBarScreenY0) / (float) barTravel;
        int maxScroll = Math.max(0, statsContentH - statsViewportH);
        statsScrollPx = Math.round(maxScroll * t);
    }

    private static void setLootScrollFromThumbTop(int desiredThumbTop) {
        if (!lootHasScrollbar) return;
        int barTravel = Math.max(1, (lootBarScreenY1 - lootBarScreenY0) - lootThumbScreenH);
        int minTop = lootBarScreenY0;
        int maxTop = lootBarScreenY0 + barTravel;
        int clampedTop = Math.max(minTop, Math.min(maxTop, desiredThumbTop));
        float t = (clampedTop - lootBarScreenY0) / (float) barTravel;
        int maxScroll = Math.max(0, lootContentH - lootViewportH);
        lootScrollPx = Math.round(maxScroll * t);
    }

    private static boolean isInside(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    private static int toScreenX(int originX, float scale, int localX) {
        return originX + Math.round(localX * scale);
    }

    private static int toScreenY(int originY, float scale, int localY) {
        return originY + Math.round(localY * scale);
    }

    private static void drawMarqueeIfNeeded(
            GuiGraphics g,
            Font font,
            String text,
            int x,
            int y,
            int maxW,
            int color,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        if (text == null || text.isBlank()) return;

        int w = font.width(text);
        if (w <= maxW) {
            g.drawString(font, text, x, y, color, false);
            return;
        }

        int sx0 = screenOriginX + Math.round(x * scale);
        int sy0 = screenOriginY + Math.round(y * scale);
        int sx1 = sx0 + Math.max(1, Math.round(maxW * scale));
        int sy1 = sy0 + Math.max(1, Math.round(font.lineHeight * scale));

        int gap = Math.max(8, MARQUEE_GAP_PX);
        int loop = w + gap;

        long ms = Util.getMillis();
        double pxPerMs = MARQUEE_SPEED_PX_PER_SEC / 1000.0;

        int phase = (text.hashCode() & 0x7fffffff) % loop;
        float off = (float) ((((ms * pxPerMs) + phase) % (double) loop));

        g.enableScissor(sx0, sy0, sx1, sy1);
        try {
            g.pose().pushPose();
            g.pose().translate(-off, 0.0f, 0.0f);

            g.drawString(font, text, x, y, color, false);
            g.drawString(font, text, x + loop, y, color, false);

            g.pose().popPose();
        } finally {
            g.disableScissor();
        }
    }

    private static String formatDimensionTitle(ResourceLocation dimId) {
        if (dimId == null) return tr("gui.wildex.dimension.unknown");
        String s = dimId.toString();
        if (s.equals("minecraft:overworld")) return tr("gui.wildex.dimension.overworld");
        if (s.equals("minecraft:the_nether")) return tr("gui.wildex.dimension.nether");
        if (s.equals("minecraft:the_end")) return tr("gui.wildex.dimension.end");
        return s;
    }

    private static String formatStructureTitle(ResourceLocation structureId) {
        if (structureId == null) return tr("gui.wildex.structure.unknown");
        return structureId.toString();
    }

    private static String formatCount(int min, int max) {
        int a = Math.max(0, min);
        int b = Math.max(0, max);
        if (a <= 0 && b <= 0) return "";
        if (a == b) return "x" + a;
        return "x" + a + "–" + b;
    }

    private static void renderInfoTraits(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int x = area.x() + PAD_X;
        int y = area.y() + PAD_Y;

        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int maxY = area.y() + area.h() - PAD_Y;

        int line = Math.max(10, font.lineHeight + 3);
        int markGap = 6;

        EntityType<?> type = resolveSelectedType(state.selectedMobId());
        if (type == null) return;

        String tick = "✔";
        String cross = "✖";

        int markW = Math.max(font.width(tick), font.width(cross));
        int markColW = Math.max(10, markW + 2);
        int markColX0 = rightLimitX - markColW;
        int dividerX = markColX0 - Math.max(2, COL_GAP / 2);
        if (dividerX > x + 8) {
            g.fill(dividerX, y, dividerX + 1, maxY, DIVIDER);
        }
        int labelMaxW = Math.max(1, (dividerX - 2) - x);

        for (int i = 0; i < INFO_TRAITS.size(); i++) {
            if (y + font.lineHeight > maxY) break;

            TraitLine t = INFO_TRAITS.get(i);
            boolean has = evalTrait(type, t);

            String mark = has ? tick : cross;
            int markColor = has ? COLOR_TRUE : COLOR_FALSE;

            drawMarqueeIfNeeded(g, font, tr(t.display()), x, y, labelMaxW, inkColor, screenOriginX, screenOriginY, scale);

            int markX = markColX0 + Math.max(0, (markColW - font.width(mark)) / 2);
            g.drawString(font, mark, markX, y, markColor, false);

            int dividerY = y + font.lineHeight + 1;
            if (i < INFO_TRAITS.size() - 1 && dividerY + 1 < maxY) {
                g.fill(x, dividerY, rightLimitX, dividerY + 1, DIVIDER);
            }

            y += line;
        }
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

    private static void renderStatsNoTooltip(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            String selectedMobId,
            WildexStatsData s,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        renderStats(g, font, area, selectedMobId, s, inkColor, -1, -1, screenOriginX, screenOriginY, scale);
    }

    private static TooltipRequest renderStats(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            String selectedMobId,
            WildexStatsData s,
            int inkColor,
            int mouseX,
            int mouseY,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int x = area.x() + PAD_X;
        int y = area.y() + PAD_Y;

        int innerW = Math.max(1, area.w() - PAD_X - PAD_RIGHT);
        int innerH = Math.max(1, area.h() - PAD_Y * 2);

        int maxY = (area.y() + PAD_Y) + innerH;

        int contentW = Math.max(1, innerW);
        int maxLabelBySpace = Math.max(24, contentW - COL_GAP - STATS_MIN_VALUE_COL_W);
        int labelColW = Math.round(contentW * STATS_LABEL_COL_RATIO);
        labelColW = Math.max(STATS_LABEL_COL_MIN, Math.min(labelColW, STATS_LABEL_COL_MAX));
        labelColW = Math.min(labelColW, maxLabelBySpace);

        int valueX = x + labelColW + COL_GAP;
        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int valueW = Math.max(1, rightLimitX - valueX);

        int line = Math.max(10, font.lineHeight + 2);
        int dividerX = x + labelColW + (COL_GAP / 2);
        int dividerStartY = y;

        TooltipRequest tooltip = null;

        int mxL = -1;
        int myL = -1;
        if (mouseX >= 0 && mouseY >= 0) {
            double fx = (mouseX - screenOriginX) / (double) scale;
            double fy = (mouseY - screenOriginY) / (double) scale;
            mxL = (int) Math.floor(fx);
            myL = (int) Math.floor(fy);
        }

        int y0 = y;
        y = drawHeartsLine(g, font, x, y, labelColW, valueX, valueW, maxY, tr("gui.wildex.stats.health"), s.maxHealth(), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.health");
        }

        y0 = y;
        y = drawArmorLine(g, font, x, y, labelColW, valueX, valueW, maxY, tr("gui.wildex.stats.armor"), s.armor(), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines(
                    "tooltip.wildex.stats.armor.1",
                    "tooltip.wildex.stats.armor.2"
            );
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.move_speed"), fmtOpt(s.movementSpeed()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.move_speed");
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.attack_damage"), fmtOpt(s.attackDamage()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.attack_damage");
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.follow_range"), fmtOptWithUnit(s.followRange(), tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.follow_range");
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.knockback_res"), fmtOpt(s.knockbackResistance()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines(
                    "tooltip.wildex.stats.knockback_res.1",
                    "tooltip.wildex.stats.knockback_res.2"
            );
        }

        Dims dims = resolveDims(selectedMobId);

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.hitbox_width"), fmtOptWithUnit(dims.hitboxWidth(), tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.hitbox_width");
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.hitbox_height"), fmtOptWithUnit(dims.hitboxHeight(), tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.hitbox_height");
        }

        y0 = y;
        drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, tr("gui.wildex.stats.eye_height"), fmtOptWithUnit(dims.eyeHeight(), tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines(
                    "tooltip.wildex.stats.eye_height.1",
                    "tooltip.wildex.stats.eye_height.2"
            );
        }

        int dividerEndY = Math.min(maxY, y + line - 1);
        if (dividerX > x + 4 && dividerX < rightLimitX - 4 && dividerEndY > dividerStartY) {
            g.fill(dividerX, dividerStartY, dividerX + 1, dividerEndY, DIVIDER);
        }

        return tooltip;
    }

    private static boolean isHover(int mx, int my, int x, int y, int w, int h) {
        if (mx < 0 || my < 0) return false;
        return mx >= x && mx < (x + w) && my >= y && my < (y + h);
    }

    private static TooltipRequest tooltipLines(String... keys) {
        ArrayList<Component> out = new ArrayList<>();
        for (String key : keys) {
            if (key != null && !key.isBlank()) out.add(Component.translatable(key));
        }
        return out.isEmpty() ? null : new TooltipRequest(out);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static void renderPanelTooltip(
            GuiGraphics g,
            Font font,
            List<Component> lines,
            int mouseX,
            int mouseY,
            int panelX,
            int panelY,
            int panelW,
            int panelH
    ) {
        int maxW = Math.max(80, Math.min(TIP_MAX_W, panelW - (TIP_PAD * 2) - 2));
        ArrayList<FormattedCharSequence> wrapped = new ArrayList<>();

        for (Component c : lines) {
            if (c == null) continue;
            List<FormattedCharSequence> parts = font.split(c, maxW);
            wrapped.addAll(parts);
        }

        if (wrapped.isEmpty()) return;

        int textW = 0;
        for (FormattedCharSequence s : wrapped) textW = Math.max(textW, font.width(s));
        int textH = wrapped.size() * font.lineHeight + Math.max(0, (wrapped.size() - 1) * TIP_LINE_GAP);

        int boxW = textW + TIP_PAD * 2;
        int boxH = textH + TIP_PAD * 2;

        int x = mouseX + 10;
        int y = mouseY + 10;

        int minX = panelX + 2;
        int maxX = (panelX + panelW) - boxW - 2;
        int minY = panelY + 2;
        int maxY = (panelY + panelH) - boxH - 2;

        if (x > maxX) x = maxX;
        if (x < minX) x = minX;
        if (y > maxY) y = maxY;
        if (y < minY) y = minY;

        int x0 = x;
        int y0 = y;
        int x1 = x + boxW;
        int y1 = y + boxH;

        g.fill(x0, y0, x1, y1, TIP_BG);

        g.fill(x0, y0, x1, y0 + 1, TIP_BORDER);
        g.fill(x0, y1 - 1, x1, y1, TIP_BORDER);
        g.fill(x0, y0, x0 + 1, y1, TIP_BORDER);
        g.fill(x1 - 1, y0, x1, y1, TIP_BORDER);

        int tx = x0 + TIP_PAD;
        int ty = y0 + TIP_PAD;

        for (FormattedCharSequence line : wrapped) {
            g.drawString(font, line, tx, ty, TIP_TEXT, false);
            ty += font.lineHeight + TIP_LINE_GAP;
        }
    }

    private static Dims resolveDims(String selectedMobId) {
        ResourceLocation rl = selectedMobId == null ? null : ResourceLocation.tryParse(selectedMobId);
        if (rl == null) return EMPTY_DIMS;

        Dims cached = DIMS_CACHE.get(rl);
        if (cached != null) return cached;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        if (type == null) {
            Dims out = EMPTY_DIMS;
            DIMS_CACHE.put(rl, out);
            return out;
        }

        OptionalDouble hitboxW;
        OptionalDouble hitboxH;
        try {
            EntityDimensions d = type.getDimensions();
            hitboxW = OptionalDouble.of(d.width());
            hitboxH = OptionalDouble.of(d.height());
        } catch (Throwable t) {
            hitboxW = OptionalDouble.empty();
            hitboxH = OptionalDouble.empty();
        }

        OptionalDouble eyeH = OptionalDouble.empty();
        try {
            var mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity e = WildexEntityFactory.tryCreate(type, mc.level);
                if (e != null) {
                    eyeH = OptionalDouble.of(e.getEyeHeight());
                    e.discard();
                }
            }
        } catch (Throwable t) {
            eyeH = OptionalDouble.empty();
        }

        Dims out = new Dims(hitboxW, hitboxH, eyeH);
        DIMS_CACHE.put(rl, out);
        return out;
    }

    private static int drawHeartsLine(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int labelW,
            int valueX,
            int valueW,
            int maxY,
            String label,
            OptionalDouble maxHealth,
            int inkColor,
            int lineHeight,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        if (y >= maxY) return y;

        drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        if (maxHealth.isEmpty()) {
            g.drawString(font, "—", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int hp = Math.max(0, (int) Math.round(maxHealth.getAsDouble()));
        double heartsTotal = hp / 2.0;

        int shownHp = Math.min(hp, MAX_HEART_HP);
        int full = shownHp / 2;
        boolean half = (shownHp % 2) == 1;

        int iconY = y - 1;
        if (iconY + ICON >= maxY) return y + lineHeight;

        int per = ICON + ICON_GAP;
        int fitIcons = Math.max(0, valueW / per);

        int iconsWanted = Math.min(MAX_HEART_ICONS, full + (half ? 1 : 0));
        boolean needsCompact = (hp > MAX_HEART_HP) || (fitIcons < iconsWanted);

        if (needsCompact) {
            String mult = "x" + String.format(Locale.ROOT, "%.1f", heartsTotal);

            if (fitIcons <= 0) {
                g.drawString(font, clipToWidth(font, mult, Math.max(1, valueW)), valueX, y, inkColor, false);
                return y + lineHeight;
            }

            g.blitSprite(HEART_FULL, valueX, iconY, ICON, ICON);

            int textX = valueX + per + 2;
            int maxTextW = Math.max(1, (valueX + valueW) - textX);
            g.drawString(font, clipToWidth(font, mult, maxTextW), textX, y, inkColor, false);
            return y + lineHeight;
        }

        for (int i = 0; i < full; i++) {
            int dx = valueX + i * per;
            if (dx + ICON > valueX + valueW) break;
            g.blitSprite(HEART_FULL, dx, iconY, ICON, ICON);
        }

        if (half) {
            int dx = valueX + full * per;
            if (dx + ICON <= valueX + valueW) {
                g.blitSprite(HEART_HALF, dx, iconY, ICON, ICON);
            }
        }

        return y + lineHeight;
    }

    private static int drawArmorLine(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int labelW,
            int valueX,
            int valueW,
            int maxY,
            String label,
            OptionalDouble armor,
            int inkColor,
            int lineHeight,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        if (y >= maxY) return y;

        drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        if (armor.isEmpty()) {
            g.drawString(font, "—", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int a = Math.max(0, (int) Math.round(armor.getAsDouble()));
        if (a <= 0) {
            g.drawString(font, "—", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int full = a / 2;
        boolean half = (a % 2) == 1;

        int iconY = y - 1;
        if (iconY + ICON >= maxY) return y + lineHeight;

        int per = ICON + ICON_GAP;
        int maxIconsFit = Math.max(0, valueW / per);
        int maxIconsToShow = Math.min(10, maxIconsFit);

        if (maxIconsToShow <= 0) {
            g.drawString(font, Integer.toString(a), valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int shown = 0;

        int fullToDraw = Math.min(full, maxIconsToShow);
        for (int i = 0; i < fullToDraw; i++) {
            int dx = valueX + i * per;
            if (dx + ICON > valueX + valueW) break;
            g.blitSprite(ARMOR_FULL, dx, iconY, ICON, ICON);
            shown++;
        }

        if (half && shown < maxIconsToShow) {
            int dx = valueX + shown * per;
            if (dx + ICON <= valueX + valueW) {
                g.blitSprite(ARMOR_HALF, dx, iconY, ICON, ICON);
                shown++;
            }
        }

        int totalIcons = full + (half ? 1 : 0);
        if (totalIcons > shown) {
            String extra = "(+" + (totalIcons - shown) + ")";
            int extraX = valueX + shown * per + 4;
            int maxTextW = Math.max(1, (valueX + valueW) - extraX);
            g.drawString(font, clipToWidth(font, extra, maxTextW), extraX, y, inkColor, false);
        }

        return y + lineHeight;
    }

    private static int drawTextLine(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int labelW,
            int valueX,
            int rightLimitX,
            int maxY,
            String label,
            String value,
            int inkColor,
            int lineHeight,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        if (y >= maxY) return y;

        drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        int maxW = Math.max(1, rightLimitX - valueX);
        String clipped = clipToWidth(font, value, maxW);

        g.drawString(font, clipped, valueX, y, inkColor, false);
        return y + lineHeight;
    }

    private static String fmtOpt(OptionalDouble v) {
        return v.isPresent() ? fmt(v.getAsDouble()) : "-";
    }

    private static String fmtOptWithUnit(OptionalDouble v, String unit) {
        return v.isPresent() ? (fmt(v.getAsDouble()) + " " + unit) : "-";
    }

    private static String fmt(double v) {
        double r = Math.rint(v);
        if (Math.abs(v - r) < 1.0E-6) return Integer.toString((int) r);
        return String.format(Locale.ROOT, "%.2f", v);
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


