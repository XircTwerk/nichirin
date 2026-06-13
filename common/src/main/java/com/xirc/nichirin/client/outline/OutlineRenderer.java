package com.xirc.nichirin.client.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.common.outline.OutlineInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Renders an outline silhouette around any entity tracked in {@link OutlineTracker}.
 *
 * Approach: re-dispatch the entity through the normal renderer, but with a wrapped buffer
 * source that forces all vertex output into a flat-colour {@link OutlineRenderTypes}
 * variant, and a slightly upscaled {@link PoseStack}. The result is an enlarged flat
 * silhouette of the same model — visible as a coloured halo around the entity.
 *
 * Two variants are supported per-instance:
 *   seeThroughWalls=true  → no depth test → outline visible through walls
 *   seeThroughWalls=false → LEQUAL depth test → outline only where entity would be visible
 */
@Environment(EnvType.CLIENT)
public final class OutlineRenderer {

    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private OutlineRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (OutlineTracker.all().isEmpty()) return;
        // The edge pass needs the cel shader; without it (failed registration) draw nothing
        // rather than dispatching geometry through a null shader.
        if (OutlineShaderHolder.getShader() == null) return;

        Vec3 camPos = camera.getPosition();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (var entry : OutlineTracker.all().entrySet()) {
            UUID entityId = entry.getKey();
            List<OutlineInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            Entity host = findEntity(mc, entityId);
            if (host == null) continue;

            double ex = host.xo + (host.getX() - host.xo) * partialTick;
            double ey = host.yo + (host.getY() - host.yo) * partialTick;
            double ez = host.zo + (host.getZ() - host.zo) * partialTick;
            int packedLight = dispatcher.getPackedLightCoords(host, partialTick);

            for (OutlineInstance inst : instances) {
                // See-through outlines go through MC's screen-space glow pipeline (see
                // EntityOutlineMixin); this geometry pass only draws the depth-tested ones,
                // where LEQUAL occludes the outline per-pixel behind world geometry.
                if (inst.seeThroughWalls()) continue;

                // Set the outline width for the cel-shader vertex displacement. Each instance
                // gets its own flush so the width can vary per-instance.
                // Convert from "thickness multiplier" (e.g. 1.03) to a world-space offset.
                // Entity models are roughly 1 block tall, so thickness 1.03 → ~0.03 world
                // units of normal displacement, which reads as a thin outline edge.
                float width = Math.max(0.0f, inst.thickness() - 1.0f);
                OutlineShaderHolder.setCurrentWidth(width);

                renderInstance(host, inst, poseStack,
                        ex - camPos.x, ey - camPos.y, ez - camPos.z,
                        partialTick, packedLight, dispatcher, buffers);

                // Flush this instance's draws immediately so the per-instance width applies.
                buffers.endBatch(OutlineRenderTypes.edge(WHITE_TEX));
            }
        }
    }

    private static void renderInstance(Entity host, OutlineInstance inst, PoseStack poseStack,
                                       double relX, double relY, double relZ,
                                       float partialTick, int packedLight,
                                       EntityRenderDispatcher dispatcher,
                                       MultiBufferSource.BufferSource buffers) {
        // Going through renderer.render directly (rather than dispatcher.render) skips the
        // entity shadow, which our flat-colour wrapper would otherwise paint as a solid
        // coloured rectangle on the floor.
        @SuppressWarnings({"rawtypes", "unchecked"})
        EntityRenderer renderer = dispatcher.getRenderer(host);
        if (renderer == null) return;

        // Pass 1 — EDGE: custom cel-shader pipeline displaces each vertex along its NORMAL
        // (in the GPU vertex shader) and cull-fronts away the original-facing surfaces. The
        // result is a thin coloured edge ring behind the model, with no pose-stack scaling
        // pushing body parts apart. Only renders where the entity is visible thanks to
        // LEQUAL depth test.
        renderPass(host, inst, poseStack, relX, relY, relZ, partialTick, packedLight,
                renderer, buffers, OutlineRenderTypes.edge(WHITE_TEX));

        // Pass 2 — BEHIND_WALLS_FILL: only when seeThroughWalls=true. Un-expanded model
        // with GREATER depth test draws the silhouette only at pixels where the outline
        // is behind framebuffer geometry (i.e. behind walls). Where the entity is visible,
        // GREATER fails and nothing draws — so the body never becomes flat-coloured.
        if (inst.seeThroughWalls()) {
            renderPass(host, inst, poseStack, relX, relY, relZ, partialTick, packedLight,
                    renderer, buffers, OutlineRenderTypes.behindWallsFill(WHITE_TEX));
        }
    }

    private static void renderPass(Entity host, OutlineInstance inst, PoseStack poseStack,
                                   double relX, double relY, double relZ,
                                   float partialTick, int packedLight,
                                   @SuppressWarnings("rawtypes") EntityRenderer renderer,
                                   MultiBufferSource.BufferSource buffers,
                                   RenderType targetType) {
        MultiBufferSource flatBuffers = type -> {
            VertexConsumer raw = buffers.getBuffer(targetType);
            return new FlatColorVertexConsumer(raw, inst.r(), inst.g(), inst.b(), inst.a());
        };

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        try {
            //noinspection unchecked
            renderer.render(host, host.getYRot(), partialTick,
                    poseStack, flatBuffers, packedLight);
        } catch (Exception ignored) {
            // Never let an outline render crash the level pass.
        }

        poseStack.popPose();
    }

    private static Entity findEntity(Minecraft mc, UUID id) {
        if (mc.player != null && mc.player.getUUID().equals(id)) return mc.player;
        if (mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) return e;
        }
        return null;
    }
}
