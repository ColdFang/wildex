package de.coldfang.wildex.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WildexWorldPlayerSharePrefsData extends SavedData {

    private static final String DATA_NAME = "wildex_player_share_prefs";
    private static final String ACCEPT_OFFERS_KEY = "accept_offers";
    private static final String MIGRATED_KEY = "__migrated_to_overworld_storage";

    private static final Factory<WildexWorldPlayerSharePrefsData> FACTORY =
            new Factory<>(WildexWorldPlayerSharePrefsData::new, WildexWorldPlayerSharePrefsData::load);

    private final Set<UUID> acceptingOffers = new HashSet<>();
    private boolean migratedToOverworldStorage;

    public static WildexWorldPlayerSharePrefsData get(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        ServerLevel rootLevel = server.overworld();
        WildexWorldPlayerSharePrefsData data = rootLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.migrateLegacyDimensionData(server, rootLevel);
        return data;
    }

    private WildexWorldPlayerSharePrefsData() {
    }

    private void migrateLegacyDimensionData(MinecraftServer server, ServerLevel rootLevel) {
        if (migratedToOverworldStorage) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == rootLevel) continue;
            WildexWorldPlayerSharePrefsData legacy = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            mergeFromLegacy(legacy);
        }

        migratedToOverworldStorage = true;
        setDirty();
    }

    private void mergeFromLegacy(WildexWorldPlayerSharePrefsData legacy) {
        if (legacy == this) return;
        acceptingOffers.addAll(legacy.acceptingOffers);
    }

    public boolean isAcceptingOffers(UUID playerId) {
        if (playerId == null) return false;
        return acceptingOffers.contains(playerId);
    }

    public void setAcceptingOffers(UUID playerId, boolean accepting) {
        if (playerId == null) return;
        boolean changed;
        if (accepting) {
            changed = acceptingOffers.add(playerId);
        } else {
            changed = acceptingOffers.remove(playerId);
        }
        if (changed) setDirty();
    }

    private static WildexWorldPlayerSharePrefsData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerSharePrefsData data = new WildexWorldPlayerSharePrefsData();
        if (tag.contains(ACCEPT_OFFERS_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ACCEPT_OFFERS_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.acceptingOffers.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
        }
        data.migratedToOverworldStorage = tag.getBoolean(MIGRATED_KEY);
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        if (!acceptingOffers.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID id : acceptingOffers) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(ACCEPT_OFFERS_KEY, list);
        }
        if (migratedToOverworldStorage) {
            tag.putBoolean(MIGRATED_KEY, true);
        }
        return tag;
    }
}
