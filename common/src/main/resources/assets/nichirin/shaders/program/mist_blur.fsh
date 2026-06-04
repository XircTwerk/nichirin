#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec2 OutSize;

uniform float Time;
uniform float Intensity;
uniform vec3 MistColor;

in vec2 texCoord;
out vec4 fragColor;

const float SKY_DEPTH_CUTOFF = 0.995;

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
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

vec3 smudgeBlur(vec2 uv, float radius) {
    vec2 texel = 1.0 / OutSize;
    float angle = noise(uv * 4.0 + vec2(Time * 0.15)) * 6.2832;
    vec2 smearDir = vec2(cos(angle), sin(angle));

    vec3 result = vec3(0.0);
    float weight = 0.0;

    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 sampleUV = clamp(uv + vec2(cos(a), sin(a)) * radius * texel * 0.5, 0.001, 0.999);
        float sampleDepth = texture(DiffuseDepthSampler, sampleUV).r;
        if (sampleDepth >= SKY_DEPTH_CUTOFF) {
            continue;
        }

        result += texture(DiffuseSampler, sampleUV).rgb * 0.7;
        weight += 0.7;
    }

    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 base = vec2(cos(a), sin(a));
        vec2 stretched = base + smearDir * dot(base, smearDir) * 0.8;
        vec2 sampleUV = clamp(uv + stretched * radius * texel, 0.001, 0.999);
        float sampleDepth = texture(DiffuseDepthSampler, sampleUV).r;
        if (sampleDepth >= SKY_DEPTH_CUTOFF) {
            continue;
        }

        result += texture(DiffuseSampler, sampleUV).rgb * 0.45;
        weight += 0.45;
    }

    result += texture(DiffuseSampler, uv).rgb * 1.5;
    weight += 1.5;

    if (weight <= 1.5) {
        return texture(DiffuseSampler, uv).rgb;
    }

    return result / weight;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    float sceneDepth = texture(DiffuseDepthSampler, texCoord).r;

    if (sceneDepth >= SKY_DEPTH_CUTOFF || Intensity < 0.005) {
        fragColor = original;
        return;
    }

    float radius = Intensity * 18.0;
    vec3 smudged = smudgeBlur(texCoord, radius);

    float driftA = noise(texCoord * 3.5 + vec2(Time * 0.2, 0.0)) * 0.015 * Intensity;
    float driftB = noise(texCoord * 3.5 + vec2(0.0, Time * 0.18)) * 0.015 * Intensity;
    vec3 drifted = texture(DiffuseSampler, clamp(texCoord + vec2(driftA, driftB), 0.001, 0.999)).rgb;
    smudged = mix(smudged, drifted, 0.20);

    float luma = dot(smudged, vec3(0.299, 0.587, 0.114));
    float tintWeight = Intensity * 0.35 * smoothstep(0.15, 0.55, luma) * (1.0 - smoothstep(0.55, 0.90, luma));
    vec3 tinted = mix(smudged, MistColor * luma, tintWeight);

    vec3 result = mix(original.rgb, tinted, Intensity * 0.92);
    fragColor = vec4(result, original.a);
}
