package com.xirc.nichirin.client.gyomei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.common.gyomei.GyomeiPhysicsConfig;
import com.xirc.nichirin.common.gyomei.GyomeiWeaponSimulation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Debug visualization for the Gyomei physics sim (per the spec, this must work before any visual
 * polish). World lines: AXE = blue, CHAIN = white, GRIP = yellow, FLAIL = red. Plus a HUD readout of
 * speeds / tension / grip so the physics can be tuned by eye.
 */
@Environment(EnvType.CLIENT)
public final class GyomeiDebugRenderer {

    private GyomeiDebugRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        if (!ClientGyomeiWeaponManager.isEnabled()) return;
        GyomeiWeaponSimulation sim = ClientGyomeiWeaponManager.sim();
        if (sim == null) return;

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Vec3 cam = camera.getPosition();
        Matrix4f mat = poseStack.last().pose();

        // Interpolated markers (the chain itself is drawn smoothed by GyomeiChainRenderer).
        box(vc, mat, cam, sim.axe.renderPosition(partialTick), sim.axe.radius, 0.35f, 0.55f, 1.0f);   // axe  = blue
        box(vc, mat, cam, sim.flail.renderPosition(partialTick), sim.flail.radius, 1.0f, 0.25f, 0.25f); // flail = red
        box(vc, mat, cam, sim.point(sim.axeIndex()).renderPosition(partialTick), 0.12, 1.0f, 0.95f, 0.2f); // grip = yellow

        buffers.endBatch(RenderType.lines());
    }

    public static void renderHud(GuiGraphics g) {
        if (!ClientGyomeiWeaponManager.isEnabled()) return;
        GyomeiWeaponSimulation sim = ClientGyomeiWeaponManager.sim();
        if (sim == null) return;
        var font = Minecraft.getInstance().font;
        int x = 6, y = 6, line = 10;
        g.drawString(font, "GYOMEI PHYSICS DEBUG", x, y, 0xFFFFFFFF); y += line + 2;
        g.drawString(font, "grip:        " + sim.gripMode, x, y, 0xFFFFEE55); y += line;
        g.drawString(font, String.format("axe speed:   %.3f", sim.axe.speed()), x, y, 0xFF66AAFF); y += line;
        g.drawString(font, String.format("flail speed: %.3f", sim.flail.speed()), x, y, 0xFFFF6666); y += line;
        g.drawString(font, String.format("tension:     %.2f", sim.tension), x, y, 0xFFFFFFFF); y += line;
        g.drawString(font, String.format("nodes: %d   iters: %dx%d", sim.nodeCount(),
                GyomeiPhysicsConfig.CONSTRAINT_ITERATIONS, GyomeiPhysicsConfig.SUBSTEPS), x, y, 0xFFAAAAAA);
    }

    private static void line(VertexConsumer vc, Matrix4f mat, Vec3 cam, Vec3 a, Vec3 b,
                             float r, float g, float bl) {
        vc.addVertex(mat, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z))
                .setColor(r, g, bl, 1.0f).setNormal(0.0f, 1.0f, 0.0f);
        vc.addVertex(mat, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z))
                .setColor(r, g, bl, 1.0f).setNormal(0.0f, 1.0f, 0.0f);
    }

    private static void box(VertexConsumer vc, Matrix4f mat, Vec3 cam, Vec3 c, double s,
                            float r, float g, float b) {
        double x0 = c.x - s, x1 = c.x + s, y0 = c.y - s, y1 = c.y + s, z0 = c.z - s, z1 = c.z + s;
        Vec3[] v = {
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] e : edges) line(vc, mat, cam, v[e[0]], v[e[1]], r, g, b);
    }
}
