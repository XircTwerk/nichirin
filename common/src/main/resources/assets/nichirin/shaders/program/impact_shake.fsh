#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

uniform float Progress;
uniform float Intensity;
uniform float ShakeX;
uniform float ShakeY;
uniform float ChromaSplit;

in vec2 texCoord;
out vec4 fragColor;

const float FLASH_WHITEOUT_END = 0.025;
const float FLASH_FADE_END = 0.12;

float remap(float v, float inLo, float inHi, float outLo, float outHi) {
    return outLo + (outHi - outLo) * clamp((v - inLo) / (inHi - inLo), 0.0, 1.0);
}

void main() {
    vec2 shakeOffset = vec2(ShakeX, ShakeY);

    const vec2 UV_MIN = vec2(0.001);
    const vec2 UV_MAX = vec2(0.999);
    vec2 baseUV = clamp(texCoord + shakeOffset, UV_MIN, UV_MAX);

    float r = texture(DiffuseSampler, clamp(baseUV + vec2(ChromaSplit, 0.0), UV_MIN, UV_MAX)).r;
    float g = texture(DiffuseSampler, baseUV).g;
    float b = texture(DiffuseSampler, clamp(baseUV + vec2(-ChromaSplit, 0.0), UV_MIN, UV_MAX)).b;
    float a = texture(DiffuseSampler, baseUV).a;

    vec3 color = vec3(r, g, b);

    if (Progress < FLASH_WHITEOUT_END) {
        float p = remap(Progress, 0.0, FLASH_WHITEOUT_END, 0.0, 1.0);
        color = mix(color, vec3(1.0), (1.0 - p) * 0.16);
    } else if (Progress < FLASH_FADE_END) {
        float p = remap(Progress, FLASH_WHITEOUT_END, FLASH_FADE_END, 0.0, 1.0);
        color = mix(color, vec3(1.0), (1.0 - p) * 0.035);
    }

    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    color = mix(original, color, Intensity);

    fragColor = vec4(color, a);
}
