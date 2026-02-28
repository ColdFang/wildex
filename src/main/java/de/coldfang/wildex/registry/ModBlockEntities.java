package de.coldfang.wildex.registry;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wildex.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WildexPedestalBlockEntity>> WILDEX_PEDESTAL =
            BLOCK_ENTITY_TYPES.register(
                    "wildex_pedestal",
                    () -> BlockEntityType.Builder.of(
                            WildexPedestalBlockEntity::new,
                            ModBlocks.WILDEX_PEDESTAL.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }
}
