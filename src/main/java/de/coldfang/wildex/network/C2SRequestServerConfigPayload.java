package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestServerConfigPayload() implements CustomPacketPayload {

    public static final Type<C2SRequestServerConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_server_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestServerConfigPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
            }, buf -> new C2SRequestServerConfigPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
