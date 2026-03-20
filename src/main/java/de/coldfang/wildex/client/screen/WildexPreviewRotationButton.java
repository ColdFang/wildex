package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class WildexPreviewRotationButton {

    private static final Component PAUSE_TOOLTIP = Component.translatable("tooltip.wildex.preview_pause_rotation");
    private static final Component RESUME_TOOLTIP = Component.translatable("tooltip.wildex.preview_resume_rotation");
    private static final int BUTTON_MARGIN = 6;
    private static final int BUTTON_SIZE = 14;
    private static final int MODERN_TOP_INSET = 10;
    private static final int MODERN_LEFT_INSET = 2;

    public boolean handleClick(
            int mouseX,
            int mouseY,
            int button,
            WildexScreenLayout layout,
            String mobId,
            Runnable toggleAction
    ) {
        if (button != 0 || toggleAction == null) return false;
        WildexScreenLayout.Area area = area(layout, mobId);
        if (isOutside(mouseX, mouseY, area)) return false;

        WildexUiSounds.playButtonClick();
        toggleAction.run();
        return true;
    }

    public void render(GuiGraphics graphics, WildexScreenLayout layout, String mobId, boolean paused) {
        WildexScreenLayout.Area area = area(layout, mobId);
        if (graphics == null || area == null) return;

        int color = WildexUiTheme.current().inkMuted();
        if (paused) {
            drawPlaySymbol(graphics, area, color);
            return;
        }
        drawPauseSymbol(graphics, area, color);
    }

    public void renderTooltip(
            GuiGraphics graphics,
            Font font,
            WildexScreenLayout layout,
            String mobId,
            boolean paused,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            WildexUiTheme.Palette theme
    ) {
        if (isOutside(mouseX, mouseY, area(layout, mobId))) return;
        Component tip = paused ? RESUME_TOOLTIP : PAUSE_TOOLTIP;
        WildexUiRenderUtil.renderTooltipTopLeft(graphics, font, java.util.List.of(tip), mouseX, mouseY, screenWidth, screenHeight, theme);
    }

    private WildexScreenLayout.Area area(WildexScreenLayout layout, String mobId) {
        ResourceLocation mobKey = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (mobKey == null) return null;
        if (layout == null) return null;
        WildexScreenLayout.Area previewArea = layout.rightPreviewArea();
        if (previewArea == null) return null;

        int size = Math.max(10, Math.round(BUTTON_SIZE * layout.scale()));
        int margin = Math.max(3, Math.round(BUTTON_MARGIN * layout.scale()));
        int extraTopInset = 0;
        int extraLeftInset = 0;
        if (WildexThemes.isModernLayout()) {
            extraTopInset = Math.round(MODERN_TOP_INSET * layout.scale());
            extraLeftInset = Math.round(MODERN_LEFT_INSET * layout.scale());
        }

        int x = previewArea.x() + margin + extraLeftInset;
        int y = previewArea.y() + margin + extraTopInset;
        return new WildexScreenLayout.Area(x, y, size, size);
    }

    private static void drawPauseSymbol(GuiGraphics graphics, WildexScreenLayout.Area area, int color) {
        int x = area.x();
        int y = area.y();
        int w = area.w();
        int h = area.h();

        int barW = Math.max(2, Math.round(w * 0.20f));
        int gap = Math.max(2, Math.round(w * 0.16f));
        int innerH = Math.max(6, Math.round(h * 0.60f));
        int top = y + Math.max(1, (h - innerH) / 2);
        int bottom = top + innerH;
        int leftX = x + Math.max(1, (w - ((barW * 2) + gap)) / 2);
        int rightX = leftX + barW + gap;

        graphics.fill(leftX, top, leftX + barW, bottom, color);
        graphics.fill(rightX, top, rightX + barW, bottom, color);
    }

    private static void drawPlaySymbol(GuiGraphics graphics, WildexScreenLayout.Area area, int color) {
        int x = area.x();
        int y = area.y();
        int w = area.w();
        int h = area.h();

        int left = x + Math.max(2, Math.round(w * 0.24f));
        int right = x + w - Math.max(2, Math.round(w * 0.18f));
        int centerY = y + (h / 2);
        int halfHeight = Math.max(3, Math.round(h * 0.28f));

        int span = Math.max(1, right - left);
        for (int dx = 0; dx < span; dx++) {
            float t = 1.0f - (dx / (float) Math.max(1, span - 1));
            int spread = Math.max(1, Math.round(1 + (t * halfHeight)));
            int px = left + dx;
            graphics.fill(px, centerY - spread, px + 1, centerY + spread + 1, color);
        }
    }

    private static boolean isOutside(int mouseX, int mouseY, WildexScreenLayout.Area area) {
        return area == null
                || mouseX < area.x()
                || mouseX >= area.x() + area.w()
                || mouseY < area.y()
                || mouseY >= area.y() + area.h();
    }
}
