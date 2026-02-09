package de.coldfang.wildex;

import com.mojang.logging.LogUtils;
import de.coldfang.wildex.client.WildexClientSessionEvents;
import de.coldfang.wildex.client.WildexCompletionClientEvents;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.network.WildexKillSyncEvents;
import de.coldfang.wildex.network.WildexNetwork;
import de.coldfang.wildex.network.WildexSpyglassDiscoveryEvents;
import de.coldfang.wildex.network.WildexSpyglassPulseEvents;
import de.coldfang.wildex.registry.ModItems;
import de.coldfang.wildex.server.WildexCompletionSyncOnJoinEvents;
import de.coldfang.wildex.world.WildexGiveBookOnFirstJoinEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Wildex.MODID)
public class Wildex {

    public static final String MODID = "wildex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Wildex(IEventBus modEventBus, ModContainer modContainer) {

        ModItems.ITEMS.register(modEventBus);

        modEventBus.register(WildexNetwork.class);

        NeoForge.EVENT_BUS.register(WildexKillSyncEvents.class);
        NeoForge.EVENT_BUS.register(WildexSpyglassDiscoveryEvents.class);
        NeoForge.EVENT_BUS.register(WildexGiveBookOnFirstJoinEvents.class);
        NeoForge.EVENT_BUS.register(WildexCompletionSyncOnJoinEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(WildexSpyglassPulseEvents.class);
            NeoForge.EVENT_BUS.register(WildexClientSessionEvents.class);
            NeoForge.EVENT_BUS.register(WildexCompletionClientEvents.class);
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        LOGGER.info("Wildex initialized");
    }
}
