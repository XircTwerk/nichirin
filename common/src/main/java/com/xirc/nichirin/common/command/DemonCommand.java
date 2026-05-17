package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.PlayerDataStorage;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.system.DemonManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import com.xirc.nichirin.common.data.ProgressionHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Demon commands for managing player demon arts
 * Usage: /demon become <player> - Make player a demon (gives default_demon)
 *        /demon remove <player> - Remove demon status (clears moveset)
 *        /demon give <player> <art> [set] - Give a demon art with optional set flag
 *        /demon set <player> <art> - Set active art (only if unlocked)
 *        /demon cooldown <player> - Reset all demon art cooldowns
 */
public class DemonCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("demon")
                .requires(source -> source.hasPermission(2)) // Requires op level 2

                // /demon become <player> - Make player a demon
                .then(Commands.literal("become")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> becomeDemon(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )

                // /demon remove <player> - Remove demon status
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> removeDemon(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )

                // /demon give <player> <art> [set] - Give art with optional set flag
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("art", StringArgumentType.string())
                                        .suggests(DemonCommand::suggestDemonArts)
                                        .executes(context -> giveDemonArt(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "art"),
                                                true // Default: set active
                                        ))
                                        .then(Commands.argument("set", BoolArgumentType.bool())
                                                .executes(context -> giveDemonArt(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "art"),
                                                        BoolArgumentType.getBool(context, "set")
                                                ))
                                        )
                                )
                        )
                )

                // /demon set <player> <art> - Set active art (only if unlocked)
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("art", StringArgumentType.string())
                                        .suggests((context, builder) -> suggestUnlockedDemonArts(context, builder, EntityArgument.getPlayer(context, "player")))
                                        .executes(context -> setDemonArt(
                                                context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "art")
                                        ))
                                )
                        )
                )

                // /demon cooldown <player> - Reset all cooldowns
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
     * Makes a player become a demon by giving them the default demon moveset
     */
    private static int becomeDemon(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();

        // Check demon moveset specifically
        String currentDemonMoveset = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (currentDemonMoveset != null) {
            source.sendFailure(Component.literal(playerName + " is already a demon with " + formatArtName(currentDemonMoveset))
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // Unlock default_demon if not already unlocked
        if (!ProgressionHelper.isMovesetUnlocked(player, "default_demon")) {
            ProgressionHelper.unlockMoveset(player, "default_demon");
        }

        // Set demon moveset specifically
        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId("default_demon");

        // Initialize blood points to max
        DemonManager.setBloodPoints(player, 10);

        // Force save and sync to all players
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);

        // Verify the demon status
        boolean actuallyDemon = com.xirc.nichirin.common.data.MovesetHelper.hasDemonMoveset(player);
        String actualDemonId = com.xirc.nichirin.common.data.MovesetHelper.getDemonMovesetId(player);

        if (!actuallyDemon || !actualDemonId.equals("default_demon")) {
            source.sendFailure(Component.literal("Failed to make " + playerName + " a demon (sync issue)")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Made " + playerName + " a demon")
                .withStyle(s -> s.withColor(0x55FF55)), true);

        player.displayClientMessage(
                Component.literal("You have become a demon! You now have demon abilities and passives.")
                        .withStyle(s -> s.withColor(0xFF5555)),
                false
        );

        return 1;
    }

    /**
     * FIXED: Removes demon status from a player and clears ALL demon-related data
     */
    private static int removeDemon(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();

        // Check demon moveset specifically
        String currentDemonMoveset = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (currentDemonMoveset == null) {
            source.sendFailure(Component.literal(playerName + " is not a demon")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // FIXED: Clear demon moveset specifically
        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(null);

        // FIXED: Clean up ALL demon-specific data (blood points, regen tracking, etc.)
        DemonManager.cleanupPlayer(player);

        // FIXED: Reset food data to normal (remove infinite hunger/stamina)
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.getFoodData().setExhaustion(0.0f);

        // FIXED: Extinguish any sun fire
        if (player.isOnFire()) {
            player.clearFire();
        }

        // Force save and sync to all players
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);

        // Clean up any demon-specific server state
        try {
            com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset.cleanupPlayer(player);
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        // Verify demon removal
        boolean stillDemon = com.xirc.nichirin.common.data.MovesetHelper.hasDemonMoveset(player);
        if (stillDemon) {
            source.sendFailure(Component.literal("Failed to remove demon status from " + playerName + " (sync issue)")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Removed demon status from " + playerName)
                .withStyle(s -> s.withColor(0x55FF55)), true);

        player.displayClientMessage(
                Component.literal("You are no longer a demon. Your demon abilities have been removed.")
                        .withStyle(s -> s.withColor(0x55FFFF)),
                false
        );

        return 1;
    }

    /**
     * Gives (unlocks and optionally sets) a demon art to a player
     */
    private static int giveDemonArt(CommandContext<CommandSourceStack> context, ServerPlayer player, String art, boolean setActive) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();
        final String formattedArtName = formatArtName(art);

        if (!isDemonArt(art)) {
            source.sendFailure(Component.literal(art + " is not a demon art")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        if (!NichirinMovesetRegistry.isRegistered(art)) {
            source.sendFailure(Component.literal("Unknown demon art: " + art)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        if (ProgressionHelper.isMovesetUnlocked(player, art)) {
            source.sendFailure(Component.literal(playerName + " already has " + formattedArtName + " unlocked")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // Check demon moveset specifically if setActive is true
        if (setActive) {
            String currentDemonArt = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
            if (art.equals(currentDemonArt)) {
                source.sendFailure(Component.literal(playerName + " already has " + formattedArtName + " active")
                        .withStyle(s -> s.withColor(0xFFAA00)));
                return 0;
            }
        }

        ProgressionHelper.unlockMoveset(player, art);

        if (setActive) {
            // Set demon moveset specifically
            PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(art);
            PlayerDataStorage.savePlayerData(player);
            PlayerDataProvider.forceSync(player.server);

            source.sendSuccess(() -> Component.literal("Gave and activated " + formattedArtName + " for " + playerName)
                    .withStyle(s -> s.withColor(0x55FF55)), true);

            player.displayClientMessage(
                    Component.literal("You have been granted " + formattedArtName + "!")
                            .withStyle(s -> s.withColor(0xFF5555)),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("Unlocked " + formattedArtName + " for " + playerName + " (not set as active)")
                    .withStyle(s -> s.withColor(0x55FF55)), true);

            player.displayClientMessage(
                    Component.literal("You have unlocked " + formattedArtName + "! Use the GUI to set it active.")
                            .withStyle(s -> s.withColor(0xFF5555)),
                    false
            );
        }

        return 1;
    }

    /**
     * Sets a demon art for a player (only if unlocked)
     */
    private static int setDemonArt(CommandContext<CommandSourceStack> context, ServerPlayer player, String art) {
        CommandSourceStack source = context.getSource();

        if (!isDemonArt(art)) {
            source.sendFailure(Component.literal(art + " is not a demon art")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        if (!NichirinMovesetRegistry.isRegistered(art)) {
            source.sendFailure(Component.literal("Unknown demon art: " + art)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        if (!ProgressionHelper.isMovesetUnlocked(player, art)) {
            source.sendFailure(Component.literal(player.getName().getString() + " has not unlocked " + formatArtName(art))
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check demon moveset specifically
        String currentDemonArt = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (art.equals(currentDemonArt)) {
            source.sendFailure(Component.literal(player.getName().getString() + " already has " + formatArtName(art) + " active")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return 0;
        }

        // Set demon moveset specifically
        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(art);
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);

        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s demon art to " + formatArtName(art))
                .withStyle(s -> s.withColor(0x55FF55)), true);

        player.displayClientMessage(
                Component.literal("Your demon art is now " + formatArtName(art))
                        .withStyle(s -> s.withColor(0xFF5555)),
                false
        );

        return 1;
    }

    /**
     * Resets all demon art cooldowns for a player
     */
    private static int resetAllCooldowns(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        final String playerName = player.getName().getString();

        // Get the player's current demon moveset
        String currentDemonMoveset = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();

        if (currentDemonMoveset == null) {
            source.sendFailure(Component.literal(playerName + " has no active demon moveset")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Clear cooldowns for ALL moves from ALL movesets
        int clearedCount = 0;
        for (NichirinMovesetRegistry.MoveInfo moveInfo : NichirinMovesetRegistry.getAllMoves().values()) {
            String moveName = moveInfo.displayName;
            CooldownDisplayPacket.sendToClient(player, moveName, 0);
            clearedCount++;
        }

        int finalClearedCount = clearedCount;
        source.sendSuccess(() -> Component.literal("Reset all cooldowns for " + playerName + " (" + finalClearedCount + " moves cleared)")
                .withStyle(s -> s.withColor(0x55FF55)), true);

        player.displayClientMessage(
                Component.literal("All your cooldowns have been reset!")
                        .withStyle(s -> s.withColor(0xFF5555)),
                false
        );

        return 1;
    }

    /**
     * Suggests only demon arts
     */
    private static CompletableFuture<Suggestions> suggestDemonArts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        for (String movesetId : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isDemonArt(movesetId) && movesetId.toLowerCase().startsWith(input)) {
                builder.suggest(movesetId);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Suggests only unlocked demon arts for a specific player
     */
    private static CompletableFuture<Suggestions> suggestUnlockedDemonArts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, ServerPlayer player) {
        String input = builder.getRemaining().toLowerCase();
        var progression = PlayerDataProvider.getData(player).getProgression();

        for (String movesetId : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isDemonArt(movesetId) &&
                    progression.isMovesetUnlocked(movesetId) &&
                    movesetId.toLowerCase().startsWith(input)) {
                builder.suggest(movesetId);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Check if a moveset ID is a demon art
     */
    private static boolean isDemonArt(String movesetId) {
        if (movesetId == null) return false;
        return movesetId.equals("default_demon") || movesetId.contains("demon");
    }

    /**
     * Formats a demon art ID for display
     */
    private static String formatArtName(String artId) {
        if (artId.equals("default_demon")) {
            return "Demon Arts";
        }

        String[] parts = artId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }
}