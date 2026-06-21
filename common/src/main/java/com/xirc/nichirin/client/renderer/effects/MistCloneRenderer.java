package com.xirc.nichirin.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders 6 ghost clones of the caster orbiting the Sea of Clouds finisher circle.
 * Activated server-side via MistClonesPacket; rendered entirely client-side.
 */
@Environment(EnvType.CLIENT)
public class MistCloneRenderer {

    private static final int CLONE_COUNT = 6;

    private record CloneEntry(int casterId, Vec3 center, float radius, long spawnTick, long lifetimeTicks) {}

    private static final List<CloneEntry> ENTRIES = new ArrayList<>();

    public static void addClones(int casterId, Vec3 center, float radius, long lifetimeTicks) {
        ENTRIES.removeIf(e -> e.casterId() == casterId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ENTRIES.add(new CloneEntry(casterId, center, radius, mc.level.getGameTime(), lifetimeTicks));
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ENTRIES.isEmpty()) return;

        // Expire old entries
        long now = mc.level.getGameTime();
        ENTRIES.removeIf(e -> now - e.spawnTick() >= e.lifetimeTicks());
        if (ENTRIES.isEmpty()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = camera.getPosition();

        for (CloneEntry entry : ENTRIES) {
            Entity casterEntity = mc.level.getEntity(entry.casterId());
            if (!(casterEntity instanceof AbstractClientPlayer caster)) continue;

            // Slowly rotate the ring over time for a swirling effect
            double rotOffset = (now + partialTick) * 0.05;

            for (int i = 0; i < CLONE_COUNT; i++) {
                double angle = (2.0 * Math.PI * i / CLONE_COUNT) + rotOffset;
                double wx = entry.center().x + Math.cos(angle) * entry.radius();
                double wz = entry.center().z + Math.sin(angle) * entry.radius();
                double wy = entry.center().y;

                // Camera-relative position required by EntityRenderDispatcher
                double relX = wx - camPos.x;
                double relY = wy - camPos.y;
                double relZ = wz - camPos.z;

                // Face outward from the circle center
                float facing = (float) Math.toDegrees(angle) + 90f;

                poseStack.pushPose();
                mc.getEntityRenderDispatcher().render(
                        caster, relX, relY, relZ,
                        facing, partialTick,
                        poseStack, bufferSource, LightTexture.FULL_BRIGHT
                );
                poseStack.popPose();
            }
        }

        bufferSource.endBatch();
    }
}