package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.common.config.NichirinConfig;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.event.BreathOfNichirinEventHandler;
import com.xirc.nichirin.common.system.BloodMoonManager;
import com.xirc.nichirin.common.system.perks.NichirinPerkRegistry;
import com.xirc.nichirin.common.system.perks.PerkDefinition;
import com.xirc.nichirin.common.system.perks.PerkManager;
import com.xirc.nichirin.common.network.s2c.ProgressionSyncPacket;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * /nichirin — top-level hub command for Breath of Nichirin.
 *
 * <pre>
 *   /nichirin help
 *   /nichirin version
 *   /nichirin config list
 *   /nichirin config get  <key>
 *   /nichirin config set  <key> <value>   (op 2)
 *   /nichirin config reset <key>          (op 2)
 *   /nichirin config resetall             (op 2)
 * </pre>
 */
public class NichirinCommand {

    // Color palette
    private static final int COL_HEADER  = 0xE8C87A; // warm gold
    private static final int COL_KEY     = 0x7EC8E3; // light blue
    private static final int COL_VALUE   = 0xFFFFFF; // white
    private static final int COL_DIM     = 0x888888; // grey
    private static final int COL_OK      = 0x55FF55; // green
    private static final int COL_WARN    = 0xFFAA00; // orange
    private static final int COL_ERR     = 0xFF5555; // red

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nichirin")

                // /nichirin  (bare — same as help)
                .executes(ctx -> showHelp(ctx))

