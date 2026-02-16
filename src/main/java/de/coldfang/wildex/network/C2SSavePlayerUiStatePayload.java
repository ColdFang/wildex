package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SSavePlayerUiStatePayload(String tabId, String mobId) implements CustomPacketPayload {

    public static final Type<C2SSavePlayerUiStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "save_player_ui_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSavePlayerUiStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUtf(p.tabId() == null ? "" : p.tabId(), 32);
                        buf.writeUtf(p.mobId() == null ? "" : p.mobId(), 256);
                    },
                    buf -> new C2SSavePlayerUiStatePayload(buf.readUtf(32), buf.readUtf(256))
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
