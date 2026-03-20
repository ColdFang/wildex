package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SResetLegacyDiscoveryPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<C2SResetLegacyDiscoveryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "reset_legacy_discovery"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SResetLegacyDiscoveryPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeResourceLocation(payload.mobId()),
                    buf -> new C2SResetLegacyDiscoveryPayload(buf.readResourceLocation())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
