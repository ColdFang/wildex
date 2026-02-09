package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CWildexCompletePayload;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;

public final class WildexCompletionHelper {

    private static volatile int cachedTotalMobs = -1;

    private WildexCompletionHelper() {
    }

    public static void onMobDiscovered(ServerLevel level, ServerPlayer sp) {
        if (level == null || sp == null) return;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        UUID playerId = sp.getUUID();

        if (data.isComplete(playerId)) return;

        int total = getTotalMobCount(level);
        if (total <= 0) return;

        int discovered = getFilteredDiscoveredCount(data.getDiscovered(playerId));
        if (discovered < total) return;

        boolean newlyCompleted = data.markComplete(playerId);
        if (!newlyCompleted) return;

        PacketDistributor.sendToPlayer(sp, new S2CWildexCompletePayload());
    }

    private static int getFilteredDiscoveredCount(Set<ResourceLocation> discovered) {
        if (discovered == null || discovered.isEmpty()) return 0;
        int c = 0;
        for (ResourceLocation id : discovered) {
            if (WildexMobFilters.isTrackable(id)) c++;
        }
        return c;
    }

    public static int getTotalMobCount(ServerLevel level) {
        int v = cachedTotalMobs;
        if (v >= 0) return v;

        if (level == null) return 0;

        int total = 0;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!WildexMobFilters.isTrackable(type)) continue;

            Entity e = type.create(level);
            if (e == null) continue;

            boolean ok = e instanceof Mob;
            e.discard();

            if (!ok) continue;

            total++;
        }

        if (total <= 0) return 0;

        cachedTotalMobs = total;
        return total;
    }
}
