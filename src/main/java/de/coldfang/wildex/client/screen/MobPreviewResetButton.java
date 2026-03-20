package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MobPreviewResetButton extends Button {

    private static final float MIN_LABEL_SCALE = 0.82f;
    private static final float MAX_LABEL_SCALE = 1.08f;
    private static final float LABEL_OPTICAL_SHIFT_X = 0.8f;
    private static final float LABEL_OPTICAL_SHIFT_Y = 0.6f;
    private static final int CONTROL_FILL = 0xCC1A120C;
    private static final int CONTROL_FILL_HOVER = 0xE0261B13;
    private static final int CONTROL_FILL_DISABLED = 0x8C1A120C;
    private static final int CONTROL_FILL_RUNES = 0xCC2D1E4A;
    private static final int CONTROL_FILL_HOVER_RUNES = 0xE0432D69;
    private static final int CONTROL_FILL_DISABLED_RUNES = 0x8C2D1E4A;
    private static final int CONTROL_SYMBOL = 0xFFFFF6E8;
    private static final int CONTROL_SYMBOL_RUNES = 0xFFF6EEFF;
    private static final int CONTROL_SYMBOL_DISABLED = 0xCCB9B0A3;
    private static final int CONTROL_SYMBOL_DISABLED_RUNES = 0xCCCEC0EA;
    private String label;

    public MobPreviewResetButton(int x, int y, int w, int h, Runnable onPress) {
        this(x, y, w, h, "R", onPress);
    }

    public MobPreviewResetButton(int x, int y, int w, int h, String label, Runnable onPress) {
        super(
                x,
                y,
                w,
                h,
                Component.empty(),
                b -> Objects.requireNonNull(onPress, "onPress").run(),
                DEFAULT_NARRATION
        );
        this.label = (label == null || label.isBlank()) ? "R" : label;
    }

    public void setLabel(String label) {
        this.label = (label == null || label.isBlank()) ? "R" : label;
    }

    @Override
    public void onPress() {
        super.onPress();
        this.setFocused(false);
    }

    @Override
    public void playDownSound(@NotNull SoundManager soundManager) {
        WildexUiSounds.playButtonClick();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int drawX = this.getX();
        int drawY = this.getY();
        int drawW = this.getWidth();
        int drawH = this.getHeight();
        boolean runes = WildexThemes.current().layoutProfile() == ClientConfig.DesignStyle.RUNES;
        int fill = !this.active
                ? (runes ? CONTROL_FILL_DISABLED_RUNES : CONTROL_FILL_DISABLED)
                : (this.isHoveredOrFocused()
                ? (runes ? CONTROL_FILL_HOVER_RUNES : CONTROL_FILL_HOVER)
                : (runes ? CONTROL_FILL_RUNES : CONTROL_FILL));
        int color = this.active
                ? (runes ? CONTROL_SYMBOL_RUNES : CONTROL_SYMBOL)
                : (runes ? CONTROL_SYMBOL_DISABLED_RUNES : CONTROL_SYMBOL_DISABLED);

        WildexUiRenderUtil.drawMenuStyleControlBase(graphics, drawX, drawY, drawW, drawH, fill);

        int contentX = drawX + 2;
        int contentY = drawY + 2;
        int contentW = Math.max(1, drawW - 4);
        int contentH = Math.max(1, drawH - 4);

        int textW = WildexUiText.width(font, label);
        int textH = WildexUiText.lineHeight(font);
        int innerW = Math.max(1, contentW - 4);
        int innerH = Math.max(1, contentH - 4);
        float scaleW = innerW / (float) Math.max(1, textW);
        float scaleH = innerH / (float) Math.max(1, textH);
        float drawScale = Math.min(MAX_LABEL_SCALE, Math.min(scaleW, scaleH));
        drawScale = Math.max(MIN_LABEL_SCALE, drawScale);

        float scaledTextW = textW * drawScale;
        float scaledTextH = textH * drawScale;
        float sx = contentX + ((contentW - scaledTextW) / 2.0f) + LABEL_OPTICAL_SHIFT_X;
        float sy = contentY + ((contentH - scaledTextH) / 2.0f) + LABEL_OPTICAL_SHIFT_Y;

        graphics.pose().pushPose();
        graphics.pose().translate(sx, sy, 0.0f);
        graphics.pose().scale(drawScale, drawScale, 1.0f);
        WildexUiText.draw(graphics, font, label, 0, 0, color, false);
        graphics.pose().popPose();
    }
}



