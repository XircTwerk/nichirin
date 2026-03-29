package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.particle.*;
import com.xirc.nichirin.common.particle.BloodSplatParticleType;
import com.xirc.nichirin.common.particle.BreathingAuraWispParticleType;
import com.xirc.nichirin.common.particle.SlashImpactSparkParticleType;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

public interface NichirinParticleRegistry {
    DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create("nichirin", Registries.PARTICLE_TYPE);

    RegistrySupplier<ThunderParticleType> THUNDER =
            PARTICLES.register("thunder", ThunderParticleType::new);

    RegistrySupplier<SoundParticleType> SOUND =
            PARTICLES.register("sound", SoundParticleType::new);

    RegistrySupplier<ShockwaveParticleType> SHOCKWAVE =
            PARTICLES.register("shockwave", ShockwaveParticleType::new);

    RegistrySupplier<Flash1ParticleType> FLASH1 =
            PARTICLES.register("flash1", Flash1ParticleType::new);

    RegistrySupplier<Flash2ParticleType> FLASH2 =
            PARTICLES.register("flash2", Flash2ParticleType::new);

    RegistrySupplier<BlueFlash1ParticleType> BLUE_FLASH1 =
            PARTICLES.register("blue_flash1", BlueFlash1ParticleType::new);

    RegistrySupplier<BlueFlash2ParticleType> BLUE_FLASH2 =
            PARTICLES.register("blue_flash2", BlueFlash2ParticleType::new);

    RegistrySupplier<BlueShockwaveParticleType> BLUE_SHOCKWAVE =
            PARTICLES.register("blue_shockwave", BlueShockwaveParticleType::new);

    RegistrySupplier<ButterflyParticleType> BUTTERFLY =
            PARTICLES.register("butterfly", ButterflyParticleType::new);

    RegistrySupplier<BloodSplatParticleType> BLOOD_SPLAT =
            PARTICLES.register("blood_splat", BloodSplatParticleType::new);

    RegistrySupplier<BreathingAuraWispParticleType> BREATHING_AURA_WISP =
            PARTICLES.register("breathing_aura_wisp", BreathingAuraWispParticleType::new);

    RegistrySupplier<SlashImpactSparkParticleType> SLASH_IMPACT_SPARK =
            PARTICLES.register("slash_impact_spark", SlashImpactSparkParticleType::new);

    static void init() {
        PARTICLES.register();
    }
}