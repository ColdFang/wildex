package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SRequestMobBreedingPayload(ResourceLocation mobId) implements CustomPacketPayload {

    public static final Type<C2SRequestMobBreedingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "request_mob_breeding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestMobBreedingPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeResourceLocation(p.mobId()),
                    buf -> new C2SRequestMobBreedingPayload(buf.readResourceLocation())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
