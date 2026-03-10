package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class WildexFilterChipButton extends AbstractWidget {

    private static final int PAD_X = 5;
    private static final int MARK_W = 7;
    private static final int MARK_GAP = 4;
    private static final int ACTIVE_BORDER = 0xFF3A2618;
    private static final int INACTIVE_BORDER = 0xFF4A3427;
    private static final int ACTIVE_BG = 0xFF3A2618;
    private static final int INACTIVE_BG = 0xFFB99E81;
    private static final int ACTIVE_TEXT = 0xFFF7E7CC;
    private static final int INACTIVE_TEXT = 0xFF2B1A10;
    private static final int ACTIVE_MARK = 0xFFF2C66D;

    private final Consumer<Boolean> onChange;
    private final Component tooltip;
    private boolean checked;

    public WildexFilterChipButton(
            int x,
            int y,
            int width,
            int height,
            Component label,
            boolean initialChecked,
            Consumer<Boolean> onChange,
            Component tooltip
    ) {
        super(x, y, width, height, label == null ? Component.empty() : label);
        this.checked = initialChecked;
        this.onChange = onChange == null ? value -> {
        } : onChange;
        this.tooltip = tooltip;
    }

    public static int widthFor(Font font, Component label) {
        Font safeFont = font == null ? Minecraft.getInstance().font : font;
        return Math.max(14, WildexUiText.width(safeFont, label == null ? Component.empty() : label) + (PAD_X * 2) + MARK_GAP + MARK_W);
    }

    public boolean isChecked() {
        return this.checked;
    }

    public Component tooltip() {
        return this.tooltip;
    }

    public void setChecked(boolean checked, boolean notify) {
        if (this.checked == checked) return;
        this.checked = checked;
        if (notify) {
            this.onChange.accept(this.checked);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!this.active || !this.visible) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        this.checked = !this.checked;
        this.onChange.accept(this.checked);
        WildexUiSounds.playButtonClick();
        this.setFocused(false);
        return true;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean modern = WildexThemes.isModernLayout();
        boolean runes = WildexThemes.current().layoutProfile() == ClientConfig.DesignStyle.RUNES;
        int activeBg = modern ? 0xFF1A8E97 : (runes ? 0xFF3F2A63 : ACTIVE_BG);
        int inactiveBg = modern ? 0xFF7FB6BC : (runes ? 0xFFBDA8DB : INACTIVE_BG);
        int activeFg = modern ? 0xFFF4FEFF : (runes ? 0xFFF8F1FF : ACTIVE_TEXT);
        int inactiveFg = modern ? 0xFF0F3A3F : (runes ? 0xFF27153E : INACTIVE_TEXT);
        int activeBorder = modern ? 0xFF0D6770 : (runes ? 0xFF8E6FD1 : ACTIVE_BORDER);
        int inactiveBorder = modern ? 0xFF356D73 : (runes ? 0xFF5C4388 : INACTIVE_BORDER);
        int activeMark = modern ? 0xFFF7F29A : (runes ? 0xFFAEEBFF : ACTIVE_MARK);
        int bg = this.checked ? activeBg : inactiveBg;
        int fg = this.checked ? activeFg : inactiveFg;
        int border = this.checked ? activeBorder : inactiveBorder;

        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.getWidth();
        int y1 = y0 + this.getHeight();
        int textY = y0 + Math.max(0, (this.getHeight() - WildexUiText.lineHeight(font)) / 2) + 1;

        graphics.fill(x0, y0, x1, y1, bg);
        graphics.fill(x0, y0, x1, y0 + 1, border);
        graphics.fill(x0, y1 - 1, x1, y1, border);
        graphics.fill(x0, y0, x0 + 1, y1, border);
        graphics.fill(x1 - 1, y0, x1, y1, border);

        WildexUiText.draw(graphics, font, this.getMessage(), x0 + PAD_X, textY, fg, false);

        if (this.checked) {
            int markX = x1 - PAD_X - MARK_W;
            int markY = y0 + Math.max(1, (this.getHeight() - 5) / 2);
            graphics.fill(markX, markY + 2, markX + 1, markY + 4, activeMark);
            graphics.fill(markX + 1, markY + 3, markX + 2, markY + 5, activeMark);
            graphics.fill(markX + 2, markY + 2, markX + 3, markY + 4, activeMark);
            graphics.fill(markX + 3, markY + 1, markX + 4, markY + 3, activeMark);
            graphics.fill(markX + 4, markY, markX + 5, markY + 2, activeMark);
            graphics.fill(markX + 5, markY, markX + 6, markY + 1, activeMark);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, this.getMessage());
        out.add(NarratedElementType.USAGE, Component.literal(this.checked ? "On" : "Off"));
    }
}
