package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.registry.NichirinMoveRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import com.xirc.nichirin.common.data.ProgressionHelper;

import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Breathing commands for managing player breathing styles
 * Usage: /breathing give <player> <style> [set] - Give a breathing style with optional set flag
 *        /breathing set <player> <style> - Set active style (only if unlocked)
 *        /breathing cooldown <player> - Reset all breathing move cooldowns
 */
public class BreathingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("breathing")
                .requires(source -> source.hasPermission(2)) // Requires op level 2

                // /breathing give <player> <style> [set] - Give style with optional set flag (default true)
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests(BreathingCommand::suggestStyles)
                                        .executes(context -> giveBreathingStyle(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "style"),
                                                true // Default: set active
                                        ))
                                        .then(Commands.argument("set", BoolArgumentType.bool())
                                                .executes(context -> giveBreathingStyle(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "style"),
                                                        BoolArgumentType.getBool(context, "set")
                                                ))
                                        )
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

                // /breathing cooldown <player> - Reset all cooldowns (like CooldownClearEventHandler)
                .then(Commands.literal("cooldown")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> resetAllCooldowns(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )
        );
    }

    /**
     * Gives (unlocks and optionally sets) a breathing style to a player
     */
    private static int giveBreathingStyle(CommandContext<CommandSourceStack> context, ServerPlayer player, String style, boolean setActive) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();
        final String formattedStyleName = formatStyleName(style);

        // Check if the style exists
        if (!MovesetRegistry.isRegistered(style)) {
            source.sendFailure(Component.literal("Unknown breathing style: " + style)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if player already has this style unlocked
        if (ProgressionHelper.isStyleUnlocked(player, style)) {
            source.sendFailure(Component.literal(playerName + " already has " + formattedStyleName + " unlocked")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // If setActive is true, also check if it's currently active (additional check)
        if (setActive) {
            String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();
            if (style.equals(currentStyle)) {
                source.sendFailure(Component.literal(playerName + " already has " + formattedStyleName + " active")
                        .withStyle(s -> s.withColor(0xFFAA00)));
                return 0;
            }
        }

        // Unlock the style (this will trigger advancement if applicable)
        ProgressionHelper.unlockStyle(player, style);

        // Set it as active if requested
        if (setActive) {
            PlayerDataProvider.updateAndSync(player, style);

            // Send success message for give + set
            source.sendSuccess(() -> Component.literal("Gave and activated " + formattedStyleName + " for " + playerName)
                    .withStyle(s -> s.withColor(0x55FF55)), true);

            // Notify the player
            player.displayClientMessage(
                    Component.literal("You have been granted " + formattedStyleName + "!")
                            .withStyle(s -> s.withColor(0x55FFFF)),
                    false
            );
        } else {
            // Send success message for unlock only
            source.sendSuccess(() -> Component.literal("Unlocked " + formattedStyleName + " for " + playerName + " (not set as active)")
                    .withStyle(s -> s.withColor(0x55FF55)), true);

            // Notify the player
            player.displayClientMessage(
                    Component.literal("You have unlocked " + formattedStyleName + "! Use the GUI to set it active.")
                            .withStyle(s -> s.withColor(0x55FFFF)),
                    false
            );
        }

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
     * Resets all breathing move cooldowns for a player
     * Clears ALL possible move cooldowns from all movesets
     */
    private static int resetAllCooldowns(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();

        // Get the player's current breathing style
        String currentStyle = PlayerDataProvider.getData(player).getBreathingStyleData().getMovesetId();

        if (currentStyle == null) {
            source.sendFailure(Component.literal(playerName + " has no active breathing style")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Command: Starting cooldown reset for player: {}", playerName);

        // Clear cooldowns for ALL moves from ALL movesets
        int clearedCount = 0;
        for (NichirinMoveRegistry.MoveInfo moveInfo : NichirinMoveRegistry.getAllMoves().values()) {
            String moveName = moveInfo.displayName;
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Command: Sending clear packet for move: {}", moveName);
            CooldownDisplayPacket.sendToClient(player, moveName, 0);
            clearedCount++;
        }

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Command: Sent {} clear packets for player: {}", clearedCount, playerName);

        int finalClearedCount = clearedCount;
        source.sendSuccess(() -> Component.literal("Reset all cooldowns for " + playerName + " (" + finalClearedCount + " moves cleared)")
                .withStyle(s -> s.withColor(0x55FF55)), true);

        player.displayClientMessage(
                Component.literal("All your cooldowns have been reset!")
                        .withStyle(s -> s.withColor(0x55FFFF)),
                false
        );

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