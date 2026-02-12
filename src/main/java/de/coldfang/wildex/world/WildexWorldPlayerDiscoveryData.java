package de.coldfang.wildex.world;

import de.coldfang.wildex.util.WildexMobFilters;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WildexWorldPlayerDiscoveryData extends SavedData {

    private static final String DATA_NAME = "wildex_player_discovery";
    private static final String RECEIVED_BOOK_KEY = "__received_book";
    private static final String COMPLETE_KEY = "__wildex_complete";

    private static final Factory<WildexWorldPlayerDiscoveryData> FACTORY =
            new Factory<>(WildexWorldPlayerDiscoveryData::new, WildexWorldPlayerDiscoveryData::load);

    private final Map<UUID, Set<ResourceLocation>> discovered = new HashMap<>();
    private final Set<UUID> receivedBook = new HashSet<>();
    private final Set<UUID> complete = new HashSet<>();

    public static WildexWorldPlayerDiscoveryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private WildexWorldPlayerDiscoveryData() {
    }

    public boolean isDiscovered(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        if (!WildexMobFilters.isTrackable(mobId)) return false;
        return discovered.getOrDefault(player, Set.of()).contains(mobId);
    }

    public boolean markDiscovered(UUID player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        if (!WildexMobFilters.isTrackable(mobId)) return false;

        Set<ResourceLocation> set = discovered.computeIfAbsent(player, k -> new HashSet<>());
        boolean added = set.add(mobId);
        if (added) setDirty();
        return added;
    }

    public Set<ResourceLocation> getDiscovered(UUID player) {
        if (player == null) return Set.of();
        Set<ResourceLocation> set = discovered.get(player);
        if (set == null || set.isEmpty()) return Set.of();

        Set<ResourceLocation> out = new HashSet<>();
        for (ResourceLocation rl : set) {
            if (WildexMobFilters.isTrackable(rl)) out.add(rl);
        }

        if (out.isEmpty()) return Set.of();
        return Set.copyOf(out);
    }

    public boolean isComplete(UUID player) {
        if (player == null) return false;
        return complete.contains(player);
    }

    public boolean markComplete(UUID player) {
        if (player == null) return false;
        boolean added = complete.add(player);
        if (added) setDirty();
        return added;
    }

    public boolean hasReceivedBook(UUID player) {
        if (player == null) return false;
        return receivedBook.contains(player);
    }

    public void markReceivedBook(UUID player) {
        if (player == null) return;
        boolean added = receivedBook.add(player);
        if (added) setDirty();
    }

    private static WildexWorldPlayerDiscoveryData load(CompoundTag tag, HolderLookup.Provider provider) {
        WildexWorldPlayerDiscoveryData data = new WildexWorldPlayerDiscoveryData();

        if (tag.contains(RECEIVED_BOOK_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(RECEIVED_BOOK_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.receivedBook.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
        }

        if (tag.contains(COMPLETE_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(COMPLETE_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.complete.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
        }

        for (String playerKey : tag.getAllKeys()) {
            if (RECEIVED_BOOK_KEY.equals(playerKey)) continue;
            if (COMPLETE_KEY.equals(playerKey)) continue;

            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (Exception ignored) {
                continue;
            }

            ListTag list = tag.getList(playerKey, StringTag.TAG_STRING);
            Set<ResourceLocation> set = new HashSet<>();

            for (int i = 0; i < list.size(); i++) {
                ResourceLocation rl = ResourceLocation.tryParse(list.getString(i));
                if (WildexMobFilters.isTrackable(rl)) set.add(rl);
            }

            if (!set.isEmpty()) {
                data.discovered.put(playerId, set);
            }
        }

        return data;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Set<ResourceLocation>> e : discovered.entrySet()) {
            ListTag list = new ListTag();
            for (ResourceLocation rl : e.getValue()) {
                if (!WildexMobFilters.isTrackable(rl)) continue;
                list.add(StringTag.valueOf(rl.toString()));
            }
            if (!list.isEmpty()) tag.put(e.getKey().toString(), list);
        }

        if (!receivedBook.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID id : receivedBook) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(RECEIVED_BOOK_KEY, list);
        }

        if (!complete.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID id : complete) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(COMPLETE_KEY, list);
        }

        return tag;
    }
}
