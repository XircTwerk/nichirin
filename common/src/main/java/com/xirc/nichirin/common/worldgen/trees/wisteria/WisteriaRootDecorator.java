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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class WisteriaRootDecorator extends TreeDecorator {
    public static final MapCodec<WisteriaRootDecorator> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(decorator -> decorator.probability))
                    .apply(instance, WisteriaRootDecorator::new));
    private final float probability;

    public WisteriaRootDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return NichirinTreeDecoratorTypes.WISTERIA_ROOT_DECORATOR.get();
    }

    @Override
    public void place(Context context) {
        if (context.random().nextFloat() >= this.probability || context.roots().isEmpty()) {
            return;
        }
        BlockPos trunkBase = context.roots().get(0);
        BlockState logState = NichirinBlockRegistry.WISTERIA_LOG.get().defaultBlockState();
        int rootCount = 5 + context.random().nextInt(4);
        int offset = context.random().nextInt(4);
        for (int i = 0; i < rootCount; i++) {
            Direction direction = Direction.from2DDataValue((offset + i) % 4);
            placeRoot(context, logState, trunkBase, direction, 3 + context.random().nextInt(5));
            if (context.random().nextBoolean()) {
                Direction side = context.random().nextBoolean() ? direction.getClockWise() : direction.getCounterClockWise();
                placeRoot(context, logState, trunkBase.relative(direction), side, 1 + context.random().nextInt(3));
            }
        }
    }

    private void placeRoot(Context context, BlockState logState, BlockPos start, Direction direction, int length) {
        BlockPos pos = start;
        RandomSource random = context.random();
        for (int step = 1; step <= length; step++) {
            pos = pos.relative(direction);
            if (!canPlaceRoot(context, pos)) {
                break;
            }
            context.setBlock(pos, logState);
            if (step <= 2 && context.isAir(pos.above())) {
                context.setBlock(pos.above(), logState);
            }
            if (step > 2 && random.nextInt(3) == 0) {
                Direction side = Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos sidePos = pos.relative(side);
                if (canPlaceRoot(context, sidePos)) {
                    context.setBlock(sidePos, logState);
                }
            }
        }
    }

    private boolean canPlaceRoot(Context context, BlockPos pos) {
        return context.isAir(pos) && !context.isAir(pos.below());
    }
}
