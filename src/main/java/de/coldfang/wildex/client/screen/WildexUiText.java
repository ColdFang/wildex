package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class WildexUiText {

    private WildexUiText() {
    }

    public static float scale() {
        return WildexUiScale.snap(WildexUiScale.get());
    }

    private static int snapLogicalCoord(int screenCoordPx, float scale) {
        return Math.round(screenCoordPx / scale);
    }

    public static int lineHeight(Font font) {
        return Math.max(1, Math.round(font.lineHeight * scale()));
    }

    public static int width(Font font, String text) {
        return Math.max(0, Math.round(font.width(text) * scale()));
    }

    public static int width(Font font, Component text) {
        return Math.max(0, Math.round(font.width(text) * scale()));
    }

    public static int width(Font font, FormattedCharSequence text) {
        return Math.max(0, Math.round(font.width(text) * scale()));
    }

    public static int draw(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        float s = scale();
        if (s >= 0.999f && s <= 1.001f) {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        graphics.pose().pushPose();
        graphics.pose().scale(s, s, 1.0f);
        int out = graphics.drawString(font, text, snapLogicalCoord(x, s), snapLogicalCoord(y, s), color, shadow);
        graphics.pose().popPose();
        return out;
    }

    public static int draw(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        float s = scale();
        if (s >= 0.999f && s <= 1.001f) {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        graphics.pose().pushPose();
        graphics.pose().scale(s, s, 1.0f);
        int out = graphics.drawString(font, text, snapLogicalCoord(x, s), snapLogicalCoord(y, s), color, shadow);
        graphics.pose().popPose();
        return out;
    }

    public static int draw(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        float s = scale();
        if (s >= 0.999f && s <= 1.001f) {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        graphics.pose().pushPose();
        graphics.pose().scale(s, s, 1.0f);
        int out = graphics.drawString(font, text, snapLogicalCoord(x, s), snapLogicalCoord(y, s), color, shadow);
        graphics.pose().popPose();
        return out;
    }
}


