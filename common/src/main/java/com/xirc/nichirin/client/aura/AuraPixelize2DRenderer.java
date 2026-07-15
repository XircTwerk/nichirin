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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
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

    private static final float PIXEL_SIZE = 1.0f / 16.0f;
    private static final float PIXEL_HALF = PIXEL_SIZE * 0.5f;
    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private AuraPixelize2DRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (EntityAuraTracker.all().isEmpty()) return;

        // Vertical disc frame: the plane follows the HOST's body yaw (not the observer's
        // camera), so the aura turns with the entity as it looks around. Up is locked to
        // world-up — pitch never tilts the disc. Camera-facing instances (projectiles) use
        // the observer's yaw instead so the disc always reads full-on.
        Vector3f up = new Vector3f(0f, 1f, 0f);
        float camYawRad = (float) Math.toRadians(camera.getYRot());
        Vector3f camRight = new Vector3f((float) Math.cos(camYawRad), 0f, (float) Math.sin(camYawRad));

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
            if (entry.getValue().isEmpty()) continue;
            // Largest aura first so it draws behind smaller ones — layered auras (demon ring
            // around a breathing core) stack correctly regardless of publish order.
            List<AuraInstance> instances = new ArrayList<>(entry.getValue());
            instances.sort((a, b) -> Float.compare(b.radius(), a.radius()));

            Entity host = findEntity(mc, entityId);
            if (host == null) continue;

            // Don't draw the local player's own aura in first person.
            if (firstPerson && ownId != null && ownId.equals(entityId)) continue;

            double ex = host.xo + (host.getX() - host.xo) * partialTick;
            double ey = host.yo + (host.getY() - host.yo) * partialTick + host.getBbHeight() * 0.5;
            double ez = host.zo + (host.getZ() - host.zo) * partialTick;

            Vec3 toCam = camPos.subtract(ex, ey, ez).normalize();
            double toCamHX = toCam.x, toCamHZ = toCam.z;
            double toCamHLen = Math.sqrt(toCamHX * toCamHX + toCamHZ * toCamHZ);
            if (toCamHLen > 1e-6) { toCamHX /= toCamHLen; toCamHZ /= toCamHLen; }

            for (int idx = 0; idx < instances.size(); idx++) {
                AuraInstance instance = instances.get(idx);
                double backOffset = host.getBbWidth() * 0.5 + 0.15;
                double bias = idx * 0.08;
                poseStack.pushPose();
                poseStack.translate(
                        ex - camPos.x - toCamHX * backOffset + toCamHX * bias,
                        ey - camPos.y,
                        ez - camPos.z - toCamHZ * backOffset + toCamHZ * bias);
                renderInstance2D(vc, poseStack.last().pose(), instance, nowMs,
                        camRight, up, host.getBbWidth(), host.getBbHeight());
                poseStack.popPose();
            }
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
        // Spawn/removal transition: ease alpha and size instead of popping (katana swaps etc.).
        float fade = inst.fadeFactor(nowMs, EntityAuraTracker.FADE_MS);
        if (fade <= 0.02f) return;

        float t = ((nowMs - inst.startTimeMs()) / 1000.0f) * AuraConfig.animationSpeed;
        float pulse = (float) (Math.sin(t * inst.pulseSpeed() * Math.PI * 2.0) * 0.5 + 0.5);

        // radius is a multiplier of the entity bbox: radius=1 → disc matches the model size
        // (half-bb each axis means the full disc spans the entire bbox); radius=2 → 2× model size.
        float fadeScale = 0.7f + 0.3f * fade;
        float radiusH = inst.radius() * bbWidth  * 0.5f * (1.0f + AuraConfig.pulseAmplitude * pulse) * fadeScale;
        float radiusV = inst.radius() * bbHeight * 0.5f * (1.0f + AuraConfig.pulseAmplitude * pulse) * fadeScale;

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
        // waviness (per-instance): how far the flowing energy bands snake sideways. A near-straight
        // band (low) bends into deep sinuous S-waves (high), like body-wave hair. Added on top of the
        // baseline band sway; 0 (default) leaves the bands exactly as they were.
        float waviness = inst.wavinessAmount();

        int gridWidth = Math.max(2, (int) Math.ceil(radiusH * 2.0f / PIXEL_SIZE));
        int gridHeight = Math.max(2, (int) Math.ceil(radiusV * 2.0f / PIXEL_SIZE));
        // Exact tile size — cells touch their neighbours without overlapping (no alpha bands).
        // Clamp after the brightness multiply: any channel pushed past 1.0 would otherwise wrap
        // around in the byte cast inside setColor (1.15*255 = 293 → &0xFF → 37) and come out
        // near-black, which read as "inverted" colours (red→green, purple→orange).
        float baseR = Math.min(1.0f, inst.r() * AuraConfig.brightness);
        float baseG = Math.min(1.0f, inst.g() * AuraConfig.brightness);
        float baseB = Math.min(1.0f, inst.b() * AuraConfig.brightness);
        float baseA = Math.min(1.0f, inst.a() * AuraConfig.opacityMultiplier) * fade;
        float[][] palette = buildPalette(baseR, baseG, baseB);
        int profile = inst.materialProfile();
        float profileA = ((profile >>> 1) & 255) / 255.0f;
        float profileB = ((profile >>> 9) & 255) / 255.0f;
        float profileC = ((profile >>> 17) & 255) / 255.0f;
        float flowAngle = profileA * (float) (Math.PI * 2.0);
        float flowX = (float) Math.cos(flowAngle);
        float flowY = (float) Math.sin(flowAngle);
        float flowSpeed = 0.65f + profileB * 1.35f;
        float tileFrequency = 3.0f + profileC * 5.0f;

        int packedLight = LightTexture.FULL_BRIGHT;
        // No rigid rotation — that's what was making it look like a spinning wheel. The angular
        // motion now comes from the +t/-t phase shifts inside the wave terms (counter-flowing).
        // `rotation` is still folded in as a slow phase drift on one of the waves so the per-
        // instance rotationSpeed knob isn't dead.
        float rotPhase = rotation * 0.4f;

        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                float localX = (i - (gridWidth - 1) * 0.5f) * PIXEL_SIZE;
                float localY = (j - (gridHeight - 1) * 0.5f) * PIXEL_SIZE;
                float u = localX / radiusH;
                float v = localY / radiusV;

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
                float flowCoord = (u * flowX + v * flowY) * tileFrequency - t * flowSpeed;
                float crossCoord = (-u * flowY + v * flowX) * (2.0f + profileA * 4.0f);
                float energyBand = (float) Math.sin(flowCoord * Math.PI
                        + Math.sin(crossCoord + t) * (0.9f + waviness * 3.0f));
                energyBand = energyBand * 0.5f + 0.5f;
                float fineGrain = hashTile(i, j, profile);
                float pigmentPatch = hashTile(Math.floorDiv(i, 3), Math.floorDiv(j, 3),
                        profile ^ 0x51ed270b);
                float brushStroke = (float) Math.sin(
                        i * (0.55f + profileB * 0.35f)
                                + j * (0.18f + profileC * 0.22f)
                                + profileA * 9.0f);
                brushStroke = brushStroke * 0.5f + 0.5f;
                float chippedPigment = fineGrain > 0.90f ? -0.22f
                        : fineGrain < 0.08f ? 0.18f : 0.0f;
                float risingTongue = (float) Math.sin(
                        u * (8.0f + profileC * 5.0f)
                                - v * (3.0f + profileB * 2.0f)
                                + t * (2.4f + profileA));
                risingTongue = risingTongue * 0.5f + 0.5f;
                float flameLift = Math.max(0.0f, risingTongue - normD * 0.35f);
                float materialLight = 0.42f
                        + energyBand * 0.26f
                        + flameLift * 0.42f
                        + (pigmentPatch - 0.5f) * 0.30f
                        + (brushStroke - 0.5f) * 0.16f
                        + chippedPigment;
                float intensity = (inner * 0.85f + (rim * rim) * 0.45f) * materialLight;
                float alpha = baseA * (0.58f + intensity * 0.42f);
                if (alpha > 1.0f) alpha = 1.0f;
                float paletteValue = materialLight + inner * 0.22f + flameLift * 0.18f;
                int paletteIndex = paletteValue < 0.42f ? 0
                        : paletteValue < 0.62f ? 1
                        : paletteValue < 0.82f ? 2
                        : paletteValue < 1.02f ? 3 : 4;
                float r = palette[paletteIndex][0];
                float g = palette[paletteIndex][1];
                float b = palette[paletteIndex][2];

                float cx = right.x() * localX + up.x() * localY;
                float cy = right.y() * localX + up.y() * localY;
                float cz = right.z() * localX + up.z() * localY;

                float dxH = right.x() * PIXEL_HALF, dyH = right.y() * PIXEL_HALF, dzH = right.z() * PIXEL_HALF;
                float dxV = up.x()    * PIXEL_HALF, dyV = up.y()    * PIXEL_HALF, dzV = up.z()    * PIXEL_HALF;

                emitQuad(vc, mat,
                        cx - dxH - dxV, cy - dyH - dyV, cz - dzH - dzV,
                        cx + dxH - dxV, cy + dyH - dyV, cz + dzH - dzV,
                        cx + dxH + dxV, cy + dyH + dyV, cz + dzH + dzV,
                        cx - dxH + dxV, cy - dyH + dyV, cz - dzH + dzV,
                        r, g, b, alpha, packedLight);
            }
        }
    }

    private static float hashTile(int x, int y, int seed) {
        int h = seed ^ (x * 0x1f1f1f1f) ^ (y * 0x6d2b79f5);
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0f;
    }

    private static float[][] buildPalette(float r, float g, float b) {
        float[] hsv = rgbToHsv(r, g, b);
        return new float[][] {
                hsvToRgb(wrapHue(hsv[0] - 0.035f), Math.min(1.0f, hsv[1] * 1.10f), hsv[2] * 0.42f),
                hsvToRgb(wrapHue(hsv[0] - 0.015f), Math.min(1.0f, hsv[1] * 1.05f), hsv[2] * 0.68f),
                new float[] { r, g, b },
                hsvToRgb(wrapHue(hsv[0] + 0.018f), hsv[1] * 0.82f,
                        Math.min(1.0f, hsv[2] * 1.18f)),
                hsvToRgb(wrapHue(hsv[0] + 0.040f), hsv[1] * 0.58f,
                        Math.min(1.0f, hsv[2] * 1.32f))
        };
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0f;
        if (delta > 0.0001f) {
            if (max == r) hue = ((g - b) / delta) / 6.0f;
            else if (max == g) hue = (2.0f + (b - r) / delta) / 6.0f;
            else hue = (4.0f + (r - g) / delta) / 6.0f;
        }
        return new float[] { wrapHue(hue), max <= 0.0001f ? 0.0f : delta / max, max };
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float scaled = wrapHue(h) * 6.0f;
        int sector = (int) Math.floor(scaled);
        float f = scaled - sector;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));
        return switch (sector % 6) {
            case 0 -> new float[] { v, t, p };
            case 1 -> new float[] { q, v, p };
            case 2 -> new float[] { p, v, t };
            case 3 -> new float[] { p, q, v };
            case 4 -> new float[] { t, p, v };
            default -> new float[] { v, p, q };
        };
    }

    private static float wrapHue(float hue) {
        hue %= 1.0f;
        return hue < 0.0f ? hue + 1.0f : hue;
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
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 1f, 0f);
    }
}
