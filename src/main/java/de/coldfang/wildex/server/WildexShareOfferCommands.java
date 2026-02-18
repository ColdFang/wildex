package de.coldfang.wildex.server;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WildexShareOfferCommands {

    private WildexShareOfferCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("wildex");

        root.then(Commands.literal("offer")
                .then(Commands.literal("accept")
                        .then(Commands.argument("id", LongArgumentType.longArg(1L))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return 0;
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    WildexShareOfferService.respondToOffer(sp, id, true);
                                    return 1;
                                })))
                .then(Commands.literal("decline")
                        .then(Commands.argument("id", LongArgumentType.longArg(1L))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return 0;
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    WildexShareOfferService.respondToOffer(sp, id, false);
                                    return 1;
                                }))));

        event.getDispatcher().register(root);
    }
}
