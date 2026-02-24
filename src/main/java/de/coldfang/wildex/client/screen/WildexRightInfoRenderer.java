package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.model.WildexMobData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class WildexRightInfoRenderer {

    static final int PAD_X = 6;
    static final int PAD_Y = 6;
    static final int PAD_RIGHT = 2;
    static final int HINT_TEXT_COLOR = 0x7A2B1A10;
    static final int HINT_LINE_GAP = 3;

    private static final int TARGET_MIN_H = 124;
    private static final float MIN_SCALE = 0.78f;

    private static final int TIP_BG = 0xE61A120C;
    private static final int TIP_BORDER = 0xAA301E14;
    private static final int TIP_TEXT = 0xF2E8D5;
    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

    static final Component SHIFT_HINT_LINE_1 = Component.translatable("gui.wildex.hint.shift_details");
    static final Component SHIFT_HINT_LINE_2 = Component.translatable("gui.wildex.hint.shift_hover");

    record TooltipRequest(List<Component> lines) {
    }

    private final WildexRightInfoStatsRenderer statsRenderer = new WildexRightInfoStatsRenderer();
    private final WildexRightInfoLootRenderer lootRenderer = new WildexRightInfoLootRenderer();
    private final WildexRightInfoSpawnsRenderer spawnsRenderer = new WildexRightInfoSpawnsRenderer();
    private final WildexRightInfoMiscRenderer miscRenderer = new WildexRightInfoMiscRenderer();

    public void resetSpawnScroll() {
        spawnsRenderer.resetScroll();
    }

    public void resetStatsScroll() {
        statsRenderer.resetScroll();
    }

    public void resetLootScroll() {
        lootRenderer.resetScroll();
    }

    public void resetMiscScroll() {
        miscRenderer.resetScroll();
    }

    public void scrollSpawn(double scrollY) {
        spawnsRenderer.scroll(scrollY);
    }

    public boolean handleSpawnMouseClicked(int mouseX, int mouseY, int button, WildexScreenState state) {
        return spawnsRenderer.handleMouseClicked(mouseX, mouseY, button, state);
    }

    public boolean handleSpawnMouseDragged(int mouseX, int mouseY, int button) {
        return spawnsRenderer.handleMouseDragged(mouseX, mouseY, button);
    }

    public boolean handleSpawnMouseReleased(int button) {
        return spawnsRenderer.handleMouseReleased(button);
    }

    public boolean scrollStats(int mouseX, int mouseY, double scrollY) {
        return statsRenderer.scroll(mouseX, mouseY, scrollY);
    }

    public boolean handleStatsMouseClicked(int mouseX, int mouseY, int button) {
        return statsRenderer.handleMouseClicked(mouseX, mouseY, button);
    }

    public boolean handleStatsMouseDragged(int mouseX, int mouseY, int button) {
        return statsRenderer.handleMouseDragged(mouseX, mouseY, button);
    }

    public boolean handleStatsMouseReleased(int button) {
        return statsRenderer.handleMouseReleased(button);
    }

    public boolean scrollLoot(int mouseX, int mouseY, double scrollY) {
        return lootRenderer.scroll(mouseX, mouseY, scrollY);
    }

    public boolean handleLootMouseClicked(int mouseX, int mouseY, int button) {
        return lootRenderer.handleMouseClicked(mouseX, mouseY, button);
    }

    public boolean handleLootMouseDragged(int mouseX, int mouseY, int button) {
        return lootRenderer.handleMouseDragged(mouseX, mouseY, button);
    }

    public boolean handleLootMouseReleased(int button) {
        return lootRenderer.handleMouseReleased(button);
    }

    public boolean scrollMisc(int mouseX, int mouseY, double scrollY) {
        return miscRenderer.scroll(mouseX, mouseY, scrollY);
    }

    public boolean handleMiscMouseClicked(int mouseX, int mouseY, int button) {
        return miscRenderer.handleMouseClicked(mouseX, mouseY, button);
    }

    public boolean handleMiscMouseDragged(int mouseY, int button) {
        return miscRenderer.handleMouseDragged(mouseY, button);
    }

    public boolean handleMiscMouseReleased(int button) {
        return miscRenderer.handleMouseReleased(button);
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

        WildexScissor.enablePhysical(graphics, x0, y0, x1, y1);
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
                case STATS -> tooltip = statsRenderer.render(
                        graphics, font, local, state.selectedMobId(), data.stats(), inkColor,
                        mouseX, mouseY, x0, y0, s, shiftDown
                );
                case LOOT -> lootRenderer.render(graphics, font, local, mobRl, inkColor, x0, y0, s);
                case SPAWNS -> tooltip = spawnsRenderer.render(
                        graphics, font, local, mobRl, state, inkColor, x0, y0, s, mouseX, mouseY
                );
                case MISC -> miscRenderer.render(graphics, font, local, state, data.misc(), inkColor, x0, y0, s);
            }

            graphics.pose().popPose();

            if (tooltip != null && mouseX >= 0 && mouseY >= 0 && !tooltip.lines().isEmpty()) {
                renderPanelTooltip(graphics, font, tooltip.lines(), mouseX, mouseY, x0, y0, area.w(), area.h());
            }
        } finally {
            graphics.disableScissor();
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

    private static void renderLockedHint(GuiGraphics g, Font font, WildexScreenLayout.Area area) {
        int x = area.x() + PAD_X;
        int yTop = area.y() + PAD_Y;
        boolean modern = WildexThemes.isModernLayout();
        int lockedColor = modern ? 0xFFD9F6FF : HINT_TEXT_COLOR;

        int rightLimitX = area.x() + area.w() - PAD_RIGHT;
        int maxW = Math.max(1, rightLimitX - x);

        int lineH = Math.max(10, WildexUiText.lineHeight(font) + HINT_LINE_GAP);

        String[] lines = {
                WildexRightInfoTabUtil.tr("gui.wildex.locked.line1"),
                WildexRightInfoTabUtil.tr("gui.wildex.locked.line2"),
                WildexRightInfoTabUtil.tr("gui.wildex.locked.line3")
        };

        int totalH = lines.length * lineH;
        int startY = yTop + Math.max(0, (area.h() - (PAD_Y * 2) - totalH) / 2);

        for (String line : lines) {
            String clipped = WildexRightInfoTabUtil.clipToWidth(font, line, maxW);
            WildexUiText.draw(g, font, clipped, x, startY, lockedColor, false);
            startY += lineH;
        }
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
        for (FormattedCharSequence s : wrapped) textW = Math.max(textW, WildexUiText.width(font, s));
        int textH = wrapped.size() * WildexUiText.lineHeight(font) + Math.max(0, (wrapped.size() - 1) * TIP_LINE_GAP);

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
            WildexUiText.draw(g, font, line, tx, ty, TIP_TEXT, false);
            ty += WildexUiText.lineHeight(font) + TIP_LINE_GAP;
        }
    }
}




