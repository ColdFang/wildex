package de.coldfang.wildex.world.block.entity;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.registry.ModItems;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WildexPedestalBlockEntity extends BlockEntity {

    private static final String TAG_BOOK = "Book";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_DISPLAY_MOB = "DisplayMob";
    private static final int MOB_SWITCH_INTERVAL_TICKS = 40;
    private static final int DISCOVERY_REFRESH_INTERVAL_TICKS = 100;
    private static final int DISCOVERY_SNAPSHOT_CACHE_TTL_TICKS = 200;
    private static final int MAX_DISCOVERY_SNAPSHOT_CACHE_ENTRIES = 512;
    private static final int NEGATIVE_MOB_TYPE_CACHE_TTL_TICKS = 200;
    private static final Map<ResourceLocation, MobTypeCacheEntry> MOB_TYPE_VALIDATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, DiscoverySnapshotCacheEntry> DISCOVERY_SNAPSHOT_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, DiscoverySnapshotCacheEntry> eldest) {
                    return size() > MAX_DISCOVERY_SNAPSHOT_CACHE_ENTRIES;
                }
            }
    );

    private ItemStack storedBook = ItemStack.EMPTY;
    @Nullable
    private UUID ownerId = null;
    @Nullable
    private ResourceLocation displayMobId = null;

    private int ticksUntilSwitch = MOB_SWITCH_INTERVAL_TICKS;
    private int displayIndex = -1;
    private long lastDiscoveryRefreshTick = Long.MIN_VALUE;
    private List<ResourceLocation> cachedDiscoveredMobs = List.of();
    private int lastRawDiscoveredCount = 0;

    @Nullable
    private Entity cachedClientRenderEntity = null;
    @Nullable
    private ResourceLocation cachedClientRenderEntityId = null;

    public WildexPedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(de.coldfang.wildex.registry.ModBlockEntities.WILDEX_PEDESTAL.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WildexPedestalBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        blockEntity.tickServer(serverLevel);
    }

    public static boolean pedestalEnabled() {
        return CommonConfig.INSTANCE.debugMode.get();
    }

    public static void clearMobTypeValidationCache() {
        MOB_TYPE_VALIDATION_CACHE.clear();
        DISCOVERY_SNAPSHOT_CACHE.clear();
    }

    public static void invalidateDiscoverySnapshot(@Nullable UUID ownerId) {
        if (ownerId == null) return;
        DISCOVERY_SNAPSHOT_CACHE.remove(ownerId);
    }

    private void tickServer(ServerLevel level) {
        if (!pedestalEnabled()) {
            clearDisplayMob();
            resetRotationCache();
            return;
        }

        if (!hasBook() || ownerId == null) {
            clearDisplayMob();
            resetRotationCache();
            return;
        }

        long gameTime = level.getGameTime();
        if (lastDiscoveryRefreshTick == Long.MIN_VALUE || (gameTime - lastDiscoveryRefreshTick) >= DISCOVERY_REFRESH_INTERVAL_TICKS) {
            refreshDiscoveredMobCandidates(level, false);
            lastDiscoveryRefreshTick = gameTime;
        }

        if (cachedDiscoveredMobs.isEmpty()) {
            clearDisplayMob();
            return;
        }

        boolean currentIndexValid = displayIndex >= 0
                && displayIndex < cachedDiscoveredMobs.size()
                && Objects.equals(cachedDiscoveredMobs.get(displayIndex), displayMobId);
        if (!currentIndexValid) {
            int resolved = resolveDisplayIndex(cachedDiscoveredMobs);
            displayIndex = resolved;
            ticksUntilSwitch = 0;
        }

        ticksUntilSwitch--;
        if (ticksUntilSwitch > 0) return;

        displayIndex = (displayIndex + 1) % cachedDiscoveredMobs.size();
        ResourceLocation next = cachedDiscoveredMobs.get(displayIndex);
        setDisplayMobId(next);
        ticksUntilSwitch = MOB_SWITCH_INTERVAL_TICKS;
    }

    public boolean hasBook() {
        return !storedBook.isEmpty();
    }

    public boolean isOwner(@Nullable UUID playerId) {
        return playerId != null && ownerId != null && ownerId.equals(playerId);
    }

    public boolean canPlayerExtract(@Nullable Player player) {
        if (player == null) return false;
        return isOwner(player.getUUID());
    }

    public boolean tryInsertFromPlayer(ServerLevel level, Player player, ItemStack heldStack) {
        if (!pedestalEnabled()) return false;
        if (player == null || heldStack == null || heldStack.isEmpty()) return false;
        if (hasBook()) return false;
        if (!heldStack.is(ModItems.WILDEX_BOOK.get())) return false;

        ItemStack inserted = heldStack.copyWithCount(1);
        this.storedBook = inserted;
        this.ownerId = player.getUUID();
        this.displayIndex = -1;
        this.ticksUntilSwitch = 0;
        this.lastDiscoveryRefreshTick = Long.MIN_VALUE;
        refreshAndSelectNow(level);
        syncNow();
        return true;
    }

    public ItemStack tryExtractForPlayer(Player player) {
        if (!canPlayerExtract(player)) return ItemStack.EMPTY;
        if (!hasBook()) return ItemStack.EMPTY;

        ItemStack extracted = storedBook.copy();
        storedBook = ItemStack.EMPTY;
        ownerId = null;
        clearDisplayMob();
        resetRotationCache();
        syncNow();
        return extracted;
    }

    public void dropStoredBook() {
        if (this.level == null || this.level.isClientSide || storedBook.isEmpty()) return;
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        net.minecraft.world.Containers.dropItemStack(
                this.level,
                center.x(),
                center.y(),
                center.z(),
                storedBook.copy()
        );
        storedBook = ItemStack.EMPTY;
        ownerId = null;
        clearDisplayMob();
        resetRotationCache();
        syncNow();
    }

    @Nullable
    public ResourceLocation getDisplayMobId() {
        return displayMobId;
    }

    public DebugCounts getLastDebugCounts() {
        return new DebugCounts(lastRawDiscoveredCount, cachedDiscoveredMobs.size());
    }

    @Nullable
    public Entity getOrCreateClientRenderEntity() {
        Level level = this.level;
        if (level == null || !level.isClientSide) return null;
        if (displayMobId == null) {
            clearClientRenderEntity();
            return null;
        }

        if (cachedClientRenderEntity != null
                && Objects.equals(cachedClientRenderEntityId, displayMobId)
                && !cachedClientRenderEntity.isRemoved()) {
            return cachedClientRenderEntity;
        }

        clearClientRenderEntity();

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(displayMobId).orElse(null);
        if (type == null) return null;

        Entity created;
        try {
            created = type.create(level);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (created == null) return null;

        cachedClientRenderEntity = created;
        cachedClientRenderEntityId = displayMobId;
        return created;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedBook.isEmpty()) {
            tag.put(TAG_BOOK, storedBook.saveOptional(registries));
        }
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
        if (displayMobId != null) {
            tag.putString(TAG_DISPLAY_MOB, displayMobId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_BOOK, Tag.TAG_COMPOUND)) {
            storedBook = ItemStack.parseOptional(registries, tag.getCompound(TAG_BOOK));
        } else {
            storedBook = ItemStack.EMPTY;
        }

        if (tag.hasUUID(TAG_OWNER)) {
            ownerId = tag.getUUID(TAG_OWNER);
        } else {
            ownerId = null;
        }

        ResourceLocation loadedDisplayId = null;
        if (tag.contains(TAG_DISPLAY_MOB, Tag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(TAG_DISPLAY_MOB));
            if (parsed != null
                    && WildexMobFilters.isTrackable(parsed)
                    && BuiltInRegistries.ENTITY_TYPE.containsKey(parsed)) {
                loadedDisplayId = parsed;
            }
        }
        displayMobId = loadedDisplayId;

        resetRotationCache();
        clearClientRenderEntity();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        clearClientRenderEntity();
    }

    private void refreshDiscoveredMobCandidates(ServerLevel level, boolean forceRefresh) {
        if (ownerId == null) {
            cachedDiscoveredMobs = List.of();
            displayIndex = -1;
            lastRawDiscoveredCount = 0;
            return;
        }

        DiscoverySnapshotCacheEntry snapshot = resolveSnapshot(level, ownerId, forceRefresh);
        lastRawDiscoveredCount = snapshot.discoveredCount();
        List<ResourceLocation> next = snapshot.candidateIds();

        if (next.isEmpty()) {
            cachedDiscoveredMobs = List.of();
            displayIndex = -1;
            return;
        }

        List<ResourceLocation> frozen = next;
        if (!frozen.equals(cachedDiscoveredMobs)) {
            cachedDiscoveredMobs = frozen;
            displayIndex = resolveDisplayIndex(cachedDiscoveredMobs);
            ticksUntilSwitch = 0;
        }
    }

    private int resolveDisplayIndex(List<ResourceLocation> candidates) {
        if (displayMobId == null || candidates == null || candidates.isEmpty()) return -1;
        int idx = candidates.indexOf(displayMobId);
        return Math.max(-1, idx);
    }

    private void clearDisplayMob() {
        setDisplayMobId(null);
    }

    private void setDisplayMobId(@Nullable ResourceLocation id) {
        if (Objects.equals(this.displayMobId, id)) return;
        this.displayMobId = id;
        clearClientRenderEntity();
        syncNow();
    }

    private void resetRotationCache() {
        this.cachedDiscoveredMobs = List.of();
        this.displayIndex = -1;
        this.ticksUntilSwitch = MOB_SWITCH_INTERVAL_TICKS;
        this.lastDiscoveryRefreshTick = Long.MIN_VALUE;
        this.lastRawDiscoveredCount = 0;
    }

    private void clearClientRenderEntity() {
        if (cachedClientRenderEntity != null) {
            cachedClientRenderEntity.discard();
        }
        cachedClientRenderEntity = null;
        cachedClientRenderEntityId = null;
    }

    private void syncNow() {
        this.setChanged();
        if (this.level == null || this.level.isClientSide) return;
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
    }

    private static boolean isMobTypeInLevel(ServerLevel level, ResourceLocation id) {
        if (level == null || id == null) return false;
        long nowTick = level.getGameTime();
        MobTypeCacheEntry cached = MOB_TYPE_VALIDATION_CACHE.get(id);
        if (cached != null) {
            if (cached.mob()) return true;
            if (nowTick < cached.retryAfterTick()) return false;
            MOB_TYPE_VALIDATION_CACHE.remove(id);
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            putNegativeMobCache(id, nowTick);
            return false;
        }

        Entity e;
        try {
            e = type.create(level);
        } catch (RuntimeException ignored) {
            putNegativeMobCache(id, nowTick);
            return false;
        }
        if (e == null) {
            putNegativeMobCache(id, nowTick);
            return false;
        }

        boolean mob = e instanceof Mob;
        e.discard();
        if (mob) {
            MOB_TYPE_VALIDATION_CACHE.put(id, new MobTypeCacheEntry(true, Long.MAX_VALUE));
        } else {
            putNegativeMobCache(id, nowTick);
        }
        return mob;
    }

    private void refreshAndSelectNow(ServerLevel level) {
        refreshDiscoveredMobCandidates(level, true);
        if (!cachedDiscoveredMobs.isEmpty()) {
            this.displayIndex = 0;
            this.ticksUntilSwitch = MOB_SWITCH_INTERVAL_TICKS;
            setDisplayMobId(cachedDiscoveredMobs.get(0));
        } else {
            clearDisplayMob();
        }
    }

    private static void putNegativeMobCache(ResourceLocation id, long nowTick) {
        MOB_TYPE_VALIDATION_CACHE.put(id, new MobTypeCacheEntry(false, nowTick + NEGATIVE_MOB_TYPE_CACHE_TTL_TICKS));
    }

    private static DiscoverySnapshotCacheEntry resolveSnapshot(ServerLevel level, UUID owner, boolean forceRefresh) {
        if (level == null || owner == null) return DiscoverySnapshotCacheEntry.EMPTY;

        long nowTick = level.getGameTime();
        if (!forceRefresh) {
            DiscoverySnapshotCacheEntry cached = snapshotGet(owner);
            if (cached != null && nowTick < cached.nextRefreshTick()) {
                return cached;
            }
        }

        Set<ResourceLocation> discovered = WildexWorldPlayerDiscoveryData.get(level).getDiscovered(owner);
        int discoveredCount = discovered.size();
        if (discovered.isEmpty()) {
            DiscoverySnapshotCacheEntry empty = new DiscoverySnapshotCacheEntry(List.of(), 0, nowTick + DISCOVERY_SNAPSHOT_CACHE_TTL_TICKS);
            snapshotPut(owner, empty);
            return empty;
        }

        ArrayList<ResourceLocation> next = new ArrayList<>();
        for (ResourceLocation id : discovered) {
            if (id == null) continue;
            if (!WildexMobFilters.isTrackable(id)) continue;
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) continue;
            if (!isMobTypeInLevel(level, id)) continue;
            next.add(id);
        }

        if (next.isEmpty()) {
            DiscoverySnapshotCacheEntry empty = new DiscoverySnapshotCacheEntry(List.of(), discoveredCount, nowTick + DISCOVERY_SNAPSHOT_CACHE_TTL_TICKS);
            snapshotPut(owner, empty);
            return empty;
        }

        Collections.sort(next);
        DiscoverySnapshotCacheEntry built = new DiscoverySnapshotCacheEntry(
                List.copyOf(next),
                discoveredCount,
                nowTick + DISCOVERY_SNAPSHOT_CACHE_TTL_TICKS
        );
        snapshotPut(owner, built);
        return built;
    }

    private static @Nullable DiscoverySnapshotCacheEntry snapshotGet(UUID owner) {
        synchronized (DISCOVERY_SNAPSHOT_CACHE) {
            return DISCOVERY_SNAPSHOT_CACHE.get(owner);
        }
    }

    private static void snapshotPut(UUID owner, DiscoverySnapshotCacheEntry entry) {
        synchronized (DISCOVERY_SNAPSHOT_CACHE) {
            DISCOVERY_SNAPSHOT_CACHE.put(owner, entry);
        }
    }

    public record DebugCounts(int discoveredCount, int candidateCount) {
    }

    private record MobTypeCacheEntry(boolean mob, long retryAfterTick) {
    }

    private record DiscoverySnapshotCacheEntry(List<ResourceLocation> candidateIds, int discoveredCount, long nextRefreshTick) {
        private static final DiscoverySnapshotCacheEntry EMPTY = new DiscoverySnapshotCacheEntry(List.of(), 0, Long.MIN_VALUE);
    }
}
