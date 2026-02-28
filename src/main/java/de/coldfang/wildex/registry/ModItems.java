package de.coldfang.wildex.registry;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.item.WildexBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Wildex.MODID);

    public static final DeferredItem<Item> WILDEX_BOOK =
            ITEMS.register("wildex_book",
                    () -> new WildexBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> WILDEX_PEDESTAL =
            ITEMS.register("wildex_pedestal",
                    () -> new BlockItem(ModBlocks.WILDEX_PEDESTAL.get(), new Item.Properties()));

    private ModItems() {
    }
}
