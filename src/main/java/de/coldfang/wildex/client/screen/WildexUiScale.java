package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.config.ClientConfig;

public final class WildexUiScale {

    public static final float MIN = 1.00f;
    public static final float MAX = 4.00f;
    private static final float DISPLAY_BASE = 2.00f;
    private static final float STEP = 1.0f / 16.0f;

    private WildexUiScale() {
    }

    public static float clamp(float v) {
        if (v < MIN) return MIN;
        if (v > MAX) return MAX;
        return v;
    }

    public static float snap(float v) {
        float c = clamp(v);
        return Math.round(c / STEP) * STEP;
    }

    public static float get() {
        return snap((float) ClientConfig.INSTANCE.wildexUiScale.get().doubleValue());
    }

    public static void set(float v) {
        ClientConfig.INSTANCE.wildexUiScale.set((double) snap(v));
        ClientConfig.SPEC.save();
    }

    public static float toNormalized(float uiScale) {
        return (snap(uiScale) - MIN) / (MAX - MIN);
    }

    public static float fromNormalized(float t) {
        float n = Math.max(0.0f, Math.min(1.0f, t));
        return snap(MIN + (MAX - MIN) * n);
    }

    public static int toDisplayPercent(float uiScale) {
        return Math.round((clamp(uiScale) / DISPLAY_BASE) * 100.0f);
    }
}



