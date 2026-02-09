package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRequestMobKillsPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<C2SRequestMobKillsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_mob_kills"));


    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestMobKillsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeResourceLocation(p.mobId()),
                    buf -> new C2SRequestMobKillsPayload(buf.readResourceLocation())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
