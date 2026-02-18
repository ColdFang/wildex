package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record S2CShareCandidatesPayload(List<Candidate> candidates, boolean selfAcceptingOffers) implements CustomPacketPayload {

    public static final Type<S2CShareCandidatesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "share_candidates"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CShareCandidatesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        List<Candidate> cs = p.candidates() == null ? List.of() : p.candidates();
                        buf.writeVarInt(cs.size());
                        for (Candidate c : cs) {
                            buf.writeUUID(c.playerId());
                            buf.writeUtf(c.playerName(), 32);
                        }
                        buf.writeBoolean(p.selfAcceptingOffers());
                    },
                    buf -> {
                        int n = Math.max(0, buf.readVarInt());
                        List<Candidate> cs = new ArrayList<>(Math.min(n, 256));
                        for (int i = 0; i < n; i++) {
                            cs.add(new Candidate(buf.readUUID(), buf.readUtf(32)));
                        }
                        boolean selfAccepting = buf.readBoolean();
                        return new S2CShareCandidatesPayload(List.copyOf(cs), selfAccepting);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Candidate(UUID playerId, String playerName) {
    }
}
