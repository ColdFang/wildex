package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CMobKillsPayload(ResourceLocation mobId, int kills) implements CustomPacketPayload {

    public static final Type<S2CMobKillsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_kills"));


    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobKillsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        buf.writeVarInt(p.kills());
                    },
                    buf -> new S2CMobKillsPayload(buf.readResourceLocation(), buf.readVarInt())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
