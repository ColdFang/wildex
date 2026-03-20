package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestMobDiscoveryDetailsPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<C2SRequestMobDiscoveryDetailsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_mob_discovery_details"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestMobDiscoveryDetailsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeResourceLocation(payload.mobId()),
                    buf -> new C2SRequestMobDiscoveryDetailsPayload(buf.readResourceLocation())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
