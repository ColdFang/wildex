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

public final class WildexWorldPlayerViewedEntriesData extends SavedData {

    private static final String DATA_NAME = "wildex_player_viewed_entries";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerViewedEntriesData> FACTORY =
            new Factory<>(WildexWorldPlayerViewedEntriesData::new, WildexWorldPlayerViewedEntriesData::load);

    private final Map<UUID, Set<ResourceLocation>> viewedByPlayer = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerViewedEntriesData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();

        WildexWorldPlayerViewedEntriesData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerViewedEntriesData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;
            WildexWorldPlayerViewedEntriesData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerViewedEntriesData legacy) {
        if (legacy == this) return;
        for (Map.Entry<UUID, Set<ResourceLocation>> e : legacy.viewedByPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Set<ResourceLocation> viewed = e.getValue();
            if (playerId == null || viewed == null || viewed.isEmpty()) continue;

            Set<ResourceLocation> target = viewedByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>());
            for (ResourceLocation mobId : viewed) {
                if (!WildexMobFilters.isTrackable(mobId)) continue;
                target.add(mobId);
            }
        }
    }

    public boolean markViewed(UUID playerId, ResourceLocation mobId) {
        if (playerId == null || mobId == null) return false;
        if (!WildexMobFilters.isTrackable(mobId)) return false;

        Set<ResourceLocation> set = viewedByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>());
        boolean added = set.add(mobId);
        if (added) setDirty();
        return added;
    }

    public Set<ResourceLocation> getViewed(UUID playerId) {
        if (playerId == null) return Set.of();
        Set<ResourceLocation> set = viewedByPlayer.get(playerId);
        if (set == null || set.isEmpty()) return Set.of();

        Set<ResourceLocation> filtered = new HashSet<>();
        for (ResourceLocation mobId : set) {
            if (WildexMobFilters.isTrackable(mobId)) filtered.add(mobId);
        }
        if (filtered.isEmpty()) return Set.of();
        return Set.copyOf(filtered);
    }

    private static WildexWorldPlayerViewedEntriesData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerViewedEntriesData data = new WildexWorldPlayerViewedEntriesData();
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
            Set<ResourceLocation> viewed = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation mobId = ResourceLocation.tryParse(list.getString(i));
                if (!WildexMobFilters.isTrackable(mobId)) continue;
                viewed.add(mobId);
            }
            if (!viewed.isEmpty()) data.viewedByPlayer.put(playerId, viewed);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Set<ResourceLocation>> e : viewedByPlayer.entrySet()) {
            UUID playerId = e.getKey();
            Set<ResourceLocation> viewed = e.getValue();
            if (playerId == null || viewed == null || viewed.isEmpty()) continue;

            ListTag list = new ListTag();
            for (ResourceLocation mobId : viewed) {
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
