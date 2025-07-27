package com.xirc.nichirin.registry;

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

    static void init() {
        PARTICLES.register();
    }
}