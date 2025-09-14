package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class WysteriaRootDecorator extends TreeDecorator {
    public static final Codec<WysteriaRootDecorator> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(decorator -> decorator.probability))
                    .apply(instance, WysteriaRootDecorator::new));

    private final float probability;

    public WysteriaRootDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return com.xirc.nichirin.registry.NichirinTreeDecoratorTypes.WYSTERIA_ROOT_DECORATOR.get();
    }

    @Override
    public void place(Context context) {
        if (context.random().nextFloat() >= this.probability) {
            return;
        }

        if (context.roots().isEmpty()) return;
        BlockPos trunkBase = context.roots().get(0);

        net.minecraft.world.level.block.state.BlockState logState =
                com.xirc.nichirin.registry.NichirinBlockRegistry.WYSTERIA_LOG.get().defaultBlockState();

        // Determine trunk type (check if there's a 2x2 base)
        boolean isThickTrunk = context.isAir(trunkBase.offset(1, 0, 0).above()) == false &&
                context.isAir(trunkBase.offset(0, 0, 1).above()) == false &&
                context.isAir(trunkBase.offset(1, 0, 1).above()) == false;

        // Create 4-6 roots that ALWAYS connect to the trunk base
        int rootCount = 4 + context.random().nextInt(3); // 4-6 roots

        for (int i = 0; i < rootCount; i++) {
            Direction direction = Direction.from2DDataValue(i % 4);

            // Find connection point on trunk base
            BlockPos connectionPoint;
            if (isThickTrunk) {
                // Connect to appropriate edge of 2x2 base
                switch (direction) {
                    case NORTH -> connectionPoint = trunkBase.offset(0, 0, 0);
                    case EAST -> connectionPoint = trunkBase.offset(1, 0, 0);
                    case SOUTH -> connectionPoint = trunkBase.offset(1, 0, 1);
                    case WEST -> connectionPoint = trunkBase.offset(0, 0, 1);
                    default -> connectionPoint = trunkBase;
                }
            } else {
                connectionPoint = trunkBase;
            }

            // Create root extending from connection point
            int rootLength = 3 + context.random().nextInt(5); // 3-7 blocks
            BlockPos rootPos = connectionPoint;

            // Place connecting root segment first (on surface next to trunk)
            BlockPos firstRootPos = connectionPoint.relative(direction);
            if (context.isAir(firstRootPos) && !context.isAir(firstRootPos.below())) {
                context.setBlock(firstRootPos, logState);
                rootPos = firstRootPos;

                // Add thickness to connection area
                if (context.random().nextBoolean()) {
                    BlockPos thickPos = firstRootPos.above();
                    if (context.isAir(thickPos)) {
                        context.setBlock(thickPos, logState);
                    }
                }
            }

            // Continue root outward, ensuring connection
            for (int j = 1; j < rootLength; j++) {
                BlockPos nextPos = rootPos.relative(direction);

                if (context.isAir(nextPos) && !context.isAir(nextPos.below())) {
                    context.setBlock(nextPos, logState);
                    rootPos = nextPos;

                    // Add thickness near base
                    if (j < 3 && context.random().nextBoolean()) {
                        Direction perpDir = context.random().nextBoolean() ? direction.getClockWise() : direction.getCounterClockWise();
                        BlockPos widePos = nextPos.relative(perpDir);
                        if (context.isAir(widePos) && !context.isAir(widePos.below())) {
                            context.setBlock(widePos, logState);
                        }
                    }

                    // Create connected branches
                    if (j >= 2 && context.random().nextBoolean()) {
                        Direction branchDir = context.random().nextBoolean() ? direction.getClockWise() : direction.getCounterClockWise();
                        int branchLength = 1 + context.random().nextInt(3); // 1-3 blocks

                        BlockPos branchPos = rootPos;
                        for (int k = 1; k <= branchLength; k++) {
                            branchPos = branchPos.relative(branchDir);
                            if (context.isAir(branchPos) && !context.isAir(branchPos.below())) {
                                context.setBlock(branchPos, logState);
                            } else {
                                break; // Stop if blocked
                            }
                        }
                    }
                } else {
                    break; // Stop if can't place
                }
            }
        }
    }
}