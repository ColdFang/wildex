package de.coldfang.wildex.client;

import de.coldfang.wildex.client.screen.WildexScreen;
import de.coldfang.wildex.client.screen.WildexUiSounds;
import net.minecraft.client.Minecraft;

public final class WildexScreenOpener {

    private WildexScreenOpener() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        WildexUiSounds.playScreenOpen();
        mc.setScreen(new WildexScreen());
    }
}
