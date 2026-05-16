#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec2 OutSize;

uniform float Time;
uniform float Intensity;
uniform vec3  MistColor;

in vec2 texCoord;
out vec4 fragColor;

// smear / smudge helpers

// Pseudo-random hash for per-pixel variation
float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

// Low-frequency smooth noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),            hash(i + vec2(1,0)), f.x),
        mix(hash(i+vec2(0,1)),  hash(i + vec2(1,1)), f.x),
        f.y
    );
}

// 13-tap radial Gaussian smear.
// The sample ring is stretched in a random direction per pixel to look
// like a genuine paint smudge rather than a clean circular blur.
// Samples that land on sky pixels (depth >= 0.9999994) are skipped so
// sky colour never bleeds into horizon terrain.
vec3 smudgeBlur(vec2 uv, float radius) {
    vec2 texel = 1.0 / OutSize;

    // Per-pixel smear direction based on slow noise (drifts over time)
    float angle = noise(uv * 4.0 + vec2(Time * 0.15)) * 6.2832;
    vec2  smearDir = vec2(cos(angle), sin(angle));

    vec3  result  = vec3(0.0);
    float weight  = 0.0;

    // Two rings: inner (tight) + outer (wide smear)
    // Inner ring – 8 evenly-spaced samples at 0.45 * radius
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854; // 2π/8
        vec2 sampleUV = clamp(uv + vec2(cos(a), sin(a)) * radius * texel * 0.5, 0.001, 0.999);
        float sampleDepth = texture(DiffuseDepthSampler, sampleUV).r;
        if (sampleDepth >= 0.9999994) continue; // skip sky samples
        float w = 0.7;
        result += texture(DiffuseSampler, sampleUV).rgb * w;
        weight += w;
    }

    // Outer ring – 8 samples stretched along smear direction
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 base = vec2(cos(a), sin(a));
        // Elongate along smearDir
        vec2 stretched = base + smearDir * dot(base, smearDir) * 0.8;
        vec2 sampleUV = clamp(uv + stretched * radius * texel, 0.001, 0.999);
        float sampleDepth = texture(DiffuseDepthSampler, sampleUV).r;
        if (sampleDepth >= 0.9999994) continue; // skip sky samples
        float w = 0.45;
        result += texture(DiffuseSampler, sampleUV).rgb * w;
        weight += w;
    }

    // Centre tap (always included — we already passed the depth check above)
    result += texture(DiffuseSampler, uv).rgb * 1.5;
    weight += 1.5;

    // Guard against all neighbours being sky
    if (weight <= 1.5) return texture(DiffuseSampler, uv).rgb;

    return result / weight;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    // Depth check — never touch sky pixels
    float sceneDepth = texture(DiffuseDepthSampler, texCoord).r;
    if (sceneDepth >= 0.9999994 || Intensity < 0.005) {
        fragColor = original;
        return;
    }

    // Smudge radius in pixels, scales with intensity
    float radius = Intensity * 9.0;

    vec3 smudged = smudgeBlur(texCoord, radius);

    // Add slow spatial mist drift — samples a slightly offset copy and mixes in
    float driftA = noise(texCoord * 3.5 + vec2(Time * 0.2,  0.0)) * 0.015 * Intensity;
    float driftB = noise(texCoord * 3.5 + vec2(0.0, Time * 0.18)) * 0.015 * Intensity;
    vec3 drifted = texture(DiffuseSampler,
        clamp(texCoord + vec2(driftA, driftB), 0.001, 0.999)).rgb;
    smudged = mix(smudged, drifted, 0.20);

    // Mist colour tint — applied to midtones only so dark/bright areas stay readable
    float luma = dot(smudged, vec3(0.299, 0.587, 0.114));
    float tintWeight = Intensity * 0.35 * smoothstep(0.15, 0.55, luma) * (1.0 - smoothstep(0.55, 0.90, luma));
    vec3 tinted = mix(smudged, MistColor * luma, tintWeight);

    // Final blend — max 80% so scene is always partially visible
    float blendAlpha = Intensity * 0.80;
    vec3 result = mix(original.rgb, tinted, blendAlpha);

    fragColor = vec4(result, original.a);
}
