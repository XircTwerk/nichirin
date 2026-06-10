package com.xirc.nichirin.common.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
 *   add <entity> [r g b a] [thickness] [seeThroughWalls]   — attach an outline
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
                                                                        1.05f, false))
                                                                .then(Commands.argument("thickness", FloatArgumentType.floatArg(1.0f, 5.0f))
                                                                        .executes(ctx -> add(ctx,
                                                                                EntityArgument.getEntity(ctx, "target"),
                                                                                FloatArgumentType.getFloat(ctx, "r"),
                                                                                FloatArgumentType.getFloat(ctx, "g"),
                                                                                FloatArgumentType.getFloat(ctx, "b"),
                                                                                FloatArgumentType.getFloat(ctx, "a"),
                                                                                FloatArgumentType.getFloat(ctx, "thickness"), false))
                                                                        .then(Commands.argument("seeThroughWalls", BoolArgumentType.bool())
                                                                                .executes(ctx -> add(ctx,
                                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                                        FloatArgumentType.getFloat(ctx, "r"),
                                                                                        FloatArgumentType.getFloat(ctx, "g"),
                                                                                        FloatArgumentType.getFloat(ctx, "b"),
                                                                                        FloatArgumentType.getFloat(ctx, "a"),
                                                                                        FloatArgumentType.getFloat(ctx, "thickness"),
                                                                                        BoolArgumentType.getBool(ctx, "seeThroughWalls")))))))))))

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
