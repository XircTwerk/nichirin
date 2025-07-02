package com.xirc.nichirin.fabric.common.terrablender;

import com.xirc.nichirin.common.registry.OreRegistry;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class MaterialRulesFabric {

    private static final SurfaceRules.RuleSource SCARLET_CRIMSON_IRON_SAND = makeStateRule(OreRegistry.SCARLET_CRIMSON_IRON_SAND.get());
    private static final SurfaceRules.RuleSource SCARLET_ORE = makeStateRule(OreRegistry.SCARLET_ORE.get());
    private static final SurfaceRules.RuleSource SAND = makeStateRule(Blocks.SAND);
    private static final SurfaceRules.RuleSource STONE = makeStateRule(Blocks.STONE);


    public static SurfaceRules.RuleSource makeRules() {
        System.out.println("[Nichirin] Creating surface rules for ore generation");


        // Create Y-level conditions
        SurfaceRules.ConditionSource above60 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(60), 0);
        SurfaceRules.ConditionSource above80 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(80), 0);

        // Use noise to create scattered placement
        SurfaceRules.ConditionSource scarletSandNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, -0.05, 0.05);
        SurfaceRules.ConditionSource scarletOreNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.2, 0.3);

        // Beach/river/desert biomes
        SurfaceRules.ConditionSource sandBiomes = SurfaceRules.isBiome(
                Biomes.BEACH,
                Biomes.SNOWY_BEACH,
                Biomes.STONY_SHORE,
                Biomes.RIVER,
                Biomes.FROZEN_RIVER,
                Biomes.DESERT
        );

        // Mountain biomes
        SurfaceRules.ConditionSource mountainBiomes = SurfaceRules.isBiome(
                Biomes.WINDSWEPT_HILLS,
                Biomes.WINDSWEPT_GRAVELLY_HILLS,
                Biomes.WINDSWEPT_FOREST,
                Biomes.MEADOW,
                Biomes.FROZEN_PEAKS,
                Biomes.JAGGED_PEAKS,
                Biomes.STONY_PEAKS,
                Biomes.SNOWY_SLOPES
        );

        return SurfaceRules.sequence(
                // Scarlet Crimson Iron Sand - patches in sand areas
                SurfaceRules.ifTrue(
                        sandBiomes,
                        SurfaceRules.ifTrue(
                                above60,
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(-1, 0), // Not underwater
                                                SurfaceRules.ifTrue(
                                                        scarletSandNoise,
                                                        SurfaceRules.sequence(
                                                                // Replace some sand with scarlet sand
                                                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SAND),
                                                                SCARLET_CRIMSON_IRON_SAND
                                                        )
                                                )
                                        )
                                )
                        )
                ),

                // Scarlet Ore - exposed patches in mountains
                SurfaceRules.ifTrue(
                        mountainBiomes,
                        SurfaceRules.ifTrue(
                                above80,
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(-1, 0), // Not underwater
                                                SurfaceRules.ifTrue(
                                                        scarletOreNoise,
                                                        SurfaceRules.sequence(
                                                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, STONE),
                                                                SCARLET_ORE
                                                        )
                                                )
                                        )
                                )
                        )
                )

        );
    }


    private static SurfaceRules.RuleSource makeStateRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}