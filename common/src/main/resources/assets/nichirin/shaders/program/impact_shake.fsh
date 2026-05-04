#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

// Progress  : 0..1 over the full effect duration
// Intensity : master multiplier (fade in/out, default 1.0)
// ShakeX    : current frame horizontal UV offset (pixels → normalized by Java)
// ShakeY    : current frame vertical   UV offset (pixels → normalized by Java)
// ChromaSplit: current frame chroma split amount (0..0.006, driven by Java)

uniform float Progress;
uniform float Intensity;
uniform float ShakeX;
uniform float ShakeY;
uniform float ChromaSplit;

in vec2 texCoord;
out vec4 fragColor;

// constants
const float FLASH_WHITEOUT_END  = 0.05;   // Progress at which hard flash ends
const float FLASH_BLOOM_END     = 0.175;  // Progress at which bloom hold ends
const float FLASH_FADE_END      = 0.35;   // Progress at which all flash is gone
const vec3  BLOOM_COLOR         = vec3(0.63, 0.86, 1.0); // blue-white bloom tint

// helpers
float remap(float v, float inLo, float inHi, float outLo, float outHi) {
    return outLo + (outHi - outLo) * clamp((v - inLo) / (inHi - inLo), 0.0, 1.0);
}

void main() {
    // 1. SHAKE + CHROMATIC ABERRATION
    // ShakeX/ShakeY are already in UV space (normalized 0..1) from Java.
    // ChromaSplit is the half-split distance in UV space.

    vec2 shakeOffset = vec2(ShakeX, ShakeY);

    // Sample three slightly offset UVs for R, G, B
    float r = texture(DiffuseSampler, texCoord + shakeOffset + vec2( ChromaSplit, 0.0)).r;
    float g = texture(DiffuseSampler, texCoord + shakeOffset).g;
    float b = texture(DiffuseSampler, texCoord + shakeOffset + vec2(-ChromaSplit, 0.0)).b;
    float a = texture(DiffuseSampler, texCoord + shakeOffset).a;

    vec3 color = vec3(r, g, b);

    // 2. TWO-STAGE FLASH

    // Distance from screen center (for bloom radial falloff)
    vec2  toCenter = texCoord - vec2(0.5);
    float dist     = length(toCenter);

    // Stage 1 — hard white-out (instant, no ramp)
    if (Progress < FLASH_WHITEOUT_END) {
        // Completely overwrite color with white; no fade-in so it hits instantly.
        color = vec3(1.0);
    }

    // Stage 2 — bloom hold: center stays bright, edges clear first
    else if (Progress < FLASH_BLOOM_END) {
        float p = remap(Progress, FLASH_WHITEOUT_END, FLASH_BLOOM_END, 0.0, 1.0);

        // Radial brightness: center clears last
        // At p=0: full white everywhere; at p=1: only center bloom remains
        float edgeFade  = 1.0 - smoothstep(0.0,  0.5, dist) * p;   // edges drop
        float coreFade  = 1.0 - p * 0.55;                           // center slower

        float brightness = mix(coreFade, edgeFade, dist * 2.0);
        brightness = clamp(brightness, 0.0, 1.0);

        // Mix base color toward white/bloom
        vec3 flashColor = mix(BLOOM_COLOR, vec3(1.0), 1.0 - p * 0.5);
        color = mix(color, flashColor, brightness);

        // Extra blue energy bloom tight at center
        float bloomRadius  = 0.18;
        float bloomFalloff = 1.0 - smoothstep(0.0, bloomRadius, dist);
        float bloomAlpha   = bloomFalloff * (1.0 - p) * 0.55;
        color = mix(color, BLOOM_COLOR, bloomAlpha);
    }

    // Stage 3 — quick fade to clear
    else if (Progress < FLASH_FADE_END) {
        float p = remap(Progress, FLASH_BLOOM_END, FLASH_FADE_END, 0.0, 1.0);
        float residual = (1.0 - p) * 0.18;
        color = mix(color, BLOOM_COLOR, residual);
    }

    // 3. INTENSITY MASTER
    // Intensity is driven by Java for fade-in / fade-out of the whole effect.
    // We lerp toward original (un-shaken) sample when Intensity < 1.
    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    color = mix(original, color, Intensity);

    fragColor = vec4(color, a);
}
