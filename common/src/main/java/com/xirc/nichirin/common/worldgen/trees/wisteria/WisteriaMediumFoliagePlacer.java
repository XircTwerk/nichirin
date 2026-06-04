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

public class WisteriaMediumFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<WisteriaMediumFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            foliagePlacerParts(instance).apply(instance, WisteriaMediumFoliagePlacer::new));

    public WisteriaMediumFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return NichirinFoliagePlacerTypes.WISTERIA_MEDIUM_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random, TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        BlockPos center = attachment.pos();
        int stretchX = random.nextBoolean() ? 1 : 0;
        int stretchZ = stretchX == 0 ? 1 : 0;
        placeLayer(level, foliageSetter, random, config, center.below(2), foliageRadius + stretchX, foliageRadius + stretchZ, 0.42F, true);
        placeLayer(level, foliageSetter, random, config, center.below(), foliageRadius + stretchX, foliageRadius + stretchZ, 0.62F, false);
        placeLayer(level, foliageSetter, random, config, center, foliageRadius + 1 + stretchX, foliageRadius + stretchZ, 0.68F, false);
        placeLayer(level, foliageSetter, random, config, center.above(), foliageRadius + stretchX, foliageRadius + stretchZ, 0.46F, false);
    }

    private void placeLayer(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random, TreeConfiguration config, BlockPos center, int radiusX, int radiusZ, float fill, boolean underside) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double shape = (x * x) / (double) (radiusX * radiusX) + (z * z) / (double) (radiusZ * radiusZ);
                if (shape > 1.0D || random.nextFloat() > fill) {
                    continue;
                }
                if (shape > 0.6D && random.nextInt(2) == 0) {
                    continue;
                }
                if (underside && shape < 0.28D && random.nextInt(2) == 0) {
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
        return 4;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}
