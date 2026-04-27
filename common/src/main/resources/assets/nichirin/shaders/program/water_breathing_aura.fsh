#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Progress;   // drives time / animation
uniform float Intensity;  // 0.0 → 1.0, controlled by breathing activation

in vec2 texCoord;
out vec4 fragColor;

// ── constants ────────────────────────────────────────────────────────────────
const float PI      = 3.14159265359;
const float TAU     = 6.28318530718;

const vec3 COL_DEEP   = vec3(0.05, 0.20, 0.55);  // deep ocean blue
const vec3 COL_MID    = vec3(0.15, 0.55, 0.90);  // mid water
const vec3 COL_BRIGHT = vec3(0.55, 0.85, 1.00);  // foam / highlight
const vec3 COL_FOAM   = vec3(0.80, 0.95, 1.00);  // pure white foam

// ── helpers ──────────────────────────────────────────────────────────────────
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p *= 2.1;
        a *= 0.5;
    }
    return v;
}

// ── ribbon: a single flowing water ribbon at a given angular phase ────────────
// returns brightness [0,1] for that ribbon layer
float ribbon(vec2 uv, float phase, float time, float thickness) {
    float angle = atan(uv.y, uv.x) + phase;
    float r     = length(uv);

    // sinusoidal path that undulates over time
    float target = 0.28
        + 0.06 * sin(angle * 3.0 + time * 1.8 + phase)
        + 0.04 * sin(angle * 5.0 - time * 2.4 + phase * 1.7)
        + 0.02 * fbm(uv * 3.0 + time * 0.3);

    float band = smoothstep(thickness, 0.0, abs(r - target));
    // fade ribbon near dead-center and near screen edges
    band *= smoothstep(0.05, 0.12, r);
    band *= smoothstep(0.70, 0.50, r);
    return band;
}

// ── radial ripple ring ────────────────────────────────────────────────────────
float rippleRing(vec2 uv, float radius, float width) {
    return smoothstep(width, 0.0, abs(length(uv) - radius));
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 uv = texCoord - vec2(0.5, 0.5); // center origin, [-0.5, 0.5]
    uv.x *= OutSize.x / OutSize.y;        // aspect-correct

    float time = Progress * 8.0;          // drives all animation
    float r    = length(uv);

    // ── 1. Flowing ribbons (6 layers, evenly spaced in angle) ─────────────
    float ribbons = 0.0;
    for (int i = 0; i < 6; i++) {
        float phase = float(i) * TAU / 6.0;
        float speed = 0.9 + float(i) * 0.15;
        ribbons += ribbon(uv, phase, time * speed, 0.012 + float(i) * 0.002);
    }
    ribbons = clamp(ribbons, 0.0, 1.0);

    // ── 2. Expanding ripple rings from center ────────────────────────────
    float rings = 0.0;
    for (int i = 0; i < 6; i++) {
        // each ring starts at center and expands outward, looping
        float phase  = float(i) / 6.0;
        float travel = fract(time * 0.18 + phase); // 0→1 travel progress
        float radius = travel * 0.65;
        float alpha  = (1.0 - travel) * (1.0 - travel);
        rings += rippleRing(uv, radius, 0.008) * alpha;
    }
    rings = clamp(rings, 0.0, 1.0);

    // ── 3. Edge vignette — cool blue aura at screen border ───────────────
    float edgeDist  = 1.0 - smoothstep(0.30, 0.72, r);  // 1 near center, 0 at edge
    float auraEdge  = 1.0 - edgeDist;                    // strong at edges
    auraEdge = pow(auraEdge, 2.2);

    // subtle noise shimmer on the vignette
    float shimmer = fbm(uv * 6.0 + time * 0.4) * 0.4 + 0.6;
    auraEdge *= shimmer;

    // ── 4. Center glow ────────────────────────────────────────────────────
    float centerGlow = smoothstep(0.25, 0.0, r) * 0.35;

    // ── 5. Compose aura color ─────────────────────────────────────────────
    vec3 auraColor = vec3(0.0);

    // ribbons: bright mid-blue to foam
    auraColor += mix(COL_MID, COL_BRIGHT, ribbons * 0.7) * ribbons;

    // rings: clean bright blue
    auraColor += COL_BRIGHT * rings * 1.1;

    // edge vignette: deep → mid gradient
    auraColor += mix(COL_DEEP, COL_MID, edgeDist * 0.6) * auraEdge * 0.55;

    // center glow
    auraColor += COL_MID * centerGlow;

    // ── 6. Blend onto scene ───────────────────────────────────────────────
    // total aura mask: strongest where effects exist, falloff elsewhere
    float auraMask = clamp(
        ribbons * 0.85 + rings * 0.7 + auraEdge * 0.6 + centerGlow,
        0.0, 1.0
    );

    // slight blue tint of the whole scene at full intensity
    vec3 tintedScene = mix(original.rgb, original.rgb * vec3(0.80, 0.90, 1.05), Intensity * 0.18);

    vec3 finalColor = tintedScene + auraColor * Intensity;

    // soft vignette darkening at very edges (frames the effect)
    float vignette = smoothstep(0.85, 0.40, r);
    finalColor = mix(finalColor * 0.75, finalColor, vignette);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}
