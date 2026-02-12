package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class WildexDiscoveredOnlyCheckbox extends AbstractWidget {

    private static final int FRAME_BG = 0x22FFFFFF;
    private static final int FRAME_OUTER = 0x88301E14;
    private static final int FRAME_INNER = 0x55FFFFFF;

    private static final int BOX_IDLE = 0x22000000;
    private static final int BOX_HOVER = 0x33000000;
    private static final int CHECK_BG = 0x44000000;

    private static final int INK = 0x2B1A10;

    private static final Component NARRATION_TITLE = Component.literal("Discovered only");
    private static final Component NARRATION_ON = Component.literal("Checked. Only discovered mobs are shown.");
    private static final Component NARRATION_OFF = Component.literal("Unchecked. All mobs are shown.");

    private boolean checked;
    private final Consumer<Boolean> onChange;
    private final Component tooltip;

    public WildexDiscoveredOnlyCheckbox(int x, int y, int size, boolean initialChecked, Consumer<Boolean> onChange, Component tooltip) {
        super(x, y, size, size, Component.empty());
        this.checked = initialChecked;
        this.onChange = onChange == null ? v -> { } : onChange;
        this.tooltip = tooltip;
    }

    public boolean isChecked() {
        return checked;
    }

    public Component tooltip() {
        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!this.active || !this.visible) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        this.checked = !this.checked;
        this.onChange.accept(this.checked);
        this.setFocused(false);
        return true;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        drawFrame(g, x0, y0, x1, y1);

        int fill = this.isHoveredOrFocused() ? BOX_HOVER : BOX_IDLE;
        g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, fill);

        if (checked) {
            g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, CHECK_BG);

            var font = Minecraft.getInstance().font;
            String mark = "✔";
            int markW = font.width(mark);
            int markH = font.lineHeight;

            int cx = x0 + (getWidth() / 2);
            int cy = y0 + (getHeight() / 2);

            int tx = cx - (markW / 2);
            int ty = cy - (markH / 2);

            g.drawString(font, mark, tx, ty, INK, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, NARRATION_TITLE);
        out.add(NarratedElementType.USAGE, checked ? NARRATION_ON : NARRATION_OFF);
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
