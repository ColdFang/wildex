package de.coldfang.wildex.item;

import de.coldfang.wildex.client.screen.WildexScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class WildexBookItem extends Item {

    public WildexBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new WildexScreen());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
