package de.coldfang.wildex.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class WildexBookItem extends Item {

    public WildexBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            openScreenClientOnly();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    private static void openScreenClientOnly() {
        try {
            Class<?> openerClass = Class.forName("de.coldfang.wildex.client.WildexScreenOpener");
            openerClass.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Client screen open failed; keep item use non-fatal.
        }
    }
}
