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

    private static final WildexTheme JUNGLE = new WildexTheme(
            WildexScreenTextures.jungle(),
            DesignStyle.JUNGLE,
            WildexUiTheme.vintagePalette()
    );

    private static final WildexTheme RUNES = new WildexTheme(
            WildexScreenTextures.runes(),
            DesignStyle.RUNES,
            WildexUiTheme.vintagePalette()
    );

    private static final WildexTheme STEAMPUNK = new WildexTheme(
            WildexScreenTextures.steampunk(),
            DesignStyle.STEAMPUNK,
            WildexUiTheme.vintagePalette()
    );

    private static final List<WildexTheme> ORDERED = List.of(VINTAGE, MODERN, JUNGLE, RUNES, STEAMPUNK);

    private WildexThemes() {
    }

    public static WildexTheme current() {
        return fromDesignStyle(ClientConfig.INSTANCE.designStyle.get());
    }

    public static WildexTheme fromDesignStyle(DesignStyle style) {
        if (style == null) return VINTAGE;
        return switch (style) {
            case MODERN -> MODERN;
            case JUNGLE -> JUNGLE;
            case RUNES -> RUNES;
            case STEAMPUNK -> STEAMPUNK;
            case VINTAGE -> VINTAGE;
        };
    }

    public static boolean usesModernLayout(DesignStyle style) {
        return style == DesignStyle.MODERN;
    }

    public static boolean isModernLayout() {
        return usesModernLayout(current().layoutProfile());
    }

    public static boolean isVintageLayout() {
        return !isModernLayout();
    }

    public static DesignStyle nextStyle(DesignStyle current) {
        int idx = 0;
        for (int i = 0; i < ORDERED.size(); i++) {
            if (ORDERED.get(i).layoutProfile() == current) {
                idx = i;
                break;
            }
        }
        int nextIdx = (idx + 1) % ORDERED.size();
        return ORDERED.get(nextIdx).layoutProfile();
    }
}



