package com.xirc.nichirin.client.particle;

import com.xirc.nichirin.client.config.NichirinClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class SlashImpactSparkParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public SlashImpactSparkParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        if (!NichirinClientConfig.get().visual.enableHitParticles) return null;
        return new SlashImpactSparkParticle(level, x, y, z, sprites);
    }

    public static class SlashImpactSparkParticle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;

        protected SlashImpactSparkParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.animatedSprites = sprites;
            this.lifetime = 8;
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            this.quadSize = 0.8f + this.random.nextFloat() * 0.4f;
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setAlpha(1.0f);
            this.pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            this.pickSprite(this.animatedSprites);
            float progress = (float) this.age / (float) this.lifetime;
            this.setAlpha(1.0f - progress);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}