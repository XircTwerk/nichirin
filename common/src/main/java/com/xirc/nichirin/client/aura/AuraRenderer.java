package com.xirc.nichirin.client.aura;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.common.aura.AuraInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;

/**
 * Renders all active auras for entities currently in the client level.
 *
 * Implementation: deforms a CPU-side UV sphere mesh per frame with a sum-of-sines
 * displacement (so the silhouette is lobed/wavy like the reference image) and writes
 * it into MC's translucent buffer with view-dependent alpha (cheap rim glow via
 * dot(normal, view)).
 *
 * No custom shader programs / framebuffers — that's the planned follow-up for the
 * proper Mandelbulb+pixelization pipeline. This renderer ships a working visible
 * baseline that you can iterate on.
 */
@Environment(EnvType.CLIENT)
public final class AuraRenderer {

    private AuraRenderer() {}

    /** Called from LevelRendererMixin TAIL after the level has rendered. */
    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick,
                                 Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (EntityAuraTracker.all().isEmpty()) return;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Vec3 camPos = camera.getPosition();
        long nowMs = System.currentTimeMillis();

        for (var entry : EntityAuraTracker.all().entrySet()) {
            UUID entityId = entry.getKey();
            List<AuraInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            Entity host = findEntity(mc, entityId);
            if (host == null) continue;

            double ex = host.xo + (host.getX() - host.xo) * partialTick;
            double ey = host.yo + (host.getY() - host.yo) * partialTick + host.getBbHeight() * 0.5;
            double ez = host.zo + (host.getZ() - host.zo) * partialTick;

            poseStack.pushPose();
            poseStack.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);

            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(
                    ResourceLocation.withDefaultNamespace("textures/misc/white.png")));

            for (AuraInstance instance : instances) {
                renderInstance(poseStack, vc, instance, nowMs, camPos, ex, ey, ez);
            }

            poseStack.popPose();
        }

        RenderSystem.disableCull();
        buffers.endBatch(RenderType.entityTranslucent(
                ResourceLocation.withDefaultNamespace("textures/misc/white.png")));
        RenderSystem.enableCull();

        if (AuraDebugState.overlayEnabled) {
            AuraDebugRenderer.renderAll(poseStack, camera, partialTick);
        }
    }

    private static Entity findEntity(Minecraft mc, UUID id) {
        if (mc.player != null && mc.player.getUUID().equals(id)) return mc.player;
        if (mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) return e;
        }
        return null;
    }

    private static void renderInstance(PoseStack poseStack, VertexConsumer vc,
                                       AuraInstance inst, long nowMs,
                                       Vec3 camPos, double ex, double ey, double ez) {
        float t = ((nowMs - inst.startTimeMs()) / 1000.0f) * AuraConfig.animationSpeed;

        // Pulse drives radius + waviness so the silhouette morphs as it breathes.
        float pulse = (float) (Math.sin(t * inst.pulseSpeed() * Math.PI * 2.0) * 0.5 + 0.5);
        float radius = inst.radius() * (1.0f + AuraConfig.pulseAmplitude * pulse);
        float waveStrength = inst.distortionStrength() * (AuraConfig.waveBase + pulse * AuraConfig.waveAnimAmplitude);
        float yaw = t * inst.rotationSpeed();

        int meridians = AuraSphereMesh.meridians();
        int rings = AuraSphereMesh.rings();
        float[] pos = AuraSphereMesh.POSITIONS;

        // Pre-deform every vertex into a working buffer so quads can share edges.
        float[] deformed = new float[pos.length];
        for (int v = 0; v < pos.length; v += 3) {
            float x = pos[v];
            float y = pos[v + 1];
            float z = pos[v + 2];
            // Rotate around Y so the lobe pattern slowly orbits the host.
            float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
            float rx = x * cy + z * sy;
            float rz = -x * sy + z * cy;
            // Sum-of-sines displacement → lobed silhouette like the reference image.
            float theta = (float) Math.acos(Math.max(-1f, Math.min(1f, y)));
            float phi = (float) Math.atan2(rz, rx);
            float bulge =
                      (float) Math.sin(phi * AuraConfig.lobeCountLow  + t * 0.6) * 0.6f
                    + (float) Math.cos(theta * AuraConfig.lobeCountHigh + t * 0.4) * 0.4f;
            float disp = radius * (1.0f + waveStrength * 0.18f * bulge);
            deformed[v    ] = rx * disp;
            deformed[v + 1] =  y * disp;
            deformed[v + 2] = rz * disp;
        }

        Matrix4f mat = poseStack.last().pose();
        // Clamp after brightness — channels > 1.0 wrap around in setColor's byte cast.
        float r = Math.min(1.0f, inst.r() * AuraConfig.brightness);
        float g = Math.min(1.0f, inst.g() * AuraConfig.brightness);
        float b = Math.min(1.0f, inst.b() * AuraConfig.brightness);
        float baseA = Math.min(1.0f, inst.a() * AuraConfig.opacityMultiplier);
        int packedLight = LightTexture.FULL_BRIGHT;

        for (int ring = 0; ring < rings - 1; ring++) {
            for (int m = 0; m < meridians - 1; m++) {
                int i00 = (ring     * meridians + m    ) * 3;
                int i10 = (ring     * meridians + m + 1) * 3;
                int i11 = ((ring+1) * meridians + m + 1) * 3;
                int i01 = ((ring+1) * meridians + m    ) * 3;
                emitQuad(vc, mat, deformed, i00, i10, i11, i01,
                        r, g, b, baseA, camPos, ex, ey, ez, packedLight);
            }
        }
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f mat, float[] d,
                                 int a, int b2, int c, int d2,
                                 float r, float g, float bl, float baseA,
                                 Vec3 camPos, double ex, double ey, double ez,
                                 int packedLight) {
        // Cheap rim: alpha scales with (1 - |dot(facing, view)|) so silhouette edges glow.
        float cx = (d[a] + d[b2] + d[c] + d[d2]) * 0.25f;
        float cy = (d[a+1] + d[b2+1] + d[c+1] + d[d2+1]) * 0.25f;
        float cz = (d[a+2] + d[b2+2] + d[c+2] + d[d2+2]) * 0.25f;
        float nlen = (float) Math.sqrt(cx*cx + cy*cy + cz*cz);
        if (nlen < 1e-4f) nlen = 1e-4f;
        float nx = cx / nlen, ny = cy / nlen, nz = cz / nlen;

        double vx = (ex + cx) - camPos.x;
        double vy = (ey + cy) - camPos.y;
        double vz = (ez + cz) - camPos.z;
        double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (vlen < 1e-4) vlen = 1e-4;
        float vxn = (float) (vx / vlen), vyn = (float) (vy / vlen), vzn = (float) (vz / vlen);

        float facing = Math.abs(nx*vxn + ny*vyn + nz*vzn);
        float rim = 1.0f - facing;
        float alpha = baseA * (0.35f + rim * 0.65f);
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        vert(vc, mat, d[a],   d[a+1],   d[a+2],   r, g, bl, alpha, packedLight);
        vert(vc, mat, d[b2],  d[b2+1],  d[b2+2],  r, g, bl, alpha, packedLight);
        vert(vc, mat, d[c],   d[c+1],   d[c+2],   r, g, bl, alpha, packedLight);
        vert(vc, mat, d[d2],  d[d2+1],  d[d2+2],  r, g, bl, alpha, packedLight);
    }

    private static void vert(VertexConsumer vc, Matrix4f mat,
                             float x, float y, float z,
                             float r, float g, float b, float a, int packedLight) {
        vc.addVertex(mat, x, y, z)
          .setColor(r, g, b, a)
          .setUv(0.5f, 0.5f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(packedLight)
          .setNormal(0f, 1f, 0f);
    }
}
