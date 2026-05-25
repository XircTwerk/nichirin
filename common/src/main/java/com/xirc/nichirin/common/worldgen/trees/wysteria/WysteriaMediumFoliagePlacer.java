package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xirc.nichirin.registry.NichirinFoliagePlacerTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public class WysteriaMediumFoliagePlacer extends FoliagePlacer {
    public static final Codec<WysteriaMediumFoliagePlacer> CODEC = RecordCodecBuilder.create((instance) ->
            foliagePlacerParts(instance).apply(instance, WysteriaMediumFoliagePlacer::new));

    public WysteriaMediumFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return NichirinFoliagePlacerTypes.WYSTERIA_MEDIUM_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random,
                                 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {
        BlockPos center = attachment.pos();

        // REDUCED: Create medium umbrella-shaped canopy, less dense
        for (int y = 0; y <= 2; y++) { // REDUCED: from 3 to 2
            int radius = switch(y) {
                case 0 -> 1;
                case 1 -> 2;    // REDUCED: from 3 to 2
                case 2 -> 1;    // REDUCED: from 2 to 1
                default -> 1;
            };

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);

                    // Sparse placement with natural gaps
                    if (distance <= radius && (distance <= radius - 1 || random.nextInt(4) != 0)) {
                        // Add randomness for natural look
                        if (random.nextInt(6) != 0) { // 83% chance to place
                            BlockPos leafPos = center.offset(x, y, z);
                            foliageSetter.set(leafPos, config.foliageProvider.getState(random, leafPos));
                        }
                    }
                }
            }
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 3;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}
