package com.xirc.nichirin.common.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xirc.nichirin.client.aura.AuraConfig;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * /nichirin debug aura ... — admin commands for the entity aura system.
 *
 * Subcommands:
 *   add <entity> [r g b a] [radius] [audience]    — attach an aura
 *   remove <entity>                                — remove all auras from entity
 *   debug                                          — toggle the caller's debug overlay
 *   color <r> <g> <b> <a>                          — set caller's debug color
 *   color reset                                    — reset caller's debug color
 *   pixelize <on|off>                              — toggle pixelization pass on caller
 *   set <param> <value>                            — live-tune client AuraConfig on caller
 *
 * The debug/color/pixelize/set actions are sent to the caller's client via
 * AURA_CLIENT_CTRL_ID. The actual aura state (add/remove) lives on the server.
 */
public final class AuraCommand {

    private AuraCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("aura")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> addDefault(ctx, EntityArgument.getEntity(ctx, "target")))
                                .then(Commands.argument("r", FloatArgumentType.floatArg(0, 1))
                                        .then(Commands.argument("g", FloatArgumentType.floatArg(0, 1))
                                                .then(Commands.argument("b", FloatArgumentType.floatArg(0, 1))
                                                        .then(Commands.argument("a", FloatArgumentType.floatArg(0, 1))
                                                                .executes(ctx -> addWithColor(ctx,
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                        1.5f,
                                                                        AuraAudience.ALL))
                                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(0.1f, 16))
                                                                        .executes(ctx -> addWithColor(ctx,
                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                                FloatArgumentType.getFloat(ctx, "a"),
                                                                                FloatArgumentType.getFloat(ctx, "radius"),
                                                                                AuraAudience.ALL))
                                                                        .then(Commands.literal("self")
                                                                                .executes(ctx -> addWithColor(ctx,
                                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                                        FloatArgumentType.getFloat(ctx, "radius"),
                                                                                        AuraAudience.SELF_ONLY)))
                                                                        .then(Commands.literal("only")
                                                                                .then(Commands.argument("viewer", EntityArgument.player())
                                                                                        .executes(ctx -> addWithColor(ctx,
                                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                                                FloatArgumentType.getFloat(ctx, "a"),
                                                                                                FloatArgumentType.getFloat(ctx, "radius"),
                                                                                                playersAudience(EntityArgument.getPlayer(ctx, "viewer")))))))))))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> remove(ctx, EntityArgument.getEntity(ctx, "target")))))

                .then(Commands.literal("debug")
                        .executes(AuraCommand::sendDebugToggle))

                .then(Commands.literal("color")
                        .then(Commands.literal("reset")
                                .executes(AuraCommand::sendColorReset))
                        .then(Commands.argument("r", FloatArgumentType.floatArg(0, 1))
                                .then(Commands.argument("g", FloatArgumentType.floatArg(0, 1))
                                        .then(Commands.argument("b", FloatArgumentType.floatArg(0, 1))
                                                .then(Commands.argument("a", FloatArgumentType.floatArg(0, 1))
                                                        .executes(ctx -> sendColor(ctx,
                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                FloatArgumentType.getFloat(ctx, "a"))))))))

                .then(Commands.literal("pixelize")
                        .then(Commands.literal("on").executes(ctx -> sendPixelize(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> sendPixelize(ctx, false))))

                .then(Commands.literal("set")
                        .then(Commands.argument("param", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String k : AuraConfig.knownKeys()) {
                                        if (k.toLowerCase().startsWith(b.getRemaining().toLowerCase())) b.suggest(k);
                                    }
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> sendSet(ctx,
                                                StringArgumentType.getString(ctx, "param"),
                                                StringArgumentType.getString(ctx, "value"))))));
    }

    // ─── aura state actions (server-side) ───

    private static int addDefault(CommandContext<CommandSourceStack> ctx, Entity target) {
        AuraInstance inst = AuraInstance.builder().build();
        AuraManager.addAura(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added default aura " + shortId(inst.id()) + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int addWithColor(CommandContext<CommandSourceStack> ctx, Entity target,
                                    float r, float g, float b, float a, float radius,
                                    AuraAudience audience) {
        AuraInstance inst = AuraInstance.builder()
                .color(r, g, b, a)
                .radius(radius)
                .build();
        AuraManager.addAura(target, inst, audience);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added aura " + shortId(inst.id()) + " to " + target.getName().getString()
                        + " (radius=" + radius + ", audience=" + audience.getClass().getSimpleName() + ")"), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Entity target) {
        AuraManager.clearAuras(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Removed all auras from " + target.getName().getString()), true);
        return 1;
    }

    private static AuraAudience playersAudience(ServerPlayer p) {
        Set<UUID> set = new HashSet<>();
        set.add(p.getUUID());
        return AuraAudience.players(set);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    // ─── client-control actions (sent to caller) ───

    private static int sendDebugToggle(CommandContext<CommandSourceStack> ctx) {
        return sendCtrl(ctx, "debug_toggle", "");
    }

    private static int sendColorReset(CommandContext<CommandSourceStack> ctx) {
        return sendCtrl(ctx, "color_reset", "");
    }

    private static int sendColor(CommandContext<CommandSourceStack> ctx,
                                 float r, float g, float b, float a) {
        return sendCtrl(ctx, "color", r + " " + g + " " + b + " " + a);
    }

    private static int sendPixelize(CommandContext<CommandSourceStack> ctx, boolean on) {
        return sendCtrl(ctx, "pixelize", on ? "true" : "false");
    }

    private static int sendSet(CommandContext<CommandSourceStack> ctx, String key, String value) {
        return sendCtrl(ctx, "set", key + " " + value);
    }

    private static int sendCtrl(CommandContext<CommandSourceStack> ctx, String action, String arg) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(action);
            buf.writeUtf(arg);
            NetworkManager.sendToPlayer(p, NichirinPacketRegistry.AURA_CLIENT_CTRL_ID,
                    com.xirc.nichirin.common.util.NetworkBufferUtils.server(buf, p));
            ctx.getSource().sendSuccess(() -> Component.literal("Sent aura ctrl: " + action + " " + arg), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Run from a player."));
            return 0;
        }
    }
}
