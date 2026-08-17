package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.client.aura.AuraRenderTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Destructive Death "blue fist": a cube enveloping each empty hand, rendered with the SAME
 * geometric pixel-grid texturing as the breathing aura (small quads with flowing per-cell shades,
 * via {@link AuraRenderTypes}) — just blue, or red in Overdrive. It also sweeps a full-width box-tube
 * trail as the hand moves. Fed the arms' world-space cube geometry each frame by ArmTrailLayer;
 * drawn in the aura render hook, NOT the screen-space VFX pixel pass.
 */
@Environment(EnvType.CLIENT)
public final class FistAuraRenderer {

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    // Base colours (0..1). Blue normally, red in Overdrive.
    private static final float[] BLUE = {0.18f, 0.42f, 1.0f};
    private static final float[] RED = {1.0f, 0.18f, 0.20f};

    private static final float PIXEL = 2.0f / 16.0f;     // aura-scale chunky cells
    private static final int MAX_CELLS = 8;              // hard cap on grid subdivision per face
    // Beyond this squared distance a consecutive hand-face sample is treated as a teleport (e.g. the
    // Compass Needle charge yanks the player every tick) — the trail resets instead of spanning the
    // gap, which would subdivide a huge wall into thousands of quads and freeze the render thread.
    private static final double TELEPORT_SQR = 4.0;
    private static final float CUBE_ALPHA = 0.40f;       // the fist shell — one connected shape
    private static final float TRAIL_ALPHA = 0.5f;

    // Bright energy strips that run UP the arm (wrist -> hand). Each spans the full length so it
    // connects the wrist and hand ends. Lifted well clear of the shell so they sit ON TOP of the fist
    // as a distinct raised layer instead of embedded flush inside it.
    private static final float STRIP_ALPHA = 1.0f;
    private static final float STRIP_LIFT = 1.0f / 16.0f; // stand proud of the shell, on top of the fist
    private static final double[] STRIP_POSITIONS = {0.30, 0.70}; // around the arm (u), spaced
    private static final double STRIP_HALF_U = 0.12;      // strip width (fraction of face width)
    private static final long TRAIL_LIFETIME_MS = 240L;
    private static final int MAX_SAMPLES = 16;
    private static final long STALE_MS = 90L;

    private static final Map<Key, State> FISTS = new HashMap<>();

    private static final Matrix4f IDENTITY = new Matrix4f();

    private FistAuraRenderer() {}

    private record Key(UUID player, int arm) {}

    private static final class State {
        Vec3[] cube = new Vec3[8];
        final ArrayDeque<Sample> trail = new ArrayDeque<>();
        boolean red;
        long lastUpdate;
    }

    private record Sample(Vec3[] face, long time) {}

    /** @param cube 8 world corners: [0..3] wrist ring, [4..7] hand ring. */
    public static void capture(UUID player, int arm, Vec3[] cube, boolean red) {
        Key key = new Key(player, arm);
        State s = FISTS.computeIfAbsent(key, k -> new State());
        s.cube = cube;
        s.red = red;
        long now = Util.getMillis();
        s.lastUpdate = now;

        Vec3[] face = {cube[4], cube[5], cube[6], cube[7]};
        Sample last = s.trail.peekLast();
        if (last != null && last.face[0].distanceToSqr(face[0]) > TELEPORT_SQR) {
            // Teleport/rubber-band — start the trail fresh so we never span the gap.
            s.trail.clear();
            last = null;
        }
        if (last == null || last.face[0].distanceToSqr(face[0]) > 0.0006) {
            s.trail.addLast(new Sample(face, now));
            while (s.trail.size() > MAX_SAMPLES) s.trail.removeFirst();
        }
    }

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        long now = Util.getMillis();
        prune(now);
        if (FISTS.isEmpty()) return;

        Matrix4f matrix = poseStack.last().pose();
        Vec3 cam = camera.getPosition();
        float time = (now % 1_000_000L) / 1000.0f * 1.6f;

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType type = AuraRenderTypes.auraTranslucentNoDepthWrite(WHITE);
        VertexConsumer vc = buffers.getBuffer(type);

        for (Map.Entry<Key, State> entry : FISTS.entrySet()) {
            State s = entry.getValue();
            float[] base = s.red ? RED : BLUE;
            drawTrail(vc, matrix, cam, s, base, time, now);
            drawCube(vc, matrix, cam, s.cube, base, time);
            drawArmStrips(vc, matrix, cam, s.cube, base, time);
        }

