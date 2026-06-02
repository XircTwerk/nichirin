package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinTrunkPlacerTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.BlockState;

public class WysteriaTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<WysteriaTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            trunkPlacerParts(instance)
                    .and(IntProvider.codec(1, 8).fieldOf("branch_count").forGetter(placer -> placer.branchCount))
                    .apply(instance, WysteriaTrunkPlacer::new));

    private final IntProvider branchCount;

    public WysteriaTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider branchCount) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return NichirinTrunkPlacerTypes.WYSTERIA_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, int height, BlockPos startPos, TreeConfiguration config) {
        List<FoliagePlacer.FoliageAttachment> foliageAttachments = new ArrayList<>();
        Set<BlockPos> trunkPositions = new HashSet<>(); // Track all trunk positions

        boolean isLargeTree = height >= 8;

        // Create main trunk first and track all positions
        BlockPos currentPos = startPos;

        for (int y = 0; y < height; y++) {
            // Create thick trunk for larger trees
            if (isLargeTree && y < height * 0.6) {
                // 2x2 thick base
                BlockPos[] thickPositions = {
                        currentPos,
                        currentPos.offset(1, 0, 0),
                        currentPos.offset(0, 0, 1),
                        currentPos.offset(1, 0, 1)
                };

                for (BlockPos pos : thickPositions) {
                    this.placeLog(level, blockSetter, random, pos, config);
                    trunkPositions.add(pos);
                }
            } else {
                // Single trunk higher up
                this.placeLog(level, blockSetter, random, currentPos, config);
                trunkPositions.add(currentPos);
            }

            // Add occasional gnarled texture, but ensure it connects
            if (random.nextInt(5) == 0) {
                Direction randomDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos gnarlPos = currentPos.relative(randomDir);
                this.placeLog(level, blockSetter, random, gnarlPos, config);
                trunkPositions.add(gnarlPos);
            }

            currentPos = currentPos.above();
        }

        // Record final trunk top
        BlockPos trunkTop = currentPos.below();
        trunkPositions.add(trunkTop);

        // Add main foliage at top
        foliageAttachments.add(new FoliagePlacer.FoliageAttachment(trunkTop.above(), 0, false));

        // Create branches that ALWAYS connect to existing trunk positions
        int totalBranches = Math.max(branchCount.sample(random), 3);
        int branchStartHeight = Math.max(height / 3, 3);

        for (int i = 0; i < totalBranches; i++) {
            // Find connection point on existing trunk
            int branchY = branchStartHeight + (i * (height - branchStartHeight) / Math.max(totalBranches - 1, 1));
            BlockPos connectionPoint = startPos.above(branchY);

            // For thick trunks, find the best connection point
            if (isLargeTree && branchY < height * 0.6) {
                // Connect to edge of 2x2 trunk based on branch direction
                Direction branchDir = Direction.from2DDataValue(i % 4);
                switch (branchDir) {
                    case NORTH -> connectionPoint = startPos.above(branchY).offset(0, 0, 0);
                    case EAST -> connectionPoint = startPos.above(branchY).offset(1, 0, 0);
                    case SOUTH -> connectionPoint = startPos.above(branchY).offset(1, 0, 1);
                    case WEST -> connectionPoint = startPos.above(branchY).offset(0, 0, 1);
                }
            }

            // Ensure connection point has trunk
            if (!trunkPositions.contains(connectionPoint)) {
                this.placeLog(level, blockSetter, random, connectionPoint, config);
                trunkPositions.add(connectionPoint);
            }

            // Create branch extending from connection point
            Direction branchDir = Direction.from2DDataValue(i % 4);
            int branchLength;
            if (height <= 5) {
                branchLength = 1 + random.nextInt(2); // Small trees: 1-2 blocks
            } else if (height <= 8) {
                branchLength = 2 + random.nextInt(2); // Medium trees: 2-3 blocks
            } else {
                branchLength = 3 + random.nextInt(4); // Large trees: 3-6 blocks
            }

            BlockPos branchPos = connectionPoint;
            for (int j = 1; j <= branchLength; j++) { // Start from 1 to avoid overwriting connection
                branchPos = branchPos.relative(branchDir);
                this.placeLog(level, blockSetter, random, branchPos, config);

                // Add secondary branches that connect to main branch
                if (j >= 2 && random.nextInt(4) == 0) {
                    Direction secondaryDir = random.nextBoolean() ? branchDir.getClockWise() : branchDir.getCounterClockWise();
                    int secondaryLength = 1 + random.nextInt(2); // 1-2 blocks

                    BlockPos secondaryPos = branchPos;
                    for (int k = 1; k <= secondaryLength; k++) {
                        secondaryPos = secondaryPos.relative(secondaryDir);
                        this.placeLog(level, blockSetter, random, secondaryPos, config);
                    }
                }

                // Branch can curve slightly but stay connected
                if (j > 2 && random.nextInt(5) == 0) {
                    if (random.nextBoolean()) {
                        branchPos = branchPos.above(); // Curve up
                    } else {
                        branchPos = branchPos.below(); // Droop down
                    }
                    // Place connecting log
                    this.placeLog(level, blockSetter, random, branchPos, config);
                }
            }

            // Add foliage at branch end
            foliageAttachments.add(new FoliagePlacer.FoliageAttachment(branchPos.above(), 0, false));
        }
        return foliageAttachments;
    }
}