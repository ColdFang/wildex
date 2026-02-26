package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class WildexScissor {

    private WildexScissor() {
    }

    public static void enablePhysical(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        Minecraft mc = Minecraft.getInstance();
        float guiScale = Math.max(1.0f, (float) mc.getWindow().getGuiScale());

        int gx0 = (int) Math.floor(x0 / guiScale);
        int gy0 = (int) Math.floor(y0 / guiScale);
        int gx1 = (int) Math.ceil(x1 / guiScale);
        int gy1 = (int) Math.ceil(y1 / guiScale);

        int sx0 = Math.min(gx0, gx1);
        int sy0 = Math.min(gy0, gy1);
        int sx1 = Math.max(gx0, gx1);
        int sy1 = Math.max(gy0, gy1);

        int maxW = Math.max(1, mc.getWindow().getGuiScaledWidth());
        int maxH = Math.max(1, mc.getWindow().getGuiScaledHeight());

        sx0 = Math.max(0, Math.min(sx0, maxW));
        sx1 = Math.max(0, Math.min(sx1, maxW));
        sy0 = Math.max(0, Math.min(sy0, maxH));
        sy1 = Math.max(0, Math.min(sy1, maxH));

        // Always push a valid scissor region so matching disableScissor() calls can never underflow.
        if (sx1 <= sx0) {
            sx0 = Math.max(0, Math.min(sx0, maxW - 1));
            sx1 = Math.min(maxW, sx0 + 1);
        }
        if (sy1 <= sy0) {
            sy0 = Math.max(0, Math.min(sy0, maxH - 1));
            sy1 = Math.min(maxH, sy0 + 1);
        }

        graphics.enableScissor(sx0, sy0, sx1, sy1);
    }
}



