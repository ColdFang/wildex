package de.coldfang.wildex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CPlayerUiStatePayload(
        String tabId,
        String mobId,
        boolean discoveredOnly,
        boolean friendlyEnabled,
        boolean neutralEnabled,
        boolean hostileEnabled,
        boolean tameableEnabled,
        boolean favoritesEnabled
) implements CustomPacketPayload {

    public static final Type<S2CPlayerUiStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WildexNetwork.MOD_ID, "player_ui_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerUiStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUtf(p.tabId() == null ? "" : p.tabId(), 32);
                        buf.writeUtf(p.mobId() == null ? "" : p.mobId(), 256);
                        buf.writeBoolean(p.discoveredOnly());
                        buf.writeBoolean(p.friendlyEnabled());
                        buf.writeBoolean(p.neutralEnabled());
                        buf.writeBoolean(p.hostileEnabled());
                        buf.writeBoolean(p.tameableEnabled());
                        buf.writeBoolean(p.favoritesEnabled());
                    },
                    buf -> new S2CPlayerUiStatePayload(
                            buf.readUtf(32),
                            buf.readUtf(256),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean()
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
