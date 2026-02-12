package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexCompletionHelper;
import de.coldfang.wildex.server.WildexProgressHooks;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerKillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WildexKillSyncEvents {

    private WildexKillSyncEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();

        Entity src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer sp)) return;

        if (!(sp.level() instanceof ServerLevel serverLevel)) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());

        int kills = WildexWorldPlayerKillData.get(serverLevel).increment(sp.getUUID(), id);

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexWorldPlayerDiscoveryData disc = WildexWorldPlayerDiscoveryData.get(serverLevel);

            boolean newlyDiscovered = disc.markDiscovered(sp.getUUID(), id);
            if (newlyDiscovered) {
                PacketDistributor.sendToPlayer(sp, new S2CDiscoveredMobPayload(id));
                boolean newlyCompleted = WildexCompletionHelper.markCompleteIfEligible(serverLevel, sp);
                WildexProgressHooks.onDiscoveryChanged(sp, id);
                if (newlyCompleted) {
                    WildexCompletionHelper.notifyCompleted(sp);
                }
            }
        }

        PacketDistributor.sendToPlayer(sp, new S2CMobKillsPayload(id, kills));
    }
}
