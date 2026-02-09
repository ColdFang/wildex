package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record S2CMobSpawnsPayload(ResourceLocation mobId, List<DimSection> sections) implements CustomPacketPayload {

    public static final Type<S2CMobSpawnsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_spawns"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobSpawnsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        List<DimSection> ss = p.sections() == null ? List.of() : p.sections();
                        buf.writeVarInt(ss.size());
                        for (DimSection s : ss) {
                            buf.writeResourceLocation(s.dimensionId());
                            List<ResourceLocation> biomes = s.biomeIds() == null ? List.of() : s.biomeIds();
                            buf.writeVarInt(biomes.size());
                            for (ResourceLocation b : biomes) buf.writeResourceLocation(b);
                        }
                    },
                    buf -> {
                        ResourceLocation mobId = buf.readResourceLocation();
                        int sn = Math.max(0, buf.readVarInt());
                        List<DimSection> ss = new ArrayList<>(Math.min(sn, 64));
                        for (int i = 0; i < sn; i++) {
                            ResourceLocation dim = buf.readResourceLocation();
                            int bn = Math.max(0, buf.readVarInt());
                            List<ResourceLocation> biomes = new ArrayList<>(Math.min(bn, 4096));
                            for (int j = 0; j < bn; j++) biomes.add(buf.readResourceLocation());
                            ss.add(new DimSection(dim, List.copyOf(biomes)));
                        }
                        return new S2CMobSpawnsPayload(mobId, List.copyOf(ss));
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record DimSection(ResourceLocation dimensionId, List<ResourceLocation> biomeIds) {
    }
}
