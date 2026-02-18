package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class WildexPanelButton extends Button {

    private final Runnable action;

    public WildexPanelButton(int x, int y, int w, int h, Component label, Runnable action) {
        super(x, y, w, h, label, b -> {}, DEFAULT_NARRATION);
        this.action = action;
    }

    @Override
    public void onPress() {
        if (action != null) action.run();
        setFocused(false);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        drawFrame(graphics, x0, y0, x1, y1, theme);

        int fill;
        if (!this.active) fill = theme.buttonFillDisabled();
        else fill = this.isHoveredOrFocused() ? theme.buttonFillHover() : theme.buttonFillIdle();
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, fill);

        var font = Minecraft.getInstance().font;
        int color = this.active ? theme.buttonInk() : theme.buttonInkDisabled();
        String s = getMessage().getString();
        int textW = font.width(s);
        int textH = font.lineHeight;
        int availW = Math.max(1, getWidth() - 6);
        int availH = Math.max(1, getHeight() - 4);

        float scaleW = textW <= 0 ? 1.0f : ((float) availW / (float) textW);
        float scaleH = (float) availH / (float) textH;
        float ts = Math.min(1.0f, Math.min(scaleW, scaleH));
        if (ts < 0.62f) ts = 0.62f;

        float drawW = textW * ts;
        float drawH = textH * ts;
        float drawX = x0 + (getWidth() - drawW) / 2.0f;
        float drawY = y0 + (getHeight() - drawH) / 2.0f + 0.8f;

        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0f);
        graphics.pose().scale(ts, ts, 1.0f);
        graphics.drawString(font, s, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1, WildexUiTheme.Palette theme) {
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, theme.frameBg());

        g.fill(x0, y0, x1, y0 + 1, theme.frameOuter());
        g.fill(x0, y1 - 1, x1, y1, theme.frameOuter());
        g.fill(x0, y0, x0 + 1, y1, theme.frameOuter());
        g.fill(x1 - 1, y0, x1, y1, theme.frameOuter());

        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, theme.frameInner());
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, theme.frameInner());
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, theme.frameInner());
        g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, theme.frameInner());
    }
}
