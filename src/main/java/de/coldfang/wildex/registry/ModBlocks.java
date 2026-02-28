package de.coldfang.wildex.registry;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.world.block.WildexPedestalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wildex.MODID);

    public static final DeferredBlock<Block> WILDEX_PEDESTAL = BLOCKS.register(
            "wildex_pedestal",
            () -> new WildexPedestalBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.5f, 1200.0f)
                            .sound(SoundType.STONE)
            )
    );

    private ModBlocks() {
    }
}

