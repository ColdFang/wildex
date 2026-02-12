package de.coldfang.wildex.item;

import de.coldfang.wildex.client.screen.WildexScreen;
import net.minecraft.client.Minecraft;
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
            Minecraft.getInstance().setScreen(new WildexScreen());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
