package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinTrunkPlacerTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WisteriaTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<WisteriaTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            trunkPlacerParts(instance)
                    .and(IntProvider.codec(1, 10).fieldOf("branch_count").forGetter(placer -> placer.branchCount))
                    .apply(instance, WisteriaTrunkPlacer::new));
    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private final IntProvider branchCount;

    public WisteriaTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider branchCount) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return NichirinTrunkPlacerTypes.WISTERIA_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, int height, BlockPos startPos, TreeConfiguration config) {
        List<FoliagePlacer.FoliageAttachment> foliageAttachments = new ArrayList<>();
        List<BlockPos> trunkCenters = new ArrayList<>();
        boolean largeTree = height >= 10;
        boolean mediumTree = height >= 7;
        int leanX = random.nextInt(3) - 1;
        int leanZ = random.nextInt(3) - 1;
        if (leanX == 0 && leanZ == 0) {
            int[] lean = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            leanX = lean[0];
            leanZ = lean[1];
        }
        BlockPos center = startPos;
        for (int y = 0; y < height; y++) {
            if (y > 1 && y % 3 == 0 && random.nextInt(3) == 0) {
                center = center.offset(Integer.signum(leanX), 0, Integer.signum(leanZ));
            }
            BlockPos trunkPos = center.above(y);
            trunkCenters.add(trunkPos);
            int thickness = getThickness(height, y);
            placeTrunkLayer(level, blockSetter, random, trunkPos, config, thickness);
            if (y < 3 && (mediumTree || random.nextBoolean())) {
                placeRootFlare(level, blockSetter, random, trunkPos, config, y, largeTree);
            }
            if (y > 2 && y < height - 2 && random.nextInt(6) == 0) {
                int[] gnarl = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                this.placeLog(level, blockSetter, random, trunkPos.offset(gnarl[0], 0, gnarl[1]), config);
            }
        }
        BlockPos top = trunkCenters.get(trunkCenters.size() - 1);
        foliageAttachments.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, false));
        int totalBranches = Math.max(branchCount.sample(random), mediumTree ? 4 : 3);
        int directionOffset = random.nextInt(DIRECTIONS.length);
        for (int i = 0; i < totalBranches; i++) {
            int branchY = chooseBranchHeight(random, height, i, totalBranches);
            BlockPos connection = trunkCenters.get(Math.min(branchY, trunkCenters.size() - 1));
            int[] direction = DIRECTIONS[(directionOffset + i * 2 + random.nextInt(2)) % DIRECTIONS.length];
            int branchLength = chooseBranchLength(random, height, i);
            BlockPos branchEnd = placeBranch(level, blockSetter, random, connection, config, direction, branchLength, i % 2 == 0);
            foliageAttachments.add(new FoliagePlacer.FoliageAttachment(branchEnd.above(random.nextBoolean() ? 0 : 1), 0, false));
            if (branchLength >= 4) {
                int[] sideDirection = turn(direction, random.nextBoolean());
                BlockPos splitStart = branchEnd.offset(-direction[0], 0, -direction[1]);
                BlockPos splitEnd = placeBranch(level, blockSetter, random, splitStart, config, sideDirection, 2 + random.nextInt(3), random.nextBoolean());
                foliageAttachments.add(new FoliagePlacer.FoliageAttachment(splitEnd.above(), 0, false));
            }
        }
        return foliageAttachments;
    }

    private int getThickness(int height, int y) {
        if (height >= 10 && y < height * 0.45F) {
            return 2;
        }
        if (height >= 7 && y < 2) {
            return 2;
        }
        return 1;
    }

    private void placeTrunkLayer(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos center, TreeConfiguration config, int thickness) {
        this.placeLog(level, blockSetter, random, center, config);
        if (thickness < 2) {
            return;
        }
        this.placeLog(level, blockSetter, random, center.east(), config);
        this.placeLog(level, blockSetter, random, center.south(), config);
        this.placeLog(level, blockSetter, random, center.east().south(), config);
    }

    private void placeRootFlare(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos trunkPos, TreeConfiguration config, int y, boolean largeTree) {
        int flares = largeTree ? 4 : 2 + random.nextInt(2);
        for (int i = 0; i < flares; i++) {
            int[] direction = DIRECTIONS[(i * 2 + random.nextInt(2)) % DIRECTIONS.length];
            int length = y == 0 ? 2 + random.nextInt(largeTree ? 3 : 2) : 1;
            BlockPos root = trunkPos;
            for (int step = 1; step <= length; step++) {
                root = root.offset(direction[0], 0, direction[1]);
                this.placeLog(level, blockSetter, random, root, config);
                if (step == 1 && random.nextBoolean()) {
                    this.placeLog(level, blockSetter, random, root.above(), config);
                }
            }
        }
    }

    private int chooseBranchHeight(RandomSource random, int height, int index, int totalBranches) {
        int min = Math.max(3, height / 3);
        int max = Math.max(min + 1, height - 2);
        int band = Math.max(1, (max - min) / Math.max(1, totalBranches - 1));
        return Math.min(max, min + index * band + random.nextInt(2));
    }

    private int chooseBranchLength(RandomSource random, int height, int index) {
        int base = height >= 10 ? 5 : height >= 7 ? 4 : 3;
        int variation = height >= 10 ? 4 : 3;
        int length = base + random.nextInt(variation);
        return index % 3 == 0 ? length + 1 : length;
    }

    private BlockPos placeBranch(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos start, TreeConfiguration config, int[] direction, int length, boolean rises) {
        BlockPos pos = start;
        for (int step = 1; step <= length; step++) {
            pos = pos.offset(direction[0], 0, direction[1]);
            if (step > 2 && step % 3 == 0) {
                pos = rises ? pos.above() : pos.below();
            }
            this.placeLog(level, blockSetter, random, pos, config);
            if (step > 1 && random.nextInt(5) == 0) {
                int[] side = turn(direction, random.nextBoolean());
                this.placeLog(level, blockSetter, random, pos.offset(side[0], 0, side[1]), config);
            }
        }
        return pos;
    }

    private int[] turn(int[] direction, boolean clockwise) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i][0] == direction[0] && DIRECTIONS[i][1] == direction[1]) {
                return DIRECTIONS[(i + (clockwise ? 1 : DIRECTIONS.length - 1)) % DIRECTIONS.length];
            }
        }
        return direction;
    }
}
