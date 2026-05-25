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

public class WysteriaLargeFoliagePlacer extends FoliagePlacer {
    public static final Codec<WysteriaLargeFoliagePlacer> CODEC = RecordCodecBuilder.create((instance) ->
            foliagePlacerParts(instance).apply(instance, WysteriaLargeFoliagePlacer::new));

    public WysteriaLargeFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return NichirinFoliagePlacerTypes.WYSTERIA_LARGE_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter foliageSetter, RandomSource random,
                                 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {
        BlockPos center = attachment.pos();

        // REDUCED: Create sparser, more natural canopy
        for (int y = -1; y <= 2; y++) { // REDUCED: Height from 3 to 2
            int radius = switch(y) {
                case -1 -> 1;
                case 0 -> 2;    // REDUCED: from 3 to 2
                case 1 -> 3;    // REDUCED: from 4 to 3
                case 2 -> 2;    // REDUCED: from 4 to 2
                default -> 1;
            };

            // Create irregular, naturalistic shape with more gaps
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);

                    // REDUCED: More sparse with irregular edges and more randomness
                    boolean shouldPlace = false;
                    if (distance <= radius - 1) {
                        shouldPlace = true; // Always place in core
                    } else if (distance <= radius) {
                        shouldPlace = random.nextInt(4) != 0; // 75% chance at edge (was 67%)
                    }

                    // Add more randomness to create natural gaps
                    if (shouldPlace && distance > 0.5 && random.nextInt(5) == 0) {
                        shouldPlace = false; // 20% chance to skip any leaf for natural gaps
                    }

                    if (shouldPlace) {
                        BlockPos leafPos = center.offset(x, y, z);
                        foliageSetter.set(leafPos, config.foliageProvider.getState(random, leafPos));
                    }
                }
            }
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 4; // REDUCED: from 5 to 4
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}