package com.xirc.nichirin.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.common.aura.AuraInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;

/**
 * Debug overlay: draws a wireframe radius circle and short XYZ axes at every aura host.
 */
@Environment(EnvType.CLIENT)
public final class AuraDebugRenderer {

    private AuraDebugRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());

        Vec3 camPos = camera.getPosition();
        float r = AuraDebugState.debugR;
        float g = AuraDebugState.debugG;
        float b = AuraDebugState.debugB;
        float a = AuraDebugState.debugA;

        for (var entry : EntityAuraTracker.all().entrySet()) {
            UUID id = entry.getKey();
            List<AuraInstance> insts = entry.getValue();
            if (insts.isEmpty()) continue;
            Entity host = findEntity(mc, id);
            if (host == null) continue;

            double ex = host.xo + (host.getX() - host.xo) * partialTick;
            double ey = host.yo + (host.getY() - host.yo) * partialTick + host.getBbHeight() * 0.5;
            double ez = host.zo + (host.getZ() - host.zo) * partialTick;

            poseStack.pushPose();
            poseStack.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);
            Matrix4f mat = poseStack.last().pose();

            for (AuraInstance inst : insts) {
                drawCircle(vc, mat, inst.radius(), r, g, b, a);
                drawAxes(vc, mat, inst.radius(), r, g, b, a);
            }
            poseStack.popPose();
        }

        buffers.endBatch(RenderType.lines());
    }

    private static void drawCircle(VertexConsumer vc, Matrix4f mat, float radius,
                                   float r, float g, float b, float a) {
        int segments = 48;
        float prevX = radius, prevZ = 0f;
        for (int i = 1; i <= segments; i++) {
            float t = (float) (Math.PI * 2.0 * i / segments);
            float x = (float) (Math.cos(t) * radius);
            float z = (float) (Math.sin(t) * radius);
            line(vc, mat, prevX, 0, prevZ, x, 0, z, r, g, b, a);
            prevX = x; prevZ = z;
        }
    }

    private static void drawAxes(VertexConsumer vc, Matrix4f mat, float radius,
                                 float r, float g, float b, float a) {
        float len = radius * 1.1f;
        line(vc, mat, 0, 0, 0,  len, 0, 0,   1f, 0.2f, 0.2f, a);
        line(vc, mat, 0, 0, 0,  0, len, 0,   0.2f, 1f, 0.2f, a);
        line(vc, mat, 0, 0, 0,  0, 0, len,   0.2f, 0.2f, 1f, a);
    }

    private static void line(VertexConsumer vc, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(1f, 0f, 0f);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(1f, 0f, 0f);
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
