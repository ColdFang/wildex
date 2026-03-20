package de.coldfang.wildex.world;

import de.coldfang.wildex.util.WildexMobFilters;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WildexWorldPlayerFavoriteEntriesData extends SavedData {

    private static final String DATA_NAME = "wildex_player_favorite_entries";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerFavoriteEntriesData> FACTORY =
            new Factory<>(WildexWorldPlayerFavoriteEntriesData::new, WildexWorldPlayerFavoriteEntriesData::load);

    private final Map<UUID, Set<ResourceLocation>> favoritesByPlayer = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerFavoriteEntriesData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();

        WildexWorldPlayerFavoriteEntriesData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerFavoriteEntriesData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;
            WildexWorldPlayerFavoriteEntriesData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerFavoriteEntriesData legacy) {
        if (legacy == this) return;
        for (Map.Entry<UUID, Set<ResourceLocation>> e : legacy.favoritesByPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Set<ResourceLocation> favorites = e.getValue();
            if (playerId == null || favorites == null || favorites.isEmpty()) continue;

            Set<ResourceLocation> target = favoritesByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>());
            for (ResourceLocation mobId : favorites) {
                if (!WildexMobFilters.isTrackable(mobId)) continue;
                target.add(mobId);
            }
        }
    }

    public void setFavorite(UUID playerId, ResourceLocation mobId, boolean favorite) {
        if (playerId == null || mobId == null) return;
        if (!WildexMobFilters.isTrackable(mobId)) return;

        Set<ResourceLocation> set = favoritesByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>());
        boolean changed = favorite ? set.add(mobId) : set.remove(mobId);
        if (changed) {
            if (set.isEmpty()) {
                favoritesByPlayer.remove(playerId);
            }
            setDirty();
        }
    }

    public Set<ResourceLocation> getFavorites(UUID playerId) {
        if (playerId == null) return Set.of();
        Set<ResourceLocation> set = favoritesByPlayer.get(playerId);
        if (set == null || set.isEmpty()) return Set.of();

        Set<ResourceLocation> filtered = new HashSet<>();
        for (ResourceLocation mobId : set) {
            if (WildexMobFilters.isTrackable(mobId)) filtered.add(mobId);
        }
        if (filtered.isEmpty()) return Set.of();
        return Set.copyOf(filtered);
    }

    private static WildexWorldPlayerFavoriteEntriesData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerFavoriteEntriesData data = new WildexWorldPlayerFavoriteEntriesData();
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);

        for (String playerKey : tag.getAllKeys()) {
            if (MIGRATED_KEY.equals(playerKey)) continue;

            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (Exception ignored) {
                continue;
            }

            ListTag list = tag.getList(playerKey, Tag.TAG_STRING);
            Set<ResourceLocation> favorites = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation mobId = ResourceLocation.tryParse(list.getString(i));
                if (!WildexMobFilters.isTrackable(mobId)) continue;
                favorites.add(mobId);
            }
            if (!favorites.isEmpty()) data.favoritesByPlayer.put(playerId, favorites);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Set<ResourceLocation>> e : favoritesByPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Set<ResourceLocation> favorites = e.getValue();
            if (playerId == null || favorites == null || favorites.isEmpty()) continue;

            ListTag list = new ListTag();
            for (ResourceLocation mobId : favorites) {
                if (!WildexMobFilters.isTrackable(mobId)) continue;
                list.add(StringTag.valueOf(mobId.toString()));
            }
            if (!list.isEmpty()) tag.put(playerId.toString(), list);
        }

        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }
}
