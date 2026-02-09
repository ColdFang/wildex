package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CDiscoveredMobPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<S2CDiscoveredMobPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "discovered_mob"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDiscoveredMobPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeResourceLocation(p.mobId()),
                    buf -> new S2CDiscoveredMobPayload(buf.readResourceLocation())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
