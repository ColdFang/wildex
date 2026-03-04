package de.coldfang.wildex.world.block;

import com.mojang.serialization.MapCodec;
import de.coldfang.wildex.world.block.entity.WildexAnalyzerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullableProblems")
public final class WildexAnalyzerBlock extends BaseEntityBlock {

    public static final MapCodec<WildexAnalyzerBlock> CODEC = simpleCodec(WildexAnalyzerBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

    public WildexAnalyzerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof WildexAnalyzerBlockEntity analyzer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (analyzer.hasItem()) {
            if (level.isClientSide) {
                if (analyzer.canPlayerExtract(player) && !analyzer.isAnalyzing()) {
                    analyzer.clearClientVisualsAfterExtraction();
                }
                return ItemInteractionResult.SUCCESS;
            }

            ItemStack extracted = analyzer.tryExtractForPlayer(player);
            if (extracted.isEmpty()) {
                if (analyzer.isAnalyzing()) {
                    notifyRunning(player);
                } else {
                    notifyOwnerOnly(player);
                }
                return ItemInteractionResult.CONSUME;
            }

            if (!player.addItem(extracted)) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extracted);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean inserted = analyzer.tryInsertFromPlayer((ServerLevel) level, player, stack);
        if (!inserted) return ItemInteractionResult.FAIL;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof WildexAnalyzerBlockEntity analyzer)) return InteractionResult.PASS;
        if (!analyzer.hasItem()) return InteractionResult.PASS;

        if (level.isClientSide) {
            if (analyzer.canPlayerExtract(player) && !analyzer.isAnalyzing()) {
                analyzer.clearClientVisualsAfterExtraction();
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = analyzer.tryExtractForPlayer(player);
        if (extracted.isEmpty()) {
            if (analyzer.isAnalyzing()) {
                notifyRunning(player);
            } else {
                notifyOwnerOnly(player);
            }
            return InteractionResult.CONSUME;
        }

        if (!player.addItem(extracted)) {
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extracted);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WildexAnalyzerBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WildexAnalyzerBlockEntity analyzer) {
                analyzer.dropStoredItem();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                blockEntityType,
                de.coldfang.wildex.registry.ModBlockEntities.WILDEX_ANALYZER.get(),
                WildexAnalyzerBlockEntity::serverTick
        );
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof WildexAnalyzerBlockEntity analyzer)) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        if (!analyzer.hasItem()) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        if (analyzer.isOwner(player.getUUID())) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return 0.0f;
    }

    private static void notifyOwnerOnly(Player player) {
        if (player == null) return;
        Component text = Component.translatable("message.wildex.analyzer.owner_only_extract");
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(text, true);
            return;
        }
        player.displayClientMessage(text, true);
    }

    private static void notifyRunning(Player player) {
        if (player == null) return;
        Component text = Component.translatable("message.wildex.analyzer.running");
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(text, true);
            return;
        }
        player.displayClientMessage(text, true);
    }
}
