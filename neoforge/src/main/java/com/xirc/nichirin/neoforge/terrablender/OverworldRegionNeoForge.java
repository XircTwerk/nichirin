package com.xirc.nichirin.neoforge.terrablender;

import com.mojang.datafixers.util.Pair;
import com.xirc.nichirin.registry.NichirinBiomeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class OverworldRegionNeoForge extends Region {
    public OverworldRegionNeoForge(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addBiome(
                mapper,
                ParameterUtils.Temperature.NEUTRAL,
                ParameterUtils.Humidity.HUMID,
                ParameterUtils.Continentalness.MID_INLAND,
                ParameterUtils.Erosion.EROSION_4,
                ParameterUtils.Weirdness.MID_SLICE_NORMAL_ASCENDING,
                ParameterUtils.Depth.SURFACE,
                0.0F,
                NichirinBiomeRegistry.WISTERIA_GROVE);

        addBiome(
                mapper,
                ParameterUtils.Temperature.WARM,
                ParameterUtils.Humidity.WET,
                ParameterUtils.Continentalness.NEAR_INLAND,
                ParameterUtils.Erosion.EROSION_3,
                ParameterUtils.Weirdness.MID_SLICE_VARIANT_ASCENDING,
                ParameterUtils.Depth.SURFACE,
                0.0F,
                NichirinBiomeRegistry.WISTERIA_GROVE);
    }
}
