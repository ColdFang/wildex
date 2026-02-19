package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;

import java.util.List;

public final class WildexThemes {

    private static final WildexTheme VINTAGE = new WildexTheme(
            WildexScreenTextures.vintage(),
            DesignStyle.VINTAGE,
            WildexUiTheme.vintagePalette()
    );

    private static final WildexTheme MODERN = new WildexTheme(
            WildexScreenTextures.modern(),
            DesignStyle.MODERN,
            WildexUiTheme.modernPalette()
    );

    private static final List<WildexTheme> ORDERED = List.of(VINTAGE, MODERN);

    private WildexThemes() {
    }

    public static WildexTheme current() {
        return fromDesignStyle(ClientConfig.INSTANCE.designStyle.get());
    }

    public static WildexTheme fromDesignStyle(DesignStyle style) {
        if (style == DesignStyle.MODERN) return MODERN;
        return VINTAGE;
    }

    public static boolean isModernLayout() {
        return current().layoutProfile() == DesignStyle.MODERN;
    }

    public static boolean isVintageLayout() {
        return !isModernLayout();
    }

    public static DesignStyle nextStyle(DesignStyle current) {
        int idx = current == DesignStyle.MODERN ? 1 : 0;
        int nextIdx = (idx + 1) % ORDERED.size();
        return ORDERED.get(nextIdx).layoutProfile();
    }
}
