package com.xirc.nichirin.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.SimpleParticleType;

public class BloodSplatParticleType extends SimpleParticleType {
    public static final MapCodec<SimpleParticleType> CODEC = MapCodec.unit(BloodSplatParticleType::new);

    public BloodSplatParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return CODEC;
    }
}