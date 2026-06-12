#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

uniform float BlockSize;
uniform float PosterizeLevels;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

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
        mix(hash(i),                 hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return v;
}

void main() {
    vec2 px = texCoord * OutSize;
    vec2 snappedPx = (floor(px / BlockSize) + 0.5) * BlockSize;
    vec2 snappedUv = snappedPx / OutSize;

    vec4 col = texture(DiffuseSampler, snappedUv);

    float levels = max(2.0, PosterizeLevels);
    col.rgb = floor(col.rgb * levels + 0.5) / levels;

    float n = fbm(snappedUv * 12.0 + vec2(Time * 0.05));
    col.rgb += (n - 0.5) * 0.06;

    fragColor = vec4(clamp(col.rgb, 0.0, 1.0), col.a);
}
