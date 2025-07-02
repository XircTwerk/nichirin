package com.xirc.nichirin.forge.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

public class NichirinBiomeModifications {
    public static final ResourceKey<BiomeModifier> ADD_ORES = ResourceKey.create(
            ForgeRegistries.Keys.BIOME_MODIFIERS,
            new ResourceLocation(BreathOfNichirin.MOD_ID, "add_ores")
    );

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_ORES, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(NichirinPlacedFeatures.SCARLET_CRIMSON_IRON_SAND),
                        placedFeatures.getOrThrow(NichirinPlacedFeatures.SCARLET_ORE)
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
    }
}