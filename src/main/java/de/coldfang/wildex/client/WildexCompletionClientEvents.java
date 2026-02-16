package de.coldfang.wildex.client;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class WildexCompletionClientEvents {

    private static final int TOTAL_TICKS = 7 * 20;
    private static final int FADE_TICKS = 16;

    private static final Component TITLE = Component.translatable("toast.wildex.complete.title");
    private static final Component SUB = Component.translatable("toast.wildex.complete.subtitle");

    private static int remainingTicks = 0;

    private WildexCompletionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (WildexCompletionCache.consumeJustCompleted()) {
            remainingTicks = TOTAL_TICKS;
            WildexCompletionFxClient.start();
        }

        if (remainingTicks > 0) remainingTicks--;

        WildexCompletionFxClient.tickClient();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        if (WildexCompletionFxClient.isActive()) {
            WildexCompletionFxClient.renderHud(event.getGuiGraphics(), w, h);
        }

        if (remainingTicks <= 0) return;

        renderBanner(event.getGuiGraphics(), mc, w, h);
    }

    private static void renderBanner(GuiGraphics g, Minecraft mc, int w, int h) {
        float a = computeAlpha(remainingTicks);
        int alpha = (int) (a * 255.0f);
        if (alpha <= 0) return;

        int argbTitle = (alpha << 24) | 0xFFFFFF;
        int argbSub = (alpha << 24) | 0xE6E6E6;

        float t = 1.0f - (remainingTicks / (float) TOTAL_TICKS);
        float punch = 1.0f + (float) Math.sin(Math.min(1.0f, t) * Math.PI) * 0.06f;

        float titleScale = 2.2f * punch;
        float subScale = 1.15f;

        int y = (int) (h * 0.33f);

        Font font = mc.font;

        drawCenteredScaled(g, font, TITLE, w / 2, y, titleScale, argbTitle);
        drawCenteredScaled(g, font, SUB, w / 2, y + (int) (28 * titleScale), subScale, argbSub);
    }

    private static float computeAlpha(int remaining) {
        int lived = TOTAL_TICKS - remaining;

        if (lived < FADE_TICKS) return lived / (float) FADE_TICKS;
        if (remaining < FADE_TICKS) return remaining / (float) FADE_TICKS;

        return 1.0f;
    }

    private static void drawCenteredScaled(GuiGraphics g, Font font, Component text, int cx, int y, float scale, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0.0f);
        g.pose().scale(scale, scale, 1.0f);

        int tw = font.width(text);
        int x = -tw / 2;

        g.drawString(font, text, x, 0, argb, true);

        g.pose().popPose();
    }
}
