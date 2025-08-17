package com.xirc.nichirin.common.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;

public class ButterflyParticleType extends SimpleParticleType {
    public static final Codec<SimpleParticleType> CODEC = Codec.unit(ButterflyParticleType::new);

    public ButterflyParticleType() {
        super(false);
    }

    @Override
    public Codec<SimpleParticleType> codec() {
        return CODEC;
    }
}