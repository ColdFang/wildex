package de.coldfang.wildex.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WildexWorldPlayerCooldownData extends SavedData {

    private static final String DATA_NAME = "wildex_player_cooldowns";
    private static final String SPYGLASS_PULSE_KEY = "spyglass_pulse";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerCooldownData> FACTORY =
            new Factory<>(WildexWorldPlayerCooldownData::new, WildexWorldPlayerCooldownData::load);

    private final Map<UUID, Long> spyglassPulseCooldownEnd = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerCooldownData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        if (server == null) return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);

        ServerLevel overworld = server.overworld();
        ServerLevel rootLevel = overworld != null ? overworld : level;

        WildexWorldPlayerCooldownData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerCooldownData() {
    }

    @SuppressWarnings("resource")
    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;

            WildexWorldPlayerCooldownData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerCooldownData legacy) {
        if (legacy == this) return;

        for (Map.Entry<UUID, Long> entry : legacy.spyglassPulseCooldownEnd.entrySet()) {
            UUID playerId = entry.getKey();
            long incoming = entry.getValue() == null ? 0L : entry.getValue();
            if (playerId == null || incoming <= 0L) continue;

            long existing = spyglassPulseCooldownEnd.getOrDefault(playerId, 0L);
            long next = Math.max(existing, incoming);
            if (next != existing) {
                spyglassPulseCooldownEnd.put(playerId, next);
            }
        }
    }

    public long getSpyglassPulseCooldownEnd(UUID playerId) {
        if (playerId == null) return 0L;
        return spyglassPulseCooldownEnd.getOrDefault(playerId, 0L);
    }

    public void setSpyglassPulseCooldownEnd(UUID playerId, long endGameTime) {
        if (playerId == null) return;

        if (endGameTime <= 0L) {
            if (spyglassPulseCooldownEnd.remove(playerId) != null) setDirty();
            return;
        }

        Long prev = spyglassPulseCooldownEnd.put(playerId, endGameTime);
        if (prev == null || prev != endGameTime) setDirty();
    }

    private static WildexWorldPlayerCooldownData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerCooldownData data = new WildexWorldPlayerCooldownData();
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);

        if (!tag.contains(SPYGLASS_PULSE_KEY, Tag.TAG_COMPOUND)) return data;

        CompoundTag root = tag.getCompound(SPYGLASS_PULSE_KEY);
        for (String k : root.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(k);
            } catch (Exception ignored) {
                continue;
            }

            if (!root.contains(k, Tag.TAG_LONG)) continue;

            long end = root.getLong(k);
            if (end > 0L) data.spyglassPulseCooldownEnd.put(id, end);
        }

        return data;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        if (!spyglassPulseCooldownEnd.isEmpty()) {
            CompoundTag root = new CompoundTag();
            for (var e : spyglassPulseCooldownEnd.entrySet()) {
                root.put(e.getKey().toString(), LongTag.valueOf(e.getValue()));
            }
            tag.put(SPYGLASS_PULSE_KEY, root);
        }

        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }
}
