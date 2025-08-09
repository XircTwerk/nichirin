package com.xirc.nichirin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public class SoundParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public SoundParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        return new SoundParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
    }

    public static class SoundParticle extends TextureSheetParticle {
        protected SoundParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);

            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;

            this.lifetime = 40 + this.random.nextInt(20); // 2-3 seconds
            this.hasPhysics = false;
            this.friction = 0.98f;
            this.gravity = -0.03f;

            // Randomly pick one of the sprites (the SpriteSet should contain all 3 note textures)
            this.pickSprite(sprites);

            // Random size and color
            this.quadSize = 0.3f + this.random.nextFloat() * 0.1f;
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setAlpha(0.8f);
        }

        @Override
        public void tick() {
            super.tick();

            // Fade out over time
            this.setAlpha(1.0f - ((float) this.age / (float) this.lifetime));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}