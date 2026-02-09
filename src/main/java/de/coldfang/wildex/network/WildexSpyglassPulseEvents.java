package de.coldfang.wildex.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WildexSpyglassPulseEvents {

    private WildexSpyglassPulseEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event == null) return;
        if (!(event.getEntity() instanceof LocalPlayer p)) return;

        if (!p.getMainHandItem().is(Items.SPYGLASS)) return;

        PacketDistributor.sendToServer(new C2SSpyglassPulsePayload());
    }
}
