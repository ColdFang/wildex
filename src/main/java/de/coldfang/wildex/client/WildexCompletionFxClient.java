package de.coldfang.wildex.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class WildexCompletionFxClient {

    private static final int DURATION_TICKS = 90;

    private static final int EDGE_MAX_PX = 18;
    private static final int EDGE_MIN_PX = 6;

    private static final int BASE_ALPHA = 18;
    private static final int PEAK_ALPHA = 110;

    private static int remainingTicks = 0;

    private WildexCompletionFxClient() {
    }

    public static void start() {
        remainingTicks = DURATION_TICKS;

        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                1.0f,
                1.0f
        ));
    }

    public static void tickClient() {
        if (remainingTicks <= 0) return;
        remainingTicks--;
    }

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    public static void renderHud(GuiGraphics g, int screenW, int screenH) {
        if (remainingTicks <= 0) return;

        float t = 1.0f - (remainingTicks / (float) DURATION_TICKS);

        float in = smoothstep(clamp01(t / 0.15f));
        float out = 1.0f - smoothstep(clamp01((t - 0.80f) / 0.20f));
        float amp = in * out;

        float pulse = 0.6f + (0.4f * (float) Math.sin((t * 9.0f) * Math.PI * 2.0f));
        float strength = clamp01(amp * pulse);

        int edge = Math.round(lerp(EDGE_MIN_PX, EDGE_MAX_PX, strength));
        int alpha = Math.round(lerp(BASE_ALPHA, PEAK_ALPHA, strength));

        int argbOuter = (alpha << 24) | 0xD8B84A;
        drawFrame(g, screenW, screenH, edge, argbOuter);

        int innerAlpha = Math.round(alpha * 0.55f);
        int argbInner = (innerAlpha << 24) | 0xFFF4C9;

        int e2 = Math.max(1, Math.round(edge * 0.45f));
        drawFrame(g, screenW, screenH, e2, argbInner);
    }

    public static void clear() {
        remainingTicks = 0;
    }

    private static void drawFrame(GuiGraphics g, int screenW, int screenH, int edge, int argb) {
        g.fill(0, 0, screenW, edge, argb);
        g.fill(0, screenH - edge, screenW, screenH, argb);
        g.fill(0, edge, edge, screenH - edge, argb);
        g.fill(screenW - edge, edge, screenW, screenH - edge, argb);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    private static float smoothstep(float x) {
        return x * x * (3.0f - 2.0f * x);
    }
}
