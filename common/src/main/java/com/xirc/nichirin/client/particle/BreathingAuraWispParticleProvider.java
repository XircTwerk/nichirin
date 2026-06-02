package com.xirc.nichirin.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import net.minecraft.world.phys.Vec3;

public class BreathingAuraWispParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    // Set immediately before addParticle to pass context. Safe: particle creation is synchronous on main thread.
    public static double pendingLateralX = 0.0;
    public static double pendingLateralZ = 0.0;
    public static boolean pendingMirrored = false;
    // Lateral offset from center (in world units) so we can re-compute face position each tick.
    public static double pendingOffsetX = 0.0;
    public static double pendingOffsetZ = 0.0;

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
        double ox = pendingOffsetX;
        double oz = pendingOffsetZ;
        pendingLateralX = 0.0;
        pendingLateralZ = 0.0;
        pendingMirrored = false;
        pendingOffsetX = 0.0;
        pendingOffsetZ = 0.0;
        return new BreathingAuraWispParticle(level, x, y, z, (float) xSpeed, (float) ySpeed, (float) zSpeed, lx, lz, ox, oz, mirrored, sprites);
    }

    public static class BreathingAuraWispParticle extends TextureSheetParticle {
        private final SpriteSet animatedSprites;
        private final boolean mirrored;
        // Lateral offset from center at spawn time, to re-anchor to player face each tick
        private final double offsetX;
        private final double offsetZ;
        // Accumulated drift relative to anchor
        private double driftX;
        private double driftY;
        private double driftZ;

        protected BreathingAuraWispParticle(ClientLevel level, double x, double y, double z,
                                            float r, float g, float b, double lateralX, double lateralZ,
                                            double offsetX, double offsetZ,
                                            boolean mirrored, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.animatedSprites = sprites;
            this.mirrored = mirrored;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.lifetime = 12;
            this.hasPhysics = false;
            this.friction = 1.0f;
            this.gravity = 0.0f;
            this.xd = (this.random.nextDouble() - 0.5) * 0.006 + lateralX;
            this.yd = 0.003 + this.random.nextDouble() * 0.003;
            this.zd = (this.random.nextDouble() - 0.5) * 0.006 + lateralZ;
            this.driftX = 0;
            this.driftY = 0;
            this.driftZ = 0;
            this.quadSize = 0.045f + this.random.nextFloat() * 0.02f;
            this.setColor(r, g, b);
            this.setAlpha(0.7f);
            this.setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.setSpriteFromAge(this.animatedSprites);

            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            // Accumulate drift and re-anchor to player face position
            this.driftX += this.xd;
            this.driftY += this.yd;
            this.driftZ += this.zd;

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                float yaw = player.getYRot() * ((float) Math.PI / 180f);
                double fwdX = -Math.sin(yaw);
                double fwdZ =  Math.cos(yaw);
                double forward = 0.28;
                double anchorX = player.getX() + fwdX * forward + offsetX;
                double anchorY = player.getEyeY() - 0.10;
                double anchorZ = player.getZ() + fwdZ * forward + offsetZ;
                this.x = anchorX + driftX;
                this.y = anchorY + driftY;
                this.z = anchorZ + driftZ;
            } else {
                this.x += this.xd;
                this.y += this.yd;
                this.z += this.zd;
            }
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
            Vec3 cameraPos = camera.getPosition();
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

            buffer.addVertex(corners[0].x(), corners[0].y(), corners[0].z()).setUv(u1, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(corners[1].x(), corners[1].y(), corners[1].z()).setUv(u1, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(corners[2].x(), corners[2].y(), corners[2].z()).setUv(u0, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(corners[3].x(), corners[3].y(), corners[3].z()).setUv(u0, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}