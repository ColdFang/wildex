package de.coldfang.wildex.server;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.coldfang.wildex.util.WildexMobFilters;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WildexShareOfferCommands {

    private WildexShareOfferCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("wildex");

        root.then(Commands.literal("discover")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("mob_id", ResourceLocationArgument.id())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            ResourceLocation mobId = ResourceLocationArgument.getId(ctx, "mob_id");
                                            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown mob id: " + mobId));
                                                return 0;
                                            }
                                            if (!WildexMobFilters.isTrackable(mobId)) {
                                                ctx.getSource().sendFailure(Component.literal("Mob id is excluded or not trackable: " + mobId));
                                                return 0;
                                            }

                                            boolean changed = WildexDiscoveryService.discover(
                                                    target,
                                                    mobId,
                                                    WildexDiscoveryService.DiscoverySource.DEBUG
                                            );
                                            if (!changed) {
                                                ctx.getSource().sendFailure(Component.literal("No change: " + target.getGameProfile().getName() + " already discovered " + mobId));
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Added Wildex discovery for " + target.getGameProfile().getName() + ": " + mobId),
                                                    false
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("mob_id", ResourceLocationArgument.id())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            ResourceLocation mobId = ResourceLocationArgument.getId(ctx, "mob_id");
                                            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown mob id: " + mobId));
                                                return 0;
                                            }
                                            if (!WildexMobFilters.isTrackable(mobId)) {
                                                ctx.getSource().sendFailure(Component.literal("Mob id is excluded or not trackable: " + mobId));
                                                return 0;
                                            }

                                            boolean changed = WildexDiscoveryService.undiscover(target, mobId);
                                            if (!changed) {
                                                ctx.getSource().sendFailure(Component.literal("No change: " + target.getGameProfile().getName() + " has not discovered " + mobId));
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Removed Wildex discovery for " + target.getGameProfile().getName() + ": " + mobId),
                                                    false
                                            );
                                            return 1;
                                        })))));

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
