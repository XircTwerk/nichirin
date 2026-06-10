package com.xirc.nichirin.common.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.outline.OutlineInstance;
import com.xirc.nichirin.common.outline.OutlineManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * /nichirin debug outline ... — admin commands for the entity outline system.
 *
 *   add <entity> [r g b] [thickness] [seeThroughWalls]   — attach an outline (r/g/b as 0-255)
 *   remove <entity>                                         — remove all outlines from entity
 */
public final class OutlineCommand {

    private OutlineCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("outline")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> addDefault(ctx, EntityArgument.getEntity(ctx, "target")))
                                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(ctx -> add(ctx,
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "r") / 255f,
                                                                IntegerArgumentType.getInteger(ctx, "g") / 255f,
                                                                IntegerArgumentType.getInteger(ctx, "b") / 255f,
                                                                1.0f, 1.05f, false))
                                                        .then(Commands.argument("thickness", FloatArgumentType.floatArg(1.0f, 5.0f))
                                                                .executes(ctx -> add(ctx,
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        IntegerArgumentType.getInteger(ctx, "r") / 255f,
                                                                        IntegerArgumentType.getInteger(ctx, "g") / 255f,
                                                                        IntegerArgumentType.getInteger(ctx, "b") / 255f,
                                                                        1.0f, FloatArgumentType.getFloat(ctx, "thickness"), false))
                                                                .then(Commands.argument("seeThroughWalls", BoolArgumentType.bool())
                                                                        .executes(ctx -> add(ctx,
                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                IntegerArgumentType.getInteger(ctx, "r") / 255f,
                                                                                IntegerArgumentType.getInteger(ctx, "g") / 255f,
                                                                                IntegerArgumentType.getInteger(ctx, "b") / 255f,
                                                                                1.0f, FloatArgumentType.getFloat(ctx, "thickness"),
                                                                                BoolArgumentType.getBool(ctx, "seeThroughWalls"))))))))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> remove(ctx, EntityArgument.getEntity(ctx, "target")))));
    }

    private static int addDefault(CommandContext<CommandSourceStack> ctx, Entity target) {
        OutlineInstance inst = OutlineInstance.builder().build();
        OutlineManager.addOutline(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added default outline " + shortId(inst.id()) + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx, Entity target,
                           float r, float g, float b, float a,
                           float thickness, boolean seeThroughWalls) {
        OutlineInstance inst = OutlineInstance.builder()
                .color(r, g, b, a)
                .thickness(thickness)
                .seeThroughWalls(seeThroughWalls)
                .build();
        OutlineManager.addOutline(target, inst, AuraAudience.ALL);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added outline " + shortId(inst.id()) + " to " + target.getName().getString()
                        + " (thickness=" + thickness + ", seeThroughWalls=" + seeThroughWalls + ")"), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Entity target) {
        OutlineManager.clearOutlines(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Removed all outlines from " + target.getName().getString()), true);
        return 1;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
