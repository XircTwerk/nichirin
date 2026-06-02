#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform float Time;
uniform float Intensity;
uniform float PulseSpeed;
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

    // Skip sky pixels entirely — depth == 1.0 means no geometry was drawn there.
    float sceneDepth = texture(DiffuseDepthSampler, texCoord).r;
    if (sceneDepth >= 0.9999994 || Intensity < 0.005) {
        fragColor = original;
        return;
    }

    vec2 uv   = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    // Rim mask — only affects the outer ring; InnerRadius controls where it starts
    float rim = smoothstep(InnerRadius, 1.35, dist);

    // Turbulent FBM noise offsets the rim UV to produce jagged fire-tongue edges
    float t = Time * PulseSpeed;
    vec2 noiseUv = texCoord * 3.5 + vec2(t * 0.3, -t * 0.6);
    float flame   = fbm(noiseUv + fbm(noiseUv + vec2(t * 0.15)));

    float flameRim = rim * (0.7 + 0.3 * flame);
    float pulse    = 0.85 + 0.15 * sin(t * 2.5 + flame * 3.0);
    float alpha    = clamp(flameRim * Intensity * pulse, 0.0, 1.0);

    vec3 aura   = AuraColor * (0.9 + 0.1 * flame);
    vec3 result = mix(original.rgb, aura, alpha * 0.75);
    result += aura * alpha * 0.35;

    fragColor = vec4(result, original.a);
}