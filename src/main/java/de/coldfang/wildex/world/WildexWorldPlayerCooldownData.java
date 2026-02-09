package de.coldfang.wildex.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WildexWorldPlayerCooldownData extends SavedData {

    private static final String DATA_NAME = "wildex_player_cooldowns";
    private static final String SPYGLASS_PULSE_KEY = "spyglass_pulse";

    private static final Factory<WildexWorldPlayerCooldownData> FACTORY =
            new Factory<>(WildexWorldPlayerCooldownData::new, WildexWorldPlayerCooldownData::load);

    private final Map<UUID, Long> spyglassPulseCooldownEnd = new HashMap<>();

    public static WildexWorldPlayerCooldownData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private WildexWorldPlayerCooldownData() {
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
        if (prev == null || prev.longValue() != endGameTime) setDirty();
    }

    private static WildexWorldPlayerCooldownData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerCooldownData data = new WildexWorldPlayerCooldownData();

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
    @SuppressWarnings("NullableProblems")
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (!spyglassPulseCooldownEnd.isEmpty()) {
            CompoundTag root = new CompoundTag();
            for (var e : spyglassPulseCooldownEnd.entrySet()) {
                root.put(e.getKey().toString(), LongTag.valueOf(e.getValue().longValue()));
            }
            tag.put(SPYGLASS_PULSE_KEY, root);
        }
        return tag;
    }
}
