package com.xirc.nichirin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class ThunderParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public ThunderParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        return new ThunderParticleInstance(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
    }

    public static class ThunderParticleInstance extends TextureSheetParticle {
        private final SpriteSet sprites;

        protected ThunderParticleInstance(ClientLevel level, double x, double y, double z,
                                          double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.sprites = sprites;

            // Thunder particle properties
            this.lifetime = 20 + this.random.nextInt(20); // 1-2 seconds
            this.quadSize = 0.15f + this.random.nextFloat() * 0.15f; // Random size
            this.hasPhysics = false; // No gravity/collision

            // Thunder-like movement (erratic)
            this.xd = (this.random.nextDouble() - 0.5) * 0.1;
            this.yd = (this.random.nextDouble() - 0.5) * 0.1;
            this.zd = (this.random.nextDouble() - 0.5) * 0.1;

            // Bright white/blue color with alpha
            this.rCol = 0.9f + this.random.nextFloat() * 0.1f;
            this.gCol = 0.9f + this.random.nextFloat() * 0.1f;
            this.bCol = 1.0f;
            this.alpha = 0.8f;

            this.pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();

            // Fade out over time
            this.alpha = 1.0f - ((float)this.age / (float)this.lifetime);

            // Erratic movement like lightning
            if (this.age % 3 == 0) {
                this.xd += (this.random.nextDouble() - 0.5) * 0.02;
                this.yd += (this.random.nextDouble() - 0.5) * 0.02;
                this.zd += (this.random.nextDouble() - 0.5) * 0.02;
            }

            // Update sprite
            this.setSpriteFromAge(this.sprites);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}