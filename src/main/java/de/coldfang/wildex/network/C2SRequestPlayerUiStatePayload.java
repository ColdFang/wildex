package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestPlayerUiStatePayload() implements CustomPacketPayload {

    public static final Type<C2SRequestPlayerUiStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_player_ui_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestPlayerUiStatePayload> STREAM_CODEC =
            StreamCodec.unit(new C2SRequestPlayerUiStatePayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
