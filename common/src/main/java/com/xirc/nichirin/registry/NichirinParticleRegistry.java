package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.particle.*;
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

    static void init() {
        PARTICLES.register();
    }
}