package com.xirc.nichirin.client.chainballaxe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.chainballaxe.ChainBallAxeCollision;
import com.xirc.nichirin.common.chainballaxe.ChainBallAxeWeaponSimulation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders the chain from the flat 2D {@code chain_ball_axe_chain} texture as a smooth, pseudo-3D chain:
 * <ul>
 *   <li>positions are interpolated between physics ticks (partialTick) so it doesn't step at 20 Hz,</li>
 *   <li>the node polyline is Catmull-Rom smoothed so it flows instead of being faceted,</li>
 *   <li>links are placed one-by-one along the arc, each a single quad, and every other link is rotated
 *       90° about the chain axis — a flat link, then a perpendicular link, then flat, ... — so it reads
 *       as an interlocking chain. Links overlap by half their length so consecutive ones interlink.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class ChainBallAxeChainRenderer {

    private static final ResourceLocation CHAIN_TEX =
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/chain_ball_axe_chain.png");
    private static final double HALF_WIDTH = 0.06;  // link half-width in blocks
    private static final int CATMULL_SAMPLES = 3;   // smoothing samples per physics segment
    private static final double LINK_LEN = 0.34;    // arc length of one chain link (one texture tile)

    private ChainBallAxeChainRenderer() {}

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        if (!ClientChainBallAxeWeaponManager.isEnabled()) return;
        ChainBallAxeWeaponSimulation sim = ClientChainBallAxeWeaponManager.sim();
        if (sim == null) return;

        int n = sim.pointCount();
        if (n < 2) return;
        Vec3[] raw = new Vec3[n];
        for (int i = 0; i < n; i++) raw[i] = sim.point(i).renderPosition(partialTick);

        Vec3[] p = smooth(raw); // interpolated + curve-smoothed polyline
        int m = p.length;
        if (m < 2) return;

        // Push the SMOOTHED render points out of terrain too. The sim already keeps its nodes above ground,
        // but Catmull-Rom can overshoot below the surface between them — resolving each drawn point keeps the
        // visible chain from clipping into the floor. Points in open air hit no blocks and cost ~nothing.
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            for (int i = 0; i < m; i++) {
                p[i] = ChainBallAxeCollision.resolve(level, p[i], HALF_WIDTH + 0.04);
            }
        }

        Vec3 cam = camera.getPosition();
        Matrix4f mat = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(CHAIN_TEX);
        VertexConsumer vc = buffers.getBuffer(type);

        // Arc length of the smoothed polyline, so links can be placed at even real-world spacing.
        double[] cum = new double[m];
        for (int i = 1; i < m; i++) cum[i] = cum[i - 1] + p[i].distanceTo(p[i - 1]);
        double total = cum[m - 1];
        if (total < 1.0e-6) return;

        int light = LightTexture.FULL_BRIGHT;
        // Walk the chain placing one link every half-link (so consecutive links overlap and interlock),
        // and rotate every other link 90° about the chain axis: flat, perpendicular, flat, ...
        double step = LINK_LEN * 0.5;
        int linkIdx = 0;
        for (double s = 0.0; s <= total - 1.0e-4; s += step, linkIdx++) {
            double e = Math.min(s + LINK_LEN, total);
            Vec3 a = sampleArc(p, cum, s);
            Vec3 b = sampleArc(p, cum, e);
            Vec3 tangent = b.subtract(a);
            if (tangent.lengthSqr() < 1.0e-8) continue;
            tangent = tangent.normalize();
            Vec3 mid = a.add(b).scale(0.5);
            Vec3 toCam = cam.subtract(mid);
            toCam = toCam.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : toCam.normalize();
            Vec3 base = tangent.cross(toCam);
            if (base.lengthSqr() < 1.0e-8) base = tangent.cross(new Vec3(0, 1, 0));
            if (base.lengthSqr() < 1.0e-8) base = new Vec3(1, 0, 0);
            base = base.normalize();
            Vec3 perp = tangent.cross(base).normalize();
            // Even link = flat (camera-facing, widest); odd link = the same link turned 90° (edge-on/thin).
            Vec3 side = ((linkIdx & 1) == 0 ? base : perp).scale(HALF_WIDTH);
            float nx = (float) toCam.x, ny = (float) toCam.y, nz = (float) toCam.z;
            quad(vc, mat, cam, a.add(side), a.subtract(side),
                    b.subtract(side), b.add(side), 0f, 1f, light, nx, ny, nz);
        }

        buffers.endBatch(type);
    }

    /** Point at arc length {@code s} along the cumulative-length polyline. */
    private static Vec3 sampleArc(Vec3[] p, double[] cum, double s) {
        int n = p.length;
        if (s <= 0.0) return p[0];
        if (s >= cum[n - 1]) return p[n - 1];
        int i = 1;
        while (i < n && cum[i] < s) i++;
        double segLen = cum[i] - cum[i - 1];
        double t = segLen > 1.0e-9 ? (s - cum[i - 1]) / segLen : 0.0;
        return p[i - 1].lerp(p[i], t);
    }

    /** Catmull-Rom densify/smooth the node polyline. */
    private static Vec3[] smooth(Vec3[] pts) {
        int n = pts.length;
        if (n < 3) return pts;
        Vec3[] out = new Vec3[(n - 1) * CATMULL_SAMPLES + 1];
        int idx = 0;
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = pts[Math.max(0, i - 1)];
            Vec3 p1 = pts[i];
            Vec3 p2 = pts[i + 1];
            Vec3 p3 = pts[Math.min(n - 1, i + 2)];
            for (int s = 0; s < CATMULL_SAMPLES; s++) {
                out[idx++] = catmull(p0, p1, p2, p3, s / (double) CATMULL_SAMPLES);
            }
        }
        out[idx] = pts[n - 1];
        return out;
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return new Vec3(
                cr(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                cr(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                cr(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double cr(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, Vec3 cam,
                             Vec3 leftTop, Vec3 rightTop, Vec3 rightBottom, Vec3 leftBottom,
                             float v0, float v1, int light, float nx, float ny, float nz) {
        vertex(vc, mat, cam, leftTop, 0f, v0, light, nx, ny, nz);
        vertex(vc, mat, cam, rightTop, 1f, v0, light, nx, ny, nz);
        vertex(vc, mat, cam, rightBottom, 1f, v1, light, nx, ny, nz);
        vertex(vc, mat, cam, leftBottom, 0f, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Vec3 cam, Vec3 pos,
                               float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(mat, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
