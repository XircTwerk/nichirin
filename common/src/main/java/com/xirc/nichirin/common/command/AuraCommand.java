package com.xirc.nichirin.common.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * /nichirin debug aura ... — admin commands for the entity aura system.
 *
 * Slim, two-subcommand shape:
 *   add <entity> [r g b a] [radius] [jitter] [audience]   — attach an aura
 *   remove <entity>                                        — remove all auras from entity
 *
 * Audience defaults to "all". jitter defaults to 2.2.
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
                                                                .executes(ctx -> add(ctx,
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                        1.5f, 2.2f, AuraAudience.ALL))
                                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(0.1f, 48))
                                                                        .executes(ctx -> add(ctx,
                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                                FloatArgumentType.getFloat(ctx, "a"),
                                                                                FloatArgumentType.getFloat(ctx, "radius"),
                                                                                2.2f, AuraAudience.ALL))
                                                                        .then(Commands.argument("jitter", FloatArgumentType.floatArg(0f, 50f))
                                                                                .executes(ctx -> add(ctx,
                                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                                        FloatArgumentType.getFloat(ctx, "radius"),
                                                                                        FloatArgumentType.getFloat(ctx, "jitter"),
                                                                                        AuraAudience.ALL))
                                                                                .then(Commands.literal("self")
                                                                                        .executes(ctx -> add(ctx,
                                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                                                FloatArgumentType.getFloat(ctx, "a"),
                                                                                                FloatArgumentType.getFloat(ctx, "radius"),
                                                                                                FloatArgumentType.getFloat(ctx, "jitter"),
                                                                                                AuraAudience.SELF_ONLY)))
                                                                                .then(Commands.literal("only")
                                                                                        .then(Commands.argument("viewer", EntityArgument.player())
                                                                                                .executes(ctx -> add(ctx,
                                                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                                                        FloatArgumentType.getFloat(ctx, "radius"),
                                                                                                        FloatArgumentType.getFloat(ctx, "jitter"),
                                                                                                        playersAudience(EntityArgument.getPlayer(ctx, "viewer"))))))))))))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> remove(ctx, EntityArgument.getEntity(ctx, "target")))));
    }

    private static int addDefault(CommandContext<CommandSourceStack> ctx, Entity target) {
        AuraInstance inst = AuraInstance.builder().build();
        AuraManager.addAura(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added default aura " + shortId(inst.id()) + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx, Entity target,
                           float r, float g, float b, float a, float radius, float jitter,
                           AuraAudience audience) {
        AuraInstance inst = AuraInstance.builder()
                .color(r, g, b, a)
                .radius(radius)
                .jitter(jitter)
                .build();
        AuraManager.addAura(target, inst, audience);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added aura " + shortId(inst.id()) + " to " + target.getName().getString()
                        + " (radius=" + radius + ", jitter=" + jitter
                        + ", audience=" + audience.getClass().getSimpleName() + ")"), true);
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
}
