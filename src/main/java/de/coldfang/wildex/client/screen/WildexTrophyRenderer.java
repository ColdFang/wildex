package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WildexTrophyRenderer {

    private static final ThemeAssets VINTAGE_ASSETS = new ThemeAssets(
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_red.png"),
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png"),
            0
    );
    private static final ThemeAssets MODERN_ASSETS = new ThemeAssets(
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_modern.png"),
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_modern.png"),
            -6
    );
    private static final ThemeAssets JUNGLE_ASSETS = new ThemeAssets(
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_jungle.png"),
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png"),
            0
    );
    private static final ThemeAssets RUNES_ASSETS = new ThemeAssets(
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_runes.png"),
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png"),
            0
    );
    private static final ThemeAssets STEAMPUNK_ASSETS = new ThemeAssets(
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_steampunk.png"),
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png"),
            0
    );

    private static final int TROPHY_TEX_SIZE = 128;
    private static final int TROPHY_FRAME_INNER_SIZE = 72;
    private static final float TROPHY_ICON_SCALE = 0.88f;
    private static final int TROPHY_FRAME_PAD_X = 6;
    private static final int TROPHY_FRAME_PAD_Y = 8;
    private static final int TROPHY_ATTACH_GAP_X = 2;
    private static final int TROPHY_ATTACH_GAP_Y = 15;
    private static final int TROPHY_GLOBAL_SHIFT_X = 2;
    private static final int TOGGLE_PAD = 2;
    private static final int TOGGLE_SIZE = 12;
    private static final int TOGGLE_BG = 0xA0000000;
    private static final int TOGGLE_BORDER = 0xCCFFFFFF;
    private static final int TOGGLE_GLYPH = 0xFFF2E8D5;
    private static final int FRAME_OUTER = 0x99D6B89C;
    private static final int FRAME_INNER = 0x77FFFFFF;

    private static final Component TROPHY_TIP_TITLE = Component.translatable("tooltip.wildex.spyglass_pulse.title");
    private static final List<Component> TROPHY_TOOLTIP = List.of(
            TROPHY_TIP_TITLE,
            Component.translatable("tooltip.wildex.spyglass_pulse.line1"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line2"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line3"),
            Component.empty(),
            Component.translatable("tooltip.wildex.spyglass_pulse.line4")
    );

    @Nullable
    public RenderState render(GuiGraphics graphics, WildexScreenLayout layout, boolean visible, boolean collapsed) {
        if (!visible) {
            return null;
        }
        float trophyScale = Math.max(0.01f, layout.scale());
        int frameInnerSize = Math.max(1, Math.round(TROPHY_FRAME_INNER_SIZE * trophyScale));
        int trophyDrawSize = Math.max(1, Math.round(frameInnerSize * TROPHY_ICON_SCALE));
        int trophyPadX = Math.max(1, Math.round(TROPHY_FRAME_PAD_X * trophyScale));
        int trophyPadY = Math.max(1, Math.round(TROPHY_FRAME_PAD_Y * trophyScale));
        int frameW = frameInnerSize + (trophyPadX * 2);
        int frameH = frameInnerSize + (trophyPadY * 2);
        int texLeft = Math.round(layout.x());
        int texBottom = Math.round(layout.y() + (WildexScreenLayout.TEX_H * layout.scale()));
        int attachGapX = Math.round(TROPHY_ATTACH_GAP_X * trophyScale);
        int attachGapY = Math.round(TROPHY_ATTACH_GAP_Y * trophyScale);
        int anchorX = texLeft - frameW + attachGapX + Math.round(TROPHY_GLOBAL_SHIFT_X * trophyScale);
        int anchorY = texBottom - frameH - attachGapY;
        ThemeAssets assets = assetsFor(layout.theme().layoutProfile());
        anchorY += Math.round(assets.offsetY() * trophyScale);
        int togglePad = Math.max(1, Math.round(TOGGLE_PAD * trophyScale));
        int toggleSize = Math.max(8, Math.round(TOGGLE_SIZE * trophyScale));
        if (collapsed) {
            int collapseShift = Math.max(0, frameW - toggleSize - (togglePad * 2));
            anchorX += collapseShift;
        }
        int trophyX = anchorX + ((frameW - trophyDrawSize) / 2);
        int trophyY = anchorY + ((frameH - trophyDrawSize) / 2);
        graphics.blit(
                assets.background(),
                anchorX,
                anchorY,
                frameW,
                frameH,
                0,
                0,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE
        );
        drawThickFrame(graphics, anchorX, anchorY, anchorX + frameW, anchorY + frameH);
        graphics.blit(
                assets.icon(),
                trophyX,
                trophyY,
                trophyDrawSize,
                trophyDrawSize,
                0,
                0,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE,
                TROPHY_TEX_SIZE
        );
        Hitbox frameHitbox = new Hitbox(anchorX, anchorY, frameW, frameH);
        Hitbox toggleHitbox = new Hitbox(
                anchorX + togglePad,
                anchorY + togglePad,
                Math.min(toggleSize, Math.max(1, frameW - (togglePad * 2))),
                Math.min(toggleSize, Math.max(1, frameH - (togglePad * 2)))
        );
        drawToggle(graphics, toggleHitbox, collapsed);
        return new RenderState(frameHitbox, toggleHitbox, !collapsed);
    }

    public List<Component> tooltip() {
        return TROPHY_TOOLTIP;
    }

    private static void drawToggle(GuiGraphics g, Hitbox hb, boolean collapsed) {
        int x0 = hb.x();
        int y0 = hb.y();
        int x1 = x0 + hb.w();
        int y1 = y0 + hb.h();
        g.fill(x0, y0, x1, y1, TOGGLE_BG);
        g.fill(x0, y0, x1, y0 + 1, TOGGLE_BORDER);
        g.fill(x0, y1 - 1, x1, y1, TOGGLE_BORDER);
        g.fill(x0, y0, x0 + 1, y1, TOGGLE_BORDER);
        g.fill(x1 - 1, y0, x1, y1, TOGGLE_BORDER);

        int cx = x0 + (hb.w() / 2);
        int cy = y0 + (hb.h() / 2);
        int arm = Math.max(2, hb.w() / 4);
        g.fill(cx - arm, cy, cx + arm + 1, cy + 1, TOGGLE_GLYPH); // minus
        if (collapsed) {
            g.fill(cx, cy - arm, cx + 1, cy + arm + 1, TOGGLE_GLYPH); // plus vertical arm
        }
    }

    private static ThemeAssets assetsFor(DesignStyle style) {
        if (style == null) return VINTAGE_ASSETS;
        return switch (style) {
            case MODERN -> MODERN_ASSETS;
            case JUNGLE -> JUNGLE_ASSETS;
            case RUNES -> RUNES_ASSETS;
            case STEAMPUNK -> STEAMPUNK_ASSETS;
            case VINTAGE -> VINTAGE_ASSETS;
        };
    }

    private record ThemeAssets(ResourceLocation background, ResourceLocation icon, int offsetY) {
    }

    private static void drawThickFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        int outerThickness = 3;
        int innerThickness = 3;
        for (int i = 0; i < outerThickness; i++) {
            g.fill(x0 + i, y0 + i, x1 - i, y0 + i + 1, FRAME_OUTER);
            g.fill(x0 + i, y1 - i - 1, x1 - i, y1 - i, FRAME_OUTER);
            g.fill(x0 + i, y0 + i, x0 + i + 1, y1 - i, FRAME_OUTER);
            g.fill(x1 - i - 1, y0 + i, x1 - i, y1 - i, FRAME_OUTER);
        }
        for (int i = 0; i < innerThickness; i++) {
            int off = outerThickness + i;
            g.fill(x0 + off, y0 + off, x1 - off, y0 + off + 1, FRAME_INNER);
            g.fill(x0 + off, y1 - off - 1, x1 - off, y1 - off, FRAME_INNER);
            g.fill(x0 + off, y0 + off, x0 + off + 1, y1 - off, FRAME_INNER);
            g.fill(x1 - off - 1, y0 + off, x1 - off, y1 - off, FRAME_INNER);
        }
    }

    public record Hitbox(int x, int y, int w, int h) {
        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < (x + w) && py < (y + h);
        }
    }

    public record RenderState(Hitbox frameHitbox, Hitbox toggleHitbox, boolean expanded) {
    }
}
