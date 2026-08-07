package com.xirc.nichirin.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import com.xirc.nichirin.common.attack.moveset.demon.DestructiveDeathMoveset;
import com.xirc.nichirin.common.attack.moves.demon.destructive.CompassNeedleChargeManager;
import com.xirc.nichirin.common.attack.moves.demon.destructive.CompassNeedleTracker;
import com.xirc.nichirin.common.attack.moves.demon.destructive.DestructiveDeathState;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;
import com.xirc.nichirin.common.config.NichirinConfig;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.PlayerDataStorage;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.network.s2c.ProgressionSyncPacket;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.system.BloodMoonManager;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.system.aura.MovesetAuraTicker;
import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.common.vfx.VfxManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * /nichirin — single command hub for Breath of Nichirin.
 *
 * <pre>
 *   /nichirin help
 *   /nichirin bloodmoon                                (op 2)
 *   /nichirin unlockall &lt;player&gt;                       (op 2)
 *   /nichirin cooldown  &lt;player&gt;                       (op 2)  — resets ALL cooldowns
 *
 *   /nichirin breathing give &lt;player&gt; &lt;style&gt; [set]    (op 2)
 *   /nichirin breathing set  &lt;player&gt; &lt;style&gt;          (op 2)
 *
 *   /nichirin demon become &lt;player&gt;                    (op 2)
 *   /nichirin demon remove &lt;player&gt;                    (op 2)
 *   /nichirin demon give   &lt;player&gt; &lt;art&gt; [set]        (op 2)
 *   /nichirin demon set    &lt;player&gt; &lt;art&gt;              (op 2)
 *
 *   /nichirin config ...                               (op 2)
 * </pre>
 */
public class NichirinCommand {

