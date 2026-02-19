package de.coldfang.wildex.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class WildexRightInfoTabUtil {

    private static final int MARQUEE_GAP_PX = 16;
    private static final double MARQUEE_SPEED_PX_PER_SEC = 15.0;

    private WildexRightInfoTabUtil() {
    }

    static boolean isInside(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    static int toScreenX(int originX, float scale, int localX) {
        return Math.round(originX + localX * scale);
    }

    static int toScreenY(int originY, float scale, int localY) {
        return Math.round(originY + localY * scale);
    }

    static String tr(String key) {
        return Component.translatable(key).getString();
    }

    static String clipToWidth(Font font, String s, int maxW) {
        if (s == null || s.isEmpty()) return "";
        if (WildexUiText.width(font, s) <= maxW) return s;

        String ell = "...";
        int ellW = WildexUiText.width(font, ell);
        if (ellW >= maxW) return "";

        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String sub = s.substring(0, mid);
            if (WildexUiText.width(font, sub) + ellW <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ell;
    }

    static void drawMarqueeIfNeeded(
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
        if (text == null) return;
        if (maxW <= 0) return;

        int fullW = WildexUiText.width(font, text);
        if (fullW <= maxW) {
            WildexUiText.draw(g, font, text, x, y, color, false);
            return;
        }

        int sx0 = toScreenX(screenOriginX, scale, x);
        int sy0 = toScreenY(screenOriginY, scale, y);
        int sx1 = toScreenX(screenOriginX, scale, x + maxW);
        int sy1 = toScreenY(screenOriginY, scale, y + WildexUiText.lineHeight(font) + 1);

        long now = Util.getMillis();
        double seconds = now / 1000.0;

        int travel = fullW + MARQUEE_GAP_PX;
        double offset = (seconds * MARQUEE_SPEED_PX_PER_SEC) % travel;
        int baseX = x - (int) Math.floor(offset);

        WildexScissor.enablePhysical(g, sx0, sy0, sx1, sy1);
        try {
            WildexUiText.draw(g, font, text, baseX, y, color, false);
            WildexUiText.draw(g, font, text, baseX + travel, y, color, false);
        } finally {
            g.disableScissor();
        }
    }
}




