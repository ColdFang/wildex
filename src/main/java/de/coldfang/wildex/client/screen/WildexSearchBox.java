package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class WildexSearchBox extends EditBox {

    private int textNudgeX = 2;
    private int textNudgeY = 2;

    public WildexSearchBox(Font font, int x, int y, int w, int h, Component message) {
        super(font, x, y, w, h, message);
        this.setBordered(false);
        this.setTextShadow(false);
    }

    public void setTextNudge(int x, int y) {
        this.textNudgeX = x;
        this.textNudgeY = y;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.getWidth();
        int y1 = y0 + this.getHeight();

        drawFrame(g, x0, y0, x1, y1);

        float textScale = resolveTextScale();
        if (textScale <= 1.001f) {
            g.pose().pushPose();
            g.pose().translate(textNudgeX, textNudgeY, 0.0f);
            super.renderWidget(g, mouseX, mouseY, partialTick);
            g.pose().popPose();
            return;
        }

        int oldX = this.getX();
        int oldY = this.getY();
        int oldW = this.getWidth();
        int oldH = this.getHeight();

        int targetX = oldX + textNudgeX;
        int targetY = oldY + textNudgeY;

        this.setX(Math.round(targetX / textScale));
        this.setY(Math.round(targetY / textScale));
        this.setWidth(Math.max(1, Math.round(oldW / textScale)));
        this.setHeight(Math.max(1, Math.round(oldH / textScale)));

        int scaledMouseX = Math.round(mouseX / textScale);
        int scaledMouseY = Math.round(mouseY / textScale);

        g.pose().pushPose();
        g.pose().scale(textScale, textScale, 1.0f);
        super.renderWidget(g, scaledMouseX, scaledMouseY, partialTick);
        g.pose().popPose();

        this.setX(oldX);
        this.setY(oldY);
        this.setWidth(oldW);
        this.setHeight(oldH);
    }

    private static float resolveTextScale() {
        return Math.max(1.0f, WildexUiScale.get());
    }

    private static void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int outer = WildexThemes.isVintageLayout() ? 3 : 1;
        int inner = 1;
        int inset = outer + inner;

        g.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, theme.frameBg());

        for (int i = 0; i < outer; i++) {
            int ox0 = x0 + i;
            int oy0 = y0 + i;
            int ox1 = x1 - i;
            int oy1 = y1 - i;
            g.fill(ox0, oy0, ox1, oy0 + 1, theme.frameOuter());
            g.fill(ox0, oy1 - 1, ox1, oy1, theme.frameOuter());
            g.fill(ox0, oy0, ox0 + 1, oy1, theme.frameOuter());
            g.fill(ox1 - 1, oy0, ox1, oy1, theme.frameOuter());
        }

        for (int i = 0; i < inner; i++) {
            int ii = outer + i;
            int ix0 = x0 + ii;
            int iy0 = y0 + ii;
            int ix1 = x1 - ii;
            int iy1 = y1 - ii;
            g.fill(ix0, iy0, ix1, iy0 + 1, theme.frameInner());
            g.fill(ix0, iy1 - 1, ix1, iy1, theme.frameInner());
            g.fill(ix0, iy0, ix0 + 1, iy1, theme.frameInner());
            g.fill(ix1 - 1, iy0, ix1, iy1, theme.frameInner());
        }
    }
}



