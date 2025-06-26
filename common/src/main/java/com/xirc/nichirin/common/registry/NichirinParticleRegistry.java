package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.common.particle.ThunderParticleType;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

public class NichirinParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create("nichirin", Registries.PARTICLE_TYPE);

    public static final RegistrySupplier<ThunderParticleType> THUNDER =
            PARTICLES.register("thunder", ThunderParticleType::new);

    public static void init() {
        PARTICLES.register();
    }
}