package de.coldfang.wildex.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class WildexStyleButton extends Button {

    private final Runnable action;

    public WildexStyleButton(int x, int y, int w, int h, Runnable action) {
        super(
                x,
                y,
                w,
                h,
                Component.literal("Style"),
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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean restoreFocus = isFocused();
        setFocused(false);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (restoreFocus) setFocused(true);
    }
}
