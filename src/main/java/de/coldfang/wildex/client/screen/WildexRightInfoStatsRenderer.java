package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.model.WildexStatsData;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

final class WildexRightInfoStatsRenderer {

    private static final int ICON = 9;
    private static final int ICON_GAP = 1;
    private static final int COL_GAP = 6;
    private static final float STATS_LABEL_COL_RATIO = 0.56f;
    private static final int STATS_LABEL_COL_MIN = 56;
    private static final int STATS_LABEL_COL_MAX = 240;
    private static final int STATS_MIN_VALUE_COL_W = 48;
    private static final int DIVIDER = 0x22301E14;

    private static final int SCROLLBAR_W = 8;
    private static final int SCROLLBAR_BG = 0xFF000000;
    private static final int SCROLLBAR_THUMB = 0xFFB9B9B9;
    private static final int SCROLLBAR_SHOW_THRESHOLD_PX = 2;

    private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation ARMOR_FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation ARMOR_HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");

    private static final int MAX_HEART_ICONS = 10;
    private static final int MAX_HEART_HP = MAX_HEART_ICONS * 2;

    private static final Map<ResourceLocation, Dims> DIMS_CACHE = new HashMap<>();
    private static final Dims EMPTY_DIMS = new Dims(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());

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

    private record Dims(OptionalDouble hitboxWidth, OptionalDouble hitboxHeight, OptionalDouble eyeHeight) {
    }

    void resetScroll() {
        statsScrollPx = 0;
        statsViewportH = 1;
        statsContentH = 1;
        statsHasScrollbar = false;
        statsDraggingScrollbar = false;
    }

