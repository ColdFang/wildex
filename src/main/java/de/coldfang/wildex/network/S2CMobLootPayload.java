package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public record S2CMobLootPayload(ResourceLocation mobId, List<LootLine> lines) implements CustomPacketPayload {

    public static final int COND_NONE = 0;
    public static final int COND_PLAYER_KILL = 1;
    public static final int COND_ON_FIRE = 1 << 1;
    public static final int COND_SLIME_SIZE = 1 << 2;
    public static final int COND_CAPTAIN = 1 << 3;
    public static final int COND_FROG_KILL = 1 << 4;
    public static final int COND_SHEEP_COLOR = 1 << 5;

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
                            buf.writeVarInt(Math.max(0, l.conditionMask()));
                            List<Integer> profiles = l.conditionProfiles();
                            buf.writeVarInt(Math.max(0, profiles.size()));
                            for (int profile : profiles) {
                                buf.writeVarInt(Math.max(0, profile));
                            }
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
                            int conditionMask = Math.max(0, buf.readVarInt());
                            int profilesCount = Math.max(0, buf.readVarInt());
                            int keptProfiles = Math.min(profilesCount, 16);
                            List<Integer> profiles = new ArrayList<>(keptProfiles);
                            for (int j = 0; j < profilesCount; j++) {
                                int profileMask = Math.max(0, buf.readVarInt());
                                if (j < keptProfiles) {
                                    profiles.add(profileMask);
                                }
                            }
                            out.add(new LootLine(itemId, min, max, conditionMask, profiles));
                        }
                        return new S2CMobLootPayload(mobId, List.copyOf(out));
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record LootLine(
            ResourceLocation itemId,
            int minCount,
            int maxCount,
            int conditionMask,
            List<Integer> conditionProfiles
    ) {
        public LootLine {
            int normalizedMask = Math.max(0, conditionMask);
            List<Integer> normalizedProfiles = normalizeConditionProfiles(conditionProfiles, normalizedMask);
            if (normalizedMask == COND_NONE && !normalizedProfiles.isEmpty()) {
                int unionMask = COND_NONE;
                for (int profileMask : normalizedProfiles) {
                    unionMask |= profileMask;
                }
                normalizedMask = unionMask;
            }

            conditionMask = normalizedMask;
            conditionProfiles = normalizedProfiles;
        }

        public LootLine(ResourceLocation itemId, int minCount, int maxCount, int conditionMask) {
            this(itemId, minCount, maxCount, conditionMask, List.of());
        }
    }

    private static List<Integer> normalizeConditionProfiles(List<Integer> raw, int fallbackMask) {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        if (raw != null) {
            for (int m : raw) {
                if (m > COND_NONE) {
                    unique.add(m);
                }
            }
        }
        if (unique.isEmpty() && fallbackMask > COND_NONE) {
            unique.add(fallbackMask);
        }
        if (unique.isEmpty()) return List.of();

        List<Integer> out = new ArrayList<>(unique);
        out.sort(Comparator
                .comparingInt(Integer::bitCount)
                .thenComparingInt(Integer::intValue));
        return List.copyOf(out);
    }
}
