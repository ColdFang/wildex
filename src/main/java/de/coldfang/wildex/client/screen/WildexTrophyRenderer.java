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

    private static final int TROPHY_TEX_SIZE = 128;
    private static final int TROPHY_DRAW_SIZE = 72;
    private static final int TROPHY_FRAME_PAD_X = 6;
    private static final int TROPHY_FRAME_PAD_Y = 8;
    private static final int TROPHY_ATTACH_GAP_X = 2;
    private static final int TROPHY_ATTACH_GAP_Y = 15;

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
    public Hitbox render(GuiGraphics graphics, WildexScreenLayout layout, boolean visible) {
        if (!visible) {
            return null;
        }
        float trophyScale = Math.max(0.01f, layout.scale());
        int trophyDrawSize = Math.max(1, Math.round(TROPHY_DRAW_SIZE * trophyScale));
        int trophyPadX = Math.max(1, Math.round(TROPHY_FRAME_PAD_X * trophyScale));
        int trophyPadY = Math.max(1, Math.round(TROPHY_FRAME_PAD_Y * trophyScale));
        int frameW = trophyDrawSize + (trophyPadX * 2);
        int frameH = trophyDrawSize + (trophyPadY * 2);
        int texLeft = Math.round(layout.x());
        int texBottom = Math.round(layout.y() + (WildexScreenLayout.TEX_H * layout.scale()));
        int attachGapX = Math.round(TROPHY_ATTACH_GAP_X * trophyScale);
        int attachGapY = Math.round(TROPHY_ATTACH_GAP_Y * trophyScale);
        int anchorX = texLeft - frameW + attachGapX;
        int anchorY = texBottom - frameH - attachGapY;
        ThemeAssets assets = assetsFor(layout.theme().layoutProfile());
        anchorY += Math.round(assets.offsetY() * trophyScale);
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
        return new Hitbox(anchorX, anchorY, frameW, frameH);
    }

    public List<Component> tooltip() {
        return TROPHY_TOOLTIP;
    }

    private static ThemeAssets assetsFor(DesignStyle style) {
        return style == DesignStyle.MODERN ? MODERN_ASSETS : VINTAGE_ASSETS;
    }

    private record ThemeAssets(ResourceLocation background, ResourceLocation icon, int offsetY) {
    }

    public record Hitbox(int x, int y, int w, int h) {
        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < (x + w) && py < (y + h);
        }
    }
}