    // Color palette
    private static final int COL_HEADER  = 0xE8C87A;
    private static final int COL_KEY     = 0x7EC8E3;
    private static final int COL_VALUE   = 0xFFFFFF;
    private static final int COL_DIM     = 0x888888;
    private static final int COL_OK      = 0x55FF55;
    private static final int COL_WARN    = 0xFFAA00;
    private static final int COL_ERR     = 0xFF5555;
    private static final Map<String, ResourceLocation> VFX_DEBUG_EFFECTS = Map.ofEntries(
            Map.entry("water_surface_slash", VfxIds.WATER_SURFACE_SLASH),
            Map.entry("water_surface_slash_reverse", VfxIds.WATER_SURFACE_SLASH_REVERSE),
            Map.entry("water_wheel", VfxIds.WATER_WHEEL),
            Map.entry("drop_ripple_thrust", VfxIds.DROP_RIPPLE_THRUST),
            Map.entry("flowing_dance", VfxIds.FLOWING_DANCE),
            Map.entry("striking_tide", VfxIds.STRIKING_TIDE),
            Map.entry("waterfall_basin", VfxIds.WATERFALL_BASIN),
            Map.entry("splashing_water_flow", VfxIds.SPLASHING_WATER_FLOW),
            Map.entry("whirlpool", VfxIds.WHIRLPOOL),
            Map.entry("blessed_rain", VfxIds.BLESSED_RAIN),
            Map.entry("blessed_rain_leap", VfxIds.BLESSED_RAIN_LEAP),
            Map.entry("constant_flux", VfxIds.CONSTANT_FLUX),
            Map.entry("dead_calm", VfxIds.DEAD_CALM),
            Map.entry("unknowing_fire", VfxIds.UNKNOWING_FIRE),
            Map.entry("rising_scorching_sun", VfxIds.RISING_SCORCHING_SUN),
            Map.entry("blazing_universe", VfxIds.BLAZING_UNIVERSE),
            Map.entry("blazing_universe_impact", VfxIds.BLAZING_UNIVERSE_IMPACT),
            Map.entry("blooming_flame_undulation", VfxIds.BLOOMING_FLAME_UNDULATION),
            Map.entry("flame_tiger", VfxIds.FLAME_TIGER),
            Map.entry("rengoku", VfxIds.RENGOKU),
            Map.entry("flame_pommel_slash", VfxIds.FLAME_POMMEL_SLASH),
            Map.entry("thunderclap_flash", VfxIds.THUNDERCLAP_FLASH),
            Map.entry("godspeed", VfxIds.GODSPEED),
            Map.entry("rice_spirit_link", VfxIds.RICE_SPIRIT_LINK),
            Map.entry("rice_spirit_slash", VfxIds.RICE_SPIRIT_SLASH),
            Map.entry("thunder_swarm_slash", VfxIds.THUNDER_SWARM_SLASH),
            Map.entry("distant_thunder_charge", VfxIds.DISTANT_THUNDER_CHARGE),
            Map.entry("heat_lightning_rise", VfxIds.HEAT_LIGHTNING_RISE),
            Map.entry("thunder_strike_warning", VfxIds.THUNDER_STRIKE_WARNING),
            Map.entry("thunder_strike", VfxIds.THUNDER_STRIKE),
            Map.entry("honoikazuchi_no_kami", VfxIds.HONOIKAZUCHI_NO_KAMI),
            Map.entry("honoikazuchi_impact", VfxIds.HONOIKAZUCHI_IMPACT),
            Map.entry("low_clouds_distant_haze", VfxIds.LOW_CLOUDS_DISTANT_HAZE),
            Map.entry("eight_layered_mist", VfxIds.EIGHT_LAYERED_MIST),
            Map.entry("scattering_mist_splash", VfxIds.SCATTERING_MIST_SPLASH),
            Map.entry("shifting_flow_slash", VfxIds.SHIFTING_FLOW_SLASH),
            Map.entry("sea_of_clouds_and_haze", VfxIds.SEA_OF_CLOUDS_AND_HAZE),
            Map.entry("lunar_dispersing_mist", VfxIds.LUNAR_DISPERSING_MIST),
            Map.entry("mist_finisher", VfxIds.MIST_FINISHER),
            Map.entry("obscuring_clouds", VfxIds.OBSCURING_CLOUDS),
            Map.entry("beast_pierce", VfxIds.BEAST_PIERCE),
            Map.entry("beast_x_slice", VfxIds.BEAST_X_SLICE),
            Map.entry("beast_explosive_rush", VfxIds.BEAST_EXPLOSIVE_RUSH),
            Map.entry("beast_devour", VfxIds.BEAST_DEVOUR),
            Map.entry("beast_rapid_slash", VfxIds.BEAST_RAPID_SLASH),
            Map.entry("beast_crazy_cutting", VfxIds.BEAST_CRAZY_CUTTING),
            Map.entry("beast_palisade_bite", VfxIds.BEAST_PALISADE_BITE),
            Map.entry("beast_spatial_awareness", VfxIds.BEAST_SPATIAL_AWARENESS),
            Map.entry("beast_bendy_slash", VfxIds.BEAST_BENDY_SLASH),
            Map.entry("beast_whirling_fangs", VfxIds.BEAST_WHIRLING_FANGS),
            Map.entry("beast_throwing_strike", VfxIds.BEAST_THROWING_STRIKE),
            Map.entry("sound_resonding_slashes", VfxIds.SOUND_RESONDING_SLASHES),
            Map.entry("sound_rhythmic_step", VfxIds.SOUND_RHYTHMIC_STEP),
            Map.entry("sound_roar", VfxIds.SOUND_ROAR),
            Map.entry("sound_string_performance", VfxIds.SOUND_STRING_PERFORMANCE),
            Map.entry("sound_tempo_breaker", VfxIds.SOUND_TEMPO_BREAKER),
            Map.entry("sound_impact", VfxIds.SOUND_IMPACT),
            Map.entry("insect_quick_sting", VfxIds.INSECT_QUICK_STING),
            Map.entry("insect_bee_sting", VfxIds.INSECT_BEE_STING),
            Map.entry("insect_butterfly_dance", VfxIds.INSECT_BUTTERFLY_DANCE),
            Map.entry("insect_butterfly_dash", VfxIds.INSECT_BUTTERFLY_DASH),
            Map.entry("insect_dragonfly", VfxIds.INSECT_DRAGONFLY),
            Map.entry("insect_centipede", VfxIds.INSECT_CENTIPEDE),
            Map.entry("insect_impact", VfxIds.INSECT_IMPACT));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nichirin")

