package com.xirc.nichirin.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.SimpleParticleType;

public class BlueShockwaveParticleType extends SimpleParticleType {
    public static final MapCodec<SimpleParticleType> CODEC = MapCodec.unit(BlueShockwaveParticleType::new);

    public BlueShockwaveParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return CODEC;
    }
}