package de.coldfang.wildex.network;

import de.coldfang.wildex.client.data.model.WildexDiscoveryDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CMobDiscoveryDetailsPayload(
        ResourceLocation mobId,
        boolean hasData,
        boolean legacyMissingData,
        String sourceId,
        String sourceDetail,
        ResourceLocation dimensionId,
        int x,
        int y,
        int z,
        long discoveredAtEpochMillis
) implements CustomPacketPayload {

    public static final Type<S2CMobDiscoveryDetailsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_discovery_details"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobDiscoveryDetailsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeResourceLocation(payload.mobId());
                        buf.writeBoolean(payload.hasData());
                        buf.writeBoolean(payload.legacyMissingData());
                        buf.writeUtf(payload.sourceId() == null ? "" : payload.sourceId(), 64);
                        buf.writeUtf(payload.sourceDetail() == null ? "" : payload.sourceDetail(), 256);
                        buf.writeResourceLocation(payload.dimensionId() == null
                                ? ResourceLocation.withDefaultNamespace("overworld")
                                : payload.dimensionId());
                        buf.writeVarInt(payload.x());
                        buf.writeVarInt(payload.y());
                        buf.writeVarInt(payload.z());
                        buf.writeLong(payload.discoveredAtEpochMillis());
                    },
                    buf -> new S2CMobDiscoveryDetailsPayload(
                            buf.readResourceLocation(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readUtf(64),
                            buf.readUtf(256),
                            buf.readResourceLocation(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readLong()
                    )
            );

    public WildexDiscoveryDetails toClientModel() {
        return new WildexDiscoveryDetails(
                hasData,
                legacyMissingData,
                sourceId == null ? "" : sourceId,
                sourceDetail == null ? "" : sourceDetail,
                dimensionId == null ? ResourceLocation.withDefaultNamespace("overworld") : dimensionId,
                x,
                y,
                z,
                discoveredAtEpochMillis
        );
    }

    public static S2CMobDiscoveryDetailsPayload empty(ResourceLocation mobId, boolean legacyMissingData) {
        ResourceLocation safeMobId = mobId == null ? ResourceLocation.withDefaultNamespace("pig") : mobId;
        return new S2CMobDiscoveryDetailsPayload(
                safeMobId,
                false,
                legacyMissingData,
                "",
                "",
                ResourceLocation.withDefaultNamespace("overworld"),
                0,
                0,
                0,
                0L
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
