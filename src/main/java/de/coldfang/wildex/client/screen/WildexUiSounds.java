package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class WildexUiSounds {

    private static final float BUTTON_VOLUME = 0.12f;
    private static final float TAB_VOLUME = 0.10f;
    private static final float LIST_SELECT_VOLUME = 0.12f;
    private static final float SCREEN_OPEN_BOOK_VOLUME = 0.30f;
    private static final float SCREEN_OPEN_MODERN_VOLUME = 0.08f;

    private WildexUiSounds() {
    }

    public static void playButtonClick() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), BUTTON_VOLUME, 1.0f);
    }

    public static void playTabSwitch() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), TAB_VOLUME, 1.12f);
    }

    public static void playListSelection() {
        play(SoundEvents.BOOK_PUT, LIST_SELECT_VOLUME, 1.18f);
    }

    public static void playScreenOpen() {
        if (WildexThemes.isModernLayout()) {
            play(SoundEvents.NOTE_BLOCK_BIT.value(), SCREEN_OPEN_MODERN_VOLUME, 0.78f);
            return;
        }
        play(SoundEvents.BOOK_PUT, SCREEN_OPEN_BOOK_VOLUME, 0.98f);
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (sound == null) return;
        // SimpleSoundInstance.forUI expects (sound, pitch, volume).
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
