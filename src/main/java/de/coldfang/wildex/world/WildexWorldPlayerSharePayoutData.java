package de.coldfang.wildex.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WildexWorldPlayerSharePayoutData extends SavedData {

    private static final String DATA_NAME = "wildex_player_share_payouts";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerSharePayoutData> FACTORY =
            new Factory<>(WildexWorldPlayerSharePayoutData::new, WildexWorldPlayerSharePayoutData::load);

    private final Map<UUID, Map<ResourceLocation, Integer>> byPlayer = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerSharePayoutData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();
        WildexWorldPlayerSharePayoutData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerSharePayoutData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;
            WildexWorldPlayerSharePayoutData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }
        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerSharePayoutData legacy) {
        if (legacy == this) return;
        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> e : legacy.byPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Map<ResourceLocation, Integer> map = e.getValue();
            if (playerId == null || map == null || map.isEmpty()) continue;
            Map<ResourceLocation, Integer> target = byPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
            for (Map.Entry<ResourceLocation, Integer> m : map.entrySet()) {
                ResourceLocation itemId = m.getKey();
                int amount = Math.max(0, m.getValue() == null ? 0 : m.getValue());
                if (itemId == null || amount <= 0) continue;
                target.merge(itemId, amount, Integer::sum);
            }
        }
    }

    public void add(UUID playerId, ResourceLocation itemId, int amount) {
        if (playerId == null || itemId == null || amount <= 0) return;
        Map<ResourceLocation, Integer> map = byPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        map.merge(itemId, amount, Integer::sum);
        setDirty();
    }

    public int total(UUID playerId) {
        if (playerId == null) return 0;
        Map<ResourceLocation, Integer> map = byPlayer.get(playerId);
        if (map == null || map.isEmpty()) return 0;
        int sum = 0;
        for (int v : map.values()) {
            if (v > 0) sum += v;
        }
        return sum;
    }

    public Map<ResourceLocation, Integer> takeAll(UUID playerId) {
        if (playerId == null) return Map.of();
        Map<ResourceLocation, Integer> map = byPlayer.remove(playerId);
        if (map == null || map.isEmpty()) return Map.of();
        setDirty();
        return Map.copyOf(map);
    }

    private static WildexWorldPlayerSharePayoutData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerSharePayoutData data = new WildexWorldPlayerSharePayoutData();
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);
        for (String key : tag.getAllKeys()) {
            if (MIGRATED_KEY.equals(key)) continue;
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (Exception ignored) {
                continue;
            }
            CompoundTag payoutTag = tag.getCompound(key);
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (String itemKey : payoutTag.getAllKeys()) {
                ResourceLocation itemId = ResourceLocation.tryParse(itemKey);
                if (itemId == null) continue;
                int amount = Math.max(0, payoutTag.getInt(itemKey));
                if (amount <= 0) continue;
                map.put(itemId, amount);
            }
            if (!map.isEmpty()) {
                data.byPlayer.put(playerId, map);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> e : byPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Map<ResourceLocation, Integer> map = e.getValue();
            if (playerId == null || map == null || map.isEmpty()) continue;
            CompoundTag payoutTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Integer> m : map.entrySet()) {
                if (m.getKey() == null) continue;
                int amount = Math.max(0, m.getValue() == null ? 0 : m.getValue());
                if (amount <= 0) continue;
                payoutTag.putInt(m.getKey().toString(), amount);
            }
            if (!payoutTag.isEmpty()) {
                tag.put(playerId.toString(), payoutTag);
            }
        }
        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }
}