                // /nichirin help
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx)))

                // /nichirin bloodmoon  (op 2 — toggle blood moon on/off)
                .then(Commands.literal("bloodmoon")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> toggleBloodMoon(ctx)))

                // /nichirin unlockall <player>  (op 2)
                .then(Commands.literal("unlockall")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> unlockAll(ctx, EntityArgument.getPlayer(ctx, "player")))))

                // /nichirin config ...
                .then(Commands.literal("config")
                        .requires(src -> src.hasPermission(2))

                        // /nichirin config  (bare — opens the GUI, OP only)
                        .executes(ctx -> openConfigGui(ctx))

                        // /nichirin config list
                        .then(Commands.literal("list")
                                .executes(ctx -> configList(ctx)))

                        // /nichirin config get <key>
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .executes(ctx -> configGet(ctx,
                                                StringArgumentType.getString(ctx, "key")))))

                        // /nichirin config set <key> <value>  (op 2)
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(NichirinCommand::suggestConfigValue)
                                                .executes(ctx -> configSet(ctx,
                                                        StringArgumentType.getString(ctx, "key"),
                                                        StringArgumentType.getString(ctx, "value"))))))

                        // /nichirin config reset <key>  (op 2)
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .executes(ctx -> configReset(ctx,
                                                StringArgumentType.getString(ctx, "key")))))

                        // /nichirin config resetall  (op 2)
                        .then(Commands.literal("resetall")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> configResetAll(ctx)))
                )
        );
    }

    // Subcommand handlers

    private static int unlockAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CommandSourceStack src = ctx.getSource();
        String name = player.getName().getString();

        int stylesUnlocked = 0;
        String firstStyle = null;
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (!ProgressionHelper.isMovesetUnlocked(player, id)) {
                ProgressionHelper.unlockMoveset(player, id);
                if (firstStyle == null && id.contains("breathing")) firstStyle = id;
                stylesUnlocked++;
            } else if (firstStyle == null && id.contains("breathing")) {
                firstStyle = id;
            }
        }
        // Set an active style if the player has none
        String currentStyle = PlayerDataProvider.getData(player).getMovesetData().getMovesetId();
        if ((currentStyle == null || currentStyle.isEmpty()) && firstStyle != null) {
            PlayerDataProvider.updateAndSync(player, firstStyle);
        }

        int perksDiscovered = 0;
        for (PerkDefinition def : NichirinPerkRegistry.allPerks()) {
            if (PerkManager.discover(player, def.id)) perksDiscovered++;
        }

        int maxSlots = com.xirc.nichirin.common.config.NichirinModConfig.get().perks.maxEquippedPerks;
        PlayerDataProvider.getData(player).getPerkData().setPerkSlots(maxSlots);

        BreathOfNichirinEventHandler.syncPerksToPlayer(player);
        ProgressionSyncPacket.sendToPlayer(player);

        int finalStyles = stylesUnlocked;
        int finalPerks = perksDiscovered;
        int finalSlots = maxSlots;
        src.sendSuccess(() -> Component.literal("Unlocked everything for " + name + ": " +
                finalStyles + " style(s), " + finalPerks + " perk(s), " + finalSlots + " perk slots.")
                .withStyle(s -> s.withColor(COL_OK)), true);

        player.displayClientMessage(
                Component.literal("All breathing styles and perks unlocked! Perk slots: " + finalSlots + ".")
                        .withStyle(s -> s.withColor(0x55FFFF)), false);

        return 1;
    }

    private static int toggleBloodMoon(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var server = src.getServer();
        if (server == null) {
            src.sendFailure(Component.literal("Must be run on a server."));
            return 0;
        }
        if (BloodMoonManager.isActive()) {
            BloodMoonManager.endBloodMoon(server);
            src.sendSuccess(() -> Component.literal("Blood Moon ended.").withStyle(s -> s.withColor(0x7EC8E3)), true);
        } else {
            BloodMoonManager.startBloodMoon(server);
            src.sendSuccess(() -> Component.literal("Blood Moon started.").withStyle(s -> s.withColor(0xAA0000)), true);
        }
        return 1;
    }

    private static int openConfigGui(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayer player) {
            NichirinPacketRegistry.sendOpenConfigScreen(player);
        } else {
            // Console / command block — fall back to listing values in chat
            configList(ctx);
        }
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        src.sendSuccess(() -> header("— Breath of Nichirin Help —"), false);
        src.sendSuccess(() -> line(COL_DIM,  "Operator commands (permission level 2):"), false);
        src.sendSuccess(() -> cmd("/nichirin unlockall <player>",                  "Unlock all styles, perks, and perk slots"), false);
        src.sendSuccess(() -> cmd("/nichirin config",                              "Open config GUI"), false);
        src.sendSuccess(() -> cmd("/nichirin config list",                         "List all config values"), false);
        src.sendSuccess(() -> cmd("/nichirin config get <key>",                    "Show a single config value"), false);
        src.sendSuccess(() -> cmd("/nichirin config set <key> <value>",            "Change a config value"), false);
        src.sendSuccess(() -> cmd("/nichirin config reset <key>",                  "Reset one value to default"), false);
        src.sendSuccess(() -> cmd("/nichirin config resetall",                     "Reset all values to defaults"), false);
        src.sendSuccess(() -> line(COL_DIM,  "Other command roots:"), false);
        src.sendSuccess(() -> cmd("/breathing ...",  "Give / set breathing styles"), false);
        src.sendSuccess(() -> cmd("/demon ...",      "Give / manage demon arts"), false);
        return 1;
    }

    private static int configList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> header("— Config Values —"), false);

        for (Map.Entry<String, NichirinConfig.Entry> e : NichirinConfig.getAll().entrySet()) {
            String key   = e.getKey();
            NichirinConfig.Entry entry = e.getValue();

            String rangePart = entry.isBoolean()
                    ? "default " + entry.displayDefault()
                    : "default " + entry.displayDefault() + ", range " + entry.min() + "–" + entry.max();
            MutableComponent line = Component.literal("  ")
                    .append(Component.literal(key).withStyle(s -> s.withColor(COL_KEY)))
                    .append(Component.literal(" = ").withStyle(s -> s.withColor(COL_DIM)))
                    .append(Component.literal(entry.displayValue()).withStyle(s -> s.withColor(COL_VALUE)))
                    .append(Component.literal("  (" + rangePart + ")")
                            .withStyle(s -> s.withColor(COL_DIM)));

            src.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int configGet(CommandContext<CommandSourceStack> ctx, String key) {
        if (!NichirinConfig.hasKey(key)) {
            ctx.getSource().sendFailure(unknown(key));
            return 0;
        }
        NichirinConfig.Entry entry = NichirinConfig.getAll().get(key);
        ctx.getSource().sendSuccess(() ->
                Component.literal(key).withStyle(s -> s.withColor(COL_KEY))
                        .append(Component.literal(" = ").withStyle(s -> s.withColor(COL_DIM)))
                        .append(Component.literal(entry.displayValue()).withStyle(s -> s.withColor(COL_VALUE)))
                        .append(Component.literal("  — " + entry.description()).withStyle(s -> s.withColor(COL_DIM))),
                false);
        return 1;
    }

    private static int configSet(CommandContext<CommandSourceStack> ctx, String key, String rawValue) {
        if (!NichirinConfig.hasKey(key)) {
            ctx.getSource().sendFailure(unknown(key));
            return 0;
        }
        NichirinConfig.Entry before = NichirinConfig.getAll().get(key);
        boolean ok = NichirinConfig.setString(key, rawValue);
        if (!ok) {
            String rangeHint = before.isBoolean()
                    ? "expected true or false"
                    : "valid range: " + before.min() + "–" + before.max();
            ctx.getSource().sendFailure(
                    Component.literal("Invalid value \"" + rawValue + "\" for " + key + " (" + rangeHint + ")")
                            .withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        NichirinConfig.Entry after = NichirinConfig.getAll().get(key);
        ctx.getSource().sendSuccess(() ->
                Component.literal("Set ").withStyle(s -> s.withColor(COL_OK))
                        .append(Component.literal(key).withStyle(s -> s.withColor(COL_KEY)))
                        .append(Component.literal(" → " + after.displayValue()).withStyle(s -> s.withColor(COL_VALUE))),
                true);
        return 1;
    }

    private static int configReset(CommandContext<CommandSourceStack> ctx, String key) {
        if (!NichirinConfig.hasKey(key)) {
            ctx.getSource().sendFailure(unknown(key));
            return 0;
        }
        NichirinConfig.reset(key);
        NichirinConfig.Entry entry = NichirinConfig.getAll().get(key);
        ctx.getSource().sendSuccess(() ->
                Component.literal("Reset ").withStyle(s -> s.withColor(COL_OK))
                        .append(Component.literal(key).withStyle(s -> s.withColor(COL_KEY)))
                        .append(Component.literal(" to default (" + entry.value() + ")")
                                .withStyle(s -> s.withColor(COL_DIM))),
                true);
        return 1;
    }

    private static int configResetAll(CommandContext<CommandSourceStack> ctx) {
        NichirinConfig.resetAll();
        ctx.getSource().sendSuccess(() ->
                Component.literal("All config values reset to defaults.")
                        .withStyle(s -> s.withColor(COL_OK)),
                true);
        return 1;
    }

    // Suggestions

    private static CompletableFuture<Suggestions> suggestConfigKeys(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        for (String key : NichirinConfig.getAll().keySet()) {
            if (key.startsWith(input)) builder.suggest(key);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestConfigValue(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        try {
            String key = StringArgumentType.getString(ctx, "key");
            NichirinConfig.Entry entry = NichirinConfig.getAll().get(key);
            if (entry != null && entry.isBoolean()) {
                builder.suggest("true");
                builder.suggest("false");
            }
        } catch (Exception ignored) {}
        return builder.buildFuture();
    }

    // Formatting helpers

    private static MutableComponent header(String text) {
        return Component.literal(text).withStyle(s -> s.withColor(COL_HEADER).withBold(true));
    }

    private static MutableComponent line(int color, String text) {
        return Component.literal(text).withStyle(s -> s.withColor(color));
    }

    private static MutableComponent cmd(String command, String description) {
        return Component.literal("  ")
                .append(Component.literal(command).withStyle(s -> s.withColor(COL_KEY)))
                .append(Component.literal("  — " + description).withStyle(s -> s.withColor(COL_DIM)));
    }

    private static MutableComponent unknown(String key) {
        return Component.literal("Unknown config key: " + key + ". Use /nichirin config list to see all keys.")
                .withStyle(s -> s.withColor(COL_ERR));
    }
}
