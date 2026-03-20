package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class WildexDiscoveredOnlyCheckbox extends AbstractWidget {

    private static final Component NARRATION_TITLE = Component.translatable("gui.wildex.discovered_only");
    private static final Component NARRATION_ON = Component.translatable("narration.wildex.discovered_only.on");
    private static final Component NARRATION_OFF = Component.translatable("narration.wildex.discovered_only.off");
    private static final int CONTROL_FILL = 0xCC1A120C;
    private static final int CONTROL_FILL_HOVER = 0xE0261B13;
    private static final int CONTROL_FILL_RUNES = 0xCC2D1E4A;
    private static final int CONTROL_FILL_HOVER_RUNES = 0xE0432D69;
    private static final int CONTROL_SYMBOL = 0xFFFFF6E8;
    private static final int CONTROL_SYMBOL_RUNES = 0xFFF6EEFF;

    private boolean checked;
    private final Consumer<Boolean> onChange;
    private final Component tooltip;

    public WildexDiscoveredOnlyCheckbox(int x, int y, int size, boolean initialChecked, Consumer<Boolean> onChange, Component tooltip) {
        super(x, y, size, size, Component.empty());
        this.checked = initialChecked;
        this.onChange = onChange == null ? v -> {
        } : onChange;
        this.tooltip = tooltip;
    }

    public boolean isChecked() {
        return checked;
    }

    public Component tooltip() {
        return tooltip;
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
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int size = Math.min(getWidth(), getHeight());
        int drawX = x0 + ((getWidth() - size) / 2);
        int drawY = y0 + ((getHeight() - size) / 2);
        boolean runes = WildexThemes.current().layoutProfile() == ClientConfig.DesignStyle.RUNES;
        int fill = this.isHoveredOrFocused()
                ? (runes ? CONTROL_FILL_HOVER_RUNES : CONTROL_FILL_HOVER)
                : (runes ? CONTROL_FILL_RUNES : CONTROL_FILL);
        WildexUiRenderUtil.drawMenuStyleControlBase(g, drawX, drawY, size, fill);

        if (checked) {
            var font = Minecraft.getInstance().font;
            String mark = Character.toString((char) 0x2714);
            int markW = WildexUiText.width(font, mark);
            int markH = WildexUiText.lineHeight(font);

            int cx = drawX + (size / 2);
            int cy = drawY + (size / 2);

            int tx = cx - (markW / 2);
            int ty = cy - (markH / 2);

            WildexUiText.draw(g, font, mark, tx, ty, runes ? CONTROL_SYMBOL_RUNES : CONTROL_SYMBOL, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, NARRATION_TITLE);
        out.add(NarratedElementType.USAGE, checked ? NARRATION_ON : NARRATION_OFF);
    }
}



