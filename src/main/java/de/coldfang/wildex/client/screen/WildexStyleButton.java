package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WildexStyleButton extends Button {

    private static final int FILL_IDLE = 0x33140E0A;
    private static final int FILL_HOVER = 0x55231811;
    private static final int FILL_DISABLED = 0x16000000;
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
    public void playDownSound(@NotNull SoundManager soundManager) {
        WildexUiSounds.playButtonClick();
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
        Integer baseBgColor = this.backgroundColorSupplier == null ? null : this.backgroundColorSupplier.get();
        Integer resolvedBgColor = baseBgColor == null ? null : resolveSolidBackgroundColor(baseBgColor, this.active, this.isHovered());
        int surfaceColor = resolveSurfaceColor(resolvedBgColor, bg, fill, this.active, this.isHovered());
        drawSurface(graphics, ix0, iy0, ix1, iy1, bg, resolvedBgColor, surfaceColor, fill, this.active, this.isHovered());
        drawFrame(graphics, x0, y0, x1, y1, surfaceColor, this.active, this.isHovered());

        var font = Minecraft.getInstance().font;
        int color = (resolvedBgColor == null && bg == null)
                ? (this.active ? resolveInkColor(surfaceColor, true) : resolveInkColor(surfaceColor, false))
                : resolveInkColor(surfaceColor, this.active);
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

    private void drawSurface(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            ResourceLocation bg,
            Integer resolvedBgColor,
            int surfaceColor,
            int fill,
            boolean active,
            boolean hovered
    ) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }

        if (resolvedBgColor != null) {
            graphics.fill(x0, y0, x1, y1, resolvedBgColor);
        } else if (bg != null) {
            graphics.blit(
                    bg,
                    x0,
                    y0,
                    x1 - x0,
                    y1 - y0,
                    0,
                    0,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE,
                    BG_TEX_SIZE
            );
            graphics.fill(x0, y0, x1, y1, fill);
            graphics.fill(
                    x0,
                    y0,
                    x1,
                    y1,
                    withAlpha(mixOpaque(surfaceColor, 0xFFFFFFFF, hovered ? 0.12f : 0.05f), hovered ? 0x20 : 0x14)
            );
        } else {
            graphics.fill(x0, y0, x1, y1, surfaceColor);
        }

        drawSurfacePolish(graphics, x0, y0, x1, y1, surfaceColor, active, hovered);
    }

    private void drawFrame(GuiGraphics g, int x0, int y0, int x1, int y1, int surfaceColor, boolean active, boolean hovered) {
        int outerCol = this.customOuterFrameColor == null
                ? resolveDefaultOuterFrameColor(surfaceColor, active, hovered)
                : resolveCustomFrameColor(this.customOuterFrameColor, active, hovered, false);
        int innerCol = this.customInnerFrameColor == null
                ? resolveDefaultInnerFrameColor(surfaceColor, active, hovered)
                : resolveCustomFrameColor(this.customInnerFrameColor, active, hovered, true);
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
            int accent = withAlpha(resolveFrameAccentColor(surfaceColor, active, hovered), active ? 0xCC : 0x92);
            g.fill(x0 + 1, y0 + 1, x0 + 3, y0 + 2, accent);
            g.fill(x1 - 3, y0 + 1, x1 - 1, y0 + 2, accent);
            g.fill(x0 + 1, y1 - 2, x0 + 3, y1 - 1, accent);
            g.fill(x1 - 3, y1 - 2, x1 - 1, y1 - 1, accent);
        }

        if (active && hovered && x1 - x0 >= 12 && y1 - y0 >= 8) {
            int hoverLine = withAlpha(resolveFrameAccentColor(surfaceColor, true, true), 0x70);
            g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, hoverLine);
            g.fill(x0 + 2, y1 - 3, x1 - 2, y1 - 2, withAlpha(mixOpaque(surfaceColor, 0xFF000000, 0.60f), 0x38));
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

    private static int resolveSurfaceColor(Integer resolvedBgColor, ResourceLocation bg, int fill, boolean active, boolean hovered) {
        if (resolvedBgColor != null) {
            return forceOpaque(resolvedBgColor);
        }

        int themeBase = forceOpaque(WildexUiTheme.buttonBackground());
        if (bg != null) {
            int textured = hovered ? mixOpaque(themeBase, 0xFFFFFFFF, 0.08f) : themeBase;
            return active ? textured : mixOpaque(textured, 0xFF8E847A, 0.36f);
        }

        int overlayBase = forceOpaque(fill);
        int mixed = mixOpaque(themeBase, overlayBase, active ? 0.34f : 0.20f);
        if (hovered && active) {
            mixed = mixOpaque(mixed, 0xFFFFFFFF, 0.06f);
        }
        return mixed;
    }

    private static void drawSurfacePolish(GuiGraphics graphics, int x0, int y0, int x1, int y1, int surfaceColor, boolean active, boolean hovered) {
        int width = x1 - x0;
        int height = y1 - y0;
        if (width <= 2 || height <= 2) {
            return;
        }

        int topBandH = Math.max(1, Math.min(4, height / 3));
        int bottomBandH = Math.max(1, Math.min(4, height / 4));
        int leftBandW = Math.max(1, Math.min(2, width / 12));
        int rightBandW = Math.max(1, Math.min(2, width / 12));

        int topGlow = withAlpha(mixOpaque(surfaceColor, 0xFFFFFFFF, hovered ? 0.64f : 0.50f), active ? (hovered ? 0x82 : 0x66) : 0x38);
        graphics.fill(x0, y0, x1, Math.min(y1, y0 + topBandH), topGlow);

        int sheenY = y0 + topBandH;
        if (sheenY < y1 - 1) {
            graphics.fill(
                    x0 + 1,
                    sheenY,
                    x1 - 1,
                    sheenY + 1,
                    withAlpha(mixOpaque(surfaceColor, 0xFFFFFFFF, 0.82f), active ? (hovered ? 0x68 : 0x44) : 0x24)
            );
        }

        graphics.fill(
                x0,
                Math.max(y0, y1 - bottomBandH),
                x1,
                y1,
                withAlpha(mixOpaque(surfaceColor, 0xFF000000, hovered ? 0.28f : 0.38f), active ? 0x74 : 0x46)
        );

        if (height >= 8) {
            graphics.fill(
                    x0,
                    y0 + 1,
                    x0 + leftBandW,
                    y1 - 1,
                    withAlpha(mixOpaque(surfaceColor, 0xFFFFFFFF, 0.66f), active ? (hovered ? 0x5C : 0x40) : 0x22)
            );
            graphics.fill(
                    x1 - rightBandW,
                    y0 + 1,
                    x1,
                    y1 - 1,
                    withAlpha(mixOpaque(surfaceColor, 0xFF000000, 0.52f), active ? 0x34 : 0x20)
            );
        }

        if (width >= 18 && height >= 10) {
            int centerY = y0 + Math.max(2, height / 2);
            graphics.fill(
                    x0 + 3,
                    centerY,
                    x1 - 3,
                    centerY + 1,
                    withAlpha(mixOpaque(surfaceColor, 0xFFFFFFFF, 0.74f), hovered ? 0x22 : 0x14)
            );
        }
    }

    private static int resolveSolidBackgroundColor(int baseColor, boolean active, boolean hovered) {
        int color = forceOpaque(baseColor);
        if (!active) {
            return mixOpaque(color, 0xFF8E847A, 0.40f);
        }
        if (hovered) {
            return mixOpaque(color, 0xFFFFFFFF, 0.14f);
        }
        return color;
    }

    private static int resolveInkColor(int backgroundColor, boolean active) {
        int activeInk = luminance(backgroundColor) >= 0.62f ? 0xFF261910 : 0xFFF8F4EC;
        if (active) return activeInk;
        return mixOpaque(activeInk, backgroundColor, 0.45f);
    }

    private static int resolveDefaultOuterFrameColor(int surfaceColor, boolean active, boolean hovered) {
        float blend = WildexThemes.isModernLayout() ? 0.66f : 0.58f;
        int base = mixOpaque(surfaceColor, 0xFF130C08, blend);
        if (!active) {
            return mixOpaque(base, 0xFF82786F, 0.35f);
        }
        return hovered ? mixOpaque(base, 0xFFFFFFFF, 0.08f) : base;
    }

    private static int resolveDefaultInnerFrameColor(int surfaceColor, boolean active, boolean hovered) {
        float blend = WildexThemes.isModernLayout() ? 0.46f : 0.36f;
        int base = mixOpaque(surfaceColor, 0xFFFFFFFF, blend);
        if (!active) {
            return mixOpaque(base, surfaceColor, 0.32f);
        }
        return hovered ? mixOpaque(base, 0xFFFFFFFF, 0.14f) : base;
    }

    private static int resolveCustomFrameColor(int frameColor, boolean active, boolean hovered, boolean inner) {
        int base = forceOpaque(frameColor);
        if (!active) {
            return mixOpaque(base, 0xFF8E847A, inner ? 0.26f : 0.32f);
        }
        return hovered ? mixOpaque(base, 0xFFFFFFFF, inner ? 0.18f : 0.10f) : base;
    }

    private static int resolveFrameAccentColor(int surfaceColor, boolean active, boolean hovered) {
        int accent = mixOpaque(surfaceColor, 0xFFFFFFFF, hovered ? 0.84f : 0.72f);
        if (!active) {
            return mixOpaque(accent, surfaceColor, 0.38f);
        }
        return accent;
    }

    private static int forceOpaque(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static int mixOpaque(int fromColor, int toColor, float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        int from = forceOpaque(fromColor);
        int to = forceOpaque(toColor);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + ((tr - fr) * clamped));
        int g = Math.round(fg + ((tg - fg) * clamped));
        int b = Math.round(fb + ((tb - fb) * clamped));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float luminance(int color) {
        int opaque = forceOpaque(color);
        float r = ((opaque >> 16) & 0xFF) / 255.0f;
        float g = ((opaque >> 8) & 0xFF) / 255.0f;
        float b = (opaque & 0xFF) / 255.0f;
        return (0.2126f * r) + (0.7152f * g) + (0.0722f * b);
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



