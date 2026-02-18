package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record C2SSendShareOfferPayload(UUID targetPlayerId, ResourceLocation mobId, int price) implements CustomPacketPayload {

    public static final Type<C2SSendShareOfferPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "send_share_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSendShareOfferPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUUID(p.targetPlayerId());
                        buf.writeResourceLocation(p.mobId());
                        buf.writeVarInt(Math.max(0, p.price()));
                    },
                    buf -> new C2SSendShareOfferPayload(
                            buf.readUUID(),
                            buf.readResourceLocation(),
                            Math.max(0, buf.readVarInt())
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
