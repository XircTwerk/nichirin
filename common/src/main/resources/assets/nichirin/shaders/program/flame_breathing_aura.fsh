#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Progress;   // drives time / animation
uniform float Intensity;  // 0.0 → 1.0, controlled by breathing activation

in vec2 texCoord;
out vec4 fragColor;

// ── constants ────────────────────────────────────────────────────────────────
const float PI  = 3.14159265359;
const float TAU = 6.28318530718;

const vec3 COL_CORE   = vec3(1.00, 0.98, 0.80);  // white-hot inner core
const vec3 COL_INNER  = vec3(1.00, 0.65, 0.10);  // bright orange
const vec3 COL_MID    = vec3(0.90, 0.28, 0.04);  // deep orange-red
const vec3 COL_OUTER  = vec3(0.55, 0.08, 0.02);  // dark ember red
const vec3 COL_EMBER  = vec3(1.00, 0.80, 0.30);  // flying ember sparks

// ── helpers ──────────────────────────────────────────────────────────────────
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float hash1(float n) {
    return fract(sin(n) * 43758.5453);
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

// ── flame tendril ─────────────────────────────────────────────────────────────
// models a single upward-rising flame column at horizontal offset ox
// returns brightness [0,1]
float flameTendril(vec2 uv, float ox, float time, float seed) {
    // remap so ox is the center of this flame column
    float dx = uv.x - ox;

    // vertical extent: flames rise from bottom, fade upward
    // uv.y = 0 is center, positive = up, negative = down in our coord system
    // we want flames rising from the bottom edge (uv.y = -0.5 ish in aspect space)
    float vy = uv.y; // positive = upward in screen (since texCoord.y is flipped)

    // only draw below the midpoint (flames rise from feet upward)
    if (vy > 0.15) return 0.0;

    float heightProgress = clamp((-vy + 0.05) / 0.55, 0.0, 1.0); // 0 at mid, 1 at bottom

    // wobble the column horizontally with noise
    float wobble = (fbm(vec2(seed + time * 0.8, heightProgress * 3.0)) - 0.5) * 0.12 * heightProgress;
    float distX  = abs(dx - wobble);

    // column width narrows toward the tip
    float width = mix(0.005, 0.025, heightProgress);
    float col   = smoothstep(width * 2.5, 0.0, distX);

    // fade at very tip (top of flame)
    col *= smoothstep(0.0, 0.15, heightProgress);
    // fade at very base
    col *= smoothstep(1.0, 0.7, heightProgress);

    return col;
}

// ── heat shimmer distortion offset ───────────────────────────────────────────
vec2 heatDistort(vec2 uv, float time) {
    float r = length(uv);
    // strongest near screen edges and below center (around the player body)
    float strength = smoothstep(0.50, 0.25, r) * 0.006
                   + smoothstep(0.0, 0.3, -uv.y) * 0.004;

    float dx = (fbm(uv * 8.0 + vec2(time * 1.2, 0.0)) - 0.5) * 2.0;
    float dy = (fbm(uv * 8.0 + vec2(0.0, time * 0.9 + 3.7)) - 0.5) * 2.0;

    return vec2(dx, dy) * strength;
}

// ── ember spark ───────────────────────────────────────────────────────────────
// returns brightness of a single flying ember particle
float ember(vec2 uv, float idx, float time) {
    float cycle  = fract(time * 0.28 + hash1(idx) * 1.0);   // lifetime 0→1
    float startX = (hash1(idx + 10.0) - 0.5) * 0.50;
    float startY = -0.30 - hash1(idx + 20.0) * 0.15;         // starts near bottom

    // drift upward and sideways
    float px = startX + sin(cycle * TAU * 0.7 + idx) * 0.06 * cycle;
    float py = startY + cycle * (0.45 + hash1(idx + 30.0) * 0.25);

    float dist = length(uv - vec2(px, py));
    float sz   = 0.006 + hash1(idx + 40.0) * 0.005;
    float brightness = smoothstep(sz, 0.0, dist);

    // flicker and fade at end of life
    float flicker = 0.6 + 0.4 * sin(time * 15.0 + idx * 7.3);
    float fade    = (1.0 - cycle) * (1.0 - cycle);

    return brightness * flicker * fade;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 uv = texCoord - vec2(0.5, 0.5);
    uv.x *= OutSize.x / OutSize.y;

    float time = Progress * 7.0;

    // ── 1. Heat distortion — warp the scene sample ─────────────────────────
    vec2 distort = heatDistort(uv, time) * Intensity;
    vec4 warpedScene = texture(DiffuseSampler, texCoord + distort);

    // ── 2. Flame tendrils (8 columns, spread around the player) ────────────
    float flames = 0.0;
    float offsets[8];
    offsets[0] = -0.28; offsets[1] = -0.18; offsets[2] = -0.08; offsets[3] =  0.02;
    offsets[4] =  0.12; offsets[5] =  0.22; offsets[6] = -0.38; offsets[7] =  0.32;

    for (int i = 0; i < 8; i++) {
        flames += flameTendril(uv, offsets[i], time + float(i) * 0.4, float(i) * 1.73);
    }
    flames = clamp(flames, 0.0, 1.0);

    // ── 3. Edge vignette — hot orange-red at screen border ─────────────────
    float r         = length(uv);
    float edgeDist  = 1.0 - smoothstep(0.28, 0.68, r);
    float auraEdge  = 1.0 - edgeDist;
    auraEdge = pow(auraEdge, 2.0);

    // noise texture on the edge makes it look like licking flames
    float edgeNoise = fbm(uv * 5.0 - vec2(0.0, time * 0.5)) * 0.5 + 0.5;
    auraEdge *= mix(0.6, 1.0, edgeNoise);

    // ── 4. Center core glow ─────────────────────────────────────────────────
    float coreGlow = smoothstep(0.22, 0.0, r) * 0.40;

    // ── 5. Ember sparks ─────────────────────────────────────────────────────
    float embers = 0.0;
    for (int i = 0; i < 20; i++) {
        embers += ember(uv, float(i), time);
    }
    embers = clamp(embers, 0.0, 1.5);

    // ── 6. Compose aura color ───────────────────────────────────────────────
    vec3 auraColor = vec3(0.0);

    // flame tendrils: core → inner orange gradient by height
    float flameHeight = clamp((-uv.y) / 0.5, 0.0, 1.0);
    vec3  flameTint   = mix(COL_CORE, COL_INNER, flameHeight);
    flameTint         = mix(flameTint, COL_MID, pow(flameHeight, 2.0));
    auraColor += flameTint * flames * 1.2;

    // edge vignette: outer red → mid orange
    auraColor += mix(COL_OUTER, COL_MID, edgeDist) * auraEdge * 0.65;

    // core glow: white-hot at center
    auraColor += mix(COL_CORE, COL_INNER, 0.4) * coreGlow;

    // embers: bright yellow-white sparks
    auraColor += COL_EMBER * embers * 0.9;

    // ── 7. Warm color tint on the whole scene ───────────────────────────────
    // gives the world a hot-atmosphere orange cast
    vec3 warmTint = mix(
        warpedScene.rgb,
        warpedScene.rgb * vec3(1.06, 0.96, 0.85),
        Intensity * 0.25
    );

    // ── 8. Blend onto scene ─────────────────────────────────────────────────
    float auraMask = clamp(
        flames * 0.9 + auraEdge * 0.7 + coreGlow + embers * 0.5,
        0.0, 1.0
    );

    vec3 finalColor = warmTint + auraColor * Intensity;

    // vignette — slightly darken screen corners to frame the fire
    float vignette = smoothstep(0.90, 0.38, r);
    finalColor = mix(finalColor * 0.70, finalColor, vignette);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}
