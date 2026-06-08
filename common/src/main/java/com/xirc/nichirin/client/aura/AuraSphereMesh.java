package com.xirc.nichirin.client.aura;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Pre-computed UV sphere topology: vertex positions at unit radius and quad indices.
 * Deformation and per-frame colour/alpha are applied at render time in the renderer.
 *
 * Layout:
 *   - LATS    rings from north (theta=0) to south (theta=pi), inclusive of both poles.
 *   - LONGS   meridian count (we duplicate the seam — index LONGS == index 0 — for simpler
 *             quad emission without wrap-arounds).
 *
 * Vertex indexing: i = ring * (LONGS + 1) + meridian.
 */
@Environment(EnvType.CLIENT)
public final class AuraSphereMesh {
    public static final int LATS  = 14;
    public static final int LONGS = 24;

    /** Unit-sphere vertex positions: [vertexIndex * 3 + (0|1|2)]. */
    public static final float[] POSITIONS;

    static {
        int rings = LATS + 1;
        int meridians = LONGS + 1;
        POSITIONS = new float[rings * meridians * 3];
        for (int r = 0; r < rings; r++) {
            float theta = (float) (Math.PI * r / LATS);   // 0 .. pi
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            for (int m = 0; m < meridians; m++) {
                float phi = (float) (2.0 * Math.PI * m / LONGS); // 0 .. 2pi
                float sinP = (float) Math.sin(phi);
                float cosP = (float) Math.cos(phi);
                int i = (r * meridians + m) * 3;
                POSITIONS[i    ] = sinT * cosP;
                POSITIONS[i + 1] = cosT;
                POSITIONS[i + 2] = sinT * sinP;
            }
        }
    }

    private AuraSphereMesh() {}

    public static int meridians() { return LONGS + 1; }
    public static int rings()     { return LATS + 1; }
}
