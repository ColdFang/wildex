package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record S2CMobSpawnsPayload(
        ResourceLocation mobId,
        List<DimSection> naturalSections,
        List<StructureSection> structureSections
) implements CustomPacketPayload {

    public static final Type<S2CMobSpawnsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_spawns"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobSpawnsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        List<DimSection> ss = p.naturalSections() == null ? List.of() : p.naturalSections();
                        buf.writeVarInt(ss.size());
                        for (DimSection s : ss) {
                            buf.writeResourceLocation(s.dimensionId());
                            List<ResourceLocation> biomes = s.biomeIds() == null ? List.of() : s.biomeIds();
                            buf.writeVarInt(biomes.size());
                            for (ResourceLocation b : biomes) buf.writeResourceLocation(b);
                        }

                        List<StructureSection> structures = p.structureSections() == null ? List.of() : p.structureSections();
                        buf.writeVarInt(structures.size());
                        for (StructureSection s : structures) {
                            buf.writeResourceLocation(s.structureId());
                            List<ResourceLocation> biomes = s.biomeIds() == null ? List.of() : s.biomeIds();
                            buf.writeVarInt(biomes.size());
                            for (ResourceLocation b : biomes) buf.writeResourceLocation(b);
                        }
                    },
                    buf -> {
                        ResourceLocation mobId = buf.readResourceLocation();
                        int sn = Math.max(0, buf.readVarInt());
                        List<DimSection> natural = new ArrayList<>(Math.min(sn, 64));
                        for (int i = 0; i < sn; i++) {
                            ResourceLocation dim = buf.readResourceLocation();
                            int bn = Math.max(0, buf.readVarInt());
                            List<ResourceLocation> biomes = new ArrayList<>(Math.min(bn, 4096));
                            for (int j = 0; j < bn; j++) biomes.add(buf.readResourceLocation());
                            natural.add(new DimSection(dim, List.copyOf(biomes)));
                        }

                        int stn = Math.max(0, buf.readVarInt());
                        List<StructureSection> structures = new ArrayList<>(Math.min(stn, 256));
                        for (int i = 0; i < stn; i++) {
                            ResourceLocation structureId = buf.readResourceLocation();
                            int bn = Math.max(0, buf.readVarInt());
                            List<ResourceLocation> biomes = new ArrayList<>(Math.min(bn, 4096));
                            for (int j = 0; j < bn; j++) biomes.add(buf.readResourceLocation());
                            structures.add(new StructureSection(structureId, List.copyOf(biomes)));
                        }
                        return new S2CMobSpawnsPayload(mobId, List.copyOf(natural), List.copyOf(structures));
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record DimSection(ResourceLocation dimensionId, List<ResourceLocation> biomeIds) {
    }

    public record StructureSection(ResourceLocation structureId, List<ResourceLocation> biomeIds) {
    }
}
