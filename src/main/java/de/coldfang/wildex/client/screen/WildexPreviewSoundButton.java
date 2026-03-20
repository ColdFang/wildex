package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.util.WildexEntityFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class WildexPreviewSoundButton {

    private static final Component TOOLTIP = Component.translatable("tooltip.wildex.preview_play_sound");
    private static final int BUTTON_MARGIN = 6;
    private static final int BUTTON_SIZE = 14;
    private static final int MODERN_TOP_INSET = 10;
    private static final int MODERN_RIGHT_INSET = 2;

    public boolean handleClick(int mouseX, int mouseY, int button, WildexScreenLayout layout, String mobId) {
        if (button != 0) return false;
        WildexScreenLayout.Area area = area(layout, mobId);
        if (isOutside(mouseX, mouseY, area)) return false;

        WildexUiSounds.playButtonClick();
        playMobSound(mobId);
        return true;
    }

    public void render(GuiGraphics graphics, WildexScreenLayout layout, String mobId) {
        WildexScreenLayout.Area area = area(layout, mobId);
        if (graphics == null || area == null) return;

        int color = WildexUiTheme.current().inkMuted();
        int x = area.x();
        int y = area.y();
        int w = area.w();
        int h = area.h();

        int bodyW = Math.max(2, Math.round(w * 0.18f));
        int bodyX = x + Math.max(1, Math.round(w * 0.10f));
        int bodyY0 = y + Math.max(2, Math.round(h * 0.32f));
        int bodyY1 = y + h - Math.max(2, Math.round(h * 0.32f));
        graphics.fill(bodyX, bodyY0, bodyX + bodyW, bodyY1, color);

        int coneX0 = bodyX + bodyW;
        int coneX1 = x + Math.max(bodyW + 3, Math.round(w * 0.58f));
        int centerY = y + (h / 2);
        int coneHalf = Math.max(2, Math.round(h * 0.22f));
        for (int dx = 0; dx < Math.max(1, coneX1 - coneX0); dx++) {
            float t = dx / (float) Math.max(1, coneX1 - coneX0 - 1);
            int spread = Math.max(1, Math.round(coneHalf + (t * coneHalf)));
            int px = coneX0 + dx;
            graphics.fill(px, centerY - spread, px + 1, centerY + spread + 1, color);
        }

        int arcX = x + Math.max(coneX1 + 1, Math.round(x + w * 0.62f));
        int arcHeight = Math.max(3, Math.round(h * 0.42f));
        int arcTop = centerY - arcHeight / 2;
        int arcBottom = arcTop + arcHeight;
        graphics.fill(arcX, arcTop + 1, arcX + 1, arcBottom, color);
        graphics.fill(arcX + 1, arcTop, arcX + 2, arcTop + 1, color);
        graphics.fill(arcX + 1, arcBottom, arcX + 2, arcBottom + 1, color);

        int outerArcX = arcX + Math.max(2, Math.round(w * 0.15f));
        int outerArcHeight = Math.max(5, Math.round(h * 0.68f));
        int outerArcTop = centerY - outerArcHeight / 2;
        int outerArcBottom = outerArcTop + outerArcHeight;
        graphics.fill(outerArcX, outerArcTop + 1, outerArcX + 1, outerArcBottom, color);
        graphics.fill(outerArcX + 1, outerArcTop, outerArcX + 2, outerArcTop + 1, color);
        graphics.fill(outerArcX + 1, outerArcBottom, outerArcX + 2, outerArcBottom + 1, color);
    }

    public void renderTooltip(
            GuiGraphics graphics,
            net.minecraft.client.gui.Font font,
            WildexScreenLayout layout,
            String mobId,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            WildexUiTheme.Palette theme
    ) {
        if (isOutside(mouseX, mouseY, area(layout, mobId))) return;
        WildexUiRenderUtil.renderTooltipTopLeft(graphics, font, java.util.List.of(TOOLTIP), mouseX, mouseY, screenWidth, screenHeight, theme);
    }

    private WildexScreenLayout.Area area(WildexScreenLayout layout, String mobId) {
        if (!isVisible(mobId)) return null;
        if (layout == null) return null;
        WildexScreenLayout.Area previewArea = layout.rightPreviewArea();
        if (previewArea == null) return null;

        int size = Math.max(10, Math.round(BUTTON_SIZE * layout.scale()));
        int margin = Math.max(3, Math.round(BUTTON_MARGIN * layout.scale()));
        int extraTopInset = 0;
        int extraRightInset = 0;
        if (WildexThemes.isModernLayout()) {
            extraTopInset = Math.round(MODERN_TOP_INSET * layout.scale());
            extraRightInset = Math.round(MODERN_RIGHT_INSET * layout.scale());
        }

        int x = previewArea.x() + previewArea.w() - size - margin - extraRightInset;
        int y = previewArea.y() + margin + extraTopInset;
        return new WildexScreenLayout.Area(x, y, size, size);
    }

    private static boolean isVisible(String mobId) {
        ResourceLocation mobKey = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (mobKey == null) return false;
        if (!WildexClientConfigView.hiddenMode()) return true;
        return WildexDiscoveryCache.isDiscovered(mobKey);
    }

    private static boolean isOutside(int mouseX, int mouseY, WildexScreenLayout.Area area) {
        return area == null
                || mouseX < area.x()
                || mouseX >= area.x() + area.w()
                || mouseY < area.y()
                || mouseY >= area.y() + area.h();
    }

    private static void playMobSound(String mobId) {
        ResourceLocation id = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        if (id == null) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Entity listener = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (level == null || listener == null) return;

        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        Entity entity = WildexEntityFactory.tryCreate(type, level);
        if (!(entity instanceof Mob mob)) {
            WildexEntityFactory.discardQuietly(entity);
            return;
        }

        try {
            SoundEvent sound = resolveAmbientSound(mob);
            if (sound == null) return;

            float volume = resolveFloatMethod(mob, "getSoundVolume");
            float pitch = resolveFloatMethod(mob, "getVoicePitch");
            SoundSource source = mob.getSoundSource();

            level.playLocalSound(listener.getX(), listener.getY(), listener.getZ(), sound, source, volume, pitch, false);
        } catch (Throwable ignored) {
        } finally {
            WildexEntityFactory.discardQuietly(mob);
        }
    }

    private static SoundEvent resolveAmbientSound(Mob mob) {
        if (mob == null) return null;
        try {
            Method ambientGetter = findMethod(mob.getClass(), "getAmbientSound");
            if (ambientGetter == null) return null;
            Object sound = ambientGetter.invoke(mob);
            return sound instanceof SoundEvent event ? event : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float resolveFloatMethod(Mob mob, String methodName) {
        if (mob == null || methodName == null || methodName.isBlank()) return 1.0f;
        try {
            Method method = findMethod(mob.getClass(), methodName);
            if (method == null) return 1.0f;
            Object value = method.invoke(mob);
            return value instanceof Number number ? number.floatValue() : 1.0f;
        } catch (Throwable ignored) {
            return 1.0f;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        if (type == null || name == null || name.isBlank()) return null;

        try {
            return type.getMethod(name, params);
        } catch (Throwable ignored) {
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, params);
                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                }
                return method;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
