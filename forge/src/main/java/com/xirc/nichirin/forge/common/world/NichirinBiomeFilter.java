package com.xirc.nichirin.forge.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class NichirinBiomeFilter extends PlacementFilter {
    public static final NichirinBiomeFilter BEACH_DESERT = new NichirinBiomeFilter(BiomeType.BEACH_DESERT);
    public static final NichirinBiomeFilter MOUNTAIN = new NichirinBiomeFilter(BiomeType.MOUNTAIN);

    public enum BiomeType {
        BEACH_DESERT,
        MOUNTAIN
    }

    private final BiomeType biomeType;

    private NichirinBiomeFilter(BiomeType biomeType) {
        this.biomeType = biomeType;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        Holder<Biome> biome = level.getBiome(pos);

        switch (biomeType) {
            case BEACH_DESERT:
                // Check if it's a beach, river, or desert biome
                return biome.is(Biomes.BEACH) ||
                        biome.is(Biomes.SNOWY_BEACH) ||
                        biome.is(Biomes.STONY_SHORE) ||
                        biome.is(Biomes.RIVER) ||
                        biome.is(Biomes.FROZEN_RIVER) ||
                        biome.is(Biomes.DESERT) ||
                        biome.is(BiomeTags.IS_BEACH) ||
                        biome.is(BiomeTags.IS_RIVER) ||
                        biome.value().getBaseTemperature() > 1.5f; // Hot biomes like desert

            case MOUNTAIN:
                // Check if it's a mountain/hill biome
                return biome.is(Biomes.WINDSWEPT_HILLS) ||
                        biome.is(Biomes.WINDSWEPT_GRAVELLY_HILLS) ||
                        biome.is(Biomes.WINDSWEPT_FOREST) ||
                        biome.is(Biomes.MEADOW) ||
                        biome.is(Biomes.FROZEN_PEAKS) ||
                        biome.is(Biomes.JAGGED_PEAKS) ||
                        biome.is(Biomes.STONY_PEAKS) ||
                        biome.is(Biomes.SNOWY_SLOPES) ||
                        biome.is(BiomeTags.IS_MOUNTAIN) ||
                        biome.is(BiomeTags.IS_HILL);

            default:
                return false;
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        // This would need to be registered properly in a real implementation
        return PlacementModifierType.BIOME_FILTER;
    }
}