package de.coldfang.wildex.client.render;

import de.coldfang.wildex.Wildex;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class WildexPedestalAzureLibCompat {

    private static final String AZ_ENTITY_RENDERER_CLASS = "mod.azure.azurelib.common.render.entity.AzEntityRenderer";
    private static final String AZ_RENDER_LAYER_CLASS = "mod.azure.azurelib.common.render.layer.AzRenderLayer";
    private static final Set<Object> PREPARED_RENDERERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ThreadLocal<PedestalHologramState> ACTIVE_STATE = new ThreadLocal<>();

    private WildexPedestalAzureLibCompat() {
    }

    static boolean prepareRenderer(EntityRenderer<?> renderer) {
        if (renderer == null || !inherits(renderer.getClass(), AZ_ENTITY_RENDERER_CLASS)) {
            return false;
        }

        synchronized (PREPARED_RENDERERS) {
            if (PREPARED_RENDERERS.contains(renderer)) {
                return true;
            }

            try {
                Object config = renderer.getClass().getMethod("config").invoke(renderer);
                List<Object> renderLayers = getMutableRenderLayers(config);
                renderLayers.add(createLayerProxy(renderer.getClass().getClassLoader()));
                PREPARED_RENDERERS.add(renderer);
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                Wildex.LOGGER.warn("Failed to install Wildex AzureLib hologram layer on {}", renderer.getClass().getName(), e);
                return false;
            }
        }
    }

    static ActiveHologram activate(Entity entity, float pulse, boolean renderShellPass, boolean renderGhostPass) {
        PedestalHologramState previous = ACTIVE_STATE.get();
        ACTIVE_STATE.set(new PedestalHologramState(entity, pulse, renderShellPass, renderGhostPass));
        return () -> {
            if (previous == null) {
                ACTIVE_STATE.remove();
            } else {
                ACTIVE_STATE.set(previous);
            }
        };
    }

    private static List<Object> getMutableRenderLayers(Object config) throws ReflectiveOperationException {
        Method renderLayersMethod = config.getClass().getMethod("renderLayers");
        @SuppressWarnings("unchecked")
        List<Object> renderLayers = (List<Object>) renderLayersMethod.invoke(config);
        try {
            renderLayers.add(null);
            renderLayers.removeLast();
            return renderLayers;
        } catch (UnsupportedOperationException ignored) {
            Field renderLayersField = findRenderLayersField(config.getClass());
            renderLayersField.setAccessible(true);
            List<Object> replacement = new ArrayList<>(renderLayers);
            renderLayersField.set(config, replacement);
            return replacement;
        }
    }

    private static Object createLayerProxy(ClassLoader classLoader) throws ReflectiveOperationException {
        Class<?> layerInterface = Class.forName(AZ_RENDER_LAYER_CLASS, false, classLoader);
        InvocationHandler handler = new AzureLayerInvocationHandler();
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{layerInterface}, handler);
    }

    private static boolean inherits(Class<?> type, String targetClassName) {
        Class<?> current = type;
        while (current != null) {
            if (targetClassName.equals(current.getName())) {
                return true;
            }
            for (Class<?> iface : current.getInterfaces()) {
                if (inherits(iface, targetClassName)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Field findRenderLayersField(Class<?> type) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField("renderLayers");
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("renderLayers");
    }

    private static final class AzureLayerInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "preRender":
                    preRender(args[0]);
                    return null;
                case "render":
                    render(args[0]);
                    return null;
                case "renderForBone":
                    return null;
                case "toString":
                    return "WildexPedestalAzureLibLayer";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return null;
            }
        }

        private void preRender(Object context) throws ReflectiveOperationException {
            PedestalHologramState state = resolveActiveState(context);
            if (state == null) return;

            ResourceLocation texture = resolveTexture(context, state.entity());
            // AzureLib models with broad overlapping surfaces become visibly "see-through"
            // when the whole body is rendered as translucent. Keep the base pass depth-stable
            // and let the hologram feel come from the overlay passes instead.
            RenderType renderType = RenderType.entityCutoutNoCull(texture);

            applyState(
                    context,
                    renderType,
                    argbBlueOne(1.0f, 0.10f, 0.92f)
            );
        }

        private void render(Object context) throws ReflectiveOperationException {
            PedestalHologramState state = resolveActiveState(context);
            if (state == null) return;

            ResourceLocation texture = resolveTexture(context, state.entity());
            float pulse = state.pulse();

            if (state.renderShellPass()) {
                reRender(
                        context,
                        RenderType.entityTranslucentEmissive(texture),
                        argbBlueOne(0.22f + 0.05f * pulse, 0.11f, 0.95f),
                        1.012f,
                        0.004f,
                        -0.25f
                );
            }
            if (state.renderGhostPass()) {
                float ghostShift = 0.004f + 0.003f * pulse;
                reRender(
                        context,
                        RenderType.entityTranslucent(texture),
                        argbBlueOne(0.08f, 0.10f, 0.90f),
                        1.001f,
                        ghostShift,
                        -0.1f
                );
            }
        }

        private void reRender(
                Object context,
                RenderType renderType,
                int color,
                float scale,
                float yOffset,
                float depthBias
        ) throws ReflectiveOperationException {
            PoseStackAccess pose = new PoseStackAccess(context);
            int previousPackedLight = (int) invoke(context, "packedLight");
            int previousRenderColor = (int) invoke(context, "renderColor");
            RenderType previousRenderType = (RenderType) invoke(context, "renderType");
            com.mojang.blaze3d.vertex.VertexConsumer previousVertexConsumer =
                    (com.mojang.blaze3d.vertex.VertexConsumer) invoke(context, "vertexConsumer");

            pose.push();
            try {
                pose.translateY(yOffset);
                pose.scale(scale, scale, scale);
                beginDepthBias(depthBias);
                RenderSystem.depthMask(false);
                try {
                    applyState(context, renderType, color);
                    Object pipeline = invoke(context, "rendererPipeline");
                    invokeReRender(pipeline, context);
                } finally {
                    RenderSystem.depthMask(true);
                    endDepthBias();
                }
            } finally {
                pose.pop();
                invokeVoid(context, "setPackedLight", int.class, previousPackedLight);
                invokeVoid(context, "setRenderColor", int.class, previousRenderColor);
                invokeVoid(context, "setRenderType", RenderType.class, previousRenderType);
                invokeVoid(context, "setVertexConsumer", com.mojang.blaze3d.vertex.VertexConsumer.class, previousVertexConsumer);
            }
        }

        private void applyState(Object context, RenderType renderType, int color) throws ReflectiveOperationException {
            invokeVoid(context, "setRenderType", RenderType.class, renderType);
            invokeVoid(context, "setPackedLight", int.class, LightTexture.FULL_BRIGHT);
            invokeVoid(context, "setRenderColor", int.class, color);
            invokeVoid(context, "setVertexConsumer", com.mojang.blaze3d.vertex.VertexConsumer.class, getBuffer(context, renderType));
        }

        private ResourceLocation resolveTexture(Object context, Entity entity) throws ReflectiveOperationException {
            Object pipeline = invoke(context, "rendererPipeline");
            EntityRenderer<?> renderer = (EntityRenderer<?>) invoke(pipeline, "getRenderer");
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>) renderer;
            return entityRenderer.getTextureLocation(entity);
        }

        private com.mojang.blaze3d.vertex.VertexConsumer getBuffer(Object context, RenderType renderType) throws ReflectiveOperationException {
            MultiBufferSource buffer = (MultiBufferSource) invoke(context, "multiBufferSource");
            return buffer.getBuffer(renderType);
        }

        private @Nullable PedestalHologramState resolveActiveState(Object context) throws ReflectiveOperationException {
            PedestalHologramState state = ACTIVE_STATE.get();
            if (state == null) return null;

            Object animatable = invoke(context, "animatable");
            Object currentEntity = invoke(context, "currentEntity");
            if (state.entity() == animatable || state.entity() == currentEntity) {
                return state;
            }
            return null;
        }

        private Object invoke(Object target, String methodName) throws ReflectiveOperationException {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        }

        private void invokeVoid(Object target, String methodName, Class<?> parameterType, Object arg) throws ReflectiveOperationException {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, arg);
        }

        private void invokeReRender(Object target, Object context) throws ReflectiveOperationException {
            Method method = findCompatibleMethod(target.getClass(), "reRender", context.getClass());
            method.invoke(target, context);
        }

        private Method findCompatibleMethod(Class<?> type, String methodName, Class<?> argType) throws NoSuchMethodException {
            Class<?> current = type;
            while (current != null) {
                for (Method method : current.getDeclaredMethods()) {
                    if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                        continue;
                    }
                    Class<?> parameterType = method.getParameterTypes()[0];
                    if (parameterType.isAssignableFrom(argType)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                current = current.getSuperclass();
            }
            throw new NoSuchMethodException(methodName + "(" + argType.getName() + ")");
        }

        private int argbBlueOne(float alpha, float red, float green) {
            return argb(alpha, red, green, 1.0f);
        }

        private int argb(float alpha, float red, float green, float blue) {
            int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
            int r = Mth.clamp((int) (red * 255.0f), 0, 255);
            int g = Mth.clamp((int) (green * 255.0f), 0, 255);
            int b = Mth.clamp((int) (blue * 255.0f), 0, 255);
            return a << 24 | r << 16 | g << 8 | b;
        }

        private void beginDepthBias(float units) {
            if (units == 0.0f) return;
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.0f, units);
        }

        private void endDepthBias() {
            RenderSystem.depthMask(true);
            RenderSystem.polygonOffset(0.0f, 0.0f);
            RenderSystem.disablePolygonOffset();
        }
    }

    @FunctionalInterface
    interface ActiveHologram extends AutoCloseable {
        @Override
        void close();
    }

    private record PedestalHologramState(Entity entity, float pulse, boolean renderShellPass, boolean renderGhostPass) {
    }

    private static final class PoseStackAccess {
        private final com.mojang.blaze3d.vertex.PoseStack poseStack;

        private PoseStackAccess(Object context) throws ReflectiveOperationException {
            this.poseStack = (com.mojang.blaze3d.vertex.PoseStack) context.getClass().getMethod("poseStack").invoke(context);
        }

        private void push() {
            poseStack.pushPose();
        }

        private void pop() {
            poseStack.popPose();
        }

        private void translateY(float y) {
            poseStack.translate(0.0f, y, 0.0f);
        }

        private void scale(float x, float y, float z) {
            poseStack.scale(x, y, z);
        }
    }
}
