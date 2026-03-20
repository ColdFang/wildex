package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.model.WildexDiscoveryDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class WildexDiscoveryDetailsFormatter {

    private static final DateTimeFormatter WHEN_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm", Locale.ROOT);

    private WildexDiscoveryDetailsFormatter() {
    }

    public static Component sourceTypeLine(WildexDiscoveryDetails details) {
        if (details == null) return Component.translatable("gui.wildex.discovery.loading");
        String sourceId = details.sourceId() == null ? "" : details.sourceId().trim().toLowerCase(java.util.Locale.ROOT);
        return Component.translatable("gui.wildex.discovery.source." + sourceId);
    }

    public static Component coordsLine(WildexDiscoveryDetails details) {
        if (details == null) return Component.empty();
        return Component.literal(details.x() + "," + details.y() + "," + details.z());
    }

    public static Component dimensionLine(WildexDiscoveryDetails details) {
        if (details == null) return Component.translatable("gui.wildex.dimension.unknown");
        return resolveDimensionName(details.dimensionId());
    }

    public static Component whenLine(WildexDiscoveryDetails details) {
        if (details == null || details.discoveredAtEpochMillis() <= 0L) {
            return Component.translatable("gui.wildex.discovery.when.unknown");
        }

        String formatted = WHEN_FORMATTER.format(
                Instant.ofEpochMilli(details.discoveredAtEpochMillis()).atZone(ZoneId.systemDefault())
        );
        return Component.literal(formatted);
    }

    private static Component resolveDimensionName(ResourceLocation dimensionId) {
        if (dimensionId == null) return Component.translatable("gui.wildex.dimension.unknown");
        if (ResourceLocation.withDefaultNamespace("overworld").equals(dimensionId)) {
            return Component.translatable("gui.wildex.dimension.overworld");
        }
        if (ResourceLocation.withDefaultNamespace("the_nether").equals(dimensionId)) {
            return Component.translatable("gui.wildex.dimension.nether");
        }
        if (ResourceLocation.withDefaultNamespace("the_end").equals(dimensionId)) {
            return Component.translatable("gui.wildex.dimension.end");
        }
        String path = dimensionId.getPath();
        if (path.isBlank()) {
            return Component.literal(dimensionId.getNamespace());
        }
        String readable = path.replace('_', ' ').replace('/', ' ').trim();
        if (readable.isBlank()) {
            readable = dimensionId.toString();
        }
        return Component.literal(readable);
    }
}
