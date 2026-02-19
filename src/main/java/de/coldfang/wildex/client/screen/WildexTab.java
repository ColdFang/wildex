package de.coldfang.wildex.client.screen;

import net.minecraft.network.chat.Component;

public enum WildexTab {

    STATS(Component.translatable("gui.wildex.tab.stats")),
    LOOT(Component.translatable("gui.wildex.tab.loot")),
    SPAWNS(Component.translatable("gui.wildex.tab.spawns")),
    MISC(Component.translatable("gui.wildex.tab.info"));

    private final Component label;

    WildexTab(Component label) {
        this.label = label;
    }

    public Component label() {
        return label;
    }
}



