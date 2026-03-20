package de.coldfang.wildex.network;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.server.WildexDiscoveryService;
import de.coldfang.wildex.util.WildexMobIdCanonicalizer;
import de.coldfang.wildex.world.WildexWorldPlayerKillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

        ResourceLocation id = WildexMobIdCanonicalizer.canonicalize(BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()));
        if (id == null) return;

        int kills = WildexWorldPlayerKillData.get(sp.serverLevel()).increment(sp.getUUID(), id);

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexDiscoveryService.discover(
                    sp,
                    id,
                    WildexDiscoveryService.DiscoverySource.KILL,
                    WildexDiscoveryService.DiscoveryCapture.atEntity(dead)
            );
        }

        PacketDistributor.sendToPlayer(sp, new S2CMobKillsPayload(id, kills));
    }
}
