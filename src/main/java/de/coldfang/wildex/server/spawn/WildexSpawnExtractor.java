package de.coldfang.wildex.server.spawn;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WildexSpawnExtractor {

    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("the_nether");
    private static final ResourceLocation END = ResourceLocation.withDefaultNamespace("the_end");

    private WildexSpawnExtractor() {
    }

    public static Map<ResourceLocation, List<ResourceLocation>> collectSpawnBiomesByDimension(MinecraftServer server, EntityType<?> type) {
        if (server == null || type == null) return Map.of();

        Map<ResourceLocation, Set<ResourceLocation>> out = new LinkedHashMap<>();

        fillTaggedDimension(server, type, out, OVERWORLD, BiomeTags.IS_OVERWORLD);
        fillTaggedDimension(server, type, out, NETHER, BiomeTags.IS_NETHER);
        fillTaggedDimension(server, type, out, END, BiomeTags.IS_END);

        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation dimId = level.dimension().location();
            if (dimId.equals(OVERWORLD) || dimId.equals(NETHER) || dimId.equals(END)) continue;

            BiomeSource src = resolveBiomeSource(level);
            if (src == null) continue;

            Set<ResourceLocation> set = out.computeIfAbsent(dimId, k -> new LinkedHashSet<>());

            for (Holder<Biome> h : src.possibleBiomes()) {
                Biome b = h.value();
                if (isAbsentIn(b, type)) continue;

                ResourceLocation biomeId = level.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getKey(b);

                if (biomeId != null) set.add(biomeId);
            }
        }

        Map<ResourceLocation, List<ResourceLocation>> frozen = new LinkedHashMap<>();
        for (var e : out.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            List<ResourceLocation> list = new ArrayList<>(e.getValue());
            list.sort(Comparator.comparing(ResourceLocation::toString));
            frozen.put(e.getKey(), List.copyOf(list));
        }

        return Map.copyOf(frozen);
    }

    private static void fillTaggedDimension(
            MinecraftServer server,
            EntityType<?> type,
            Map<ResourceLocation, Set<ResourceLocation>> out,
            ResourceLocation dimId,
            TagKey<Biome> biomeTag
    ) {
        ServerLevel any = server.overworld();
        var reg = any.registryAccess().registryOrThrow(Registries.BIOME);

        for (Holder.Reference<Biome> h : reg.holders().toList()) {
            if (!h.is(biomeTag)) continue;

            Biome b = h.value();
            if (isAbsentIn(b, type)) continue;

            ResourceLocation biomeId = h.key().location();
            out.computeIfAbsent(dimId, k -> new LinkedHashSet<>()).add(biomeId);
        }
    }

    private static BiomeSource resolveBiomeSource(ServerLevel level) {
        try {
            ChunkGenerator gen = level.getChunkSource().getGenerator();
            return gen.getBiomeSource();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isAbsentIn(Biome biome, EntityType<?> type) {
        MobSpawnSettings s = biome.getMobSettings();
        for (MobCategory cat : MobCategory.values()) {
            try {
                for (MobSpawnSettings.SpawnerData d : s.getMobs(cat).unwrap()) {
                    if (d.type == type) return false;
                }
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
