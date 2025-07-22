package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.common.data.MovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import com.xirc.nichirin.common.data.ProgressionHelper;

import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Command for managing player breathing styles
 * Usage: /breathing add <player> <style>
 *        /breathing get <player>
 */
public class BreathingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("breathing")
                .requires(source -> source.hasPermission(2)) // Requires op level 2

                // /breathing add <player> <style>
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests(BreathingCommand::suggestStyles)
                                        .executes(context -> addBreathingStyle(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "style")
                                        ))
                                )
                        )
                )

                // /breathing get <player>
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> getBreathingStyle(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )
        );
    }

    /**
     * Adds a breathing style to a player
     */
    private static int addBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player, String style) {
        CommandSourceStack source = context.getSource();

        // Check if the style exists
        if (!MovesetRegistry.isRegistered(style)) {
            source.sendFailure(Component.translatable("command.nichirin.breathing.unknown", style)
                    .withStyle(style1 -> style1.withColor(0xFF5555)));
            return 0;
        }

        // First unlock the style (this will trigger advancement if it's thunder_breathing)
        ProgressionHelper.unlockStyle(player, style);

        // Then set it as active
        PlayerDataProvider.updateAndSync(player, style);

        // Send success message
        source.sendSuccess(() -> Component.translatable("command.nichirin.breathing.add.success",
                        player.getName(), Component.translatable("breathing_style." + style))
                .withStyle(style1 -> style1.withColor(0x55FF55)), true);

        // Notify the player
        player.displayClientMessage(
                Component.translatable("command.nichirin.breathing.player.add.success",
                                Component.translatable("breathing_style." + style))
                        .withStyle(style1 -> style1.withColor(0x55FFFF)),
                false
        );

        return 1;
    }

    /**
     * Gets a player's current breathing style
     */
    private static int getBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();

        if (currentStyle != null) {
            source.sendSuccess(() -> Component.translatable("command.nichirin.breathing.get.has",
                            player.getName(), Component.translatable("breathing_style." + currentStyle))
                    .withStyle(style -> style.withColor(0x55FFFF)), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.nichirin.breathing.get.none", player.getName())
                    .withStyle(style -> style.withColor(0xAAAAAA)), false);
        }

        return 1;
    }

    /**
     * Suggests available breathing styles
     */
    private static CompletableFuture<Suggestions> suggestStyles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        // Add all registered breathing styles
        for (String style : MovesetRegistry.getAllMovesetIds()) {
            if (style.toLowerCase().startsWith(input)) {
                builder.suggest(style);
            }
        }

        return builder.buildFuture();
    }
}