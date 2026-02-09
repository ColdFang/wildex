package de.coldfang.wildex.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.client.screen.WildexScreen;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

@EventBusSubscriber(modid = Wildex.MODID, value = Dist.CLIENT)
public final class WildexKeybinds {

    public static final KeyMapping OPEN_WILDEX = new KeyMapping(
            "key.wildex.open",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            "key.categories.wildex"
    );

    private static boolean pendingOpen = false;

    private WildexKeybinds() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WILDEX);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event == null) return;

        if (!OPEN_WILDEX.consumeClick()) return;
        pendingOpen = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (event == null) return;

        if (!pendingOpen) return;

        Minecraft mc = Minecraft.getInstance();

        pendingOpen = false;

        Player player = mc.player;
        if (player == null) return;

        if (CommonConfig.INSTANCE.requireBookForKeybind.get()) {
            boolean hasBook = player.getInventory().contains(
                    ModItems.WILDEX_BOOK.get().getDefaultInstance()
            );
            if (!hasBook) return;
        }

        mc.setScreen(new WildexScreen());
    }
}
