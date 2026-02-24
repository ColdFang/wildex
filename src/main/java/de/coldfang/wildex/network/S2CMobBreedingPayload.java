package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record S2CMobBreedingPayload(
        ResourceLocation mobId,
        boolean ownable,
        List<ResourceLocation> breedingItemIds
) implements CustomPacketPayload {

    public static final Type<S2CMobBreedingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_breeding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobBreedingPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        buf.writeBoolean(p.ownable());
                        List<ResourceLocation> ids = p.breedingItemIds() == null ? List.of() : p.breedingItemIds();
                        buf.writeVarInt(ids.size());
                        for (ResourceLocation id : ids) {
                            buf.writeResourceLocation(id);
                        }
                    },
                    buf -> {
                        ResourceLocation mobId = buf.readResourceLocation();
                        boolean ownable = buf.readBoolean();
                        int n = Math.max(0, buf.readVarInt());
                        List<ResourceLocation> out = new ArrayList<>(Math.min(n, 256));
                        for (int i = 0; i < n; i++) {
                            out.add(buf.readResourceLocation());
                        }
                        return new S2CMobBreedingPayload(mobId, ownable, List.copyOf(out));
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
