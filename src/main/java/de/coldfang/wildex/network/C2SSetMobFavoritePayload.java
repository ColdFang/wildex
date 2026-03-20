package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SSetMobFavoritePayload(ResourceLocation mobId, boolean favorite) implements CustomPacketPayload {

    public static final Type<C2SSetMobFavoritePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "set_mob_favorite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetMobFavoritePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        buf.writeBoolean(p.favorite());
                    },
                    buf -> new C2SSetMobFavoritePayload(
                            buf.readResourceLocation(),
                            buf.readBoolean()
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
