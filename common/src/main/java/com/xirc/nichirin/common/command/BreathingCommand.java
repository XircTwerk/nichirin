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
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Command for managing player breathing styles
 * Usage: /breathing set <player> <style>
 *        /breathing remove <player>
 */
public class BreathingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("breathing")
                .requires(source -> source.hasPermission(2)) // Requires op level 2

                // /breathing set <player> <style>
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests(BreathingCommand::suggestStyles)
                                        .executes(context -> setBreathingStyle(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "style")
                                        ))
                                )
                        )
                )

                // /breathing remove <player>
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> removeBreathingStyle(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
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
     * Sets a breathing style for a player
     */
    private static int setBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player, String style) {
        CommandSourceStack source = context.getSource();

        // Check if the style exists
        if (!MovesetRegistry.isRegistered(style)) {
            source.sendFailure(Component.literal("Unknown breathing style: " + style)
                    .withStyle(style1 -> style1.withColor(0xFF5555)));
            return 0;
        }

        // Set the breathing style
        PlayerDataProvider.updateAndSync(player, style);

        // Send success message
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s breathing style to " + formatStyleName(style))
                .withStyle(style1 -> style1.withColor(0x55FF55)), true);

        // Notify the player
        player.displayClientMessage(
                Component.literal("Your breathing style has been set to " + formatStyleName(style))
                        .withStyle(style1 -> style1.withColor(0x55FFFF)),
                false
        );

        return 1;
    }

    /**
     * Removes a player's breathing style
     */
    private static int removeBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        // Remove the breathing style
        PlayerDataProvider.updateAndSync(player, null);

        // Send success message
        source.sendSuccess(() -> Component.literal("Removed " + player.getName().getString() + "'s breathing style")
                .withStyle(style -> style.withColor(0xFFAA00)), true);

        // Notify the player
        player.displayClientMessage(
                Component.literal("Your breathing style has been removed")
                        .withStyle(style -> style.withColor(0xFF5555)),
                false
        );

        return 1;
    }

    /**
     * Gets a player's current breathing style
     */
    private static int getBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        String currentStyle = PlayerDataProvider.getData(player).getMovesetId();

        if (currentStyle != null) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + formatStyleName(currentStyle))
                    .withStyle(style -> style.withColor(0x55FFFF)), false);
        } else {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no breathing style")
                    .withStyle(style -> style.withColor(0xAAAAAA)), false);
        }

        return 1;
    }

    /**
     * Suggests available breathing styles
     */
    private static CompletableFuture<Suggestions> suggestStyles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        for (String style : MovesetRegistry.getAllMovesetIds()) {
            if (style.toLowerCase().startsWith(input)) {
                builder.suggest(style);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Formats a style name for display
     */
    private static String formatStyleName(String style) {
        // Convert snake_case to Title Case
        String[] parts = style.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }
}