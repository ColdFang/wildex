package de.coldfang.wildex.registry;

import de.coldfang.wildex.world.block.entity.WildexPedestalBlockEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class WildexCreativeTabEvents {

    private WildexCreativeTabEvents() {
    }

    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(ModItems.WILDEX_BOOK.get());
            if (WildexPedestalBlockEntity.pedestalEnabled()) {
                event.accept(ModItems.WILDEX_PEDESTAL.get());
            }
        }
    }
}
