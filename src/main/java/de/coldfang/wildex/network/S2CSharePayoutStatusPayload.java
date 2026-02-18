package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CSharePayoutStatusPayload(int pendingTotal) implements CustomPacketPayload {

    public static final Type<S2CSharePayoutStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "share_payout_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSharePayoutStatusPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeVarInt(Math.max(0, p.pendingTotal())),
                    buf -> new S2CSharePayoutStatusPayload(Math.max(0, buf.readVarInt()))
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
