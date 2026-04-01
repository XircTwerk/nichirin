package com.xirc.nichirin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class BreathingAuraWispParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public BreathingAuraWispParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        // xSpeed/ySpeed/zSpeed are repurposed as r/g/b color (0.0-1.0)
        return new BreathingAuraWispParticle(level, x, y, z, (float) xSpeed, (float) ySpeed, (float) zSpeed, sprites);
    }

    public static class BreathingAuraWispParticle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;

        protected BreathingAuraWispParticle(ClientLevel level, double x, double y, double z,
                                            float r, float g, float b, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.animatedSprites = sprites;
            this.lifetime = 10;
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;
            // slow upward float with tiny random drift
            this.xd = (this.random.nextDouble() - 0.5) * 0.006;
            this.yd = 0.003 + this.random.nextDouble() * 0.003;
            this.zd = (this.random.nextDouble() - 0.5) * 0.006;
            this.quadSize = 0.045f + this.random.nextFloat() * 0.02f;
            this.setColor(r, g, b);
            this.setAlpha(0.7f);
            this.pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            this.pickSprite(this.animatedSprites);
            float progress = (float) this.age / (float) this.lifetime;
            // fade in quickly, hold, fade out
            float alpha;
            if (progress < 0.2f) {
                alpha = 0.7f * (progress / 0.2f);
            } else if (progress > 0.7f) {
                alpha = 0.7f * (1.0f - (progress - 0.7f) / 0.3f);
            } else {
                alpha = 0.7f;
            }
            this.setAlpha(alpha);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
