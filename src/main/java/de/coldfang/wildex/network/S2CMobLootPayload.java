package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record S2CMobLootPayload(ResourceLocation mobId, List<LootLine> lines) implements CustomPacketPayload {

    public static final Type<S2CMobLootPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "mob_loot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMobLootPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeResourceLocation(p.mobId());
                        List<LootLine> ls = p.lines() == null ? List.of() : p.lines();
                        buf.writeVarInt(ls.size());
                        for (LootLine l : ls) {
                            buf.writeResourceLocation(l.itemId());
                            buf.writeVarInt(l.minCount());
                            buf.writeVarInt(l.maxCount());
                        }
                    },
                    buf -> {
                        ResourceLocation mobId = buf.readResourceLocation();
                        int n = Math.max(0, buf.readVarInt());
                        List<LootLine> out = new ArrayList<>(Math.min(n, 256));
                        for (int i = 0; i < n; i++) {
                            ResourceLocation itemId = buf.readResourceLocation();
                            int min = Math.max(0, buf.readVarInt());
                            int max = Math.max(0, buf.readVarInt());
                            out.add(new LootLine(itemId, min, max));
                        }
                        return new S2CMobLootPayload(mobId, List.copyOf(out));
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record LootLine(ResourceLocation itemId, int minCount, int maxCount) {
    }
}
