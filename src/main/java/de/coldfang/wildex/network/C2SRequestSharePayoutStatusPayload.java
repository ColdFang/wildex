package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestSharePayoutStatusPayload() implements CustomPacketPayload {

    public static final Type<C2SRequestSharePayoutStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_share_payout_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestSharePayoutStatusPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
            }, buf -> new C2SRequestSharePayoutStatusPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
