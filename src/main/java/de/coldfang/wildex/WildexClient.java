package de.coldfang.wildex;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Wildex.MODID, dist = Dist.CLIENT)
public class WildexClient {

    public WildexClient(ModContainer container) {
        // Register config screen for client & common configs
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );
    }
}
