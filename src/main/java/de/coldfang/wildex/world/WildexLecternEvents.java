package de.coldfang.wildex.world;

import de.coldfang.wildex.network.S2COpenWildexScreenPayload;
import de.coldfang.wildex.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WildexLecternEvents {

    private WildexLecternEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event == null) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        var pos = event.getPos();
        var state = sp.serverLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof LecternBlock)) return;
        if (!(sp.serverLevel().getBlockEntity(pos) instanceof LecternBlockEntity lectern)) return;
        if (!lectern.getBook().is(ModItems.WILDEX_BOOK.get())) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (sp.isShiftKeyDown() && sp.getMainHandItem().isEmpty()) {
            takeWildexFromLectern(sp, lectern, state, pos);
            return;
        }

        PacketDistributor.sendToPlayer(sp, new S2COpenWildexScreenPayload());
    }

    private static void takeWildexFromLectern(
            ServerPlayer player,
            LecternBlockEntity lectern,
            BlockState state,
            BlockPos pos
    ) {
        ItemStack book = lectern.getBook().copy();
        if (book.isEmpty()) return;

        lectern.setBook(ItemStack.EMPTY);
        LecternBlock.resetBookState(player, player.serverLevel(), pos, state, false);

        if (!player.addItem(book)) {
            player.drop(book, false);
        }
    }
}
