package com.xirc.nichirin.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.SimpleParticleType;

public class ButterflyParticleType extends SimpleParticleType {
    public static final MapCodec<SimpleParticleType> CODEC = MapCodec.unit(ButterflyParticleType::new);

    public ButterflyParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return CODEC;
    }
}