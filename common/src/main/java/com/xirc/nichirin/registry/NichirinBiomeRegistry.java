package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public interface NichirinBiomeRegistry {
    ResourceKey<Biome> WISTERIA_GROVE = ResourceKey.create(
            Registries.BIOME,
            BreathOfNichirin.id("wisteria_grove"));
}
