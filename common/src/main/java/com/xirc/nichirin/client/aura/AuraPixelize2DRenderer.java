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

        // radius is a multiplier of the entity bbox: radius=1 → disc matches the model size
        // (half-bb each axis means the full disc spans the entire bbox); radius=2 → 2× model size.
        float radiusH = inst.radius() * bbWidth  * 0.5f * (1.0f + AuraConfig.pulseAmplitude * pulse);
        float radiusV = inst.radius() * bbHeight * 0.5f * (1.0f + AuraConfig.pulseAmplitude * pulse);

        // jitter (per-instance) scales how much the silhouette morphs over time.
        float jitter = inst.jitterAmount();
        // Cap the dynamic delta on lobe counts so very high jitter doesn't push lobe frequency
        // past the grid's resolving power (which aliases back into a smooth/circular look).
        float lobeDelta = Math.min(jitter, 3.5f);
        float lobeLow  = AuraConfig.lobeCountLow  + (float) Math.sin(t * 0.37) * 1.5f * lobeDelta;
        float lobeHigh = AuraConfig.lobeCountHigh + (float) Math.cos(t * 0.29) * 1.8f * lobeDelta;
        // Linear (not quadratic) in jitter so cranking jitter doesn't blow the edge wave past the
        // distFromCentre range and degenerate the silhouette into a fully-filled circle.
        float waveStrength = inst.distortionStrength()
                * (AuraConfig.waveBase + pulse * AuraConfig.waveAnimAmplitude) * jitter;
        float rotation = t * inst.rotationSpeed();
        // Low-frequency macro warp — at high jitter this gives the disc a few big lumps. Below the
        // threshold the silhouette stays the smooth ring you see at default jitter (~2.2).
        float macroLobes = 3.0f + (float) Math.sin(t * 0.21) * 1.0f;
        float macroAmplitude = Math.min(0.45f, Math.max(0f, jitter - 3.0f) * 0.04f);
        // Radial breath frequency — waves ripple OUTWARD from the centre so the silhouette feels
        // like a churning aura rather than a rotating wheel.
        float radialFreq = 6.0f + (float) Math.sin(t * 0.17) * 2.0f;
        float radialAmplitude = 0.05f + jitter * 0.005f;

        int gridN = Math.max(2, AuraConfig.pixelize2dGridSize);
        // Exact tile size — cells touch their neighbours without overlapping (no alpha bands).
        float cellHalfH = radiusH / gridN;
        float cellHalfV = radiusV / gridN;

        // Clamp after the brightness multiply: any channel pushed past 1.0 would otherwise wrap
        // around in the byte cast inside setColor (1.15*255 = 293 → &0xFF → 37) and come out
        // near-black, which read as "inverted" colours (red→green, purple→orange).
        float baseR = Math.min(1.0f, inst.r() * AuraConfig.brightness);
        float baseG = Math.min(1.0f, inst.g() * AuraConfig.brightness);
        float baseB = Math.min(1.0f, inst.b() * AuraConfig.brightness);
        float baseA = Math.min(1.0f, inst.a() * AuraConfig.opacityMultiplier);

        int packedLight = LightTexture.FULL_BRIGHT;
        // No rigid rotation — that's what was making it look like a spinning wheel. The angular
        // motion now comes from the +t/-t phase shifts inside the wave terms (counter-flowing).
        // `rotation` is still folded in as a slow phase drift on one of the waves so the per-
        // instance rotationSpeed knob isn't dead.
        float rotPhase = rotation * 0.4f;

        for (int i = 0; i < gridN; i++) {
            for (int j = 0; j < gridN; j++) {
                float u = ((i + 0.5f) / gridN) * 2.0f - 1.0f;
                float v = ((j + 0.5f) / gridN) * 2.0f - 1.0f;

                float angle = (float) Math.atan2(v, u);
                float distFromCentre = (float) Math.sqrt(u * u + v * v);

                // Two counter-flowing angular waves so the silhouette CHURNS rather than rotates as
                // a rigid wheel. The two layers slide past each other and produce a roiling look.
                float lobeWave =
                          (float) Math.sin(angle * lobeLow  + t * 1.3f + rotPhase) * 0.45f
                        + (float) Math.cos(angle * lobeHigh - t * 1.1f) * 0.35f
                        // Radial ripple: waves moving outward from the centre.
                        + (float) Math.sin(distFromCentre * radialFreq - t * 2.2f) * radialAmplitude;
                // tanh-saturate the wave so the swing matches the old look at default jitter (~2.2)
                // but can't run away at extreme jitter. Asymmetric scale: small upward bumps, larger
                // downward notches — so the silhouette stays at-most a clean circle (never a square)
                // while still reading as lumpy/distorted at high jitter.
                float waveTerm = (float) Math.tanh(waveStrength * lobeWave * 0.33f);
                float waveContribution = waveTerm >= 0
                        ? waveTerm * 0.12f      // bumps stay small — never push past the unit circle
                        : waveTerm * 0.5f;      // notches can bite deep for visible lumps
                float macroWarp = macroAmplitude * (float) Math.sin(angle * macroLobes + t * 0.5f);
                float macroContribution = macroWarp >= 0
                        ? macroWarp * 0.25f     // gentle outward lump cap
                        : macroWarp;            // full inward bite
                float edge = 0.97f + waveContribution + macroContribution;
                // Hard cap at 1.0 → silhouette is bounded by the unit circle and can never fill the
                // grid corners (which is what was making it look like a square at high jitter).
                if (edge < 0.35f) edge = 0.35f;
                if (edge > 1.0f) edge = 1.0f;

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
