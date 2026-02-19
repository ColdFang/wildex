package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

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
    private static final int ITEM_BASE_SIZE = 16;

    private final Runnable action;
    private final Supplier<ItemStack> trailingItemSupplier;

    public WildexStyleButton(int x, int y, int w, int h, Component label, Runnable action) {
        this(x, y, w, h, label, action, null);
    }

    public WildexStyleButton(int x, int y, int w, int h, Component label, Runnable action, Supplier<ItemStack> trailingItemSupplier) {
        super(
                x,
                y,
                w,
                h,
                label,
                b -> {},
                DEFAULT_NARRATION
        );
        this.action = action;
        this.trailingItemSupplier = trailingItemSupplier;
    }

    public WildexStyleButton(int x, int y, int w, int h, Runnable action) {
        this(x, y, w, h, Component.translatable("gui.wildex.theme"), action);
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
        else fill = this.isHovered() ? FILL_HOVER : FILL_IDLE;
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, fill);

        var font = Minecraft.getInstance().font;
        int color = this.active ? INK : INK_DISABLED;
        ItemStack trailing = this.trailingItemSupplier == null ? ItemStack.EMPTY : this.trailingItemSupplier.get();
        boolean hasTrailing = trailing != null && !trailing.isEmpty();
        int iconSize = hasTrailing ? Math.max(10, Math.round(12 * WildexUiScale.get())) : 0;
        int iconGap = hasTrailing ? Math.max(2, Math.round(3 * WildexUiScale.get())) : 0;
        int iconRightPad = hasTrailing ? Math.max(2, Math.round(2 * WildexUiScale.get())) : 0;
        int reserveRight = hasTrailing ? iconSize + iconGap + iconRightPad : 0;

        int textAreaX0 = x0 + 2;
        int textAreaX1 = x1 - 2 - reserveRight;
        int textAreaW = Math.max(1, textAreaX1 - textAreaX0);
        int textW = WildexUiText.width(font, getMessage());
        int tx = textAreaX0 + Math.max(0, (textAreaW - textW) / 2);
        int ty = y0 + (getHeight() - WildexUiText.lineHeight(font)) / 2 + TEXT_Y_OFFSET;
        WildexUiText.draw(graphics, font, getMessage(), tx, ty, color, false);

        if (hasTrailing) {
            int iconX = x1 - 2 - iconRightPad - iconSize;
            int iconY = y0 + Math.max(1, (getHeight() - iconSize) / 2);
            drawScaledItem(graphics, trailing, iconX, iconY, iconSize);
        }
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

    private static void drawScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (size == ITEM_BASE_SIZE) {
            graphics.renderItem(stack, x, y);
            return;
        }
        float s = size / (float) ITEM_BASE_SIZE;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(s, s, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }
}