                .executes(NichirinCommand::showHelp)

                .then(Commands.literal("help")
                        .executes(NichirinCommand::showHelp))

                .then(Commands.literal("bloodmoon")
                        .requires(src -> src.hasPermission(2))
                        .executes(NichirinCommand::toggleBloodMoon))

                .then(Commands.literal("unlockall")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> unlockAll(ctx, EntityArgument.getPlayer(ctx, "player")))))

                // Single unified cooldown reset — clears every registered move's cooldown.
                .then(Commands.literal("cooldown")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> resetAllCooldowns(ctx, EntityArgument.getPlayer(ctx, "player")))))

                .then(Commands.literal("debug")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("blurry")
                                .executes(ctx -> giveBlurry(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> giveBlurry(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("forgetpact")
                                .executes(ctx -> clearAkazaPact(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clearAkazaPact(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("vfx")
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            VFX_DEBUG_EFFECTS.keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> playDebugVfx(ctx,
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "effect")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> playDebugVfx(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "effect"))))))
                        .then(AuraCommand.build())
                        .then(OutlineCommand.build()))

                // Breathing and Demon subtrees — same shape as the old /breathing and /demon roots.
                .then(buildBreathingSubcommand())
                .then(buildDemonSubcommand())

                .then(Commands.literal("config")
                        .requires(src -> src.hasPermission(2))
                        .executes(NichirinCommand::openConfigGui)
                        .then(Commands.literal("list")
                                .executes(NichirinCommand::configList))
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .executes(ctx -> configGet(ctx, StringArgumentType.getString(ctx, "key")))))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(NichirinCommand::suggestConfigValue)
                                                .executes(ctx -> configSet(ctx,
                                                        StringArgumentType.getString(ctx, "key"),
                                                        StringArgumentType.getString(ctx, "value"))))))
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NichirinCommand::suggestConfigKeys)
                                        .executes(ctx -> configReset(ctx, StringArgumentType.getString(ctx, "key")))))
                        .then(Commands.literal("resetall")
                                .requires(src -> src.hasPermission(2))
                                .executes(NichirinCommand::configResetAll))
                )
        );

    }

    private static int clearAkazaPact(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        boolean removed = com.xirc.nichirin.common.system.UpperMoonPact.unmark(player, "akaza");
        ctx.getSource().sendSuccess(() -> Component.literal(
                        removed
                                ? "Removed Akaza's pact from " + player.getName().getString() + " — he will hunt you again."
                                : player.getName().getString() + " has no pact with Akaza.")
                .withStyle(s -> s.withColor(removed ? COL_OK : COL_WARN)), true);
        return 1;
    }

    private static int giveBlurry(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        boolean applied = player.addEffect(new MobEffectInstance(NichirinEffectRegistry.blurry(), 160, 0, false, true, true));
        NichirinPacketRegistry.sendToPlayer(
                new TriggerShaderPacket("com.xirc.nichirin.client.shader.MistBlurShaderEffect", true, 1.0f),
                player);
        ctx.getSource().sendSuccess(() -> Component.literal((applied ? "Applied" : "Refreshed")
                        + " blurry debug effect to " + player.getName().getString()
                        + " (effect id: nichirin:blurry)")
                .withStyle(s -> s.withColor(COL_OK)), true);
        return 1;
    }

    private static int playDebugVfx(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String effectName) {
        ResourceLocation effectId = VFX_DEBUG_EFFECTS.get(effectName);
        if (effectId == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown VFX: " + effectName)
                    .withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        var look = player.getLookAngle();
        var direction = new net.minecraft.world.phys.Vec3(look.x, 0.0, look.z);
        if (direction.lengthSqr() < 1.0E-6) direction = new net.minecraft.world.phys.Vec3(0.0, 0.0, 1.0);
        direction = direction.normalize();
        var origin = player.position().add(direction.scale(1.25)).add(0.0, 0.08, 0.0);
        VfxManager.playOwned((net.minecraft.server.level.ServerLevel) player.level(), player,
                effectId, origin, direction, 1.0f);
        ctx.getSource().sendSuccess(() -> Component.literal("Played " + effectName + " VFX for "
                + player.getName().getString()).withStyle(s -> s.withColor(COL_OK)), false);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBreathingSubcommand() {
        return Commands.literal("breathing")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests(NichirinCommand::suggestBreathingStyles)
                                        .executes(ctx -> giveBreathingStyle(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "style"),
                                                true))
                                        .then(Commands.argument("set", BoolArgumentType.bool())
                                                .executes(ctx -> giveBreathingStyle(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "style"),
                                                        BoolArgumentType.getBool(ctx, "set")))))))

                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("random")
                                        .executes(ctx -> setRandomBreathingStyle(ctx,
                                                EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.argument("style", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestUnlockedBreathingStyles(ctx, b, EntityArgument.getPlayer(ctx, "player")))
                                        .executes(ctx -> setBreathingStyle(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "style"))))));
    }

    private static int giveBreathingStyle(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String style, boolean setActive) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();
        String formatted = formatStyleName(style);

        if (!isBreathingStyle(style)) {
            src.sendFailure(Component.literal(style + " is not a breathing technique").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!NichirinMovesetRegistry.isRegistered(style)) {
            src.sendFailure(Component.literal("Unknown breathing style: " + style).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (ProgressionHelper.isMovesetUnlocked(player, style)) {
            src.sendFailure(Component.literal(playerName + " already has " + formatted + " unlocked").withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }
        if (setActive) {
            String currentStyle = PlayerDataProvider.getData(player).getMovesetData().getMovesetId();
            if (style.equals(currentStyle)) {
                src.sendFailure(Component.literal(playerName + " already has " + formatted + " active").withStyle(s -> s.withColor(COL_WARN)));
                return 0;
            }
        }

        ProgressionHelper.unlockMoveset(player, style);

        if (setActive) {
            PlayerDataProvider.updateAndSync(player, style);
            src.sendSuccess(() -> Component.literal("Gave and activated " + formatted + " for " + playerName).withStyle(s -> s.withColor(COL_OK)), true);
            player.displayClientMessage(Component.literal("You have been granted " + formatted + "!").withStyle(s -> s.withColor(0x55FFFF)), false);
        } else {
            src.sendSuccess(() -> Component.literal("Unlocked " + formatted + " for " + playerName + " (not set as active)").withStyle(s -> s.withColor(COL_OK)), true);
            player.displayClientMessage(Component.literal("You have unlocked " + formatted + "! Use the GUI to set it active.").withStyle(s -> s.withColor(0x55FFFF)), false);
        }
        return 1;
    }

    private static int setBreathingStyle(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String style) {
        CommandSourceStack src = ctx.getSource();

        if (!isBreathingStyle(style)) {
            src.sendFailure(Component.literal(style + " is not a breathing technique").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!NichirinMovesetRegistry.isRegistered(style)) {
            src.sendFailure(Component.literal("Unknown breathing style: " + style).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!ProgressionHelper.isMovesetUnlocked(player, style)) {
            src.sendFailure(Component.literal(player.getName().getString() + " has not unlocked " + formatStyleName(style)).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        String currentStyle = PlayerDataProvider.getData(player).getMovesetData().getMovesetId();
        if (style.equals(currentStyle)) {
            src.sendFailure(Component.literal(player.getName().getString() + " already has " + formatStyleName(style) + " active").withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }

        PlayerDataProvider.updateAndSync(player, style);
        src.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s breathing style to " + formatStyleName(style)).withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("Your breathing style is now " + formatStyleName(style)).withStyle(s -> s.withColor(0x55FFFF)), false);
        return 1;
    }

    /** Sets the player to a random one of their unlocked breathing styles (preferring a change). */
    private static int setRandomBreathingStyle(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();

        var progression = PlayerDataProvider.getData(player).getProgression();
        String current = PlayerDataProvider.getData(player).getMovesetData().getMovesetId();

        List<String> unlocked = new ArrayList<>();
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isBreathingStyle(id) && progression.isMovesetUnlocked(id)) {
                unlocked.add(id);
            }
        }

        if (unlocked.isEmpty()) {
            src.sendFailure(Component.literal(playerName + " has no unlocked breathing styles").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }

        // Avoid re-picking the currently active style when there's another option.
        List<String> pool = new ArrayList<>(unlocked);
        if (pool.size() > 1 && current != null) {
            pool.remove(current);
        }

        String chosen = pool.get(player.getRandom().nextInt(pool.size()));
        String formatted = formatStyleName(chosen);
        PlayerDataProvider.updateAndSync(player, chosen);
        src.sendSuccess(() -> Component.literal("Set " + playerName + "'s breathing style to " + formatted + " (random)").withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("Your breathing style is now " + formatted).withStyle(s -> s.withColor(0x55FFFF)), false);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildDemonSubcommand() {
        return Commands.literal("demon")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("become")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> becomeDemon(ctx, EntityArgument.getPlayer(ctx, "player")))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> removeDemon(ctx, EntityArgument.getPlayer(ctx, "player")))))

                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("art", StringArgumentType.string())
                                        .suggests(NichirinCommand::suggestDemonArts)
                                        .executes(ctx -> giveDemonArt(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "art"),
                                                true))
                                        .then(Commands.argument("set", BoolArgumentType.bool())
                                                .executes(ctx -> giveDemonArt(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "art"),
                                                        BoolArgumentType.getBool(ctx, "set")))))))

                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("art", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestUnlockedDemonArts(ctx, b, EntityArgument.getPlayer(ctx, "player")))
                                        .executes(ctx -> setDemonArt(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "art"))))));
    }

    private static int becomeDemon(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();

        String currentDemonMoveset = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (currentDemonMoveset != null) {
            src.sendFailure(Component.literal(playerName + " is already a demon with " + formatArtName(currentDemonMoveset)).withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }

        if (!ProgressionHelper.isMovesetUnlocked(player, "default_demon")) {
            ProgressionHelper.unlockMoveset(player, "default_demon");
        }
        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId("default_demon");
        DemonManager.setBloodPoints(player, NichirinModConfig.get().demon.maxBloodPoints);
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);

        if (!MovesetHelper.hasDemonMoveset(player) || !"default_demon".equals(MovesetHelper.getDemonMovesetId(player))) {
            src.sendFailure(Component.literal("Failed to make " + playerName + " a demon (sync issue)").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Made " + playerName + " a demon").withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("You have become a demon! You now have demon abilities and passives.").withStyle(s -> s.withColor(COL_ERR)), false);
        return 1;
    }

    private static int removeDemon(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();

        String currentDemonMoveset = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (currentDemonMoveset == null) {
            src.sendFailure(Component.literal(playerName + " is not a demon").withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }

        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(null);
        DemonManager.cleanupPlayer(player);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.getFoodData().setExhaustion(0.0f);
        if (player.isOnFire()) player.clearFire();

        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);
        NichirinPacketRegistry.sendDemonSync(player, 0, 0, false);

        try {
            DefaultDemonMoveset.cleanupPlayer(player);
        } catch (Exception ignored) {}
        try {
            DestructiveDeathMoveset.cleanupPlayer(player);
        } catch (Exception ignored) {}
        MovesetAuraTicker.clear(player);

        if (MovesetHelper.hasDemonMoveset(player)) {
            src.sendFailure(Component.literal("Failed to remove demon status from " + playerName + " (sync issue)").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Removed demon status from " + playerName).withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("You are no longer a demon. Your demon abilities have been removed.").withStyle(s -> s.withColor(0x55FFFF)), false);
        return 1;
    }

    private static int giveDemonArt(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String art, boolean setActive) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();
        String formatted = formatArtName(art);

        if (!isDemonArt(art)) {
            src.sendFailure(Component.literal(art + " is not a demon art").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!NichirinMovesetRegistry.isRegistered(art)) {
            src.sendFailure(Component.literal("Unknown demon art: " + art).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (ProgressionHelper.isMovesetUnlocked(player, art)) {
            src.sendFailure(Component.literal(playerName + " already has " + formatted + " unlocked").withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }
        if (setActive) {
            String currentDemonArt = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
            if (art.equals(currentDemonArt)) {
                src.sendFailure(Component.literal(playerName + " already has " + formatted + " active").withStyle(s -> s.withColor(COL_WARN)));
                return 0;
            }
        }

        ProgressionHelper.unlockMoveset(player, art);

        if (setActive) {
            PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(art);
            PlayerDataStorage.savePlayerData(player);
            PlayerDataProvider.forceSync(player.server);
            src.sendSuccess(() -> Component.literal("Gave and activated " + formatted + " for " + playerName).withStyle(s -> s.withColor(COL_OK)), true);
            player.displayClientMessage(Component.literal("You have been granted " + formatted + "!").withStyle(s -> s.withColor(COL_ERR)), false);
        } else {
            src.sendSuccess(() -> Component.literal("Unlocked " + formatted + " for " + playerName + " (not set as active)").withStyle(s -> s.withColor(COL_OK)), true);
            player.displayClientMessage(Component.literal("You have unlocked " + formatted + "! Use the GUI to set it active.").withStyle(s -> s.withColor(COL_ERR)), false);
        }
        return 1;
    }

    private static int setDemonArt(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String art) {
        CommandSourceStack src = ctx.getSource();

        if (!isDemonArt(art)) {
            src.sendFailure(Component.literal(art + " is not a demon art").withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!NichirinMovesetRegistry.isRegistered(art)) {
            src.sendFailure(Component.literal("Unknown demon art: " + art).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        if (!ProgressionHelper.isMovesetUnlocked(player, art)) {
            src.sendFailure(Component.literal(player.getName().getString() + " has not unlocked " + formatArtName(art)).withStyle(s -> s.withColor(COL_ERR)));
            return 0;
        }
        String currentDemonArt = PlayerDataProvider.getData(player).getMovesetData().getDemonMovesetId();
        if (art.equals(currentDemonArt)) {
            src.sendFailure(Component.literal(player.getName().getString() + " already has " + formatArtName(art) + " active").withStyle(s -> s.withColor(COL_WARN)));
            return 0;
        }

        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId(art);
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);
        src.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s demon art to " + formatArtName(art)).withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("Your demon art is now " + formatArtName(art)).withStyle(s -> s.withColor(COL_ERR)), false);
        return 1;
    }

    private static int resetAllCooldowns(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CommandSourceStack src = ctx.getSource();
        String playerName = player.getName().getString();

        CqcMoveset.resetCooldowns(player);
        CompassNeedleChargeManager.clear(player.getUUID());
        CompassNeedleTracker.cancel(player);
        DestructiveDeathState.deactivateCompass(player);
        DestructiveDeathState.clearCompassCooldown(player);

        int cleared = 0;
        for (NichirinMovesetRegistry.MoveInfo moveInfo : NichirinMovesetRegistry.getAllMoves().values()) {
            CooldownDisplayPacket.sendToClient(player, moveInfo.displayName, 0);
            cleared++;
        }
        // CQC moves aren't in the global move registry — clear their client cooldown HUD (which also
        // gates firing) by display name so the command actually frees them up.
        for (var config : CqcMoveset.configurations().values()) {
            CooldownDisplayPacket.sendToClient(player, config.getDisplayName(), 0);
            cleared++;
        }

        int finalCleared = cleared;
        src.sendSuccess(() -> Component.literal("Reset all cooldowns for " + playerName + " (" + finalCleared + " moves cleared)").withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("All your cooldowns have been reset!").withStyle(s -> s.withColor(0x55FFFF)), false);
        return 1;
    }

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
        String currentStyle = PlayerDataProvider.getData(player).getMovesetData().getMovesetId();
        if ((currentStyle == null || currentStyle.isEmpty()) && firstStyle != null) {
            PlayerDataProvider.updateAndSync(player, firstStyle);
        }
        ProgressionSyncPacket.sendToPlayer(player);

        int finalStyles = stylesUnlocked;
        src.sendSuccess(() -> Component.literal("Unlocked everything for " + name + ": " + finalStyles + " style(s).").withStyle(s -> s.withColor(COL_OK)), true);
        player.displayClientMessage(Component.literal("All breathing styles unlocked!").withStyle(s -> s.withColor(0x55FFFF)), false);
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
            configList(ctx);
        }
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> header("— Breath of Nichirin Help —"), false);
        src.sendSuccess(() -> line(COL_DIM, "Operator commands (permission level 2):"), false);
        src.sendSuccess(() -> cmd("/nichirin breathing give <player> <style> [set]", "Unlock a breathing style"), false);
        src.sendSuccess(() -> cmd("/nichirin breathing set <player> <style>",        "Set active style (must be unlocked)"), false);
        src.sendSuccess(() -> cmd("/nichirin demon become <player>",                 "Make player a demon"), false);
        src.sendSuccess(() -> cmd("/nichirin demon remove <player>",                 "Remove demon status"), false);
        src.sendSuccess(() -> cmd("/nichirin demon give <player> <art> [set]",       "Unlock a demon art"), false);
        src.sendSuccess(() -> cmd("/nichirin demon set <player> <art>",              "Set active demon art"), false);
        src.sendSuccess(() -> cmd("/nichirin cooldown <player>",                     "Reset ALL move cooldowns"), false);
        src.sendSuccess(() -> cmd("/nichirin unlockall <player>",                    "Unlock every moveset"), false);
        src.sendSuccess(() -> cmd("/nichirin bloodmoon",                             "Toggle the Blood Moon"), false);
        src.sendSuccess(() -> cmd("/nichirin config",                                "Open config GUI"), false);
        src.sendSuccess(() -> cmd("/nichirin config list|get|set|reset|resetall",    "Config from chat"), false);
        return 1;
    }

    private static int configList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> header("— Config Values —"), false);

        for (Map.Entry<String, NichirinConfig.Entry> e : NichirinConfig.getAll().entrySet()) {
            String key = e.getKey();
            NichirinConfig.Entry entry = e.getValue();

            String rangePart = entry.isBoolean()
                    ? "default " + entry.displayDefault()
                    : "default " + entry.displayDefault() + ", range " + entry.min() + "–" + entry.max();
            MutableComponent line = Component.literal("  ")
                    .append(Component.literal(key).withStyle(s -> s.withColor(COL_KEY)))
                    .append(Component.literal(" = ").withStyle(s -> s.withColor(COL_DIM)))
                    .append(Component.literal(entry.displayValue()).withStyle(s -> s.withColor(COL_VALUE)))
                    .append(Component.literal("  (" + rangePart + ")").withStyle(s -> s.withColor(COL_DIM)));

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
                        .append(Component.literal(" to default (" + entry.value() + ")").withStyle(s -> s.withColor(COL_DIM))),
                true);
        return 1;
    }

    private static int configResetAll(CommandContext<CommandSourceStack> ctx) {
        NichirinConfig.resetAll();
        ctx.getSource().sendSuccess(() ->
                Component.literal("All config values reset to defaults.").withStyle(s -> s.withColor(COL_OK)),
                true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestBreathingStyles(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isBreathingStyle(id) && id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestUnlockedBreathingStyles(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder, ServerPlayer player) {
        String input = builder.getRemaining().toLowerCase();
        var progression = PlayerDataProvider.getData(player).getProgression();
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isBreathingStyle(id) && progression.isMovesetUnlocked(id) && id.toLowerCase().startsWith(input)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestDemonArts(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isDemonArt(id) && id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestUnlockedDemonArts(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder, ServerPlayer player) {
        String input = builder.getRemaining().toLowerCase();
        var progression = PlayerDataProvider.getData(player).getProgression();
        for (String id : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (isDemonArt(id) && progression.isMovesetUnlocked(id) && id.toLowerCase().startsWith(input)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }

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

    private static boolean isBreathingStyle(String movesetId) {
        return movesetId != null && movesetId.contains("breathing");
    }

    private static boolean isDemonArt(String movesetId) {
        if (movesetId == null) return false;
        return movesetId.equals("default_demon")
                || movesetId.equals("destructive_death")
                || movesetId.contains("demon");
    }

    private static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private static String formatArtName(String artId) {
        if (artId.equals("default_demon")) return "Demon Arts";
        return formatStyleName(artId);
    }

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
