package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRequestDiscoveredMobsPayload() implements CustomPacketPayload {

    public static final Type<C2SRequestDiscoveredMobsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_discovered_mobs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestDiscoveredMobsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                    },
                    buf -> new C2SRequestDiscoveredMobsPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
