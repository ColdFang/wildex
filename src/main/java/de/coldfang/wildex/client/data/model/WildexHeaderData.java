package de.coldfang.wildex.client.data.model;

import net.minecraft.network.chat.Component;

public record WildexHeaderData(
        Component name,
        WildexAggression aggression
) {
    public static WildexHeaderData empty() {
        return new WildexHeaderData(Component.empty(), WildexAggression.FRIENDLY);
    }
}
