package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SSetShareAcceptOffersPayload(boolean accepting) implements CustomPacketPayload {

    public static final Type<C2SSetShareAcceptOffersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "set_share_accept_offers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetShareAcceptOffersPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBoolean(p.accepting()),
                    buf -> new C2SSetShareAcceptOffersPayload(buf.readBoolean())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
