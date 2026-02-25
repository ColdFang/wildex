package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CMobEntryViewedPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<S2CMobEntryViewedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_entry_viewed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobEntryViewedPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeResourceLocation(p.mobId()),
                    buf -> new S2CMobEntryViewedPayload(buf.readResourceLocation())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
