package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
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
    private static final int MENU_TOGGLE_OUTER_VINTAGE = 0xFF6F4E31;
    private static final int MENU_TOGGLE_INNER_VINTAGE = 0xFFC9A47A;
    private static final int MENU_TOGGLE_OUTER_MODERN = 0xFF1F9AA1;
    private static final int MENU_TOGGLE_INNER_MODERN = 0xFF93E7EC;
    private static final int MENU_TOGGLE_OUTER_RUNES = 0xFF5D3F8F;
    private static final int MENU_TOGGLE_INNER_RUNES = 0xFFC6A8FF;
    private static final int MENU_TOGGLE_ACCENT = 0xAAFFFFFF;

    private WildexUiRenderUtil() {
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, Component text, int x, int y, float scale, int color) {
        if (scale >= 0.999f) {
            WildexUiText.draw(graphics, font, text, x, y, color, false);
            return;
        }
        float inv = 1.0f / scale;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        WildexUiText.draw(graphics, font, text, Math.round(x * inv), Math.round(y * inv), color, false);
        graphics.pose().popPose();
    }

    public static void drawMenuStyleToggleButton(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            boolean collapsed,
            int fillColor,
            int symbolColor
    ) {
        if (graphics == null || size <= 1) return;

        int x1 = x + size;
        int y1 = y + size;
        int outer = menuToggleOuter();
        int inner = menuToggleInner();
        int outerThickness = size >= 14 ? 2 : 1;
        int innerThickness = size >= 14 ? 2 : 1;
        int inset = outerThickness + innerThickness;

        if ((x1 - inset) > (x + inset) && (y1 - inset) > (y + inset)) {
            graphics.fill(x + inset, y + inset, x1 - inset, y1 - inset, fillColor);
        } else {
            graphics.fill(x, y, x1, y1, fillColor);
        }

        for (int i = 0; i < outerThickness; i++) {
            drawRectPerimeterLayer(graphics, x, y, x1, y1, i, outer);
        }
        for (int i = 0; i < innerThickness; i++) {
            drawRectPerimeterLayer(graphics, x, y, x1, y1, outerThickness + i, inner);
        }

        if (size >= 12) {
            graphics.fill(x + 1, y + 1, x + 2, y + 2, MENU_TOGGLE_ACCENT);
            graphics.fill(x1 - 2, y + 1, x1 - 1, y + 2, MENU_TOGGLE_ACCENT);
            graphics.fill(x + 1, y1 - 2, x + 2, y1 - 1, MENU_TOGGLE_ACCENT);
            graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, MENU_TOGGLE_ACCENT);
        }

        int cx = x + (size / 2);
        int cy = y + (size / 2);
        int thick = size >= 20 ? 2 : 1;
        int arm = Math.max(1, (size - 12) / 2);
        int yTop = cy - (thick / 2);
        int xLeft = cx - (thick / 2);

        graphics.fill(cx - arm, yTop, cx + arm + 1, yTop + thick, symbolColor);
        if (collapsed) {
            graphics.fill(xLeft, cy - arm, xLeft + thick, cy + arm + 1, symbolColor);
        }
    }

    public static void drawMenuStyleControlBase(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            int fillColor
    ) {
        drawMenuStyleControlBase(graphics, x, y, size, size, fillColor);
    }

    public static void drawMenuStyleControlBase(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        if (graphics == null || width <= 1 || height <= 1) return;

        int x1 = x + width;
        int y1 = y + height;
        int outer = menuToggleOuter();
        int inner = menuToggleInner();
        int minSide = Math.min(width, height);
        int outerThickness = minSide >= 14 ? 2 : 1;
        int innerThickness = minSide >= 14 ? 2 : 1;
        int inset = outerThickness + innerThickness;

        if ((x1 - inset) > (x + inset) && (y1 - inset) > (y + inset)) {
            graphics.fill(x + inset, y + inset, x1 - inset, y1 - inset, fillColor);
        } else {
            graphics.fill(x, y, x1, y1, fillColor);
        }

        for (int i = 0; i < outerThickness; i++) {
            drawRectPerimeterLayer(graphics, x, y, x1, y1, i, outer);
        }
        for (int i = 0; i < innerThickness; i++) {
            drawRectPerimeterLayer(graphics, x, y, x1, y1, outerThickness + i, inner);
        }

        if (minSide >= 12) {
            graphics.fill(x + 1, y + 1, x + 2, y + 2, MENU_TOGGLE_ACCENT);
            graphics.fill(x1 - 2, y + 1, x1 - 1, y + 2, MENU_TOGGLE_ACCENT);
            graphics.fill(x + 1, y1 - 2, x + 2, y1 - 1, MENU_TOGGLE_ACCENT);
            graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, MENU_TOGGLE_ACCENT);
        }
    }

    private static int menuToggleOuter() {
        ClientConfig.DesignStyle style = WildexThemes.current().layoutProfile();
        if (style == ClientConfig.DesignStyle.MODERN) return MENU_TOGGLE_OUTER_MODERN;
        if (style == ClientConfig.DesignStyle.RUNES) return MENU_TOGGLE_OUTER_RUNES;
        return MENU_TOGGLE_OUTER_VINTAGE;
    }

    private static int menuToggleInner() {
        ClientConfig.DesignStyle style = WildexThemes.current().layoutProfile();
        if (style == ClientConfig.DesignStyle.MODERN) return MENU_TOGGLE_INNER_MODERN;
        if (style == ClientConfig.DesignStyle.RUNES) return MENU_TOGGLE_INNER_RUNES;
        return MENU_TOGGLE_INNER_VINTAGE;
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
        renderTooltipInternal(g, font, lines, mouseX + 10, mouseY + 10, width, height, theme, false);
    }

    public static void renderTooltipTopLeft(
            GuiGraphics g,
            Font font,
            List<Component> lines,
            int mouseX,
            int mouseY,
            int width,
            int height,
            WildexUiTheme.Palette theme
    ) {
        renderTooltipInternal(g, font, lines, mouseX - 10, mouseY - 10, width, height, theme, true);
    }

    private static void renderTooltipInternal(
            GuiGraphics g,
            Font font,
            List<Component> lines,
            int preferredX,
            int preferredY,
            int width,
            int height,
            WildexUiTheme.Palette theme,
            boolean anchorTopLeft
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
        for (FormattedCharSequence s : wrapped) textW = Math.max(textW, WildexUiText.width(font, s));
        int textH = wrapped.size() * WildexUiText.lineHeight(font) + Math.max(0, (wrapped.size() - 1) * TIP_LINE_GAP);

        int boxW = textW + TIP_PAD * 2;
        int boxH = textH + TIP_PAD * 2;

        int x = anchorTopLeft ? (preferredX - boxW) : preferredX;
        int y = anchorTopLeft ? (preferredY - boxH) : preferredY;

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
            WildexUiText.draw(g, font, line, tx, ty, theme.tooltipText(), false);
            ty += WildexUiText.lineHeight(font) + TIP_LINE_GAP;
        }
    }

    public static void drawPanelFrame(GuiGraphics graphics, WildexScreenLayout.Area a, WildexUiTheme.Palette theme) {
        drawPanelFrame(graphics, a, theme, 1, 1);
    }

    public static void drawPanelFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int outerThickness,
            int innerThickness
    ) {
        if (a == null) return;
        int outer = Math.max(1, outerThickness);
        int inner = Math.max(0, innerThickness);

        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int inset = outer + inner;

        graphics.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, theme.frameBg());

        for (int i = 0; i < outer; i++) {
            drawRectPerimeterLayer(graphics, x0, y0, x1, y1, i, theme.frameOuter());
        }

        for (int i = 0; i < inner; i++) {
            drawRectPerimeterLayer(graphics, x0, y0, x1, y1, outer + i, theme.frameInner());
        }
    }

    public static void drawRoundedPanelFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int cornerCut,
            int outerThickness,
            int innerThickness
    ) {
        if (a == null) return;
        int outer = Math.max(1, outerThickness);
        int inner = Math.max(0, innerThickness);
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int c = Math.max(1, cornerCut);
        int inset = outer + inner;

        graphics.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, theme.frameBg());

        for (int i = 0; i < outer; i++) {
            int cut = Math.max(0, c - i);
            drawRoundedPerimeterLayer(graphics, x0, y0, x1, y1, i, cut, theme.frameOuter());
        }

        for (int i = 0; i < inner; i++) {
            int off = outer + i;
            int cut = Math.max(0, c - off);
            drawRoundedPerimeterLayer(graphics, x0, y0, x1, y1, off, cut, theme.frameInner());
        }
    }

    private static void drawRectPerimeterLayer(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int inset,
            int color
    ) {
        int lx0 = x0 + inset;
        int ly0 = y0 + inset;
        int lx1 = x1 - inset;
        int ly1 = y1 - inset;
        if (lx1 - lx0 <= 0 || ly1 - ly0 <= 0) return;

        graphics.fill(lx0, ly0, lx1, ly0 + 1, color);
        graphics.fill(lx0, ly1 - 1, lx1, ly1, color);
        graphics.fill(lx0, ly0, lx0 + 1, ly1, color);
        graphics.fill(lx1 - 1, ly0, lx1, ly1, color);
    }

    private static void drawRoundedPerimeterLayer(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int inset,
            int cornerCut,
            int color
    ) {
        int lx0 = x0 + inset;
        int ly0 = y0 + inset;
        int lx1 = x1 - inset;
        int ly1 = y1 - inset;
        int cut = Math.max(0, cornerCut);
        if (lx1 - lx0 <= 0 || ly1 - ly0 <= 0) return;

        graphics.fill(lx0 + cut, ly0, lx1 - cut, ly0 + 1, color);
        graphics.fill(lx0 + cut, ly1 - 1, lx1 - cut, ly1, color);
        graphics.fill(lx0, ly0 + cut, lx0 + 1, ly1 - cut, color);
        graphics.fill(lx1 - 1, ly0 + cut, lx1, ly1 - cut, color);
        if (cut > 0) {
            drawFrameCornerChamfers(graphics, lx0, ly0, lx1, ly1, cut, color);
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



