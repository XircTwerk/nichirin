package com.xirc.nichirin.common.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * /nichirin debug aura ... — admin commands for the entity aura system.
 *
 *   add <entity> [r g b] [radius] [jitter] [waviness]   — attach an aura (shown to everyone)
 *   remove <entity>                                     — remove all auras from entity
 *
 * jitter defaults to 2.2, waviness defaults to 0 (off).
 */
public final class AuraCommand {

    private AuraCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("aura")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> addDefault(ctx, EntityArgument.getEntity(ctx, "target")))
                                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(ctx -> add(ctx,
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                rgb(ctx, "r"), rgb(ctx, "g"), rgb(ctx, "b"),
                                                                1.0f, 1.5f, 2.2f, 0.0f))
                                                        .then(Commands.argument("radius", FloatArgumentType.floatArg(0.1f, 48))
                                                                .executes(ctx -> add(ctx,
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        rgb(ctx, "r"), rgb(ctx, "g"), rgb(ctx, "b"),
                                                                        1.0f, FloatArgumentType.getFloat(ctx, "radius"),
                                                                        2.2f, 0.0f))
                                                                .then(Commands.argument("jitter", FloatArgumentType.floatArg(0f, 50f))
                                                                        .executes(ctx -> add(ctx,
                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                rgb(ctx, "r"), rgb(ctx, "g"), rgb(ctx, "b"),
                                                                                1.0f, FloatArgumentType.getFloat(ctx, "radius"),
                                                                                FloatArgumentType.getFloat(ctx, "jitter"), 0.0f))
                                                                        .then(Commands.argument("waviness", FloatArgumentType.floatArg(0f, 50f))
                                                                                .executes(ctx -> add(ctx,
                                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                                        rgb(ctx, "r"), rgb(ctx, "g"), rgb(ctx, "b"),
                                                                                        1.0f, FloatArgumentType.getFloat(ctx, "radius"),
                                                                                        FloatArgumentType.getFloat(ctx, "jitter"),
                                                                                        FloatArgumentType.getFloat(ctx, "waviness")))))))))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> remove(ctx, EntityArgument.getEntity(ctx, "target")))));
    }

    private static float rgb(CommandContext<CommandSourceStack> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name) / 255f;
    }

    private static int addDefault(CommandContext<CommandSourceStack> ctx, Entity target) {
        AuraInstance inst = AuraInstance.builder().build();
        AuraManager.addAura(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added default aura " + shortId(inst.id()) + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx, Entity target,
                           float r, float g, float b, float a, float radius, float jitter, float waviness) {
        AuraInstance inst = AuraInstance.builder()
                .color(r, g, b, a)
                .radius(radius)
                .jitter(jitter)
                .waviness(waviness)
                .build();
        AuraManager.addAura(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added aura " + shortId(inst.id()) + " to " + target.getName().getString()
                        + " (radius=" + radius + ", jitter=" + jitter + ", waviness=" + waviness + ")"), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Entity target) {
        AuraManager.clearAuras(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Removed all auras from " + target.getName().getString()), true);
        return 1;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
