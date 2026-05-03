#version 150

uniform sampler2D DiffuseSampler;

uniform float Time;
uniform float Intensity;
uniform float WaveFrequency;
uniform float WaveAmplitude;
uniform vec3  AuraColor;
uniform float InnerRadius;

in vec2 texCoord;
out vec4 fragColor;

// Noise helpers
float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),             hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    if (Intensity < 0.005) {
        fragColor = original;
        return;
    }

    vec2  uv   = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    float rim = smoothstep(InnerRadius, 1.40, dist);

    float t = Time;

    // Travelling wave: angle around the screen drives overlapping sine/cosine ripples
    float angle    = atan(uv.y, uv.x);
    float ripple   = sin(angle * WaveFrequency + t * 2.0)
                   * cos(angle * (WaveFrequency * 0.5) - t * 1.3)
                   * 0.5 + 0.5;

    // Layer a slower, bigger wave for depth
    float ripple2  = sin(angle * (WaveFrequency * 0.4) - t * 0.9 + 1.3) * 0.5 + 0.5;

    float waterRim = rim * mix(ripple, ripple2, 0.4);

    // Gentle surface shimmer
    float shimmer  = noise(texCoord * 12.0 + vec2(t * 0.5, t * 0.3)) * 0.4 + 0.6;

    // Pulse (slow, like breathing itself)
    float pulse    = 0.88 + 0.12 * sin(t * 1.2);

    float alpha    = waterRim * shimmer * Intensity * pulse;
    alpha          = clamp(alpha, 0.0, 1.0);

    // Bend the sample UV near the rim to simulate refraction through water
    float warp     = WaveAmplitude * rim * Intensity;
    vec2 warpDir   = vec2(
        sin(angle * WaveFrequency * 0.7 + t * 1.8),
        cos(angle * WaveFrequency * 0.7 - t * 2.1)
    );
    vec2 warpedUv  = texCoord + warpDir * warp;
    vec4 warpedScene = texture(DiffuseSampler, clamp(warpedUv, 0.001, 0.999));

    vec3 caustic  = AuraColor * (0.85 + 0.15 * ripple * shimmer);
    vec3 result   = mix(warpedScene.rgb, caustic, alpha * 0.70);
    result += caustic * alpha * 0.28;

    fragColor = vec4(result, warpedScene.a);
}
