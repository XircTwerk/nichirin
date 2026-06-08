package com.xirc.nichirin.client.aura;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.common.aura.AuraInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * The aura renderer — draws each aura as a flat NxN grid of camera-aligned colored quads
 * (one "pixel" per cell) positioned at the host entity's world centre.
 *
 * The grid uses a vertical billboard: it rotates around world-Y to face the camera
 * horizontally, but does NOT tilt with head pitch (so looking down at your feet doesn't
 * make the disc face you edge-on). Same screen position as the entity, but depth-tested
 * against world geometry so it can't clip through walls.
 *
 * The disc renders with a no-depth-write translucent type so the entity body always
 * reads above it via the standard LEQUAL depth test.
 */
@Environment(EnvType.CLIENT)
public final class AuraPixelize2DRenderer {

    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private AuraPixelize2DRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (EntityAuraTracker.all().isEmpty()) return;

        // Vertical billboard frame: right rotates around world-Y to face the camera
        // horizontally; up is locked to world-up. Head pitch (looking up/down) does NOT
        // tilt the aura.
        float camYawRad = (float) Math.toRadians(camera.getYRot());
        Vector3f right = new Vector3f((float) Math.cos(camYawRad), 0f, (float) Math.sin(camYawRad));
        Vector3f up    = new Vector3f(0f, 1f, 0f);

        boolean firstPerson = mc.options != null
                && mc.options.getCameraType() == CameraType.FIRST_PERSON;
        UUID ownId = mc.player != null ? mc.player.getUUID() : null;

        Vec3 camPos = camera.getPosition();
        long nowMs = System.currentTimeMillis();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // No-depth-write translucent type: disc paints colour but doesn't touch the depth
        // buffer. The standard LEQUAL test still occludes the disc behind walls and
        // behind the entity body.
        RenderType auraType = AuraRenderTypes.auraTranslucentNoDepthWrite(WHITE_TEX);
        VertexConsumer vc = buffers.getBuffer(auraType);

        for (var entry : EntityAuraTracker.all().entrySet()) {
            UUID entityId = entry.getKey();
            List<AuraInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            Entity host = findEntity(mc, entityId);
            if (host == null) continue;

            // Don't draw the local player's own aura in first person.
            if (firstPerson && ownId != null && ownId.equals(entityId)) continue;

            double ex = host.xo + (host.getX() - host.xo) * partialTick;
            double ey = host.yo + (host.getY() - host.yo) * partialTick + host.getBbHeight() * 0.5;
            double ez = host.zo + (host.getZ() - host.zo) * partialTick;

            poseStack.pushPose();
            poseStack.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);

