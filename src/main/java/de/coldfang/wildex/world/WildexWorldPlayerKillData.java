package de.coldfang.wildex.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WildexWorldPlayerKillData extends SavedData {

    private static final String DATA_NAME = "wildex_player_kills";

    private static final Factory<WildexWorldPlayerKillData> FACTORY =
            new Factory<>(WildexWorldPlayerKillData::new, WildexWorldPlayerKillData::load);

    private final Map<UUID, Map<ResourceLocation, Integer>> kills = new HashMap<>();

    public static WildexWorldPlayerKillData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private WildexWorldPlayerKillData() {
    }

    public int getKills(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return 0;
        return kills.getOrDefault(player, Map.of()).getOrDefault(mobId, 0);
    }

    public int increment(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return 0;

        Map<ResourceLocation, Integer> map = kills.computeIfAbsent(player, k -> new HashMap<>());
        int next = map.getOrDefault(mobId, 0) + 1;
        map.put(mobId, next);

        setDirty();
        return next;
    }

    private static WildexWorldPlayerKillData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerKillData data = new WildexWorldPlayerKillData();

        for (String playerKey : tag.getAllKeys()) {
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
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> playerEntry : kills.entrySet()) {
            CompoundTag playerTag = new CompoundTag();

            for (Map.Entry<ResourceLocation, Integer> mobEntry : playerEntry.getValue().entrySet()) {
                playerTag.putInt(mobEntry.getKey().toString(), Math.max(0, mobEntry.getValue()));
            }

            tag.put(playerEntry.getKey().toString(), playerTag);
        }

        return tag;
    }
}
