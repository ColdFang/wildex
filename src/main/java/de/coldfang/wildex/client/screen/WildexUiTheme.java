package de.coldfang.wildex.client.screen;

public final class WildexUiTheme {

    private static final Palette VINTAGE = new Palette(
            0x2B1A10,
            0xF2E8D8,
            0x55301E14,
            0x88301E14,
            0x22FFFFFF,
            0x55FFFFFF,
            0xE61A120C,
            0xAA301E14,
            0xF2E8D5,
            0x1F1008,
            0xFFE9D6B8,
            0x2A000000,
            0x66301E14,
            0xFF000000,
            0xFFB9B9B9,
            0xCC000000,
            0xFFFFFFFF,
            0x22000000,
            0x33000000,
            0x44000000,
            0x6A1A120C,
            0x8A24170F,
            0x16000000,
            0xFFF6E6D1,
            0x99C8B8A7
    );

    private static final Palette MODERN = new Palette(
            0xE7DBCD,
            0xFFF8EF,
            0x8AB7A996,
            0xCCB8A38E,
            0x5A1A120D,
            0x7A6E5E50,
            0xEE120C09,
            0xD09F8A77,
            0xF6EBDD,
            0xF0DFCC,
            0xFF241A12,
            0x55392A20,
            0x7A8A7865,
            0xFF000000,
            0xFFB9B9B9,
            0xC0140F0C,
            0xCCBDAA95,
            0x401E140E,
            0x55271B13,
            0x6632251A,
            0xA01E150F,
            0xC02A1D14,
            0x28000000,
            0xFFF3E7D8,
            0x99D4C4B3
    );

    private WildexUiTheme() {
    }

    public static Palette current() {
        return WildexThemes.current().palette();
    }

    static Palette vintagePalette() {
        return VINTAGE;
    }

    static Palette modernPalette() {
        return MODERN;
    }

    public record Palette(
            int ink,
            int inkOnDark,
            int inkMuted,
            int frameOuter,
            int frameBg,
            int frameInner,
            int tooltipBg,
            int tooltipBorder,
            int tooltipText,
            int heading,
            int listBg,
            int rowHover,
            int rowSeparator,
            int scrollTrack,
            int scrollThumb,
            int selectionBg,
            int selectionBorder,
            int checkboxIdle,
            int checkboxHover,
            int checkboxChecked,
            int buttonFillIdle,
            int buttonFillHover,
            int buttonFillDisabled,
            int buttonInk,
            int buttonInkDisabled
    ) {
    }
}
