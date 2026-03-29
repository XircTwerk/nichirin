package com.xirc.nichirin.common.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;

public class BloodSplatParticleType extends SimpleParticleType {
    public static final Codec<SimpleParticleType> CODEC = Codec.unit(BloodSplatParticleType::new);

    public BloodSplatParticleType() {
        super(false);
    }

    @Override
    public Codec<SimpleParticleType> codec() {
        return CODEC;
    }
}
