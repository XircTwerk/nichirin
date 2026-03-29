package com.xirc.nichirin.common.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;

public class BreathingAuraWispParticleType extends SimpleParticleType {
    public static final Codec<SimpleParticleType> CODEC = Codec.unit(BreathingAuraWispParticleType::new);

    public BreathingAuraWispParticleType() {
        super(false);
    }

    @Override
    public Codec<SimpleParticleType> codec() {
        return CODEC;
    }
}
