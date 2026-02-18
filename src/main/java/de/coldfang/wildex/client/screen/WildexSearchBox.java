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

        g.pose().pushPose();
        g.pose().translate(textNudgeX, textNudgeY, 0.0f);
        super.renderWidget(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }

    private static void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
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
