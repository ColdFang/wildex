package de.coldfang.wildex.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.coldfang.wildex.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class WildexBookRenderer {

    public void render(
            GuiGraphics graphics,
            WildexScreenLayout layout,
            WildexScreenState state,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (layout == null) return;

        Objects.hash(state, mouseX, mouseY, partialTick);

        ResourceLocation tex =
                WildexScreenTextures.background(ClientConfig.INSTANCE.designStyle.get());

        Minecraft.getInstance()
                .getTextureManager()
                .getTexture(tex)
                .setFilter(false, false);

        graphics.pose().pushPose();
        graphics.pose().translate(layout.x(), layout.y(), 0.0f);
        graphics.pose().scale(layout.scale(), layout.scale(), 1.0f);

        RenderSystem.enableBlend();

        graphics.blit(
                tex,
                0,
                0,
                0,
                0,
                WildexScreenLayout.TEX_W,
                WildexScreenLayout.TEX_H,
                WildexScreenLayout.TEX_W,
                WildexScreenLayout.TEX_H
        );

        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }
}
