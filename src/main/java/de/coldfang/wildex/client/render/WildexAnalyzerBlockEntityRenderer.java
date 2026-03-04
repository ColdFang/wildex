package de.coldfang.wildex.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.coldfang.wildex.world.block.entity.WildexAnalyzerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class WildexAnalyzerBlockEntityRenderer implements BlockEntityRenderer<WildexAnalyzerBlockEntity> {

    private static final double MAX_RENDER_DISTANCE = 32.0;
    private static final float ITEM_X = 0.50f;
    private static final float ITEM_Y = 0.94f;
    private static final float ITEM_Z = 0.46f;
    private static final float ITEM_SCALE = 0.30f;

    private final ItemRenderer itemRenderer;

    public WildexAnalyzerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() {
        return (int) MAX_RENDER_DISTANCE;
    }

    @Override
    public boolean shouldRender(@NotNull WildexAnalyzerBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, MAX_RENDER_DISTANCE);
    }

    @Override
    @SuppressWarnings("unused")
    public void render(
            @NotNull WildexAnalyzerBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = blockEntity.getStoredItemForRender();
        if (stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float t = mc.level.getGameTime() + partialTick;
        float bob = blockEntity.isAnalyzing() ? (float) Math.sin(t * 0.18f) * 0.015f : 0.0f;
        float spin = blockEntity.isAnalyzing() ? (t * 4.0f) % 360.0f : 0.0f;
        boolean blockItem = stack.getItem() instanceof BlockItem;
        float itemY = ITEM_Y + (blockItem ? 0.01f : 0.0f);
        float itemScale = blockItem ? (ITEM_SCALE * 0.72f) : ITEM_SCALE;

        poseStack.pushPose();
        poseStack.translate(ITEM_X, itemY + bob, ITEM_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(itemScale, itemScale, itemScale);

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                Mth.floor(t)
        );
        poseStack.popPose();
    }
}
