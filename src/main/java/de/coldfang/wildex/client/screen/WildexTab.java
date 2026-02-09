package de.coldfang.wildex.client.screen;

import net.minecraft.network.chat.Component;

public enum WildexTab {

    STATS(Component.literal("Stats")),
    LOOT(Component.literal("Loot")),
    SPAWNS(Component.literal("Spawn")),
    MISC(Component.literal("Info"));

    private final Component label;

    WildexTab(Component label) {
        this.label = label;
    }

    public Component label() {
        return label;
    }
}
