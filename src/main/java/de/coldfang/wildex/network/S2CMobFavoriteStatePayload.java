package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CMobFavoriteStatePayload(ResourceLocation mobId, boolean favorite) implements CustomPacketPayload {

    public static final Type<S2CMobFavoriteStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_favorite_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobFavoriteStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        buf.writeBoolean(p.favorite());
                    },
                    buf -> new S2CMobFavoriteStatePayload(
                            buf.readResourceLocation(),
                            buf.readBoolean()
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
