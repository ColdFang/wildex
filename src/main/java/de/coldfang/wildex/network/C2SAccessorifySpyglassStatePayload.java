package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SAccessorifySpyglassStatePayload(boolean active) implements CustomPacketPayload {

    public static final Type<C2SAccessorifySpyglassStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "accessorify_spyglass_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAccessorifySpyglassStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    C2SAccessorifySpyglassStatePayload::active,
                    C2SAccessorifySpyglassStatePayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
