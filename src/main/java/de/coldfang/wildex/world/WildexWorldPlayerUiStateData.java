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

public final class WildexWorldPlayerUiStateData extends SavedData {

    private static final String DATA_NAME = "wildex_player_ui_state";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";
    private static final String TAB_KEY = "tab";
    private static final String MOB_KEY = "mob";
    private static final String DEFAULT_TAB = "STATS";

    private static final Factory<WildexWorldPlayerUiStateData> FACTORY =
            new Factory<>(WildexWorldPlayerUiStateData::new, WildexWorldPlayerUiStateData::load);

    private final Map<UUID, UiState> byPlayer = new HashMap<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerUiStateData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();

        WildexWorldPlayerUiStateData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerUiStateData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;

            WildexWorldPlayerUiStateData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerUiStateData legacy) {
        if (legacy == this) return;
        for (Map.Entry<UUID, UiState> e : legacy.byPlayer.entrySet()) {
            UUID playerId = e.getKey();
            UiState state = e.getValue();
            if (playerId == null || state == null) continue;
            byPlayer.put(playerId, sanitize(state));
        }
    }

    public UiState getState(UUID playerId) {
        if (playerId == null) return UiState.DEFAULT;
        UiState state = byPlayer.get(playerId);
        return state == null ? UiState.DEFAULT : state;
    }

    public void setState(UUID playerId, String tabId, String mobId) {
        if (playerId == null) return;

        UiState next = sanitize(new UiState(tabId, mobId));
        UiState prev = byPlayer.get(playerId);
        if (next.equals(prev)) return;

        byPlayer.put(playerId, next);
        setDirty();
    }

    private static UiState sanitize(UiState in) {
        if (in == null) return UiState.DEFAULT;
        String tab = sanitizeTab(in.tabId());
        String mob = sanitizeMob(in.mobId());
        return new UiState(tab, mob);
    }

    private static String sanitizeTab(String tabId) {
        if (tabId == null) return DEFAULT_TAB;
        String s = tabId.trim().toUpperCase(java.util.Locale.ROOT);
        if (s.isEmpty() || s.length() > 32) return DEFAULT_TAB;
        return s;
    }

    private static String sanitizeMob(String mobId) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId == null ? "" : mobId);
        return rl == null ? "" : rl.toString();
    }

    private static WildexWorldPlayerUiStateData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerUiStateData data = new WildexWorldPlayerUiStateData();
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);

        for (String playerKey : tag.getAllKeys()) {
            if (MIGRATED_KEY.equals(playerKey)) continue;

            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (Exception ignored) {
                continue;
            }

            CompoundTag t = tag.getCompound(playerKey);
            String tab = t.getString(TAB_KEY);
            String mob = t.getString(MOB_KEY);
            data.byPlayer.put(playerId, sanitize(new UiState(tab, mob)));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, UiState> e : byPlayer.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            CompoundTag t = new CompoundTag();
            t.putString(TAB_KEY, sanitizeTab(e.getValue().tabId()));
            t.putString(MOB_KEY, sanitizeMob(e.getValue().mobId()));
            tag.put(e.getKey().toString(), t);
        }
        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }

    public record UiState(String tabId, String mobId) {
        public static final UiState DEFAULT = new UiState(DEFAULT_TAB, "");
    }
}
