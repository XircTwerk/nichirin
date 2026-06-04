package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinTreeDecoratorTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.HashSet;
import java.util.Set;

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
        BlockState leafState = NichirinBlockRegistry.WISTERIA_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        Set<BlockPos> leaves = new HashSet<>(context.leaves());
        int maxClusters = Math.max(6, leaves.size() / 8);
        int clusters = 0;
        for (BlockPos leafPos : context.leaves()) {
            if (clusters >= maxClusters) {
                break;
            }
            if (!isGoodAnchor(context, leaves, leafPos)) {
                continue;
            }
            RandomSource random = context.random();
            if (random.nextInt(isOuterEdge(leaves, leafPos) ? 3 : 6) != 0) {
                continue;
            }
            int length = minLength + random.nextInt(maxLength - minLength + 1);
            if (!isOuterEdge(leaves, leafPos)) {
                length = Math.max(minLength, length - 2);
            }
            if (placeCurtain(context, leafState, leafPos, length)) {
                clusters++;
            }
        }
    }

    private boolean isGoodAnchor(Context context, Set<BlockPos> leaves, BlockPos leafPos) {
        if (!context.isAir(leafPos.below())) {
            return false;
        }
        return isOuterEdge(leaves, leafPos) || !leaves.contains(leafPos.below(2));
    }

    private boolean isOuterEdge(Set<BlockPos> leaves, BlockPos leafPos) {
        int openSides = 0;
        for (Direction direction : Plane.HORIZONTAL) {
            if (!leaves.contains(leafPos.relative(direction))) {
                openSides++;
            }
        }
        return openSides >= 2;
    }

    private boolean placeCurtain(Context context, BlockState leafState, BlockPos anchor, int length) {
        boolean placed = false;
        RandomSource random = context.random();
        Direction lean = Plane.HORIZONTAL.getRandomDirection(random);
        for (int i = 1; i <= length; i++) {
            BlockPos hangPos = anchor.below(i);
            if (i > 3 && random.nextInt(4) == 0) {
                hangPos = hangPos.relative(lean);
            }
            if (!context.isAir(hangPos)) {
                break;
            }
            context.setBlock(hangPos, leafState);
            placed = true;
            if (i >= 3 && random.nextInt(4) == 0) {
                placeSidePetals(context, leafState, hangPos, i);
            }
            if (i > minLength && random.nextInt(maxLength + 2) < i) {
                break;
            }
        }
        return placed;
    }

    private void placeSidePetals(Context context, BlockState leafState, BlockPos hangPos, int depth) {
        RandomSource random = context.random();
        int attempts = depth > 4 ? 2 : 1;
        for (int i = 0; i < attempts; i++) {
            Direction direction = Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos sidePos = hangPos.relative(direction);
            if (context.isAir(sidePos)) {
                context.setBlock(sidePos, leafState);
            }
        }
    }
}