            for (AuraInstance instance : instances) {
                renderInstance2D(vc, poseStack.last().pose(), instance, nowMs,
                        right, up, host.getBbWidth(), host.getBbHeight());
            }
            poseStack.popPose();
        }

        RenderSystem.disableCull();
        buffers.endBatch(auraType);
        RenderSystem.enableCull();
    }

    private static Entity findEntity(Minecraft mc, UUID id) {
        if (mc.player != null && mc.player.getUUID().equals(id)) return mc.player;
        if (mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) return e;
        }
        return null;
    }

    private static void renderInstance2D(VertexConsumer vc, Matrix4f mat,
                                         AuraInstance inst, long nowMs,
                                         Vector3f right, Vector3f up,
                                         float bbWidth, float bbHeight) {
        float t = ((nowMs - inst.startTimeMs()) / 1000.0f) * AuraConfig.animationSpeed;
        float pulse = (float) (Math.sin(t * inst.pulseSpeed() * Math.PI * 2.0) * 0.5 + 0.5);

        // Disc is bbox-aware so a tall slim entity gets a tall slim 2D bulb.
        float radiusH = inst.radius() * bbWidth  * 0.85f * (1.0f + AuraConfig.pulseAmplitude * pulse);
        float radiusV = inst.radius() * bbHeight * 0.55f * (1.0f + AuraConfig.pulseAmplitude * pulse);

        // jitter (per-instance) scales how much the silhouette morphs over time.
        float jitter = inst.jitterAmount();
        float lobeLow  = AuraConfig.lobeCountLow  + (float) Math.sin(t * 0.37) * 1.5f * jitter;
        float lobeHigh = AuraConfig.lobeCountHigh + (float) Math.cos(t * 0.29) * 1.8f * jitter;
        float waveStrength = inst.distortionStrength()
                * (AuraConfig.waveBase + pulse * AuraConfig.waveAnimAmplitude * jitter);
        float rotation = t * inst.rotationSpeed();

        int gridN = Math.max(2, AuraConfig.pixelize2dGridSize);
        // Exact tile size — cells touch their neighbours without overlapping (no alpha bands).
        float cellHalfH = radiusH / gridN;
        float cellHalfV = radiusV / gridN;

        float baseR = inst.r() * AuraConfig.brightness;
        float baseG = inst.g() * AuraConfig.brightness;
        float baseB = inst.b() * AuraConfig.brightness;
        float baseA = inst.a() * AuraConfig.opacityMultiplier;

        int packedLight = LightTexture.FULL_BRIGHT;
        float cosR = (float) Math.cos(rotation);
        float sinR = (float) Math.sin(rotation);

        for (int i = 0; i < gridN; i++) {
            for (int j = 0; j < gridN; j++) {
                float u = ((i + 0.5f) / gridN) * 2.0f - 1.0f;
                float v = ((j + 0.5f) / gridN) * 2.0f - 1.0f;

                float ur = u * cosR - v * sinR;
                float vr = u * sinR + v * cosR;
                float angle = (float) Math.atan2(vr, ur);
                float distFromCentre = (float) Math.sqrt(ur * ur + vr * vr);

                float lobeWave =
                          (float) Math.sin(angle * lobeLow  + t * 1.3f) * 0.5f
                        + (float) Math.cos(angle * lobeHigh + t * 0.9f) * 0.35f;
                float edge = 0.95f + waveStrength * 0.18f * lobeWave;

                if (distFromCentre > edge) continue;

                float normD = Math.min(1.0f, distFromCentre / edge);
                float inner = 1.0f - normD;
                float rim   = 1.0f - inner;
                float intensity = inner * 0.85f + (rim * rim) * 0.45f;
                float alpha = baseA * (0.25f + intensity * 0.75f);
                if (alpha > 1.0f) alpha = 1.0f;
                float whiten = inner * inner * 0.4f;
                float r = baseR + (1.0f - baseR) * whiten;
                float g = baseG + (1.0f - baseG) * whiten;
                float b = baseB + (1.0f - baseB) * whiten;

                float cx = right.x() * u * radiusH + up.x() * v * radiusV;
                float cy = right.y() * u * radiusH + up.y() * v * radiusV;
                float cz = right.z() * u * radiusH + up.z() * v * radiusV;

                float dxH = right.x() * cellHalfH, dyH = right.y() * cellHalfH, dzH = right.z() * cellHalfH;
                float dxV = up.x()    * cellHalfV, dyV = up.y()    * cellHalfV, dzV = up.z()    * cellHalfV;

                emitQuad(vc, mat,
                        cx - dxH - dxV, cy - dyH - dyV, cz - dzH - dzV,
                        cx + dxH - dxV, cy + dyH - dyV, cz + dzH - dzV,
                        cx + dxH + dxV, cy + dyH + dyV, cz + dzH + dzV,
                        cx - dxH + dxV, cy - dyH + dyV, cz - dzH + dzV,
                        r, g, b, alpha, packedLight);
            }
        }
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f mat,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a, int packedLight) {
        vert(vc, mat, x1, y1, z1, r, g, b, a, packedLight);
        vert(vc, mat, x2, y2, z2, r, g, b, a, packedLight);
        vert(vc, mat, x3, y3, z3, r, g, b, a, packedLight);
        vert(vc, mat, x4, y4, z4, r, g, b, a, packedLight);
    }

    private static void vert(VertexConsumer vc, Matrix4f mat,
                             float x, float y, float z,
                             float r, float g, float b, float a, int packedLight) {
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 1f, 0f);
    }
}
