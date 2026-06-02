package com.xirc.nichirin.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.SimpleParticleType;

public class BlueFlash1ParticleType extends SimpleParticleType {
    public static final MapCodec<SimpleParticleType> CODEC = MapCodec.unit(BlueFlash1ParticleType::new);

    public BlueFlash1ParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return CODEC;
    }
}