        buffers.endBatch(type);
    }

    public static boolean hasFists() {
        prune(Util.getMillis());
        return !FISTS.isEmpty();
    }

    /**
     * Draws the fist onto a first-person arm. The caller (FirstPersonFistMixin) supplies the 8 cube
     * corners already transformed into camera/view space, so we draw with an identity matrix and no
     * camera offset. No trail here — the first-person viewmodel swing would make a box tube noisy.
     *
     * @param cube 8 view-space corners: [0..3] wrist ring, [4..7] hand ring.
     */
    public static void renderFirstPerson(MultiBufferSource buffer, Vec3[] cube, boolean red) {
        if (cube == null || cube.length < 8) return;
        long now = Util.getMillis();
        float time = (now % 1_000_000L) / 1000.0f * 1.6f;
        float[] base = red ? RED : BLUE;
        VertexConsumer vc = buffer.getBuffer(AuraRenderTypes.auraTranslucentNoDepthWrite(WHITE));
        drawCube(vc, IDENTITY, Vec3.ZERO, cube, base, time);
        drawArmStrips(vc, IDENTITY, Vec3.ZERO, cube, base, time);
    }

    private static void drawCube(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3[] c,
                                 float[] base, float time) {
        if (c == null || c.length < 8) return;
        gridFace(vc, m, cam, c[0], c[1], c[2], c[3], base, CUBE_ALPHA, time);   // top
        gridFace(vc, m, cam, c[4], c[5], c[6], c[7], base, CUBE_ALPHA, time);   // bottom (hand)
        gridFace(vc, m, cam, c[3], c[2], c[6], c[7], base, CUBE_ALPHA, time);   // front
        gridFace(vc, m, cam, c[0], c[1], c[5], c[4], base, CUBE_ALPHA, time);   // back
        gridFace(vc, m, cam, c[0], c[3], c[7], c[4], base, CUBE_ALPHA, time);   // left
        gridFace(vc, m, cam, c[1], c[2], c[6], c[5], base, CUBE_ALPHA, time);   // right
    }

    /**
     * Draws the bright energy strips that run UP each long face (wrist -> hand). Every strip spans the
     * full length so it ties the wrist and hand ends into one connected shape, lifted just off the shell.
     */
    private static void drawArmStrips(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3[] c,
                                      float[] base, float time) {
        if (c == null || c.length < 8) return;
        Vec3 center = cubeCenter(c);
        // Each long face as (wristA, wristB, handB, handA): u runs around the arm, v runs wrist -> hand.
        Vec3[][] faces = {
                {c[3], c[2], c[6], c[7]}, // front
                {c[0], c[1], c[5], c[4]}, // back
                {c[0], c[3], c[7], c[4]}, // left
                {c[1], c[2], c[6], c[5]}  // right
        };
        for (Vec3[] face : faces) {
            Vec3 lift = outwardNormal(face, center).scale(STRIP_LIFT);
            for (double u : STRIP_POSITIONS) {
                stripOnFace(vc, m, cam, face, lift, u, base, time);
            }
        }
    }

    private static void stripOnFace(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3[] face, Vec3 lift,
                                    double uCenter, float[] base, float time) {
        Vec3 a = face[0], b = face[1], c = face[2], d = face[3];
        double u0 = uCenter - STRIP_HALF_U, u1 = uCenter + STRIP_HALF_U;
        // Full length (v: 0 -> 1) so the strip runs the whole arm and joins the fist.
        Vec3 p00 = bilerp(a, b, c, d, u0, 0.0).add(lift);
        Vec3 p10 = bilerp(a, b, c, d, u1, 0.0).add(lift);
        Vec3 p11 = bilerp(a, b, c, d, u1, 1.0).add(lift);
        Vec3 p01 = bilerp(a, b, c, d, u0, 1.0).add(lift);
        gridFace(vc, m, cam, p00, p10, p11, p01, base, STRIP_ALPHA, time);
    }

    private static Vec3 cubeCenter(Vec3[] c) {
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < 8; i++) { x += c[i].x; y += c[i].y; z += c[i].z; }
        return new Vec3(x / 8.0, y / 8.0, z / 8.0);
    }

    /**
     * Unit normal of a face, flipped if needed so it ALWAYS points away from the cube centre. Deriving
     * it purely from the cross product depended on consistent winding across faces, which this cube's
     * corner order doesn't guarantee — so some faces pushed their strips inward/sideways. Orienting
     * against the centre removes that ambiguity.
     */
    private static Vec3 outwardNormal(Vec3[] face, Vec3 center) {
        Vec3 a = face[0], b = face[1], d = face[3];
        Vec3 n = b.subtract(a).cross(d.subtract(a));
        double len = n.length();
        if (len < 1.0e-6) return new Vec3(0.0, 1.0, 0.0);
        n = n.scale(1.0 / len);
        Vec3 faceCenter = a.add(face[2]).scale(0.5); // a and face[2] are diagonally opposite corners
        return n.dot(faceCenter.subtract(center)) < 0 ? n.scale(-1.0) : n;
    }

    /** Sweeps the four hand-face corners through their history as a pixel-textured box tube. */
    private static void drawTrail(VertexConsumer vc, Matrix4f m, Vec3 cam, State s,
                                  float[] base, float time, long now) {
        Sample previous = null;
        for (Sample current : s.trail) {
            if (previous != null && previous.face[0].distanceToSqr(current.face[0]) <= TELEPORT_SQR) {
                float life = 1.0f - Math.min(1.0f, (now - current.time) / (float) TRAIL_LIFETIME_MS);
                if (life > 0.0f) {
                    float alpha = TRAIL_ALPHA * life * life;
                    for (int i = 0; i < 4; i++) {
                        int j = (i + 1) % 4;
                        gridFace(vc, m, cam, previous.face[i], previous.face[j],
                                current.face[j], current.face[i], base, alpha, time);
                    }
                }
            }
            previous = current;
        }
    }

    /** Renders a quad as a grid of PIXEL-sized cells, each with an aura-style flowing shade. */
    private static void gridFace(VertexConsumer vc, Matrix4f m, Vec3 cam,
                                 Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                 float[] base, float baseAlpha, float time) {
        int nU = Math.min(MAX_CELLS, Math.max(1, (int) Math.ceil(a.distanceTo(b) / PIXEL)));
        int nV = Math.min(MAX_CELLS, Math.max(1, (int) Math.ceil(a.distanceTo(d) / PIXEL)));
        for (int i = 0; i < nU; i++) {
            for (int j = 0; j < nV; j++) {
                Vec3 p00 = bilerp(a, b, c, d, i / (double) nU, j / (double) nV);
                Vec3 p10 = bilerp(a, b, c, d, (i + 1) / (double) nU, j / (double) nV);
                Vec3 p11 = bilerp(a, b, c, d, (i + 1) / (double) nU, (j + 1) / (double) nV);
                Vec3 p01 = bilerp(a, b, c, d, i / (double) nU, (j + 1) / (double) nV);

                // Position-based texturing (NOT per-face): the flowing shade and pixel dither come from
                // the cell's own position, so the pattern flows continuously across every face of the
                // fist instead of reseeding — and seaming — at each face boundary.
                Vec3 ctr = p00.add(p11).scale(0.5);
                float flow = (float) Math.sin((ctr.x + ctr.y * 0.7 + ctr.z) * 5.0 - time * 2.1);
                int gx = (int) Math.floor(ctr.x / PIXEL);
                int gy = (int) Math.floor(ctr.y / PIXEL);
                int gz = (int) Math.floor(ctr.z / PIXEL);
                boolean checker = ((gx + gy + gz) & 1) == 0;
                int shade = flow < -0.58f ? 1 : flow > 0.62f ? 4 : checker ? 3 : 2;
                float alpha = baseAlpha * (0.72f + 0.28f * (flow * 0.5f + 0.5f));
                float r = shadeCh(base[0], shade);
                float g = shadeCh(base[1], shade);
                float bl = shadeCh(base[2], shade);
                cell(vc, m, cam, p00, p10, p11, p01, r, g, bl, alpha);
            }
        }
    }

    private static Vec3 bilerp(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double u, double v) {
        Vec3 top = a.lerp(b, u);
        Vec3 bottom = d.lerp(c, u);
        return top.lerp(bottom, v);
    }

    private static void cell(VertexConsumer vc, Matrix4f m, Vec3 cam,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float alpha) {
        vertex(vc, m, cam, a, r, g, bl, alpha);
        vertex(vc, m, cam, b, r, g, bl, alpha);
        vertex(vc, m, cam, c, r, g, bl, alpha);
        vertex(vc, m, cam, d, r, g, bl, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 p,
                               float r, float g, float b, float a) {
        vc.addVertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor(r, g, b, Math.min(1.0f, a))
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static float shadeCh(float c, int shade) {
        return switch (shade) {
            case 1 -> c * 0.56f;
            case 2 -> c * 0.78f;
            case 4 -> c + (1.0f - c) * 0.30f;
            default -> c;
        };
    }

    private static void prune(long now) {
        Iterator<Map.Entry<Key, State>> it = FISTS.entrySet().iterator();
        while (it.hasNext()) {
            State s = it.next().getValue();
            while (!s.trail.isEmpty() && now - s.trail.peekFirst().time > TRAIL_LIFETIME_MS) {
                s.trail.removeFirst();
            }
            if (now - s.lastUpdate > STALE_MS && s.trail.isEmpty()) it.remove();
        }
    }
}
