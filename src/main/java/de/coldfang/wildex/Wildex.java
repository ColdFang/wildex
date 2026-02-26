package de.coldfang.wildex;

import com.mojang.logging.LogUtils;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.integration.exposure.WildexExposureIntegrationBootstrap;
import de.coldfang.wildex.integration.kubejs.WildexKubeJsLifecycleEvents;
import de.coldfang.wildex.network.WildexKillSyncEvents;
import de.coldfang.wildex.network.WildexNetwork;
import de.coldfang.wildex.network.WildexRuntimeCacheEvents;
import de.coldfang.wildex.network.WildexSpyglassDiscoveryEvents;
import de.coldfang.wildex.registry.WildexCreativeTabEvents;
import de.coldfang.wildex.registry.ModItems;
import de.coldfang.wildex.server.WildexCompletionSyncOnJoinEvents;
import de.coldfang.wildex.server.WildexShareOfferCommands;
import de.coldfang.wildex.world.WildexGiveBookOnFirstJoinEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(Wildex.MODID)
public class Wildex {

    public static final String MODID = "wildex";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String COMMON_CONFIG_FILE = "wildex-common.toml";
    private static final String CLIENT_CONFIG_FILE = "wildex-client.toml";
    private static final String MIGRATIONS_FILE = "wildex-migrations.properties";
    private static final String CONFIG_SUBDIR = MODID;

    public Wildex(IEventBus modEventBus, ModContainer modContainer) {

        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(WildexCreativeTabEvents::onBuildCreativeTabContents);

        modEventBus.register(WildexNetwork.class);
        modEventBus.addListener(CommonConfig::onConfigLoading);
        modEventBus.addListener(CommonConfig::onConfigReloading);

        NeoForge.EVENT_BUS.register(WildexKillSyncEvents.class);
        NeoForge.EVENT_BUS.register(WildexSpyglassDiscoveryEvents.class);
        NeoForge.EVENT_BUS.register(WildexRuntimeCacheEvents.class);
        NeoForge.EVENT_BUS.register(WildexKubeJsLifecycleEvents.class);
        NeoForge.EVENT_BUS.register(WildexGiveBookOnFirstJoinEvents.class);
        NeoForge.EVENT_BUS.register(WildexCompletionSyncOnJoinEvents.class);
        NeoForge.EVENT_BUS.register(WildexShareOfferCommands.class);
        WildexExposureIntegrationBootstrap.registerIfAvailable();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            bootstrapClientOnly(modEventBus);
        }

        migrateLegacyConfigLayoutIfNeeded();

        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, CONFIG_SUBDIR + "/" + COMMON_CONFIG_FILE);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, CONFIG_SUBDIR + "/" + CLIENT_CONFIG_FILE);

        LOGGER.info("Wildex initialized");
    }

    private static void bootstrapClientOnly(IEventBus modEventBus) {
        try {
            Class<?> bootstrap = Class.forName("de.coldfang.wildex.client.WildexClientBootstrap");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to bootstrap Wildex client-only hooks", e);
        }
    }

    private static void migrateLegacyConfigLayoutIfNeeded() {
        Path configRoot = FMLPaths.CONFIGDIR.get();
        if (configRoot == null) return;

        Path wildexDir = configRoot.resolve(CONFIG_SUBDIR);
        try {
            Files.createDirectories(wildexDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create Wildex config directory at {}", wildexDir, e);
            return;
        }

        migrateLegacyFile(configRoot.resolve(COMMON_CONFIG_FILE), wildexDir.resolve(COMMON_CONFIG_FILE));
        migrateLegacyFile(configRoot.resolve(CLIENT_CONFIG_FILE), wildexDir.resolve(CLIENT_CONFIG_FILE));
        migrateLegacyFile(configRoot.resolve(MIGRATIONS_FILE), wildexDir.resolve(MIGRATIONS_FILE));
    }

    private static void migrateLegacyFile(Path legacyPath, Path targetPath) {
        if (legacyPath == null || targetPath == null) return;
        if (!Files.exists(legacyPath)) return;
        if (Files.exists(targetPath)) return;

        try {
            Files.move(legacyPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Migrated Wildex config file from {} to {}", legacyPath.getFileName(), targetPath);
            return;
        } catch (IOException ignored) {
            // Fall through to copy/delete fallback.
        }

        try {
            Files.copy(legacyPath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
            Files.deleteIfExists(legacyPath);
            LOGGER.info("Migrated Wildex config file from {} to {}", legacyPath.getFileName(), targetPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to migrate Wildex config file {} to {}", legacyPath, targetPath, e);
        }
    }
}
