package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CServerConfigPayload(
        boolean hiddenMode,
        boolean requireBookForKeybind,
        boolean debugMode,
        boolean shareOffersEnabled,
        boolean shareOffersPaymentEnabled,
        String shareOfferCurrencyItem,
        int shareOfferMaxPrice
) implements CustomPacketPayload {

    public static final Type<S2CServerConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "server_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CServerConfigPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBoolean(p.hiddenMode());
                        buf.writeBoolean(p.requireBookForKeybind());
                        buf.writeBoolean(p.debugMode());
                        buf.writeBoolean(p.shareOffersEnabled());
                        buf.writeBoolean(p.shareOffersPaymentEnabled());
                        buf.writeUtf(p.shareOfferCurrencyItem() == null ? "minecraft:emerald" : p.shareOfferCurrencyItem(), 128);
                        buf.writeVarInt(Math.max(0, p.shareOfferMaxPrice()));
                    },
                    buf -> new S2CServerConfigPayload(
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readUtf(128),
                            Math.max(0, buf.readVarInt())
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
