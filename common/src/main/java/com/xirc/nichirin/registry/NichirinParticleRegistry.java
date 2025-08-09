package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.particle.Flash1ParticleType;
import com.xirc.nichirin.common.particle.ShockwaveParticleType;
import com.xirc.nichirin.common.particle.SoundParticleType;
import com.xirc.nichirin.common.particle.ThunderParticleType;
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

    static void init() {
        PARTICLES.register();
    }
}