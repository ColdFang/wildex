package de.coldfang.wildex.world;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WildexGiveBookOnFirstJoinEvents {

    private WildexGiveBookOnFirstJoinEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!CommonConfig.INSTANCE.giveBookOnFirstJoin.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        ServerLevel level = sp.serverLevel();

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        if (data.hasReceivedBook(sp.getUUID())) return;

        ItemStack book = new ItemStack(ModItems.WILDEX_BOOK.get());
        boolean added = sp.getInventory().add(book);
        if (!added) {
            sp.drop(book, false);
        }

        data.markReceivedBook(sp.getUUID());
    }
}
