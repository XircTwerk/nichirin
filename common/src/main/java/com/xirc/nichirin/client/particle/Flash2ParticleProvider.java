package com.xirc.nichirin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class Flash2ParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public Flash2ParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        return new Flash2Particle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
    }

    public static class Flash2Particle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;

        protected Flash2Particle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);

            this.animatedSprites = sprites;
            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;

            this.lifetime = 6; //Lifespan in ticks
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;

            // Start with first frame
            this.setSpriteFromAge(sprites);

            this.quadSize = 1.0f + this.random.nextFloat() * 0.5f; // Random size
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setAlpha(0.8f);
        }

        @Override
        public void tick() {
            super.tick();

            // Update sprite based on age (cycles through the 6 textures)
            this.setSpriteFromAge(this.animatedSprites);

            // Optional: Scale up over time
            float progress = (float) this.age / (float) this.lifetime;
            this.quadSize = (1.0f + progress * 2.0f) + this.random.nextFloat() * 0.5f;

            // Fade out towards the end
            if (progress > 0.7f) {
                float fadeProgress = (progress - 0.7f) / 0.3f;
                this.setAlpha(0.8f * (1.0f - fadeProgress));
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}