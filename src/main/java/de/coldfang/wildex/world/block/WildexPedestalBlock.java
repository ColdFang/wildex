package de.coldfang.wildex.world.block;

import com.mojang.serialization.MapCodec;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullableProblems")
public final class WildexPedestalBlock extends BaseEntityBlock {

    public static final MapCodec<WildexPedestalBlock> CODEC = simpleCodec(WildexPedestalBlock::new);

    public WildexPedestalBlock(Properties properties) {
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
        if (!(level.getBlockEntity(pos) instanceof WildexPedestalBlockEntity pedestal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean enabled = WildexPedestalBlockEntity.pedestalEnabled();
        if (pedestal.hasBook()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            ItemStack extracted = pedestal.tryExtractForPlayer(player);
            if (extracted.isEmpty()) {
                notifyOwnerOnly(player);
                return ItemInteractionResult.FAIL;
            }

            if (!player.addItem(extracted)) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extracted);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (!enabled) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!stack.is(de.coldfang.wildex.registry.ModItems.WILDEX_BOOK.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean inserted = pedestal.tryInsertFromPlayer((ServerLevel) level, player, stack);
        if (!inserted) return ItemInteractionResult.FAIL;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (pedestal.getDisplayMobId() == null) {
            WildexPedestalBlockEntity.DebugCounts debug = pedestal.getLastDebugCounts();
            player.displayClientMessage(
                    Component.translatable(
                            "message.wildex.pedestal.no_discoveries",
                            debug.discoveredCount(),
                            debug.candidateCount()
                    ),
                    true
            );
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
        if (!(level.getBlockEntity(pos) instanceof WildexPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (!pedestal.hasBook()) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack extracted = pedestal.tryExtractForPlayer(player);
        if (extracted.isEmpty()) {
            notifyOwnerOnly(player);
            return InteractionResult.FAIL;
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
    public @NotNull BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WildexPedestalBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WildexPedestalBlockEntity pedestal) {
                pedestal.dropStoredBook();
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
                de.coldfang.wildex.registry.ModBlockEntities.WILDEX_PEDESTAL.get(),
                WildexPedestalBlockEntity::serverTick
        );
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof WildexPedestalBlockEntity pedestal)) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        if (!pedestal.hasBook()) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        if (pedestal.isOwner(player.getUUID())) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return 0.0f;
    }

    private static void notifyOwnerOnly(Player player) {
        if (player == null) return;
        Component text = Component.translatable("message.wildex.pedestal.owner_only_extract");
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(text, true);
            return;
        }
        player.displayClientMessage(text, true);
    }
}
