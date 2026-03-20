package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2COpenWildexScreenPayload() implements CustomPacketPayload {

    public static final Type<S2COpenWildexScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "open_wildex_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenWildexScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new S2COpenWildexScreenPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
