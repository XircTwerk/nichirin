package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinFoliagePlacerTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public class WisteriaLargeFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<WisteriaLargeFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            foliagePlacerParts(instance).apply(instance, WisteriaLargeFoliagePlacer::new));

    public WisteriaLargeFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return NichirinFoliagePlacerTypes.WISTERIA_LARGE_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random, TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        BlockPos center = attachment.pos();
        int stretchX = random.nextBoolean() ? 2 : 0;
        int stretchZ = stretchX == 0 ? 2 : 0;
        placeLayer(level, foliageSetter, random, config, center.below(2), foliageRadius, foliageRadius, 0.36F, true);
        placeLayer(level, foliageSetter, random, config, center.below(), foliageRadius + 1 + stretchX, foliageRadius + stretchZ, 0.56F, true);
        placeLayer(level, foliageSetter, random, config, center, foliageRadius + 2 + stretchX, foliageRadius + 1 + stretchZ, 0.64F, false);
        placeLayer(level, foliageSetter, random, config, center.above(), foliageRadius + 1, foliageRadius, 0.48F, false);
        placeLayer(level, foliageSetter, random, config, center.above(2), foliageRadius - 1, foliageRadius - 1, 0.3F, false);
    }

    private void placeLayer(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random, TreeConfiguration config, BlockPos center, int radiusX, int radiusZ, float fill, boolean underside) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double shape = (x * x) / (double) (radiusX * radiusX) + (z * z) / (double) (radiusZ * radiusZ);
                if (shape > 1.0D || random.nextFloat() > fill) {
                    continue;
                }
                if (shape > 0.55D && random.nextInt(2) == 0) {
                    continue;
                }
                if (underside && shape < 0.35D && random.nextInt(2) == 0) {
                    continue;
                }
                BlockPos leafPos = center.offset(x, 0, z);
                if (level.isStateAtPosition(leafPos, state -> state.canBeReplaced())) {
                    foliageSetter.set(leafPos, config.foliageProvider.getState(random, leafPos));
                }
            }
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 5;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}
