package de.coldfang.wildex.server;

import de.coldfang.wildex.network.S2CWildexCompletePayload;
import de.coldfang.wildex.config.CommonConfig;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class WildexCompletionHelper {

    private static volatile int cachedTotalMobs = -1;
    private static volatile String cachedConfigSignature = "";

    private WildexCompletionHelper() {
    }

    public static void onMobDiscovered(ServerLevel level, ServerPlayer sp) {
        if (markCompleteIfEligible(level, sp)) {
            notifyCompleted(sp);
        }
    }

    public static boolean markCompleteIfEligible(ServerLevel level, ServerPlayer sp) {
        if (level == null || sp == null) return false;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        UUID playerId = sp.getUUID();

        if (data.isComplete(playerId)) return false;

        int total = getTotalMobCount(level);
        if (total <= 0) return false;

        int discovered = getFilteredDiscoveredCount(data.getDiscovered(playerId));
        if (discovered < total) return false;

        return data.markComplete(playerId);
    }

    public static void notifyCompleted(ServerPlayer sp) {
        if (sp == null) return;
        WildexProgressHooks.onCompleted(sp);
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
        String sig = configSignature();
        if (v >= 0 && sig.equals(cachedConfigSignature)) return v;

        if (level == null) return 0;

        int total = 0;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!WildexMobFilters.isTrackable(type)) continue;

            Entity e;
            try {
                e = type.create(level);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (e == null) continue;

            boolean isMob = e instanceof Mob;
            e.discard();
            if (!isMob) continue;

            total++;
        }

        if (total <= 0) return 0;

        cachedTotalMobs = total;
        cachedConfigSignature = sig;
        return total;
    }

    private static String configSignature() {
        List<String> raw = new ArrayList<>();
        List<? extends String> cfg = CommonConfig.INSTANCE.excludedModIds.get();
        for (String s : cfg) {
            if (s == null) continue;
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) raw.add(trimmed.toLowerCase(Locale.ROOT));
        }
        Collections.sort(raw);
        return String.join("|", raw);
    }
}
