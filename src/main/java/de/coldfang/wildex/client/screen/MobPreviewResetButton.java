package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public final class MobPreviewResetButton extends Button {

    private static final float LABEL_SCALE = 0.75f;

    public MobPreviewResetButton(int x, int y, int w, int h, Runnable onPress) {
        super(
                x,
                y,
                w,
                h,
                Component.empty(),
                b -> Objects.requireNonNull(onPress, "onPress").run(),
                DEFAULT_NARRATION
        );
    }

    @Override
    public void onPress() {
        super.onPress();
        this.setFocused(false);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean wasFocused = this.isFocused();
        if (wasFocused) this.setFocused(false);

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        if (wasFocused) this.setFocused(true);

        var font = Minecraft.getInstance().font;
        String label = "R";

        int color = this.getFGColor();
        int cx = this.getX() + this.getWidth() / 2;
        int cy = this.getY() + this.getHeight() / 2;

        int textW = font.width(label);
        int textH = font.lineHeight;

        float sx = cx - (textW * LABEL_SCALE) / 2.0f;
        float sy = cy - (textH * LABEL_SCALE) / 2.0f;

        graphics.pose().pushPose();
        graphics.pose().translate(sx, sy, 0.0f);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0f);
        graphics.drawString(font, label, 0, 0, color, false);
        graphics.pose().popPose();
    }
}
