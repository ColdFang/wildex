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

public final class WildexWorldPlayerKillData extends SavedData {

    private static final String DATA_NAME = "wildex_player_kills";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerKillData> FACTORY =
            new Factory<>(WildexWorldPlayerKillData::new, WildexWorldPlayerKillData::load);

    private final Map<UUID, Map<ResourceLocation, Integer>> kills = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerKillData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();

        WildexWorldPlayerKillData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerKillData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;

            WildexWorldPlayerKillData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerKillData legacy) {
        if (legacy == this) return;

        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> playerEntry : legacy.kills.entrySet()) {
            UUID playerId = playerEntry.getKey();
            if (playerId == null || playerEntry.getValue() == null || playerEntry.getValue().isEmpty()) continue;

            Map<ResourceLocation, Integer> target = kills.computeIfAbsent(playerId, ignored -> new HashMap<>());
            for (Map.Entry<ResourceLocation, Integer> mobEntry : playerEntry.getValue().entrySet()) {
                ResourceLocation mobId = mobEntry.getKey();
                if (mobId == null) continue;

                int incoming = Math.max(0, mobEntry.getValue() == null ? 0 : mobEntry.getValue());
                if (incoming <= 0) continue;

                int existing = Math.max(0, target.getOrDefault(mobId, 0));
                long merged = (long) existing + (long) incoming;
                int next = merged > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) merged;

                if (next != existing) {
                    target.put(mobId, next);
                }
            }
        }
    }

    public int getKills(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return 0;
        return kills.getOrDefault(player, Map.of()).getOrDefault(mobId, 0);
    }

    public int increment(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return 0;

        Map<ResourceLocation, Integer> map = kills.computeIfAbsent(player, k -> new HashMap<>());
        int existing = Math.max(0, map.getOrDefault(mobId, 0));
        int next = existing >= Integer.MAX_VALUE ? Integer.MAX_VALUE : existing + 1;
        map.put(mobId, next);

        setDirty();
        return next;
    }

    @SuppressWarnings("unused")
    public Map<ResourceLocation, Integer> getMobKillCounts(UUID player) {
        if (player == null) return Map.of();
        Map<ResourceLocation, Integer> map = kills.get(player);
        if (map == null || map.isEmpty()) return Map.of();
        return Map.copyOf(map);
    }

    private static WildexWorldPlayerKillData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerKillData data = new WildexWorldPlayerKillData();
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);

        for (String playerKey : tag.getAllKeys()) {
            if (MIGRATED_KEY.equals(playerKey)) continue;

            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (Exception ignored) {
                continue;
            }

            CompoundTag playerTag = tag.getCompound(playerKey);
            Map<ResourceLocation, Integer> mobMap = new HashMap<>();

            for (String mobKey : playerTag.getAllKeys()) {
                ResourceLocation mobId = ResourceLocation.tryParse(mobKey);
                if (mobId == null) continue;

                mobMap.put(mobId, Math.max(0, playerTag.getInt(mobKey)));
            }

            data.kills.put(playerId, mobMap);
        }

        return data;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> playerEntry : kills.entrySet()) {
            CompoundTag playerTag = new CompoundTag();

            for (Map.Entry<ResourceLocation, Integer> mobEntry : playerEntry.getValue().entrySet()) {
                playerTag.putInt(mobEntry.getKey().toString(), Math.max(0, mobEntry.getValue()));
            }

            tag.put(playerEntry.getKey().toString(), playerTag);
        }

        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }

        return tag;
    }
}
