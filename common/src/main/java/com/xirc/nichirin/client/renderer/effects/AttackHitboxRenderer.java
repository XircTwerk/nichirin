package com.xirc.nichirin.client.renderer.effects;

import com.google.common.collect.EvictingQueue;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Renders attack hitboxes for visual debugging
 */
public class AttackHitboxRenderer {

    // Limit to 8 hitboxes at once, old ones get evicted
    private static final Queue<Pair<LongLongPair, AABB>> hitboxes = EvictingQueue.create(8);

    // High priority hitboxes that don't get evicted
    private static final List<Pair<LongLongPair, AABB>> highPriorityBoxes = new ArrayList<>();

    /**
     * Initialize the renderer
     */
    public static void init() {
    }

    /**
     * Add a single hitbox with default duration
     */
    public static void addHitbox(final AABB box) {
        addHitbox(box, 2500, false);
    }

    /**
     * Add multiple hitboxes at once
     */
    public static void addHitboxes(final Collection<AABB> boxes) {
        for (AABB box : boxes) {
            addHitbox(box);
        }
    }

    /**
     * Add hitboxes from an array
     */
    public static void addHitboxes(final AABB[] boxes) {
        for (AABB box : boxes) {
            addHitbox(box);
        }
    }

    /**
     * Add a hitbox with custom duration and priority
     */
    public static synchronized void addHitbox(final AABB box, final long durationMs, final boolean highPriority) {
        var hitboxEntry = Pair.of(LongLongPair.of(Util.getEpochMillis(), durationMs), box);

        if (highPriority) {
            highPriorityBoxes.add(hitboxEntry);
        } else {
            hitboxes.add(hitboxEntry);
        }
    }

    /**
     * Main renderingrendering method called every frame
     */
    public static synchronized void render(final PoseStack matrices, final Vec3 camPos,
                                           final LevelRenderer worldRenderer, final MultiBufferSource consumerProvider) {

        // Only render if debug hitboxes are enabled
        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }

        if (getHitboxCount() == 0) {
            return;
        }

        try {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            matrices.pushPose();

            renderBoxCollection(consumerProvider, matrices, camPos, hitboxes);
            renderBoxCollection(consumerProvider, matrices, camPos, highPriorityBoxes);

            matrices.popPose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Render a collection of hitboxes and remove expired ones
     */
    private static void renderBoxCollection(final MultiBufferSource consumerProvider, final PoseStack matrices,
                                            final Vec3 camPos, final Collection<Pair<LongLongPair, AABB>> boxes) {

        final Iterator<Pair<LongLongPair, AABB>> iterator = boxes.iterator();
        int renderedCount = 0;

        while (iterator.hasNext()) {
            final Pair<LongLongPair, AABB> pair = iterator.next();

            // Render the hitbox
            renderSingleBox(consumerProvider, pair.right(), matrices, camPos);
            renderedCount++;

            // Check if expired and remove
            long currentTime = Util.getEpochMillis();
            long creationTime = pair.left().leftLong();
            long duration = pair.left().rightLong();

            if (currentTime - creationTime > duration) {
                iterator.remove();
            }
        }
    }

    /**
     * Render a single hitbox with both faces and wireframe
     */
    private static void renderSingleBox(final MultiBufferSource consumerProvider, final AABB box,
                                        final PoseStack matrices, final Vec3 camPos) {

        // Semi-transparent red faces
        renderBoxFaces(box, matrices, camPos, 0x54FF0000);

        // Red wireframe outline
        renderBoxWireframe(consumerProvider, box, matrices, camPos);
    }

    /**
     * Render filled faces of the hitbox
     */
    private static void renderBoxFaces(final AABB box, final PoseStack matrices, final Vec3 camPos, final int color) {

        final Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false); // Don't write to depth buffer

        final float minX = (float) (box.minX - camPos.x);
        final float minY = (float) (box.minY - camPos.y);
        final float minZ = (float) (box.minZ - camPos.z);
        final float maxX = (float) (box.maxX - camPos.x);
        final float maxY = (float) (box.maxY - camPos.y);
        final float maxZ = (float) (box.maxZ - camPos.z);

        final Matrix4f matrix = matrices.last().pose();

        // Top face (Y+)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color);

        // Bottom face (Y-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color);

        // North face (Z-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color);

        // East face (X+)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color);

        // South face (Z+)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color);

        // West face (X-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    /**
     * Render wireframe outline of the hitbox
     */
    private static void renderBoxWireframe(final MultiBufferSource consumerProvider, final AABB box,
                                           final PoseStack matrices, final Vec3 camPos) {

        final VertexConsumer lineConsumer = consumerProvider.getBuffer(RenderType.LINES);

        // Convert to camera-relative coordinates
        final float minX = (float) (box.minX - camPos.x);
        final float minY = (float) (box.minY - camPos.y);
        final float minZ = (float) (box.minZ - camPos.z);
        final float maxX = (float) (box.maxX - camPos.x);
        final float maxY = (float) (box.maxY - camPos.y);
        final float maxZ = (float) (box.maxZ - camPos.z);

        final AABB relativeBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // Render the wireframe (bright red)
        LevelRenderer.renderLineBox(matrices, lineConsumer, relativeBox, 1.0f, 0.0f, 0.0f, 1.0f);
    }

    /**
     * Clear all hitboxes (useful for cleanup)
     */
    public static synchronized void clearAll() {
        hitboxes.clear();
        highPriorityBoxes.clear();
    }

    /**
     * Get the current number of rendered hitboxes
     */
    public static synchronized int getHitboxCount() {
        return hitboxes.size() + highPriorityBoxes.size();
    }


}