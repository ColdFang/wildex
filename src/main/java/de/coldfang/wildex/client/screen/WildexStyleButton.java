package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class WildexStyleButton extends Button {

    private static final int FRAME_BG = 0x3AFFFFFF;
    private static final int FRAME_OUTER = 0x99D6B89C;
    private static final int FRAME_INNER = 0x77FFFFFF;

    private static final int FILL_IDLE = 0x33140E0A;
    private static final int FILL_HOVER = 0x55231811;
    private static final int FILL_DISABLED = 0x16000000;

    private static final int INK = 0xFFEFDCC7;
    private static final int INK_DISABLED = 0x99C8B8A7;
    private static final int TEXT_Y_OFFSET = 1;

    private final Runnable action;

    public WildexStyleButton(int x, int y, int w, int h, Runnable action) {
        super(
                x,
                y,
                w,
                h,
                Component.literal("Theme"),
                b -> {},
                DEFAULT_NARRATION
        );
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
        setFocused(false);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        drawFrame(graphics, x0, y0, x1, y1);

        int fill;
        if (!this.active) fill = FILL_DISABLED;
        else fill = this.isHoveredOrFocused() ? FILL_HOVER : FILL_IDLE;
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, fill);

        var font = Minecraft.getInstance().font;
        int color = this.active ? INK : INK_DISABLED;
        int tx = x0 + (getWidth() - font.width(getMessage())) / 2;
        int ty = y0 + (getHeight() - font.lineHeight) / 2 + TEXT_Y_OFFSET;
        graphics.drawString(font, getMessage(), tx, ty, color, false);
    }

    private static void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, FRAME_BG);

        g.fill(x0, y0, x1, y0 + 1, FRAME_OUTER);
        g.fill(x0, y1 - 1, x1, y1, FRAME_OUTER);
        g.fill(x0, y0, x0 + 1, y1, FRAME_OUTER);
        g.fill(x1 - 1, y0, x1, y1, FRAME_OUTER);

        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, FRAME_INNER);
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, FRAME_INNER);
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, FRAME_INNER);
        g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, FRAME_INNER);
    }
}
