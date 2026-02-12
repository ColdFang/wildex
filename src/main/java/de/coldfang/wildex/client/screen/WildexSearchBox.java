package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class WildexSearchBox extends EditBox {

    private static final int BG = 0x22FFFFFF;
    private static final int BORDER = 0x88301E14;
    private static final int INNER = 0x55FFFFFF;

    private static final int TEXT_NUDGE_X = 2;
    private static final int TEXT_NUDGE_Y = 2;

    public WildexSearchBox(Font font, int x, int y, int w, int h, Component message) {
        super(font, x, y, w, h, message);
        this.setBordered(false);
        this.setTextShadow(false);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.getWidth();
        int y1 = y0 + this.getHeight();

        drawFrame(g, x0, y0, x1, y1);

        g.pose().pushPose();
        g.pose().translate(TEXT_NUDGE_X, TEXT_NUDGE_Y, 0.0f);
        super.renderWidget(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }

    private static void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, BG);

        g.fill(x0, y0, x1, y0 + 1, BORDER);
        g.fill(x0, y1 - 1, x1, y1, BORDER);
        g.fill(x0, y0, x0 + 1, y1, BORDER);
        g.fill(x1 - 1, y0, x1, y1, BORDER);

        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, INNER);
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, INNER);
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, INNER);
        g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, INNER);
    }
}
