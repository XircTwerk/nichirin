package com.xirc.nichirin.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.SimpleParticleType;

public class BlueFlash2ParticleType extends SimpleParticleType {
    public static final MapCodec<SimpleParticleType> CODEC = MapCodec.unit(BlueFlash2ParticleType::new);

    public BlueFlash2ParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return CODEC;
    }
}