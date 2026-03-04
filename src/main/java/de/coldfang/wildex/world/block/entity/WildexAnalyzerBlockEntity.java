package de.coldfang.wildex.world.block.entity;

import de.coldfang.wildex.server.WildexDiscoveryService;
import de.coldfang.wildex.server.WildexCompletionHelper;
import de.coldfang.wildex.server.loot.WildexAnalyzerLootIndex;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class WildexAnalyzerBlockEntity extends BlockEntity {

    private static final String TAG_ITEM = "Item";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_ANALYZING = "Analyzing";
    private static final String TAG_ANALYSIS_TICKS = "AnalysisTicks";
    private static final String TAG_WAS_POWERED = "WasPowered";

    private static final int ANALYSIS_DURATION_TICKS = 10 * 20;
    private static final int ANALYSIS_SCAN_BUDGET_PER_TICK = 3;
    private static final int ANALYSIS_SCAN_BUDGET_ON_FINISH = 120;

    private ItemStack storedItem = ItemStack.EMPTY;
    @Nullable
    private UUID ownerId = null;
    private boolean analyzing = false;
    private int analysisTicks = 0;
    private boolean wasPoweredLastTick = false;

    public WildexAnalyzerBlockEntity(BlockPos pos, BlockState blockState) {
        super(de.coldfang.wildex.registry.ModBlockEntities.WILDEX_ANALYZER.get(), pos, blockState);
    }

    @SuppressWarnings("unused")
    public static void serverTick(Level level, BlockPos pos, BlockState state, WildexAnalyzerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        blockEntity.tickServer(serverLevel);
    }

    public static void clearAnalyzerCaches() {
        WildexAnalyzerLootIndex.clear();
    }

    public boolean hasItem() {
        return !storedItem.isEmpty();
    }

    public ItemStack getStoredItemForRender() {
        return storedItem;
    }

    public boolean isAnalyzing() {
        return analyzing;
    }

    public boolean isOwner(@Nullable UUID playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }

    public boolean canPlayerExtract(@Nullable Player player) {
        if (player == null) return false;
        return isOwner(player.getUUID());
    }

    public void clearClientVisualsAfterExtraction() {
        if (this.level == null || !this.level.isClientSide) return;
        this.storedItem = ItemStack.EMPTY;
        this.ownerId = null;
        this.analyzing = false;
        this.analysisTicks = 0;
    }

    public boolean tryInsertFromPlayer(ServerLevel level, Player player, ItemStack heldStack) {
        if (level == null || player == null || heldStack == null || heldStack.isEmpty()) return false;
        if (hasItem()) return false;
        if (analyzing) return false;

        this.storedItem = heldStack.copyWithCount(1);
        this.ownerId = player.getUUID();
        this.analysisTicks = 0;
        this.analyzing = false;
        this.wasPoweredLastTick = level.hasNeighborSignal(this.worldPosition);

        if (this.wasPoweredLastTick) {
            startAnalysis(level);
        } else {
            syncNow();
        }
        return true;
    }

    public ItemStack tryExtractForPlayer(Player player) {
        if (!canPlayerExtract(player)) return ItemStack.EMPTY;
        if (analyzing) return ItemStack.EMPTY;
        if (!hasItem()) return ItemStack.EMPTY;

        ItemStack extracted = storedItem.copy();
        storedItem = ItemStack.EMPTY;
        ownerId = null;
        analysisTicks = 0;
        analyzing = false;
        syncNow();
        return extracted;
    }

    public void dropStoredItem() {
        if (this.level == null || this.level.isClientSide || storedItem.isEmpty()) return;

        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        Containers.dropItemStack(
                this.level,
                center.x(),
                center.y(),
                center.z(),
                storedItem.copy()
        );

        storedItem = ItemStack.EMPTY;
        ownerId = null;
        analysisTicks = 0;
        analyzing = false;
        syncNow();
    }

    private void tickServer(ServerLevel level) {
        boolean powered = level.hasNeighborSignal(this.worldPosition);

        if (!hasItem()) {
            ownerId = null;
            if (analyzing || analysisTicks != 0) {
                analyzing = false;
                analysisTicks = 0;
                syncNow();
            }
            wasPoweredLastTick = powered;
            return;
        }

        if (!analyzing && powered && !wasPoweredLastTick) {
            startAnalysis(level);
        }

        wasPoweredLastTick = powered;

        if (analyzing && !powered) {
            abortAnalysisNoPower(level);
            return;
        }

        if (!analyzing) return;

        analysisTicks++;
        ResourceLocation itemId = getStoredItemId();
        if (itemId != null) {
            WildexAnalyzerLootIndex.process(level, ANALYSIS_SCAN_BUDGET_PER_TICK);
        }

        if ((analysisTicks % 6) == 0) {
            spawnAnalyzeParticles(level, false);
        }
        if ((analysisTicks % 40) == 0) {
            level.playSound(
                    null,
                    this.worldPosition,
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    0.25f,
                    1.35f
            );
        }

        if (analysisTicks >= ANALYSIS_DURATION_TICKS) {
            finishAnalysis(level);
        }
    }

    private void startAnalysis(ServerLevel level) {
        if (level == null || analyzing || !hasItem()) return;

        analyzing = true;
        analysisTicks = 0;

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                0.60f,
                1.25f
        );
        spawnAnalyzeParticles(level, true);
        syncNow();
    }

    private void finishAnalysis(ServerLevel level) {
        analyzing = false;
        analysisTicks = 0;

        ResourceLocation itemId = getStoredItemId();
        if (itemId == null) {
            ItemStack returned = storedItem.copy();
            storedItem = ItemStack.EMPTY;
            syncNow();
            if (!returned.isEmpty()) {
                dropReturnedOnAnalyzer(level, returned);
            }
            notifyOwner(Component.translatable("message.wildex.analyzer.no_new_knowledge"));
            ownerId = null;
            syncNow();
            return;
        }

        WildexAnalyzerLootIndex.Resolution resolution = WildexAnalyzerLootIndex.resolve(
                level,
                itemId,
                ANALYSIS_SCAN_BUDGET_ON_FINISH
        );
        List<ResourceLocation> candidates = resolution.mobIds();

        ResourceLocation target = pickUndiscoveredCandidate(level, candidates);
        if (target != null && tryDiscover(level, target)) {
            storedItem = ItemStack.EMPTY;
            notifyOwner(Component.translatable("message.wildex.analyzer.discovered", mobName(target)));
            ownerId = null;

            level.playSound(
                    null,
                    this.worldPosition,
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.BLOCKS,
                    0.65f,
                    1.35f
            );
            spawnAnalyzeParticles(level, true);
            syncNow();
            return;
        }

        ItemStack returned = storedItem.copy();
        storedItem = ItemStack.EMPTY;
        syncNow();

        if (!returned.isEmpty()) {
            dropReturnedOnAnalyzer(level, returned);
        }
        notifyOwner(Component.translatable("message.wildex.analyzer.no_new_knowledge"));
        ownerId = null;

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS,
                0.55f,
                0.90f
        );
        syncNow();
    }

    private void abortAnalysisNoPower(ServerLevel level) {
        if (level == null || !analyzing) return;

        analyzing = false;
        analysisTicks = 0;

        ItemStack returned = storedItem.copy();
        storedItem = ItemStack.EMPTY;
        syncNow();

        if (!returned.isEmpty()) {
            dropReturnedOnAnalyzer(level, returned);
        }

        notifyOwner(Component.translatable("message.wildex.analyzer.no_new_knowledge"));
        ownerId = null;

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS,
                0.55f,
                0.90f
        );
        syncNow();
    }

    private @Nullable ResourceLocation pickUndiscoveredCandidate(ServerLevel level, List<ResourceLocation> candidates) {
        if (level == null || ownerId == null || candidates == null || candidates.isEmpty()) return null;

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        for (ResourceLocation candidate : candidates) {
            if (candidate == null) continue;
            if (!data.isDiscovered(ownerId, candidate)) return candidate;
        }
        return null;
    }

    private boolean tryDiscover(ServerLevel level, ResourceLocation mobId) {
        if (level == null || ownerId == null || mobId == null) return false;

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            return WildexDiscoveryService.discover(owner, mobId, WildexDiscoveryService.DiscoverySource.ANALYZER);
        }

        WildexWorldPlayerDiscoveryData data = WildexWorldPlayerDiscoveryData.get(level);
        boolean added = data.markDiscovered(ownerId, mobId);
        if (!added) return false;

        if (WildexCompletionHelper.isCurrentlyComplete(level, ownerId)) {
            data.markComplete(ownerId);
        }

        WildexPedestalBlockEntity.invalidateDiscoverySnapshot(ownerId);
        return true;
    }

    private void dropReturnedOnAnalyzer(ServerLevel level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) return;

        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        double dropY = this.worldPosition.getY() + (stack.getItem() instanceof BlockItem ? 1.20 : 1.08);
        ItemEntity itemEntity = new ItemEntity(level, center.x(), dropY, center.z(), stack.copy());
        itemEntity.setDeltaMovement(
                level.random.triangle(0.0, 0.02),
                0.16,
                level.random.triangle(0.0, 0.02)
        );
        level.addFreshEntity(itemEntity);
    }

    private void notifyOwner(Component message) {
        if (message == null || ownerId == null || this.level == null || this.level.isClientSide) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;
        owner.displayClientMessage(message, false);
    }

    private static Component mobName(ResourceLocation mobId) {
        if (mobId == null) return Component.literal("Unknown");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
        if (type == null) return Component.literal(mobId.toString());
        return type.getDescription();
    }

    private @Nullable ResourceLocation getStoredItemId() {
        if (storedItem.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(storedItem.getItem());
    }

    private void spawnAnalyzeParticles(ServerLevel level, boolean burst) {
        if (level == null) return;
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        int count = burst ? 24 : 8;
        double spread = burst ? 0.30 : 0.14;
        double speed = burst ? 0.02 : 0.006;

        level.sendParticles(
                ParticleTypes.ENCHANT,
                c.x(),
                c.y() + 1.05,
                c.z(),
                count,
                spread,
                0.10,
                spread,
                speed
        );

        if (burst) {
            level.sendParticles(
                    ParticleTypes.GLOW,
                    c.x(),
                    c.y() + 1.10,
                    c.z(),
                    12,
                    0.22,
                    0.12,
                    0.22,
                    0.01
            );
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedItem.isEmpty()) {
            tag.put(TAG_ITEM, storedItem.saveOptional(registries));
        }
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
        if (analyzing) {
            tag.putBoolean(TAG_ANALYZING, true);
        }
        if (analysisTicks > 0) {
            tag.putInt(TAG_ANALYSIS_TICKS, analysisTicks);
        }
        if (wasPoweredLastTick) {
            tag.putBoolean(TAG_WAS_POWERED, true);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ITEM, Tag.TAG_COMPOUND)) {
            storedItem = ItemStack.parseOptional(registries, tag.getCompound(TAG_ITEM));
        } else {
            storedItem = ItemStack.EMPTY;
        }

        ownerId = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        analyzing = tag.getBoolean(TAG_ANALYZING);
        analysisTicks = Math.max(0, tag.getInt(TAG_ANALYSIS_TICKS));
        wasPoweredLastTick = tag.getBoolean(TAG_WAS_POWERED);

        if (storedItem.isEmpty()) {
            ownerId = null;
            analyzing = false;
            analysisTicks = 0;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncNow() {
        this.setChanged();
        if (this.level == null || this.level.isClientSide) return;

        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 11);
    }
}
