package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CWildexCompletePayload() implements CustomPacketPayload {

    public static final Type<S2CWildexCompletePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "wildex_complete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CWildexCompletePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {},
                    buf -> new S2CWildexCompletePayload()
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
