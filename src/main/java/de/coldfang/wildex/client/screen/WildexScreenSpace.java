package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class WildexScreenSpace {

    private final float guiScale;
    private final int physicalWidth;
    private final int physicalHeight;

    private WildexScreenSpace(float guiScale, int physicalWidth, int physicalHeight) {
        this.guiScale = Math.max(1.0f, guiScale);
        this.physicalWidth = Math.max(1, physicalWidth);
        this.physicalHeight = Math.max(1, physicalHeight);
    }

    public static WildexScreenSpace fromWindow(Minecraft mc) {
        return new WildexScreenSpace(
                (float) mc.getWindow().getGuiScale(),
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight()
        );
    }

    public float guiScale() {
        return guiScale;
    }

    public int physicalWidth() {
        return physicalWidth;
    }

    public int physicalHeight() {
        return physicalHeight;
    }

    public int toPhysicalX(double guiX) {
        return (int) Math.floor(guiX * this.guiScale);
    }

    public int toPhysicalY(double guiY) {
        return (int) Math.floor(guiY * this.guiScale);
    }

    public void pushInverseScale(GuiGraphics graphics) {
        graphics.pose().pushPose();
        float inv = 1.0f / this.guiScale;
        graphics.pose().scale(inv, inv, 1.0f);
    }

    public void popScale(GuiGraphics graphics) {
        graphics.pose().popPose();
    }
}



