package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinTreeDecoratorTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaHangingLeavesDecorator extends TreeDecorator {
    public static final MapCodec<WisteriaHangingLeavesDecorator> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.intRange(1, 8).fieldOf("min_length").forGetter(decorator -> decorator.minLength),
                    Codec.intRange(2, 20).fieldOf("max_length").forGetter(decorator -> decorator.maxLength)
            ).apply(instance, WisteriaHangingLeavesDecorator::new));

    private final int minLength;
    private final int maxLength;

    public WisteriaHangingLeavesDecorator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return NichirinTreeDecoratorTypes.WISTERIA_HANGING_LEAVES_DECORATOR.get();
    }

    @Override
    public void place(Context context) {
        // Use the wisteria leaves block with persistent property
        BlockState leafState =
                NichirinBlockRegistry.WISTERIA_LEAVES.get()
                        .defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, true); // Make hanging leaves persistent

        int clustersPlaced = 0;

        // REDUCED: Only create hanging clusters from some leaf positions
        for (BlockPos leafPos : context.leaves()) {
            if (context.random().nextInt(5) == 0) { // REDUCED: 20% chance instead of 50%
                int hangLength = minLength + context.random().nextInt(maxLength - minLength + 1);

                // Create main hanging vine
                boolean clusterPlaced = false;
                for (int i = 1; i <= hangLength; i++) {
                    BlockPos hangPos = leafPos.below(i);

                    if (context.isAir(hangPos)) {
                        context.setBlock(hangPos, leafState);
                        clusterPlaced = true;

                        // REDUCED: Add width to create cluster effect less frequently
                        if (i > 3 && context.random().nextInt(3) == 0) { // Only every 3rd level and 33% chance
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    if (x == 0 && z == 0) continue;
                                    if (context.random().nextInt(6) == 0) { // REDUCED: 17% chance instead of 33%
                                        BlockPos sidePos = hangPos.offset(x, 0, z);
                                        if (context.isAir(sidePos)) {
                                            context.setBlock(sidePos, leafState);
                                        }
                                    }
                                }
                            }
                        }

                        // REDUCED: Create fewer secondary hanging strands
                        if (i > 4 && context.random().nextInt(6) == 0) { // REDUCED: 17% chance instead of 25%
                            Direction randomDir = Plane.HORIZONTAL.getRandomDirection(context.random());
                            int secondaryLength = 1 + context.random().nextInt(2); // Shorter secondary strands
                            for (int j = 1; j <= secondaryLength; j++) {
                                BlockPos secondaryPos = hangPos.relative(randomDir, j);
                                if (context.isAir(secondaryPos)) {
                                    context.setBlock(secondaryPos, leafState);
                                } else {
                                    break;
                                }
                            }
                        }
                    } else {
                        break; // Stop if we hit something
                    }

                    // Increasing chance to break as the vine gets longer
                    if (context.random().nextInt(12) < i) { // Slightly more likely to break early
                        break;
                    }
                }

                if (clusterPlaced) {
                    clustersPlaced++;
                }
            }
        }
    }
}