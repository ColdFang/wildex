package de.coldfang.wildex.integration.accessorify;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class WildexSpyglassUseHelper {

    private WildexSpyglassUseHelper() {
    }

    public static boolean isClientSpyglassActive(Player player) {
        return isVanillaSpyglassActive(player) || WildexAccessorifyIntegration.isClientAccessorySpyglassActive(player);
    }

    public static boolean isServerSpyglassActive(ServerPlayer player) {
        return isVanillaSpyglassActive(player) || WildexAccessorifySpyglassState.isActive(player);
    }

    private static boolean isVanillaSpyglassActive(LivingEntity entity) {
        return entity != null && entity.isUsingItem() && entity.getUseItem().is(Items.SPYGLASS);
    }
}
