package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SSpyglassPulsePayload() implements CustomPacketPayload {

    public static final Type<C2SSpyglassPulsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "spyglass_pulse"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSpyglassPulsePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {},
                    buf -> new C2SSpyglassPulsePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
