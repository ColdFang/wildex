package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import net.minecraft.resources.ResourceLocation;

public record WildexTheme(
        ResourceLocation backgroundTexture,
        DesignStyle layoutProfile,
        WildexUiTheme.Palette palette
) {
}
