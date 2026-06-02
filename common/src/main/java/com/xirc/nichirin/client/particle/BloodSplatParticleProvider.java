package com.xirc.nichirin.client.particle;

import com.xirc.nichirin.client.config.NichirinClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class BloodSplatParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public BloodSplatParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        if (!NichirinClientConfig.get().visual.enableHitParticles) return null;
        return new BloodSplatParticle(level, x, y, z, sprites);
    }

    public static class BloodSplatParticle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;

        protected BloodSplatParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.animatedSprites = sprites;
            this.lifetime = 6;
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            this.quadSize = 0.4f + this.random.nextFloat() * 0.2f;
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setAlpha(0.95f);
            this.pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            this.pickSprite(this.animatedSprites);
            float progress = (float) this.age / (float) this.lifetime;
            if (progress > 0.5f) {
                this.setAlpha(0.95f * (1.0f - (progress - 0.5f) / 0.5f));
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}