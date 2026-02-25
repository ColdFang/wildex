package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public record S2CViewedMobEntriesPayload(Set<ResourceLocation> mobIds) implements CustomPacketPayload {

    public static final Type<S2CViewedMobEntriesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "viewed_mob_entries"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CViewedMobEntriesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        Set<ResourceLocation> ids = p.mobIds() == null ? Set.of() : p.mobIds();
                        buf.writeVarInt(ids.size());
                        for (ResourceLocation rl : ids) {
                            buf.writeResourceLocation(rl);
                        }
                    },
                    buf -> {
                        int n = Math.max(0, buf.readVarInt());
                        Set<ResourceLocation> ids = new HashSet<>(Math.min(n, 2048));
                        for (int i = 0; i < n; i++) {
                            ids.add(buf.readResourceLocation());
                        }
                        return new S2CViewedMobEntriesPayload(Set.copyOf(ids));
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
