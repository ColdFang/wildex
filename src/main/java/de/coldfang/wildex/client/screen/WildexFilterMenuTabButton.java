package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class WildexFilterMenuTabButton extends AbstractWidget {

    private static final int PAD_X = 8;
    private static final int ICON_PAD_RIGHT = 1;
    private static final int LABEL_INDICATOR_GAP = 6;
    private static final int TEXT_PAD_Y_TOP = 2;
    private static final int TEXT_PAD_Y_BOTTOM = 1;
    private static final float MIN_TEXT_SCALE = 0.75f;
    private static final ResourceLocation HOPPER_ICON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/hopper.png");

    private final Runnable onPress;
    private final BooleanSupplier openSupplier;
    private final Supplier<String> indicatorSupplier;
    private final boolean showFunnelIcon;

    public WildexFilterMenuTabButton(
            int x,
            int y,
            int width,
            int height,
            Component label,
            Runnable onPress,
            BooleanSupplier openSupplier,
            Supplier<String> indicatorSupplier,
            boolean showFunnelIcon
    ) {
        super(x, y, width, height, label == null ? Component.empty() : label);
        this.onPress = onPress == null ? () -> {} : onPress;
        this.openSupplier = Objects.requireNonNull(openSupplier, "openSupplier");
        this.indicatorSupplier = indicatorSupplier == null ? () -> "" : indicatorSupplier;
        this.showFunnelIcon = showFunnelIcon;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!this.active || !this.visible) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        this.onPress.run();
        WildexUiSounds.playButtonClick();
        this.setFocused(false);
        return true;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        boolean modern = WildexThemes.isModernLayout();
        boolean open = this.openSupplier.getAsBoolean();

        int x0 = this.getX();
        int y0 = this.getY();

        var font = Minecraft.getInstance().font;
        int color = open
                ? (modern ? 0xFF000000 : theme.selectionBg())
                : theme.ink();
        int indicatorColor = modern ? 0xFFFFFFFF : color;

        String indicator = this.indicatorSupplier.get();
        if (indicator == null) indicator = "";

        int padTop = (this.height <= 16) ? 1 : TEXT_PAD_Y_TOP;
        int padBottom = (this.height <= 16) ? 0 : TEXT_PAD_Y_BOTTOM;
        int availableH = Math.max(1, (this.height - 2) - padTop - padBottom);
        int labelYBase = y0 + 1 + padTop;

        int iconSize = filterIconSize();
        int iconX = iconLeftX();
        int iconY = y0 + Math.max(1, (this.height - iconSize) / 2);
        int indicatorW = indicator.isBlank() ? 0 : WildexUiText.width(font, indicator);
        int labelAreaX0 = x0 + PAD_X;
        int labelAreaX1 = indicatorW > 0 ? iconX - LABEL_INDICATOR_GAP : iconX;
        int labelAreaW = Math.max(1, labelAreaX1 - labelAreaX0);

        if (this.showFunnelIcon) {
            drawFilterIcon(graphics, iconX, iconY, iconSize);
        } else {
            String label = this.getMessage().getString();
            int labelW = WildexUiText.width(font, label);
            int labelH = WildexUiText.lineHeight(font);
            float scaleW = labelW <= 0 ? 1.0f : Math.min(1.0f, labelAreaW / (float) labelW);
            float scaleH = availableH / (float) labelH;
            float textScale = Math.max(MIN_TEXT_SCALE, Math.min(1.0f, Math.min(scaleW, scaleH)));

            float scaledLabelW = labelW * textScale;
            float scaledLabelH = labelH * textScale;
            int labelX = Math.round(labelAreaX0 + Math.max(0.0f, (labelAreaW - scaledLabelW) / 2.0f));
            int labelY = Math.round(labelYBase + Math.max(0.0f, (availableH - scaledLabelH) / 2.0f));

            if (textScale >= 0.999f) {
                WildexUiText.draw(graphics, font, label, labelX, labelY, color, false);
            } else {
                graphics.pose().pushPose();
                graphics.pose().translate(labelX, labelY, 0.0f);
                graphics.pose().scale(textScale, textScale, 1.0f);
                WildexUiText.draw(graphics, font, label, 0, 0, color, false);
                graphics.pose().popPose();
            }
        }

        if (!indicator.isBlank()) {
            int indicatorX = Math.max(labelAreaX0, iconX - LABEL_INDICATOR_GAP - indicatorW);
            int indicatorY = y0 + Math.max(0, (this.height - WildexUiText.lineHeight(font)) / 2) + 1;
            WildexUiText.draw(graphics, font, indicator, indicatorX, indicatorY, indicatorColor, false);
        }
    }

    private int filterIconSize() {
        int preferred = Math.max(16, Math.round(16.0f * WildexUiScale.get()));
        int maxByHeight = Math.max(12, this.height - 2);
        return Math.min(preferred, maxByHeight);
    }

    private int iconLeftX() {
        int iconSize = filterIconSize();
        return (this.getX() + this.width) - ICON_PAD_RIGHT - iconSize;
    }

    private int iconRightX() {
        return iconLeftX() + filterIconSize();
    }

    public Component tooltip() {
        return this.getMessage();
    }

    private static void drawFilterIcon(GuiGraphics graphics, int x, int y, int iconSize) {
        graphics.blit(
                HOPPER_ICON_TEXTURE,
                x,
                y,
                iconSize,
                iconSize,
                0,
                0,
                16,
                16,
                16,
                16
        );
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, this.getMessage());
        out.add(NarratedElementType.USAGE, Component.literal(this.openSupplier.getAsBoolean() ? "Expanded" : "Collapsed"));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int x0 = iconLeftX();
        int x1 = iconRightX();
        int y0 = this.getY();
        int y1 = y0 + this.height;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }
}
