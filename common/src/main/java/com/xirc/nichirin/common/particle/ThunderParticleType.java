package com.xirc.nichirin.common.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;

public class ThunderParticleType extends SimpleParticleType {
    public static final Codec<SimpleParticleType> CODEC = Codec.unit(ThunderParticleType::new);

    public ThunderParticleType() {
        super(false);
    }

    @Override
    public Codec<SimpleParticleType> codec() {
        return CODEC;
    }
}