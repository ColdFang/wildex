package de.coldfang.wildex.world;

import de.coldfang.wildex.util.WildexMobFilters;
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

public final class WildexWorldPlayerDiscoveryDetailsData extends SavedData {

    private static final String DATA_NAME = "wildex_player_discovery_details";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final String TAG_SOURCE = "Source";
    private static final String TAG_SOURCE_DETAIL = "SourceDetail";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";
    private static final String TAG_DISCOVERED_AT = "DiscoveredAt";

    private static final Factory<WildexWorldPlayerDiscoveryDetailsData> FACTORY =
            new Factory<>(WildexWorldPlayerDiscoveryDetailsData::new, WildexWorldPlayerDiscoveryDetailsData::load);

    private final Map<UUID, Map<ResourceLocation, DiscoveryDetails>> detailsByPlayer = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerDiscoveryDetailsData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();

        WildexWorldPlayerDiscoveryDetailsData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerDiscoveryDetailsData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;
            WildexWorldPlayerDiscoveryDetailsData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerDiscoveryDetailsData legacy) {
        if (legacy == this) return;

        for (Map.Entry<UUID, Map<ResourceLocation, DiscoveryDetails>> entry : legacy.detailsByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            Map<ResourceLocation, DiscoveryDetails> details = entry.getValue();
            if (playerId == null || details == null || details.isEmpty()) continue;

            Map<ResourceLocation, DiscoveryDetails> target = detailsByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
            for (Map.Entry<ResourceLocation, DiscoveryDetails> detailEntry : details.entrySet()) {
                ResourceLocation mobId = detailEntry.getKey();
                DiscoveryDetails detail = detailEntry.getValue();
                if (!WildexMobFilters.isTrackable(mobId) || detail == null) continue;
                target.putIfAbsent(mobId, detail);
            }
        }
    }

    public DiscoveryDetails getDetails(UUID playerId, ResourceLocation mobId) {
        if (playerId == null || mobId == null) return null;
        if (!WildexMobFilters.isTrackable(mobId)) return null;
        Map<ResourceLocation, DiscoveryDetails> details = detailsByPlayer.get(playerId);
        if (details == null || details.isEmpty()) return null;
        return details.get(mobId);
    }

    public boolean hasDetails(UUID playerId, ResourceLocation mobId) {
        return getDetails(playerId, mobId) != null;
    }

    public void setDetailsIfAbsent(UUID playerId, ResourceLocation mobId, DiscoveryDetails details) {
        if (playerId == null || mobId == null || details == null) return;
        if (!WildexMobFilters.isTrackable(mobId)) return;

        Map<ResourceLocation, DiscoveryDetails> byMob = detailsByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        if (byMob.putIfAbsent(mobId, details) == null) {
            setDirty();
        }
    }

    public void removeDetails(UUID playerId, ResourceLocation mobId) {
        if (playerId == null || mobId == null) return;
        if (!WildexMobFilters.isTrackable(mobId)) return;

        Map<ResourceLocation, DiscoveryDetails> byMob = detailsByPlayer.get(playerId);
        if (byMob == null || byMob.isEmpty()) return;

        if (byMob.remove(mobId) == null) return;
        if (byMob.isEmpty()) {
            detailsByPlayer.remove(playerId);
        }
        setDirty();
    }

    private static WildexWorldPlayerDiscoveryDetailsData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerDiscoveryDetailsData data = new WildexWorldPlayerDiscoveryDetailsData();
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
            Map<ResourceLocation, DiscoveryDetails> detailsByMob = new HashMap<>();
            for (String mobKey : playerTag.getAllKeys()) {
                ResourceLocation mobId = ResourceLocation.tryParse(mobKey);
                if (!WildexMobFilters.isTrackable(mobId)) continue;

                CompoundTag detailTag = playerTag.getCompound(mobKey);
                DiscoveryDetails details = readDetails(detailTag);
                if (details == null) continue;
                detailsByMob.put(mobId, details);
            }

            if (!detailsByMob.isEmpty()) {
                data.detailsByPlayer.put(playerId, detailsByMob);
            }
        }

        return data;
    }

    private static DiscoveryDetails readDetails(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;

        String sourceId = tag.getString(TAG_SOURCE).trim();
        if (sourceId.isEmpty()) return null;

        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
        if (dimensionId == null) {
            dimensionId = ResourceLocation.withDefaultNamespace("overworld");
        }

        return new DiscoveryDetails(
                sourceId,
                tag.getString(TAG_SOURCE_DETAIL),
                dimensionId,
                tag.getInt(TAG_X),
                tag.getInt(TAG_Y),
                tag.getInt(TAG_Z),
                tag.getLong(TAG_DISCOVERED_AT)
        );
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<ResourceLocation, DiscoveryDetails>> entry : detailsByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            Map<ResourceLocation, DiscoveryDetails> details = entry.getValue();
            if (playerId == null || details == null || details.isEmpty()) continue;

            CompoundTag playerTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, DiscoveryDetails> detailEntry : details.entrySet()) {
                ResourceLocation mobId = detailEntry.getKey();
                DiscoveryDetails value = detailEntry.getValue();
                if (!WildexMobFilters.isTrackable(mobId) || value == null) continue;

                CompoundTag detailTag = new CompoundTag();
                detailTag.putString(TAG_SOURCE, value.sourceId());
                if (!value.sourceDetail().isBlank()) {
                    detailTag.putString(TAG_SOURCE_DETAIL, value.sourceDetail());
                }
                detailTag.putString(TAG_DIMENSION, value.dimensionId().toString());
                detailTag.putInt(TAG_X, value.x());
                detailTag.putInt(TAG_Y, value.y());
                detailTag.putInt(TAG_Z, value.z());
                detailTag.putLong(TAG_DISCOVERED_AT, value.discoveredAtEpochMillis());
                playerTag.put(mobId.toString(), detailTag);
            }

            if (!playerTag.isEmpty()) {
                tag.put(playerId.toString(), playerTag);
            }
        }

        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }

    public record DiscoveryDetails(
            String sourceId,
            String sourceDetail,
            ResourceLocation dimensionId,
            int x,
            int y,
            int z,
            long discoveredAtEpochMillis
    ) {
    }
}
