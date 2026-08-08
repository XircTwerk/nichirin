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
    private static final float CUBE_ALPHA = 0.62f;
    private static final float TRAIL_ALPHA = 0.5f;
    private static final long TRAIL_LIFETIME_MS = 240L;
    private static final int MAX_SAMPLES = 16;
    private static final long STALE_MS = 90L;

    private static final Map<Key, State> FISTS = new HashMap<>();

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
            long seed = entry.getKey().player().getLeastSignificantBits() + entry.getKey().arm() * 977L;
            drawTrail(vc, matrix, cam, s, base, time, seed, now);
            drawCube(vc, matrix, cam, s.cube, base, time, seed);
        }

        buffers.endBatch(type);
    }

    public static boolean hasFists() {
        prune(Util.getMillis());
        return !FISTS.isEmpty();
    }

    private static void drawCube(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3[] c,
                                 float[] base, float time, long seed) {
        if (c == null || c.length < 8) return;
        gridFace(vc, m, cam, c[0], c[1], c[2], c[3], base, CUBE_ALPHA, time, seed);        // top
        gridFace(vc, m, cam, c[4], c[5], c[6], c[7], base, CUBE_ALPHA, time, seed + 11);   // bottom (hand)
        gridFace(vc, m, cam, c[3], c[2], c[6], c[7], base, CUBE_ALPHA, time, seed + 23);   // front
        gridFace(vc, m, cam, c[0], c[1], c[5], c[4], base, CUBE_ALPHA, time, seed + 37);   // back
        gridFace(vc, m, cam, c[0], c[3], c[7], c[4], base, CUBE_ALPHA, time, seed + 53);   // left
        gridFace(vc, m, cam, c[1], c[2], c[6], c[5], base, CUBE_ALPHA, time, seed + 71);   // right
    }

    /** Sweeps the four hand-face corners through their history as a pixel-textured box tube. */
    private static void drawTrail(VertexConsumer vc, Matrix4f m, Vec3 cam, State s,
                                  float[] base, float time, long seed, long now) {
        Sample previous = null;
        for (Sample current : s.trail) {
            if (previous != null && previous.face[0].distanceToSqr(current.face[0]) <= TELEPORT_SQR) {
                float life = 1.0f - Math.min(1.0f, (now - current.time) / (float) TRAIL_LIFETIME_MS);
                if (life > 0.0f) {
                    float alpha = TRAIL_ALPHA * life * life;
                    for (int i = 0; i < 4; i++) {
                        int j = (i + 1) % 4;
                        gridFace(vc, m, cam, previous.face[i], previous.face[j],
                                current.face[j], current.face[i], base, alpha, time, seed + i * 13L);
                    }
                }
            }
            previous = current;
        }
    }

    /** Renders a quad as a grid of PIXEL-sized cells, each with an aura-style flowing shade. */
    private static void gridFace(VertexConsumer vc, Matrix4f m, Vec3 cam,
                                 Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                 float[] base, float baseAlpha, float time, long seed) {
        int nU = Math.min(MAX_CELLS, Math.max(1, (int) Math.ceil(a.distanceTo(b) / PIXEL)));
        int nV = Math.min(MAX_CELLS, Math.max(1, (int) Math.ceil(a.distanceTo(d) / PIXEL)));
        for (int i = 0; i < nU; i++) {
            for (int j = 0; j < nV; j++) {
                Vec3 p00 = bilerp(a, b, c, d, i / (double) nU, j / (double) nV);
                Vec3 p10 = bilerp(a, b, c, d, (i + 1) / (double) nU, j / (double) nV);
                Vec3 p11 = bilerp(a, b, c, d, (i + 1) / (double) nU, (j + 1) / (double) nV);
                Vec3 p01 = bilerp(a, b, c, d, i / (double) nU, (j + 1) / (double) nV);

                float flow = (float) Math.sin(i * 0.63 - j * 0.31 - time * 2.1 + seed * 0.0007);
                int shade = flow < -0.58f ? 1 : flow > 0.62f ? 4 : ((i + j) & 1) == 0 ? 3 : 2;
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
