package com.xirc.nichirin.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BreathingAuraWispParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    // Set immediately before addParticle to pass outward lateral drift direction.
    // Safe since particle creation is synchronous on the main thread.
    public static double pendingLateralX = 0.0;
    public static double pendingLateralZ = 0.0;
    // True for the particle that should be horizontally mirrored (left nostril).
    public static boolean pendingMirrored = false;

    public BreathingAuraWispParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        // xSpeed/ySpeed/zSpeed are repurposed as r/g/b color (0.0-1.0)
        double lx = pendingLateralX;
        double lz = pendingLateralZ;
        boolean mirrored = pendingMirrored;
        pendingLateralX = 0.0;
        pendingLateralZ = 0.0;
        pendingMirrored = false;
        return new BreathingAuraWispParticle(level, x, y, z, (float) xSpeed, (float) ySpeed, (float) zSpeed, lx, lz, mirrored, sprites);
    }

    public static class BreathingAuraWispParticle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;
        private final boolean mirrored;

        protected BreathingAuraWispParticle(ClientLevel level, double x, double y, double z,
                                            float r, float g, float b, double lateralX, double lateralZ,
                                            boolean mirrored, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.animatedSprites = sprites;
            this.mirrored = mirrored;
            this.lifetime = 10;
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;
            this.xd = (this.random.nextDouble() - 0.5) * 0.006 + lateralX;
            this.yd = 0.003 + this.random.nextDouble() * 0.003;
            this.zd = (this.random.nextDouble() - 0.5) * 0.006 + lateralZ;
            this.quadSize = 0.045f + this.random.nextFloat() * 0.02f;
            this.setColor(r, g, b);
            this.setAlpha(0.7f);
            this.setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            this.setSpriteFromAge(this.animatedSprites);
            float progress = (float) this.age / (float) this.lifetime;
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
        public void render(VertexConsumer buffer, Camera camera, float partialTick) {
            if (!mirrored) {
                super.render(buffer, camera, partialTick);
                return;
            }
            // Horizontal mirror: swap u0/u1 so the sprite is flipped left-right
            net.minecraft.world.phys.Vec3 cameraPos = camera.getPosition();
            float px = (float)(Mth.lerp(partialTick, xo, x) - cameraPos.x());
            float py = (float)(Mth.lerp(partialTick, yo, y) - cameraPos.y());
            float pz = (float)(Mth.lerp(partialTick, zo, z) - cameraPos.z());

            Quaternionf q = new Quaternionf(camera.rotation());
            if (roll != 0.0f) q.rotateZ(Mth.lerp(partialTick, oRoll, roll));

            float size = getQuadSize(partialTick);
            Vector3f[] corners = {
                new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0),
                new Vector3f(1,  1, 0),  new Vector3f(1, -1, 0)
            };
            for (Vector3f c : corners) { c.rotate(q); c.mul(size); c.add(px, py, pz); }

            float u0 = getU1(); // swapped for horizontal flip
            float u1 = getU0();
            float v0 = getV0();
            float v1 = getV1();
            int light = getLightColor(partialTick);

            buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
            buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
            buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
            buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
