package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CWildexCompleteStatusPayload(boolean complete) implements CustomPacketPayload {

    public static final Type<S2CWildexCompleteStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "wildex_complete_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CWildexCompleteStatusPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBoolean(p.complete),
                    buf -> new S2CWildexCompleteStatusPayload(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
