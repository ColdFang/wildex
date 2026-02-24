package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WildexStyleButton extends Button {

    private static final int FRAME_OUTER = 0x99D6B89C;
    private static final int FRAME_INNER = 0x77FFFFFF;

    private static final int FILL_IDLE = 0x33140E0A;
    private static final int FILL_HOVER = 0x55231811;
    private static final int FILL_DISABLED = 0x16000000;

    private static final int INK = 0xFFEFDCC7;
    private static final int INK_DISABLED = 0x99C8B8A7;
    private static final int TEXT_Y_OFFSET = 1;
    private static final int ITEM_BASE_SIZE = 16;
    private static final int BG_TEX_SIZE = 128;

    private final Runnable action;
    private final Supplier<ItemStack> trailingItemSupplier;
    private final Supplier<ResourceLocation> backgroundTextureSupplier;
    private final Supplier<String> trailingSymbolSupplier;
    private final float labelScale;
    private final Supplier<Integer> backgroundColorSupplier;
    private final boolean thickFrame;
    private final Integer customOuterFrameColor;
    private final Integer customInnerFrameColor;
    private Integer customOuterThickness = null;
    private Integer customInnerThickness = null;
    private Integer customFillInset = null;
    private Integer clipLeftX = null;
    private int trailingOffsetX = 0;

    public WildexStyleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            Runnable action,
            Supplier<ItemStack> trailingItemSupplier,
            Supplier<ResourceLocation> backgroundTextureSupplier
    ) {
        this(x, y, w, h, label, action, trailingItemSupplier, backgroundTextureSupplier, null, 1.0f, null, true, null, null);
    }

    public WildexStyleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            Runnable action,
            Supplier<ItemStack> trailingItemSupplier,
            Supplier<ResourceLocation> backgroundTextureSupplier,
            Supplier<String> trailingSymbolSupplier
    ) {
        this(x, y, w, h, label, action, trailingItemSupplier, backgroundTextureSupplier, trailingSymbolSupplier, 1.0f, null, true, null, null);
    }

    public WildexStyleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            Runnable action,
            Supplier<ItemStack> trailingItemSupplier,
            Supplier<ResourceLocation> backgroundTextureSupplier,
            Supplier<String> trailingSymbolSupplier,
            float labelScale,
            Supplier<Integer> backgroundColorSupplier,
            boolean thickFrame,
            Integer customOuterFrameColor,
            Integer customInnerFrameColor
    ) {
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
        this.backgroundTextureSupplier = backgroundTextureSupplier;
        this.trailingSymbolSupplier = trailingSymbolSupplier;
        this.labelScale = Math.max(0.75f, labelScale);
        this.backgroundColorSupplier = backgroundColorSupplier;
        this.thickFrame = thickFrame;
        this.customOuterFrameColor = customOuterFrameColor;
        this.customInnerFrameColor = customInnerFrameColor;
    }

    @Override
    public void onPress() {
        action.run();
        setFocused(false);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean useClip = this.clipLeftX != null;
        if (useClip) {
            WildexScissor.enablePhysical(graphics, this.clipLeftX, getY(), getX() + getWidth(), getY() + getHeight());
        }

        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        int fill;
        if (!this.active) fill = FILL_DISABLED;
        else fill = this.isHovered() ? FILL_HOVER : FILL_IDLE;
        int fillInset = resolveFillInset();
        int ix0 = x0 + fillInset;
        int iy0 = y0 + fillInset;
        int ix1 = x1 - fillInset;
        int iy1 = y1 - fillInset;

        ResourceLocation bg = this.backgroundTextureSupplier == null ? null : this.backgroundTextureSupplier.get();
        Integer bgColor = this.backgroundColorSupplier == null ? null : this.backgroundColorSupplier.get();
        if (bgColor != null && ix1 > ix0 && iy1 > iy0) {
            graphics.fill(ix0, iy0, ix1, iy1, bgColor);
            int overlay = !this.active
                    ? 0x66000000
                    : (this.isHovered() ? 0x22000000 : 0x11000000);
            graphics.fill(ix0, iy0, ix1, iy1, overlay);
        } else if (bg != null && ix1 > ix0 && iy1 > iy0) {
            graphics.blit(
                    bg,
                    ix0,
                    iy0,
                    ix1 - ix0,
                    iy1 - iy0,
                    0,
                    0,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE
            );
            graphics.fill(ix0, iy0, ix1, iy1, fill);
        } else {
            graphics.fill(ix0, iy0, ix1, iy1, fill);
        }
        drawFrame(graphics, x0, y0, x1, y1);

        var font = Minecraft.getInstance().font;
        int color = this.active ? INK : INK_DISABLED;
        ItemStack trailing = this.trailingItemSupplier == null ? ItemStack.EMPTY : this.trailingItemSupplier.get();
        boolean hasTrailing = trailing != null && !trailing.isEmpty();
        String trailingSymbol = this.trailingSymbolSupplier == null ? "" : this.trailingSymbolSupplier.get();
        boolean hasTrailingSymbol = trailingSymbol != null && !trailingSymbol.isBlank();
        int iconSize = hasTrailing ? Math.max(10, Math.round(12 * WildexUiScale.get())) : 0;
        int iconGap = hasTrailing ? Math.max(2, Math.round(3 * WildexUiScale.get())) : 0;
        int iconRightPad = hasTrailing ? Math.max(2, Math.round(2 * WildexUiScale.get())) : 0;
        int symbolRightPad = hasTrailingSymbol ? Math.max(2, Math.round(3 * WildexUiScale.get())) : 0;
        int symbolGap = hasTrailingSymbol ? Math.max(2, Math.round(3 * WildexUiScale.get())) : 0;
        int scaledSymbolW = hasTrailingSymbol ? Math.max(1, Math.round(WildexUiText.width(font, trailingSymbol) * this.labelScale)) : 0;
        int reserveRight = hasTrailing
                ? iconSize + iconGap + iconRightPad
                : (hasTrailingSymbol ? scaledSymbolW + symbolGap + symbolRightPad : 0);

        int contentInset = Math.max(2, Math.min(6, fillInset));
        int textAreaX0 = x0 + contentInset;
        int textAreaX1 = x1 - contentInset - reserveRight;
        int textAreaW = Math.max(1, textAreaX1 - textAreaX0);
        int scaledTextW = Math.max(1, Math.round(WildexUiText.width(font, getMessage()) * this.labelScale));
        int scaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * this.labelScale));
        int tx = textAreaX0 + Math.max(0, (textAreaW - scaledTextW) / 2);
        int ty = y0 + (getHeight() - scaledLineH) / 2 + TEXT_Y_OFFSET;
        drawScaledUiText(graphics, font, getMessage(), tx, ty, color, this.labelScale);

        if (hasTrailing) {
            int iconX = x1 - contentInset - iconRightPad - iconSize + trailingOffsetX;
            int iconY = y0 + Math.max(1, (getHeight() - iconSize) / 2);
            drawScaledItem(graphics, trailing, iconX, iconY, iconSize);
        } else if (hasTrailingSymbol) {
            int sx = x1 - contentInset - symbolRightPad - scaledSymbolW + trailingOffsetX;
            int sy = y0 + (getHeight() - scaledLineH) / 2 + TEXT_Y_OFFSET;
            drawScaledUiText(graphics, font, trailingSymbol, sx, sy, color, this.labelScale);
        }

        if (useClip) {
            graphics.disableScissor();
        }
    }

    public void setClipLeftX(Integer clipLeftX) {
        this.clipLeftX = clipLeftX;
    }

    public void setTrailingOffsetX(int trailingOffsetX) {
        this.trailingOffsetX = trailingOffsetX;
    }

    public void setFrameThickness(Integer outerThickness, Integer innerThickness) {
        if (outerThickness == null || outerThickness < 1) {
            this.customOuterThickness = null;
        } else {
            this.customOuterThickness = outerThickness;
        }
        if (innerThickness == null || innerThickness < 1) {
            this.customInnerThickness = null;
        } else {
            this.customInnerThickness = innerThickness;
        }
    }

    public void setFillInset(Integer inset) {
        if (inset == null || inset < 2) {
            this.customFillInset = null;
        } else {
            this.customFillInset = inset;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int left = this.getX();
        if (this.clipLeftX != null) {
            left = Math.max(left, this.clipLeftX);
        }
        return mouseX >= left
                && mouseY >= this.getY()
                && mouseX < this.getX() + this.width
                && mouseY < this.getY() + this.height;
    }

    private void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        int outerCol = this.customOuterFrameColor == null ? FRAME_OUTER : this.customOuterFrameColor;
        int innerCol = this.customInnerFrameColor == null ? FRAME_INNER : this.customInnerFrameColor;
        int outerThickness = resolveOuterThickness();
        int innerThickness = resolveInnerThickness();

        for (int i = 0; i < outerThickness; i++) {
            g.fill(x0 + i, y0 + i, x1 - i, y0 + i + 1, outerCol);
            g.fill(x0 + i, y1 - i - 1, x1 - i, y1 - i, outerCol);
            g.fill(x0 + i, y0 + i, x0 + i + 1, y1 - i, outerCol);
            g.fill(x1 - i - 1, y0 + i, x1 - i, y1 - i, outerCol);
        }

        for (int i = 0; i < innerThickness; i++) {
            int off = outerThickness + i;
            g.fill(x0 + off, y0 + off, x1 - off, y0 + off + 1, innerCol);
            g.fill(x0 + off, y1 - off - 1, x1 - off, y1 - off, innerCol);
            g.fill(x0 + off, y0 + off, x0 + off + 1, y1 - off, innerCol);
            g.fill(x1 - off - 1, y0 + off, x1 - off, y1 - off, innerCol);
        }

        if (this.thickFrame && (outerThickness > 1 || innerThickness > 1)) {
            int accent = 0xAAFFFFFF;
            g.fill(x0 + 1, y0 + 1, x0 + 3, y0 + 2, accent);
            g.fill(x1 - 3, y0 + 1, x1 - 1, y0 + 2, accent);
            g.fill(x0 + 1, y1 - 2, x0 + 3, y1 - 1, accent);
            g.fill(x1 - 3, y1 - 2, x1 - 1, y1 - 1, accent);
        }
    }

    private int resolveOuterThickness() {
        return this.customOuterThickness == null ? (this.thickFrame ? 3 : 1) : this.customOuterThickness;
    }

    private int resolveInnerThickness() {
        return this.customInnerThickness == null ? (this.thickFrame ? 3 : 1) : this.customInnerThickness;
    }

    private int resolveFillInset() {
        if (this.customFillInset != null) {
            return Math.max(2, this.customFillInset);
        }
        return 2;
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

    private static void drawScaledUiText(GuiGraphics graphics, net.minecraft.client.gui.Font font, Component text, int x, int y, int color, float scale) {
        if (Math.abs(scale - 1.0f) < 0.001f) {
            WildexUiText.draw(graphics, font, text, x, y, color, false);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        WildexUiText.draw(graphics, font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }

    private static void drawScaledUiText(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text, int x, int y, int color, float scale) {
        if (Math.abs(scale - 1.0f) < 0.001f) {
            WildexUiText.draw(graphics, font, text, x, y, color, false);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        WildexUiText.draw(graphics, font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }
}



