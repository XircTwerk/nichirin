package com.xirc.nichirin.client.aura;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Renders entity auras as layered, segmented energy wisps and sparse pixel fragments. */
@Environment(EnvType.CLIENT)
public final class AuraPixelize2DRenderer {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Map<AuraInstance, AuraWispLayout> LAYOUT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AuraPixelize2DRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || EntityAuraTracker.all().isEmpty()) return;

        boolean firstPerson = minecraft.options != null
                && minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
        UUID ownId = minecraft.player != null ? minecraft.player.getUUID() : null;
        Vec3 cameraPosition = camera.getPosition();
        float cameraYaw = (float) Math.toRadians(camera.getYRot());
        float cameraRightX = (float) Math.cos(cameraYaw);
        float cameraRightZ = (float) Math.sin(cameraYaw);
        float maximumDistanceSquared = AuraConfig.maximumRenderDistance * AuraConfig.maximumRenderDistance;
        long nowMs = System.currentTimeMillis();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType auraType = AuraRenderTypes.auraTranslucentNoDepthWrite(WHITE_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(auraType);

        for (var entry : EntityAuraTracker.all().entrySet()) {
            UUID entityId = entry.getKey();
            if (entry.getValue().isEmpty()) continue;
            if (firstPerson && ownId != null && ownId.equals(entityId)) continue;

            Entity host = findEntity(minecraft, entityId);
            if (host == null) continue;
            // No aura while invisible (covers the Invisibility effect and Obscuring Clouds, which
            // turns its user invisible).
            if (host.isInvisible()) continue;

            double x = host.xo + (host.getX() - host.xo) * partialTick;
            double y = host.yo + (host.getY() - host.yo) * partialTick;
            double z = host.zo + (host.getZ() - host.zo) * partialTick;
            double dx = x - cameraPosition.x;
            double dy = y - cameraPosition.y;
            double dz = z - cameraPosition.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > maximumDistanceSquared) continue;
            double distance = Math.sqrt(Math.max(0.0001, distanceSquared));
            float toCameraX = (float) (-dx / distance);
            float toCameraY = (float) (-dy / distance);
            float toCameraZ = (float) (-dz / distance);
            float bodySideAngle = host instanceof LivingEntity living
                    ? (float) Math.toRadians(Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot))
                    : 0.0f;

            List<AuraInstance> instances = new ArrayList<>(entry.getValue());
            instances.sort((a, b) -> Float.compare(b.radius(), a.radius()));

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            Matrix4f matrix = poseStack.last().pose();
            for (AuraInstance instance : instances) {
                if (host instanceof LivingEntity && !instance.cameraFacing()) {
                    AuraWispLayout layout = LAYOUT_CACHE.computeIfAbsent(instance,
                            ignored -> AuraWispLayout.create(entityId, instance));
                    renderAura(consumer, matrix, instance, layout, nowMs,
                            host.getBbWidth(), host.getBbHeight(), cameraRightX, cameraRightZ,
                            bodySideAngle, toCameraX, toCameraY, toCameraZ);
                } else {
                    renderCameraFacingAura(consumer, matrix, instance, nowMs,
                            host.getBbWidth(), host.getBbHeight(), cameraRightX, cameraRightZ,
                            toCameraX, toCameraY, toCameraZ);
                }
            }
            poseStack.popPose();
        }

        buffers.endBatch(auraType);
    }

    private static void renderAura(VertexConsumer consumer, Matrix4f matrix, AuraInstance aura,
                                   AuraWispLayout layout, long nowMs, float bodyWidth, float bodyHeight,
                                   float cameraRightX, float cameraRightZ, float bodySideAngle,
                                   float toCameraX, float toCameraY, float toCameraZ) {
        float fade = aura.fadeFactor(nowMs, EntityAuraTracker.FADE_MS);
        if (fade <= 0.02f) return;

        float time = ((nowMs - aura.startTimeMs()) / 1000.0f) * AuraConfig.animationSpeed;
        float pulse = (float) (Math.sin(time * aura.pulseSpeed() * Math.PI * 2.0) * 0.5 + 0.5);
        float scale = Math.max(0.05f, aura.radius() * AuraConfig.auraScale)
                * (0.78f + 0.22f * fade)
                * (1.0f + AuraConfig.pulseAmplitude * pulse);
        float red = clamp(aura.r() * AuraConfig.brightness);
        float green = clamp(aura.g() * AuraConfig.brightness);
        float blue = clamp(aura.b() * AuraConfig.brightness);
        float alpha = clamp(aura.a() * AuraConfig.opacityMultiplier) * fade;
        float distortion = Math.max(0.2f, aura.distortionStrength() * 3.0f + aura.jitterAmount() * 0.09f);
        // Keep the complete wisp envelope behind the host without visually detaching the aura.
        float backOffset = bodyWidth * (0.50f + AuraConfig.maximumRadius * scale)
                + AuraConfig.swayAmount * scale + 0.08f;
        float backX = -toCameraX * backOffset;
        float backY = -toCameraY * backOffset;
        float backZ = -toCameraZ * backOffset;

        renderPlayerCore(consumer, matrix, aura, time, pulse, bodyWidth, bodyHeight,
                red, green, blue, alpha, cameraRightX, cameraRightZ,
                backX, backY, backZ);
        for (AuraWispLayout.Wisp wisp : layout.wisps) {
            renderWisp(consumer, matrix, wisp, time, bodyWidth, bodyHeight, scale, distortion,
                    red, green, blue, alpha, cameraRightX, cameraRightZ, bodySideAngle,
                    backX, backY, backZ);
        }
        for (AuraWispLayout.Fragment fragment : layout.fragments) {
            renderFragment(consumer, matrix, fragment, time, bodyWidth, bodyHeight, scale,
                    red, green, blue, alpha, cameraRightX, cameraRightZ, backX, backY, backZ);
        }
    }

    private static void renderPlayerCore(VertexConsumer consumer, Matrix4f matrix, AuraInstance aura,
                                         float time, float pulse, float bodyWidth, float bodyHeight,
                                         float red, float green, float blue, float baseAlpha,
                                         float cameraRightX, float cameraRightZ,
                                         float backX, float backY, float backZ) {
        float radiusFactor = Math.min(1.65f, 0.55f + aura.radius() * 0.34f);
        float radiusX = bodyHeight * 0.5f * AuraConfig.auraScale * radiusFactor
                * (1.0f + AuraConfig.pulseAmplitude * pulse * 0.55f);
        float radiusY = radiusX * 2.90f;
        float pixel = Math.max(1.0f / 16.0f, AuraConfig.playerPixelSize);
        int columns = Math.max(3, (int) Math.ceil(radiusX * 2.0f / pixel));
        int rows = Math.max(3, (int) Math.ceil(radiusY * 2.0f / pixel));
        float originX = backX;
        float originY = bodyHeight * 0.5f + backY;
        float originZ = backZ;

        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                float localX = (column - (columns - 1) * 0.5f) * pixel;
                float localY = (row - (rows - 1) * 0.5f) * pixel;
                float u = localX / radiusX;
                float v = localY / radiusY;
                float squircle = (float) (Math.pow(Math.abs(u), 4.0)
                        + Math.pow(Math.abs(v), 4.0));
                float distance = (float) Math.pow(squircle, 0.25);
                float angle = (float) Math.atan2(v, u);
                float edge = 0.96f + (float) Math.sin(angle * 7.0f - time * 0.9f
                        + aura.materialProfile() * 0.001f) * 0.035f;
                if (distance > edge) continue;

                float inner = 1.0f - distance / edge;
                float flowingShade = (float) Math.sin(
                        column * 0.63f - row * 0.31f - time * 2.1f
                                + aura.materialProfile() * 0.0007f);
                int colorShade = flowingShade < -0.58f ? 1
                        : flowingShade > 0.62f ? 4 : inner > 0.52f ? 3 : 2;
                float alpha = baseAlpha * (0.16f + inner * 0.18f);
                float centerX = originX + cameraRightX * localX;
                float centerY = originY + localY;
                float centerZ = originZ + cameraRightZ * localX;
                emitBillboard(consumer, matrix, centerX, centerY, centerZ,
                        cameraRightX, 0.0f, cameraRightZ,
                        0.0f, 1.0f, 0.0f,
                        pixel, pixel,
                        shade(red, colorShade), shade(green, colorShade), shade(blue, colorShade),
                        clamp(alpha));
            }
        }
    }

    private static void renderCameraFacingAura(VertexConsumer consumer, Matrix4f matrix,
                                               AuraInstance aura, long nowMs,
                                               float bodyWidth, float bodyHeight,
                                               float cameraRightX, float cameraRightZ,
                                               float toCameraX, float toCameraY, float toCameraZ) {
        float fade = aura.fadeFactor(nowMs, EntityAuraTracker.FADE_MS);
        if (fade <= 0.02f) return;
        float time = ((nowMs - aura.startTimeMs()) / 1000.0f) * AuraConfig.animationSpeed;
        float pulse = (float) (Math.sin(time * aura.pulseSpeed() * Math.PI * 2.0) * 0.5 + 0.5);
        float scale = 1.0f + AuraConfig.pulseAmplitude * pulse;
        float radiusX = Math.max(AuraConfig.pixelSize,
                aura.radius() * bodyWidth * 0.5f * scale);
        float radiusY = Math.max(AuraConfig.pixelSize,
                aura.radius() * bodyHeight * 0.5f * scale);
        float red = clamp(aura.r() * AuraConfig.brightness);
        float green = clamp(aura.g() * AuraConfig.brightness);
        float blue = clamp(aura.b() * AuraConfig.brightness);
        float baseAlpha = clamp(aura.a() * AuraConfig.opacityMultiplier) * fade;
        float backOffset = bodyWidth * 0.58f + 0.08f;
        float backX = -toCameraX * backOffset;
        float backY = -toCameraY * backOffset;
        float backZ = -toCameraZ * backOffset;
        float pixel = Math.max(1.0f / 32.0f, AuraConfig.pixelSize);
        int columns = Math.max(2, (int) Math.ceil(radiusX * 2.0f / pixel));
        int rows = Math.max(2, (int) Math.ceil(radiusY * 2.0f / pixel));

        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                float localX = (column - (columns - 1) * 0.5f) * pixel;
                float localY = (row - (rows - 1) * 0.5f) * pixel;
                float u = localX / radiusX;
                float v = localY / radiusY;
                float distance = (float) Math.sqrt(u * u + v * v);
                float angle = (float) Math.atan2(v, u);
                float edge = 0.92f + (float) Math.sin(angle * 6.0f - time * 1.6f
                        + aura.materialProfile() * 0.001f) * 0.08f * aura.distortionStrength();
                if (distance > edge) continue;
                int colorShade = distance > edge * 0.78f ? 4
                        : ((column + row + aura.materialProfile()) & 3) == 0 ? 1 : 3;
                float alpha = baseAlpha * (0.58f + (1.0f - distance / edge) * 0.42f);
                float centerX = backX + cameraRightX * localX;
                float centerY = backY + bodyHeight * 0.5f + localY;
                float centerZ = backZ + cameraRightZ * localX;
                emitBillboard(consumer, matrix, centerX, centerY, centerZ,
                        cameraRightX, 0.0f, cameraRightZ,
                        0.0f, 1.0f, 0.0f,
                        pixel, pixel,
                        shade(red, colorShade), shade(green, colorShade), shade(blue, colorShade),
                        clamp(alpha));
            }
        }
    }

    private static void renderWisp(VertexConsumer consumer, Matrix4f matrix, AuraWispLayout.Wisp wisp,
                                   float time, float bodyWidth, float bodyHeight, float scale,
                                   float distortion, float red, float green, float blue, float baseAlpha,
                                   float cameraRightX, float cameraRightZ, float bodySideAngle,
                                   float backX, float backY, float backZ) {
        int segments = Math.max(4, AuraConfig.verticalSegmentCount);
        float phase = wisp.phase() * (float) (Math.PI * 2.0);
        float angle = bodySideAngle + wisp.angle()
                + (float) Math.sin(time * 0.42f * wisp.speed() + phase)
                * AuraConfig.overallRotationSpeed * 3.0f;
        float radiusPulse = 1.0f + (float) Math.sin(time * 0.48f * wisp.speed() + phase) * 0.08f;
        float radius = wisp.radius() * bodyWidth * scale * radiusPulse;
        float originX = (float) Math.cos(angle) * radius;
        float originZ = (float) Math.sin(angle) * radius;
        float projectedRadius = Math.abs(originX * cameraRightX + originZ * cameraRightZ);
        float maximumProjectedRadius = Math.max(AuraConfig.playerPixelSize,
                AuraConfig.maximumRadius * bodyWidth * scale);
        float normalizedRadius = Math.min(1.0f, projectedRadius / maximumProjectedRadius);
        float circularEnvelope = (float) Math.sqrt(Math.max(0.12f,
                1.0f - normalizedRadius * normalizedRadius));
        float envelopeHalfHeight = bodyHeight * scale
                * AuraConfig.roundedEnvelopeVerticalScale * circularEnvelope;
        float height = Math.min(wisp.height() * bodyHeight * scale,
                envelopeHalfHeight * 2.0f);
        float width = Math.max(AuraConfig.playerPixelSize * 2.0f, wisp.width() * bodyWidth * scale);
        width *= 0.72f + circularEnvelope * 0.28f;
        float stretch = 1.0f + (float) Math.sin(time * 0.73f * wisp.speed() + phase * 1.7f) * 0.075f;
        height *= stretch;
        float verticalOffset = (wisp.startY() - 0.46f) * bodyHeight * 0.16f;
        float baseY = bodyHeight * 0.5f + verticalOffset - height * 0.5f;
        float bendAngle = angle + wisp.bendDirection() * 1.4f;
        float bendX = (float) Math.cos(bendAngle);
        float bendZ = (float) Math.sin(bendAngle);
        float sway = AuraConfig.swayAmount * scale * distortion;
        float flow = time * AuraConfig.upwardMovementSpeed * wisp.speed() + phase;

        float axisX;
        float axisZ;
        if (wisp.facingMode() == 0) {
            axisX = cameraRightX;
            axisZ = cameraRightZ;
        } else if (wisp.facingMode() == 1) {
            axisX = -(float) Math.sin(angle);
            axisZ = (float) Math.cos(angle);
        } else {
            axisX = (float) Math.cos(angle);
            axisZ = (float) Math.sin(angle);
        }

        for (int segment = 0; segment < segments; segment++) {
            float v0 = segment / (float) segments;
            float v1 = (segment + 1.0f) / segments;
            float y0 = backY + snap(baseY + height * v0, AuraConfig.playerPixelSize);
            float y1 = backY + snap(baseY + height * v1, AuraConfig.playerPixelSize);
            float shift0 = bendAt(v0, time, phase, wisp, sway);
            float shift1 = bendAt(v1, time, phase, wisp, sway);
            float x0 = backX + originX + bendX * shift0;
            float z0 = backZ + originZ + bendZ * shift0;
            float x1 = backX + originX + bendX * shift1;
            float z1 = backZ + originZ + bendZ * shift1;
            float width0 = steppedWidth(width, v0, flow, wisp.topShape(), AuraConfig.playerPixelSize);
            float width1 = steppedWidth(width, v1, flow, wisp.topShape(), AuraConfig.playerPixelSize);

            float verticalFade = smoothEdge(v0) * smoothEdge(1.0f - v1);
            float segmentAlpha = baseAlpha * wisp.alpha() * (0.46f + verticalFade * 0.54f);
            if (segment == wisp.gapSegment()) {
                segmentAlpha *= 0.10f + 0.12f
                        * ((float) Math.sin(time * 1.7f + phase) * 0.5f + 0.5f);
                width0 *= 0.74f;
                width1 *= 0.62f;
            }
            int shade = shadeForSegment(segment, flow, wisp.topShape());
            emitRibbonSegment(consumer, matrix, x0, y0, z0, x1, y1, z1,
                    axisX, axisZ, width0, width1,
                    shade(red, shade), shade(green, shade), shade(blue, shade),
                    clamp(segmentAlpha));

            if (wisp.crossed()) {
                float secondAxisX = -axisZ;
                float secondAxisZ = axisX;
                emitRibbonSegment(consumer, matrix, x0, y0, z0, x1, y1, z1,
                        secondAxisX, secondAxisZ, width0 * 0.72f, width1 * 0.72f,
                        shade(red, shade), shade(green, shade), shade(blue, shade),
                        clamp(segmentAlpha * 0.52f));
            }

            if (segment >= segments - 3 && ((wisp.topShape() + segment) & 1) == 0) {
                float split = width * (0.48f + 0.10f * wisp.topShape());
                emitRibbonSegment(consumer, matrix,
                        x0 + axisX * split, y0, z0 + axisZ * split,
                        x1 + axisX * split * 1.2f, y1, z1 + axisZ * split * 1.2f,
                        axisX, axisZ, width0 * 0.22f, width1 * 0.14f,
                        shade(red, Math.min(4, shade + 1)),
                        shade(green, Math.min(4, shade + 1)),
                        shade(blue, Math.min(4, shade + 1)),
                        clamp(segmentAlpha * 0.72f));
            }
        }
    }

    private static float bendAt(float vertical, float time, float phase,
                                AuraWispLayout.Wisp wisp, float sway) {
        float primary = (float) Math.sin(
                vertical * (3.8f + wisp.bendFrequency() * 2.2f)
                        + time * AuraConfig.swaySpeed * wisp.speed() + phase);
        float secondary = (float) Math.sin(
                vertical * 8.4f - time * AuraConfig.swaySpeed * 0.57f + phase * 1.9f);
        return (primary * 0.72f + secondary * 0.28f) * sway * (0.22f + vertical * 0.78f);
    }

    private static float steppedWidth(float width, float vertical, float flow,
                                      int topShape, float pixelSize) {
        float body = 0.62f + (float) Math.sin(vertical * Math.PI) * 0.54f;
        float rising = (float) Math.sin(vertical * 9.0f - flow * 2.2f + topShape * 0.91f) * 0.10f;
        float taper = vertical > 0.76f
                ? 1.0f - (vertical - 0.76f) * (0.72f + topShape * 0.05f)
                : 1.0f;
        float result = width * Math.max(0.28f, body + rising) * Math.max(0.34f, taper);
        return Math.max(pixelSize, snap(result, pixelSize));
    }

    private static int shadeForSegment(int segment, float flow, int shape) {
        float value = (float) Math.sin(segment * 1.71f - flow * 2.4f + shape * 0.83f);
        if (value < -0.62f) return 0;
        if (value < -0.18f) return 1;
        if (value < 0.32f) return 2;
        if (value < 0.72f) return 3;
        return 4;
    }

    private static void renderFragment(VertexConsumer consumer, Matrix4f matrix,
                                       AuraWispLayout.Fragment fragment, float time,
                                       float bodyWidth, float bodyHeight, float scale,
                                       float red, float green, float blue, float baseAlpha,
                                       float cameraRightX, float cameraRightZ,
                                       float backX, float backY, float backZ) {
        float visibleWindow = clamp(AuraConfig.fragmentSpawnRate);
        if (visibleWindow <= 0.01f) return;
        float cycle = fract(time / Math.max(0.1f, fragment.lifetime()) + fragment.phase());
        if (cycle > visibleWindow) return;
        float age = cycle / visibleWindow;
        float fade = (float) Math.sin(age * Math.PI);
        if (fade <= 0.02f) return;

        float angle = fragment.angle() + time * AuraConfig.overallRotationSpeed * 0.55f;
        float radius = (fragment.radius() + age * fragment.outwardSpeed()) * bodyWidth * scale;
        float x = backX + (float) Math.cos(angle) * radius;
        float z = backZ + (float) Math.sin(angle) * radius;
        float y = backY + fragment.startY() * bodyHeight + age * fragment.riseSpeed() * bodyHeight;
        float size = Math.max(AuraConfig.playerPixelSize,
                snap(fragment.size() * bodyWidth * scale, AuraConfig.playerPixelSize));
        float rotation = fragment.rotation() + age * 0.8f;
        float cosine = (float) Math.cos(rotation);
        float sine = (float) Math.sin(rotation);
        float horizontalX = cameraRightX * cosine;
        float horizontalY = sine;
        float horizontalZ = cameraRightZ * cosine;
        float verticalX = -cameraRightX * sine;
        float verticalY = cosine;
        float verticalZ = -cameraRightZ * sine;
        float alpha = clamp(baseAlpha * fade * 0.82f);
        float r = shade(red, 4);
        float g = shade(green, 4);
        float b = shade(blue, 4);

        switch (fragment.shape()) {
            case 0 -> emitBillboard(consumer, matrix, x, y, z,
                    horizontalX, horizontalY, horizontalZ,
                    verticalX, verticalY, verticalZ, size * 2.8f, size * 0.42f, r, g, b, alpha);
            case 1 -> emitBillboard(consumer, matrix, x, y, z,
                    horizontalX, horizontalY, horizontalZ,
                    verticalX, verticalY, verticalZ, size * 4.5f, size * 0.24f, r, g, b, alpha * 0.82f);
            case 2 -> {
                emitBillboard(consumer, matrix, x, y, z,
                        horizontalX, horizontalY, horizontalZ,
                        verticalX, verticalY, verticalZ, size * 2.2f, size * 0.35f, r, g, b, alpha);
                emitBillboard(consumer, matrix, x, y, z,
                        verticalX, verticalY, verticalZ,
                        horizontalX, horizontalY, horizontalZ, size * 1.7f, size * 0.30f, r, g, b, alpha);
            }
            case 3 -> {
                emitBillboard(consumer, matrix, x, y, z,
                        horizontalX, horizontalY, horizontalZ,
                        verticalX, verticalY, verticalZ, size, size, r, g, b, alpha);
                emitBillboard(consumer, matrix,
                        x + horizontalX * size * 1.15f, y + horizontalY * size * 1.15f,
                        z + horizontalZ * size * 1.15f,
                        horizontalX, horizontalY, horizontalZ,
                        verticalX, verticalY, verticalZ, size * 0.55f, size * 0.55f, r, g, b, alpha * 0.72f);
            }
            default -> emitBillboard(consumer, matrix, x, y, z,
                    horizontalX, horizontalY, horizontalZ,
                    verticalX, verticalY, verticalZ, size, size, r, g, b, alpha);
        }
    }

    private static void emitRibbonSegment(VertexConsumer consumer, Matrix4f matrix,
                                          float x0, float y0, float z0,
                                          float x1, float y1, float z1,
                                          float axisX, float axisZ,
                                          float width0, float width1,
                                          float red, float green, float blue, float alpha) {
        float half0 = width0 * 0.5f;
        float half1 = width1 * 0.5f;
        emitQuad(consumer, matrix,
                x0 - axisX * half0, y0, z0 - axisZ * half0,
                x0 + axisX * half0, y0, z0 + axisZ * half0,
                x1 + axisX * half1, y1, z1 + axisZ * half1,
                x1 - axisX * half1, y1, z1 - axisZ * half1,
                red, green, blue, alpha);
    }

    private static void emitBillboard(VertexConsumer consumer, Matrix4f matrix,
                                      float x, float y, float z,
                                      float horizontalX, float horizontalY, float horizontalZ,
                                      float verticalX, float verticalY, float verticalZ,
                                      float width, float height,
                                      float red, float green, float blue, float alpha) {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        emitQuad(consumer, matrix,
                x - horizontalX * hw - verticalX * hh,
                y - horizontalY * hw - verticalY * hh,
                z - horizontalZ * hw - verticalZ * hh,
                x + horizontalX * hw - verticalX * hh,
                y + horizontalY * hw - verticalY * hh,
                z + horizontalZ * hw - verticalZ * hh,
                x + horizontalX * hw + verticalX * hh,
                y + horizontalY * hw + verticalY * hh,
                z + horizontalZ * hw + verticalZ * hh,
                x - horizontalX * hw + verticalX * hh,
                y - horizontalY * hw + verticalY * hh,
                z - horizontalZ * hw + verticalZ * hh,
                red, green, blue, alpha);
    }

    private static void emitQuad(VertexConsumer consumer, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float red, float green, float blue, float alpha) {
        vertex(consumer, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, matrix, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, matrix, x3, y3, z3, red, green, blue, alpha);
        vertex(consumer, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static float shade(float component, int shade) {
        return switch (shade) {
            case 0 -> component * 0.34f;
            case 1 -> component * 0.56f;
            case 2 -> component * 0.78f;
            case 3 -> component;
            default -> component + (1.0f - component) * 0.28f;
        };
    }

    private static float smoothEdge(float value) {
        float clamped = clamp(value * 5.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static float snap(float value, float step) {
        if (step <= 0.0f) return value;
        return Math.round(value / step) * step;
    }

    private static float fract(float value) {
        return value - (float) Math.floor(value);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static Entity findEntity(Minecraft minecraft, UUID id) {
        if (minecraft.player != null && minecraft.player.getUUID().equals(id)) return minecraft.player;
        if (minecraft.level == null) return null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) return entity;
        }
        return null;
    }
}