    boolean scroll(int mouseX, int mouseY, double scrollY) {
        if (!statsHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, statsViewportScreenX0, statsViewportScreenY0, statsViewportScreenX1, statsViewportScreenY1)) {
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

    boolean handleMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !statsHasScrollbar) return false;
        if (!WildexRightInfoTabUtil.isInside(mouseX, mouseY, statsBarScreenX0, statsBarScreenY0, statsBarScreenX1, statsBarScreenY1)) return false;
        if (WildexRightInfoTabUtil.isInside(mouseX, mouseY, statsBarScreenX0, statsThumbScreenY0, statsBarScreenX1, statsThumbScreenY1)) {
            statsDraggingScrollbar = true;
            statsDragOffsetY = mouseY - statsThumbScreenY0;
            return true;
        }
        setScrollFromThumbTop(mouseY - (statsThumbScreenH / 2));
        return true;
    }

    boolean handleMouseDragged(int mouseX, int mouseY, int button) {
        if (button != 0 || !statsDraggingScrollbar) return false;
        setScrollFromThumbTop(mouseY - statsDragOffsetY);
        return true;
    }

    boolean handleMouseReleased(int button) {
        if (button != 0) return false;
        boolean wasDragging = statsDraggingScrollbar;
        statsDraggingScrollbar = false;
        return wasDragging;
    }

    WildexRightInfoRenderer.TooltipRequest render(
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
        int rightLimitX = x + w - WildexRightInfoRenderer.PAD_RIGHT;

        int maxW = Math.max(1, w - (WildexRightInfoRenderer.PAD_X * 2));
        List<FormattedCharSequence> hintL1 = font.split(WildexRightInfoRenderer.SHIFT_HINT_LINE_1, maxW);
        int hintLineH = WildexUiText.lineHeight(font) + 1;
        int hintTotalH = (hintL1.size() + 1) * hintLineH;
        int hintBlockH = hintTotalH + WildexRightInfoRenderer.PAD_Y + 2;

        int dividerShiftDown = 5;
        int dividerY = Math.min((y + h) - 2, (y + h) - hintBlockH - 2 + dividerShiftDown);
        int viewportX = x;
        int viewportY = y;
        int viewportW = w;
        int viewportH = Math.max(24, dividerY - viewportY - 1);

        int line = Math.max(10, WildexUiText.lineHeight(font) + 2);
        int estimatedContentH = Math.max(viewportH, WildexRightInfoRenderer.PAD_Y + (line * 9) + 2);
        statsViewportH = viewportH;
        statsContentH = estimatedContentH;
        int maxScroll = Math.max(0, statsContentH - statsViewportH);
        boolean showStatsScrollbar = maxScroll > SCROLLBAR_SHOW_THRESHOLD_PX;
        if (!showStatsScrollbar) {
            statsScrollPx = 0;
        } else if (statsScrollPx > maxScroll) {
            statsScrollPx = maxScroll;
        }

        int sx0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, viewportX);
        int sy0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, viewportY);
        int sx1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, viewportX + viewportW);
        int sy1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, viewportY + viewportH);
        statsViewportScreenX0 = sx0;
        statsViewportScreenY0 = sy0;
        statsViewportScreenX1 = sx1;
        statsViewportScreenY1 = sy1;

        WildexScissor.enablePhysical(g, sx0, sy0, sx1, sy1);
        WildexRightInfoRenderer.TooltipRequest tooltip;
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
            g.fill(x + WildexRightInfoRenderer.PAD_X, lineY, rightLimitX, lineY + 1, DIVIDER);
        }

        int hintY = lineY + 3;
        renderShiftHintAt(g, font, area, hintY, y + h - WildexRightInfoRenderer.PAD_Y - hintTotalH);

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
            cacheScrollbarRect(screenOriginX, screenOriginY, scale, barX0, barY0, rightLimitX, barY1, thumbY0, thumbY1);
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
        int maxW = Math.max(1, area.w() - (WildexRightInfoRenderer.PAD_X * 2));
        List<FormattedCharSequence> line1 = font.split(WildexRightInfoRenderer.SHIFT_HINT_LINE_1, maxW);
        FormattedCharSequence line2 = WildexRightInfoRenderer.SHIFT_HINT_LINE_2.getVisualOrderText();
        int lineH = WildexUiText.lineHeight(font) + 1;
        int y = Math.max(minY, preferredY);
        for (FormattedCharSequence l : line1) {
            int w = WildexUiText.width(font, l);
            int x = x1 - WildexRightInfoRenderer.PAD_X - w;
            if (x < x0 + WildexRightInfoRenderer.PAD_X) x = x0 + WildexRightInfoRenderer.PAD_X;
            WildexUiText.draw(g, font, l, x, y, hintColor, false);
            y += lineH;
        }
        int w2 = WildexUiText.width(font, line2);
        int x2 = x1 - WildexRightInfoRenderer.PAD_X - w2;
        if (x2 < x0 + WildexRightInfoRenderer.PAD_X) x2 = x0 + WildexRightInfoRenderer.PAD_X;
        WildexUiText.draw(g, font, line2, x2, y, hintColor, false);
    }

    private static WildexRightInfoRenderer.TooltipRequest renderStats(
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
        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int y = area.y() + WildexRightInfoRenderer.PAD_Y;

        int innerW = Math.max(1, area.w() - WildexRightInfoRenderer.PAD_X - WildexRightInfoRenderer.PAD_RIGHT);
        int innerH = Math.max(1, area.h() - WildexRightInfoRenderer.PAD_Y * 2);

        int maxY = (area.y() + WildexRightInfoRenderer.PAD_Y) + innerH;

        int contentW = Math.max(1, innerW);
        int colGap = Math.max(4, Math.round(COL_GAP * WildexUiScale.get()));
        int maxLabelBySpace = Math.max(24, contentW - colGap - STATS_MIN_VALUE_COL_W);
        int labelColWBase = Math.round(contentW * STATS_LABEL_COL_RATIO);
        labelColWBase = Math.max(STATS_LABEL_COL_MIN, Math.min(labelColWBase, STATS_LABEL_COL_MAX));
        int requiredLabelW = requiredLabelColumnWidth(font);
        int labelColW = Math.max(labelColWBase, requiredLabelW);
        labelColW = Math.min(labelColW, maxLabelBySpace);

        int valueX = x + labelColW + colGap;
        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int valueW = Math.max(1, rightLimitX - valueX);

        int line = Math.max(10, WildexUiText.lineHeight(font) + 2);
        int dividerX = x + labelColW + (colGap / 2);
        int dividerStartY = y;

        WildexRightInfoRenderer.TooltipRequest tooltip = null;

        int mxL = -1;
        int myL = -1;
        if (mouseX >= 0 && mouseY >= 0) {
            double fx = (mouseX - screenOriginX) / (double) scale;
            double fy = (mouseY - screenOriginY) / (double) scale;
            mxL = (int) Math.floor(fx);
            myL = (int) Math.floor(fy);
        }

        int y0 = y;
        y = drawHeartsLine(g, font, x, y, labelColW, valueX, valueW, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.health"), s.maxHealth(), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.health");
        }

        y0 = y;
        y = drawArmorLine(g, font, x, y, labelColW, valueX, valueW, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.armor"), s.armor(), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.armor.1", "tooltip.wildex.stats.armor.2");
        }

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.move_speed"), fmtOpt(s.movementSpeed()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) tooltip = tooltipLines("tooltip.wildex.stats.move_speed");

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.attack_damage"), fmtOpt(s.attackDamage()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) tooltip = tooltipLines("tooltip.wildex.stats.attack_damage");

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.follow_range"), fmtOptWithUnit(s.followRange(), WildexRightInfoTabUtil.tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) tooltip = tooltipLines("tooltip.wildex.stats.follow_range");

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.knockback_res"), fmtOpt(s.knockbackResistance()), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.knockback_res.1", "tooltip.wildex.stats.knockback_res.2");
        }

        Dims dims = resolveDims(selectedMobId);

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.hitbox_width"), fmtOptWithUnit(dims.hitboxWidth(), WildexRightInfoTabUtil.tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) tooltip = tooltipLines("tooltip.wildex.stats.hitbox_width");

        y0 = y;
        y = drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.hitbox_height"), fmtOptWithUnit(dims.hitboxHeight(), WildexRightInfoTabUtil.tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) tooltip = tooltipLines("tooltip.wildex.stats.hitbox_height");

        y0 = y;
        drawTextLine(g, font, x, y, labelColW, valueX, rightLimitX, maxY, WildexRightInfoTabUtil.tr("gui.wildex.stats.eye_height"), fmtOptWithUnit(dims.eyeHeight(), WildexRightInfoTabUtil.tr("gui.wildex.unit.blocks")), inkColor, line, screenOriginX, screenOriginY, scale);
        if (tooltip == null && isHover(mxL, myL, x, y0, rightLimitX - x, line)) {
            tooltip = tooltipLines("tooltip.wildex.stats.eye_height.1", "tooltip.wildex.stats.eye_height.2");
        }

        int dividerEndY = Math.min(maxY, y + line - 1);
        if (dividerX > x + 4 && dividerX < rightLimitX - 4 && dividerEndY > dividerStartY) {
            g.fill(dividerX, dividerStartY, dividerX + 1, dividerEndY, DIVIDER);
        }

        return tooltip;
    }

    private static int requiredLabelColumnWidth(Font font) {
        int pad = Math.max(4, Math.round(4 * WildexUiScale.get()));
        int max = 0;
        for (String key : STAT_LABEL_KEYS) {
            int w = WildexUiText.width(font, WildexRightInfoTabUtil.tr(key));
            if (w > max) max = w;
        }
        return max + pad;
    }

    private static final String[] STAT_LABEL_KEYS = new String[] {
            "gui.wildex.stats.health",
            "gui.wildex.stats.armor",
            "gui.wildex.stats.move_speed",
            "gui.wildex.stats.attack_damage",
            "gui.wildex.stats.follow_range",
            "gui.wildex.stats.knockback_res",
            "gui.wildex.stats.hitbox_width",
            "gui.wildex.stats.hitbox_height",
            "gui.wildex.stats.eye_height"
    };

    private static boolean isHover(int mx, int my, int x, int y, int w, int h) {
        if (mx < 0 || my < 0) return false;
        return mx >= x && mx < (x + w) && my >= y && my < (y + h);
    }

    private static WildexRightInfoRenderer.TooltipRequest tooltipLines(String... keys) {
        ArrayList<Component> out = new ArrayList<>();
        for (String key : keys) {
            if (key != null && !key.isBlank()) out.add(Component.translatable(key));
        }
        return out.isEmpty() ? null : new WildexRightInfoRenderer.TooltipRequest(out);
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
        int icon = scaledIconPx();
        int iconGap = scaledIconGapPx();

        WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        if (maxHealth.isEmpty()) {
            WildexUiText.draw(g, font, "-", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int hp = Math.max(0, (int) Math.round(maxHealth.getAsDouble()));
        double heartsTotal = hp / 2.0;

        int shownHp = Math.min(hp, MAX_HEART_HP);
        int full = shownHp / 2;
        boolean half = (shownHp % 2) == 1;

        int iconY = y + Math.max(0, (lineHeight - icon) / 2);
        if (iconY + icon >= maxY) return y + lineHeight;

        int per = icon + iconGap;
        int fitIcons = Math.max(0, valueW / per);

        int iconsWanted = Math.min(MAX_HEART_ICONS, full + (half ? 1 : 0));
        boolean needsCompact = (hp > MAX_HEART_HP) || (fitIcons < iconsWanted);

        if (needsCompact) {
            String mult = "x" + String.format(Locale.ROOT, "%.1f", heartsTotal);

            if (fitIcons <= 0) {
                WildexUiText.draw(g, font, WildexRightInfoTabUtil.clipToWidth(font, mult, Math.max(1, valueW)), valueX, y, inkColor, false);
                return y + lineHeight;
            }

            g.blitSprite(HEART_FULL, valueX, iconY, icon, icon);

            int textX = valueX + per + 2;
            int maxTextW = Math.max(1, (valueX + valueW) - textX);
            WildexUiText.draw(g, font, WildexRightInfoTabUtil.clipToWidth(font, mult, maxTextW), textX, y, inkColor, false);
            return y + lineHeight;
        }

        for (int i = 0; i < full; i++) {
            int dx = valueX + i * per;
            if (dx + icon > valueX + valueW) break;
            g.blitSprite(HEART_FULL, dx, iconY, icon, icon);
        }

        if (half) {
            int dx = valueX + full * per;
            if (dx + icon <= valueX + valueW) {
                g.blitSprite(HEART_HALF, dx, iconY, icon, icon);
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
        int icon = scaledIconPx();
        int iconGap = scaledIconGapPx();

        WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        if (armor.isEmpty()) {
            WildexUiText.draw(g, font, "-", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int a = Math.max(0, (int) Math.round(armor.getAsDouble()));
        if (a <= 0) {
            WildexUiText.draw(g, font, "-", valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int full = a / 2;
        boolean half = (a % 2) == 1;

        int iconY = y + Math.max(0, (lineHeight - icon) / 2);
        if (iconY + icon >= maxY) return y + lineHeight;

        int per = icon + iconGap;
        int maxIconsFit = Math.max(0, valueW / per);
        int maxIconsToShow = Math.min(10, maxIconsFit);

        if (maxIconsToShow <= 0) {
            WildexUiText.draw(g, font, Integer.toString(a), valueX, y, inkColor, false);
            return y + lineHeight;
        }

        int shown = 0;
        int fullToDraw = Math.min(full, maxIconsToShow);
        for (int i = 0; i < fullToDraw; i++) {
            int dx = valueX + i * per;
            if (dx + icon > valueX + valueW) break;
            g.blitSprite(ARMOR_FULL, dx, iconY, icon, icon);
            shown++;
        }

        if (half && shown < maxIconsToShow) {
            int dx = valueX + shown * per;
            if (dx + icon <= valueX + valueW) {
                g.blitSprite(ARMOR_HALF, dx, iconY, icon, icon);
                shown++;
            }
        }

        int totalIcons = full + (half ? 1 : 0);
        if (totalIcons > shown) {
            String extra = "(+" + (totalIcons - shown) + ")";
            int extraX = valueX + shown * per + 4;
            int maxTextW = Math.max(1, (valueX + valueW) - extraX);
            WildexUiText.draw(g, font, WildexRightInfoTabUtil.clipToWidth(font, extra, maxTextW), extraX, y, inkColor, false);
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

        WildexRightInfoTabUtil.drawMarqueeIfNeeded(g, font, label, x, y, labelW, inkColor, screenOriginX, screenOriginY, scale);

        int maxW = Math.max(1, rightLimitX - valueX);
        String clipped = WildexRightInfoTabUtil.clipToWidth(font, value, maxW);
        WildexUiText.draw(g, font, clipped, valueX, y, inkColor, false);
        return y + lineHeight;
    }

    private static int scaledIconPx() {
        return Math.max(6, Math.round(ICON * WildexUiScale.get()));
    }

    private static int scaledIconGapPx() {
        return Math.max(1, Math.round(ICON_GAP * WildexUiScale.get()));
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
        statsBarScreenX0 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX0);
        statsBarScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY0);
        statsBarScreenX1 = WildexRightInfoTabUtil.toScreenX(screenOriginX, scale, barLocalX1);
        statsBarScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, barLocalY1);

        statsThumbScreenY0 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY0);
        statsThumbScreenY1 = WildexRightInfoTabUtil.toScreenY(screenOriginY, scale, thumbLocalY1);
        statsThumbScreenH = Math.max(1, statsThumbScreenY1 - statsThumbScreenY0);
    }

    private static void setScrollFromThumbTop(int desiredThumbTop) {
        if (!statsHasScrollbar) return;

        int barTop = statsBarScreenY0;
        int barBottom = statsBarScreenY1;
        int travel = Math.max(1, (barBottom - barTop) - statsThumbScreenH);
        int clamped = Math.max(barTop, Math.min(desiredThumbTop, barBottom - statsThumbScreenH));

        float t = (clamped - barTop) / (float) travel;
        int maxScroll = Math.max(0, statsContentH - statsViewportH);
        statsScrollPx = Math.round(maxScroll * t);
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
}




