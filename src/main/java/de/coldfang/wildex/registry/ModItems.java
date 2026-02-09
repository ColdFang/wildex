package de.coldfang.wildex.registry;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.item.WildexBookItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wildex.MODID);

    private static final ResourceKey<Item> WILDEX_BOOK_ID =
            ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Wildex.MODID, "wildex_book"));

    public static final DeferredItem<Item> WILDEX_BOOK = ITEMS.register(
            "wildex_book",
            () -> new WildexBookItem(
                    new Item.Properties()
                            .setId(WILDEX_BOOK_ID)
                            .stacksTo(1)
            )
    );

    private ModItems() {
    }
}
