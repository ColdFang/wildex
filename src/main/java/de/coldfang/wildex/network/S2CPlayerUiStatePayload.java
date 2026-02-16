package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CPlayerUiStatePayload(String tabId, String mobId) implements CustomPacketPayload {

    public static final Type<S2CPlayerUiStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "player_ui_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerUiStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUtf(p.tabId() == null ? "" : p.tabId(), 32);
                        buf.writeUtf(p.mobId() == null ? "" : p.mobId(), 256);
                    },
                    buf -> new S2CPlayerUiStatePayload(buf.readUtf(32), buf.readUtf(256))
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
