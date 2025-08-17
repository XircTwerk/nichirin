package com.xirc.nichirin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class ButterflyParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public ButterflyParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        return new ButterflyParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
    }

    public static class ButterflyParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        protected ButterflyParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);

            this.sprites = sprites;

            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;

            this.lifetime = 20 + this.random.nextInt(20); // 1-2 seconds
            this.hasPhysics = false;

            // Random size and color
            this.quadSize = 0.3f + this.random.nextFloat() * 0.1f;
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setAlpha(0.8f);

            // IMPORTANT: Set the sprite from the sprite set
            this.pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();

            // Fade out over time
            this.setAlpha(1.0f - ((float) this.age / (float) this.lifetime));

            // Optional: Change sprite over time for animation
            if (this.age % 5 == 0) {
                this.pickSprite(sprites);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}