package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CSpyglassDiscoveryEffectPayload(
        int entityId,
        ResourceLocation mobId
) implements CustomPacketPayload {

    public static final Type<S2CSpyglassDiscoveryEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "spyglass_discovery_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSpyglassDiscoveryEffectPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeVarInt(p.entityId());
                        buf.writeResourceLocation(p.mobId());
                    },
                    buf -> new S2CSpyglassDiscoveryEffectPayload(
                            buf.readVarInt(),
                            buf.readResourceLocation()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
