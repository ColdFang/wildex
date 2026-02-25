package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestViewedMobEntriesPayload() implements CustomPacketPayload {

    public static final Type<C2SRequestViewedMobEntriesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_viewed_mob_entries"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestViewedMobEntriesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                    },
                    buf -> new C2SRequestViewedMobEntriesPayload()
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
