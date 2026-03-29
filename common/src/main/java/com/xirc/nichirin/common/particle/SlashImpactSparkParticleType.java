package com.xirc.nichirin.common.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;

public class SlashImpactSparkParticleType extends SimpleParticleType {
    public static final Codec<SimpleParticleType> CODEC = Codec.unit(SlashImpactSparkParticleType::new);

    public SlashImpactSparkParticleType() {
        super(false);
    }

    @Override
    public Codec<SimpleParticleType> codec() {
        return CODEC;
    }
}
