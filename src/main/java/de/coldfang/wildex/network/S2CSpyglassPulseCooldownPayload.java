package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CSpyglassPulseCooldownPayload(int remainingTicks) implements CustomPacketPayload {

    public static final Type<S2CSpyglassPulseCooldownPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "spyglass_pulse_cd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSpyglassPulseCooldownPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeInt(p.remainingTicks()),
                    buf -> new S2CSpyglassPulseCooldownPayload(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
