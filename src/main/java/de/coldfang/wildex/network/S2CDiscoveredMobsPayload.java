package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public record S2CDiscoveredMobsPayload(Set<ResourceLocation> mobIds) implements CustomPacketPayload {

    public static final Type<S2CDiscoveredMobsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "discovered_mobs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDiscoveredMobsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        Set<ResourceLocation> ids = p.mobIds() == null ? Set.of() : p.mobIds();
                        buf.writeVarInt(ids.size());
                        for (ResourceLocation rl : ids) {
                            buf.writeResourceLocation(rl);
                        }
                    },
                    buf -> {
                        int n = Math.max(0, buf.readVarInt());
                        Set<ResourceLocation> ids = new HashSet<>(Math.min(n, 2048));
                        for (int i = 0; i < n; i++) {
                            ids.add(buf.readResourceLocation());
                        }
                        return new S2CDiscoveredMobsPayload(Set.copyOf(ids));
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
