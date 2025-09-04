package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import com.xirc.nichirin.common.data.ProgressionHelper;

import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Improved command for managing player breathing styles with proper validation
 * Usage: /breathing give <player> <style> - Give a breathing style (unlocks and sets it)
 *        /breathing set <player> <style> - Set active style (only if unlocked)
 *        /breathing get <player> - Get current style
 *        /breathing list <player> - List all unlocked styles
 */
public class BreathingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("breathing")
                .requires(source -> source.hasPermission(2)) // Requires op level 2

                // /breathing give <player> <style> - Force unlock and set
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests(BreathingCommand::suggestStyles)
                                        .executes(context -> giveBreathingStyle(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "style")
                                        ))
                                )
                        )
                )

                // /breathing set <player> <style> - Set active style (only if unlocked)
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests((context, builder) -> suggestUnlockedStyles(context, builder, EntityArgument.getPlayer(context, "player")))
                                        .executes(context -> setBreathingStyle(
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

                // /breathing list <player> - List all unlocked styles
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> listUnlockedStyles(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )
        );
    }

    /**
     * Gives (unlocks and sets) a breathing style to a player
     */
    private static int giveBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player, String style) {
        CommandSourceStack source = context.getSource();

        // Check if the style exists
        if (!MovesetRegistry.isRegistered(style)) {
            source.sendFailure(Component.literal("Unknown breathing style: " + style)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if player already has this breathing style active
        String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();
        if (style.equals(currentStyle)) {
            source.sendFailure(Component.literal(player.getName().getString() + " already has " + formatStyleName(style) + " active")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // Unlock the style (this will trigger advancement if applicable)
        ProgressionHelper.unlockStyle(player, style);

        // Set it as active
        PlayerDataProvider.updateAndSync(player, style);

        // Send success message
        source.sendSuccess(() -> Component.literal("Gave " + formatStyleName(style) + " to " + player.getName().getString())
                .withStyle(s -> s.withColor(0x55FF55)), true);

        // Notify the player
        player.displayClientMessage(
                Component.literal("You have been granted " + formatStyleName(style) + "!")
                        .withStyle(s -> s.withColor(0x55FFFF)),
                false
        );

        return 1;
    }

    /**
     * Sets a breathing style for a player (only if unlocked)
     */
    private static int setBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player, String style) {
        CommandSourceStack source = context.getSource();

        // Check if the style exists
        if (!MovesetRegistry.isRegistered(style)) {
            source.sendFailure(Component.literal("Unknown breathing style: " + style)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if player has unlocked this style
        if (!ProgressionHelper.isStyleUnlocked(player, style)) {
            source.sendFailure(Component.literal(player.getName().getString() + " has not unlocked " + formatStyleName(style))
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if already active
        String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();
        if (style.equals(currentStyle)) {
            source.sendFailure(Component.literal(player.getName().getString() + " already has " + formatStyleName(style) + " active")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // Set it as active
        PlayerDataProvider.updateAndSync(player, style);

        // Send success message
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s breathing style to " + formatStyleName(style))
                .withStyle(s -> s.withColor(0x55FF55)), true);

        // Notify the player
        player.displayClientMessage(
                Component.literal("Your breathing style is now " + formatStyleName(style))
                        .withStyle(s -> s.withColor(0x55FFFF)),
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
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + formatStyleName(currentStyle) + " active")
                    .withStyle(s -> s.withColor(0x55FFFF)), false);
        } else {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no breathing style active")
                    .withStyle(s -> s.withColor(0xAAAAAA)), false);
        }

        return 1;
    }

    /**
     * Lists all unlocked breathing styles for a player
     */
    private static int listUnlockedStyles(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        var progression = PlayerDataProvider.getData(player).getProgression();

        if (!progression.hasAnyBreathingStyle()) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has not unlocked any breathing styles")
                    .withStyle(s -> s.withColor(0xAAAAAA)), false);
            return 1;
        }

        StringBuilder styles = new StringBuilder();
        String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();

        for (String styleId : MovesetRegistry.getAllMovesetIds()) {
            if (progression.isStyleUnlocked(styleId)) {
                if (styles.length() > 0) {
                    styles.append(", ");
                }
                String styleName = formatStyleName(styleId);
                if (styleId.equals(currentStyle)) {
                    styleName += " (active)";
                }
                styles.append(styleName);
            }
        }

        source.sendSuccess(() -> Component.literal(player.getName().getString() + "'s unlocked breathing styles: " + styles)
                .withStyle(s -> s.withColor(0x55FFFF)), false);

        return 1;
    }

    /**
     * Suggests all available breathing styles
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
     * Suggests only unlocked breathing styles for a specific player
     */
    private static CompletableFuture<Suggestions> suggestUnlockedStyles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, ServerPlayer player) {
        String input = builder.getRemaining().toLowerCase();
        var progression = PlayerDataProvider.getData(player).getProgression();

        for (String style : MovesetRegistry.getAllMovesetIds()) {
            if (progression.isStyleUnlocked(style) && style.toLowerCase().startsWith(input)) {
                builder.suggest(style);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Formats a breathing style ID for display
     */
    private static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }
}