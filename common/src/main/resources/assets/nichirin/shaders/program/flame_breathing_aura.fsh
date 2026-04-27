#version 150

uniform sampler2D DiffuseSampler;

uniform float Time;
uniform float Intensity;
uniform float PulseSpeed;
uniform vec3  AuraColor;
uniform float InnerRadius;

in vec2 texCoord;
out vec4 fragColor;

// ── Noise helpers ──────────────────────────────────────────────────────────────
float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep
    return mix(
        mix(hash(i),            hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

// Layered noise for turbulent flame edges
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p  = p * 2.1 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    if (Intensity < 0.005) {
        fragColor = original;
        return;
    }

    // Distance from screen centre in [0, 1] aspect-corrected space
    vec2 uv   = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    // ── Rim mask ───────────────────────────────────────────────────────────────
    // Only affects the outer ring; InnerRadius controls where it starts
    float rim = smoothstep(InnerRadius, 1.35, dist);

    // ── Turbulent flame distortion on the rim ──────────────────────────────────
    float t = Time * PulseSpeed;
    // Offset UV by noise to make edges look like fire tongues
    vec2 noiseUv = texCoord * 3.5 + vec2(t * 0.3, -t * 0.6);
    float flame   = fbm(noiseUv + fbm(noiseUv + vec2(t * 0.15)));

    // Modulate rim with flame noise
    float flameRim = rim * (0.7 + 0.3 * flame);

    // ── Pulse ──────────────────────────────────────────────────────────────────
    float pulse = 0.85 + 0.15 * sin(t * 2.5 + flame * 3.0);

    // ── Final intensity ────────────────────────────────────────────────────────
    float alpha = flameRim * Intensity * pulse;
    alpha = clamp(alpha, 0.0, 1.0);

    // ── Subtle heat distortion of the underlying scene inside the rim ──────────
    // Warps the sample coord slightly near the rim boundary
    float warpStrength = rim * Intensity * 0.008 * (0.5 + 0.5 * flame);
    vec2 warpedUv = texCoord + normalize(uv) * warpStrength * (0.5 + 0.5 * noise(noiseUv));
    vec4 warpedScene = texture(DiffuseSampler, clamp(warpedUv, 0.001, 0.999));

    // Blend aura colour over (optionally warped) scene
    vec3 aura     = AuraColor * (0.9 + 0.1 * flame);
    vec3 result   = mix(warpedScene.rgb, aura, alpha * 0.75);

    // Add a bit of emissive bloom on top
    result += aura * alpha * 0.35;

    fragColor = vec4(result, warpedScene.a);
}
