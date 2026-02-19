package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class WildexScissor {

    private WildexScissor() {
    }

    public static void enablePhysical(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        float guiScale = Math.max(1.0f, (float) Minecraft.getInstance().getWindow().getGuiScale());
        int gx0 = (int) Math.floor(x0 / guiScale);
        int gy0 = (int) Math.floor(y0 / guiScale);
        int gx1 = (int) Math.ceil(x1 / guiScale);
        int gy1 = (int) Math.ceil(y1 / guiScale);
        if (gx1 <= gx0 || gy1 <= gy0) return;
        graphics.enableScissor(gx0, gy0, gx1, gy1);
    }
}



