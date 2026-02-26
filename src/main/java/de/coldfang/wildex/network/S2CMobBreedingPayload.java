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
        List<ResourceLocation> breedingItemIds,
        List<ResourceLocation> tamingItemIds
) implements CustomPacketPayload {

    public static final Type<S2CMobBreedingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_breeding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobBreedingPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        buf.writeBoolean(p.ownable());
                        writeIds(buf, p.breedingItemIds());
                        writeIds(buf, p.tamingItemIds());
                    },
                    buf -> {
                        ResourceLocation mobId = buf.readResourceLocation();
                        boolean ownable = buf.readBoolean();
                        List<ResourceLocation> breeding = readIds(buf);
                        List<ResourceLocation> taming = readIds(buf);
                        return new S2CMobBreedingPayload(mobId, ownable, breeding, taming);
                    }
            );

    private static void writeIds(RegistryFriendlyByteBuf buf, List<ResourceLocation> ids) {
        List<ResourceLocation> safe = ids == null ? List.of() : ids;
        buf.writeVarInt(safe.size());
        for (ResourceLocation id : safe) {
            buf.writeResourceLocation(id);
        }
    }

    private static List<ResourceLocation> readIds(RegistryFriendlyByteBuf buf) {
        int n = Math.max(0, buf.readVarInt());
        List<ResourceLocation> out = new ArrayList<>(Math.min(n, 256));
        for (int i = 0; i < n; i++) {
            out.add(buf.readResourceLocation());
        }
        return List.copyOf(out);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
