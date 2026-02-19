package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class WildexUiRenderUtil {

    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

    private WildexUiRenderUtil() {
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, Component text, int x, int y, float scale, int color) {
        if (scale >= 0.999f) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        float inv = 1.0f / scale;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, Math.round(x * inv), Math.round(y * inv), color, false);
        graphics.pose().popPose();
    }

    public static void renderTooltip(
            GuiGraphics g,
            Font font,
            List<Component> lines,
            int mouseX,
            int mouseY,
            int width,
            int height,
            WildexUiTheme.Palette theme
    ) {
        if (lines == null || lines.isEmpty()) return;

        int maxW = Math.max(80, Math.min(TIP_MAX_W, width - (TIP_PAD * 2) - 8));
        ArrayList<FormattedCharSequence> wrapped = new ArrayList<>();

        for (Component c : lines) {
            if (c == null) continue;
            if (c.getString().isEmpty()) {
                wrapped.add(FormattedCharSequence.EMPTY);
                continue;
            }
            wrapped.addAll(font.split(c, maxW));
        }

        if (wrapped.isEmpty()) return;

        int textW = 0;
        for (FormattedCharSequence s : wrapped) textW = Math.max(textW, font.width(s));
        int textH = wrapped.size() * font.lineHeight + Math.max(0, (wrapped.size() - 1) * TIP_LINE_GAP);

        int boxW = textW + TIP_PAD * 2;
        int boxH = textH + TIP_PAD * 2;

        int x = mouseX + 10;
        int y = mouseY + 10;

        int minX = 2;
        int maxX = width - boxW - 2;
        int minY = 2;
        int maxY = height - boxH - 2;

        if (x > maxX) x = maxX;
        if (x < minX) x = minX;
        if (y > maxY) y = maxY;
        if (y < minY) y = minY;

        int x0 = x;
        int y0 = y;
        int x1 = x + boxW;
        int y1 = y + boxH;

        g.fill(x0, y0, x1, y1, theme.tooltipBg());

        g.fill(x0, y0, x1, y0 + 1, theme.tooltipBorder());
        g.fill(x0, y1 - 1, x1, y1, theme.tooltipBorder());
        g.fill(x0, y0, x0 + 1, y1, theme.tooltipBorder());
        g.fill(x1 - 1, y0, x1, y1, theme.tooltipBorder());

        int tx = x0 + TIP_PAD;
        int ty = y0 + TIP_PAD;

        for (FormattedCharSequence line : wrapped) {
            g.drawString(font, line, tx, ty, theme.tooltipText(), false);
            ty += font.lineHeight + TIP_LINE_GAP;
        }
    }

    public static void drawPanelFrame(GuiGraphics graphics, WildexScreenLayout.Area a, WildexUiTheme.Palette theme) {
        if (a == null) return;

        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, theme.frameBg());

        graphics.fill(x0, y0, x1, y0 + 1, theme.frameOuter());
        graphics.fill(x0, y1 - 1, x1, y1, theme.frameOuter());
        graphics.fill(x0, y0, x0 + 1, y1, theme.frameOuter());
        graphics.fill(x1 - 1, y0, x1, y1, theme.frameOuter());

        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, theme.frameInner());
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, theme.frameInner());
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, theme.frameInner());
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, theme.frameInner());
    }

    public static void drawRoundedPanelFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int cornerCut
    ) {
        if (a == null) return;
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int c = Math.max(1, cornerCut);

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, theme.frameBg());

        graphics.fill(x0 + c, y0, x1 - c, y0 + 1, theme.frameOuter());
        graphics.fill(x0 + c, y1 - 1, x1 - c, y1, theme.frameOuter());
        graphics.fill(x0, y0 + c, x0 + 1, y1 - c, theme.frameOuter());
        graphics.fill(x1 - 1, y0 + c, x1, y1 - c, theme.frameOuter());

        graphics.fill(x0 + c, y0 + 1, x1 - c, y0 + 2, theme.frameInner());
        graphics.fill(x0 + c, y1 - 2, x1 - c, y1 - 1, theme.frameInner());
        graphics.fill(x0 + 1, y0 + c, x0 + 2, y1 - c, theme.frameInner());
        graphics.fill(x1 - 2, y0 + c, x1 - 1, y1 - c, theme.frameInner());

        drawFrameCornerChamfers(graphics, x0, y0, x1, y1, c, theme.frameOuter());
        drawFrameCornerChamfers(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(1, c - 1), theme.frameInner());
    }

    public static void drawDockedTrophyFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int cornerCut,
            int bgColor,
            int extendLeft,
            int extendRight
    ) {
        if (a == null) return;
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int c = Math.max(1, cornerCut);
        int xl = x0 - Math.max(0, extendLeft);
        int xr = x1 + Math.max(0, extendRight);

        graphics.fill(xl + 2, y0 + 2, xr - 2, y1 - 2, bgColor);

        graphics.fill(xl + c, y0, xr, y0 + 1, theme.frameOuter());
        graphics.fill(xl + c, y1 - 1, x1, y1, theme.frameOuter());
        graphics.fill(xl, y0 + c, xl + 1, y1 - c, theme.frameOuter());

        graphics.fill(xl + c, y0 + 1, xr, y0 + 2, theme.frameInner());
        graphics.fill(xl + c, y1 - 2, x1, y1 - 1, theme.frameInner());
        graphics.fill(xl + 1, y0 + c, xl + 2, y1 - c, theme.frameInner());

        drawLeftFrameCornerChamfers(graphics, xl, y0, y1, c, theme.frameOuter());
        drawLeftFrameCornerChamfers(graphics, xl + 1, y0 + 1, y1 - 1, Math.max(1, c - 1), theme.frameInner());
    }

    private static void drawLeftFrameCornerChamfers(
            GuiGraphics graphics,
            int x0,
            int y0,
            int y1,
            int cut,
            int color
    ) {
        for (int i = 0; i < cut; i++) {
            int l = x0 + (cut - 1 - i);
            int t = y0 + i;
            int b = y1 - 1 - i;
            graphics.fill(l, t, l + 1, t + 1, color);
            graphics.fill(l, b, l + 1, b + 1, color);
        }
    }

    private static void drawFrameCornerChamfers(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int cut,
            int color
    ) {
        for (int i = 0; i < cut; i++) {
            int l = x0 + (cut - 1 - i);
            int r = x1 - (cut - i);
            int t = y0 + i;
            int b = y1 - 1 - i;
            graphics.fill(l, t, l + 1, t + 1, color);
            graphics.fill(r, t, r + 1, t + 1, color);
            graphics.fill(l, b, l + 1, b + 1, color);
            graphics.fill(r, b, r + 1, b + 1, color);
        }
    }
}
