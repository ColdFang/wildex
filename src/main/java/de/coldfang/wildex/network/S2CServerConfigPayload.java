package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record S2CServerConfigPayload(
        boolean hiddenMode,
        boolean requireBookForKeybind,
        boolean debugMode,
        boolean shareOffersEnabled,
        boolean shareOffersPaymentEnabled,
        String shareOfferCurrencyItem,
        int shareOfferMaxPrice,
        List<String> excludedVariantMobIds
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
                        List<String> excluded = p.excludedVariantMobIds() == null ? List.of() : p.excludedVariantMobIds();
                        buf.writeVarInt(excluded.size());
                        for (String entry : excluded) {
                            buf.writeUtf(entry == null ? "" : entry, 256);
                        }
                    },
                    buf -> {
                        boolean hiddenMode = buf.readBoolean();
                        boolean requireBookForKeybind = buf.readBoolean();
                        boolean debugMode = buf.readBoolean();
                        boolean shareOffersEnabled = buf.readBoolean();
                        boolean shareOffersPaymentEnabled = buf.readBoolean();
                        String shareOfferCurrencyItem = buf.readUtf(128);
                        int shareOfferMaxPrice = Math.max(0, buf.readVarInt());
                        int count = Math.max(0, buf.readVarInt());
                        ArrayList<String> excludedVariantMobIds = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            excludedVariantMobIds.add(buf.readUtf(256));
                        }
                        return new S2CServerConfigPayload(
                                hiddenMode,
                                requireBookForKeybind,
                                debugMode,
                                shareOffersEnabled,
                                shareOffersPaymentEnabled,
                                shareOfferCurrencyItem,
                                shareOfferMaxPrice,
                                List.copyOf(excludedVariantMobIds)
                        );
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
