package de.coldfang.wildex.world;

import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import de.coldfang.wildex.world.block.entity.WildexAnalyzerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class WildexPedestalProtectionEvents {

    private WildexPedestalProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        var player = event.getPlayer();
        var level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof WildexPedestalBlockEntity pedestal) {
            if (!pedestal.hasBook()) return;
            if (pedestal.isOwner(player.getUUID())) return;

            event.setCanceled(true);
            Component text = Component.translatable("message.wildex.pedestal.owner_only_break");
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(text, true);
            } else {
                player.displayClientMessage(text, true);
            }
            return;
        }

        if (blockEntity instanceof WildexAnalyzerBlockEntity analyzer) {
            if (!analyzer.hasItem()) return;
            if (analyzer.isOwner(player.getUUID())) return;

            event.setCanceled(true);
            Component text = Component.translatable("message.wildex.analyzer.owner_only_break");
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(text, true);
            } else {
                player.displayClientMessage(text, true);
            }
        }
    }
}
