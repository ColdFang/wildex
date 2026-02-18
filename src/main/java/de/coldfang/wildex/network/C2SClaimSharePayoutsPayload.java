package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SClaimSharePayoutsPayload() implements CustomPacketPayload {

    public static final Type<C2SClaimSharePayoutsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "claim_share_payouts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SClaimSharePayoutsPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
            }, buf -> new C2SClaimSharePayoutsPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
