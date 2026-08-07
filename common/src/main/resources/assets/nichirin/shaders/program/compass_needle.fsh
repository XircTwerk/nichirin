#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec2 OutSize;
uniform float CompassTime;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    // Sky and void pixels retain depth 1.0. Never grade those pixels: applying a radial post
    // effect to the sky was fighting vanilla's skybox/cloud render and producing broken seams.
    float worldMask = 1.0 - smoothstep(0.9992, 1.0, depth);
    vec2 centered = (texCoord - 0.5) * vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    float radius = length(centered);
    float edge = smoothstep(0.28, 0.76, radius);
    float scan = 1.0 - smoothstep(0.012, 0.040,
            abs(fract(radius * 2.15 - CompassTime * 0.62) - 0.5));
    float spokes = pow(abs(cos(atan(centered.y, centered.x) * 6.0)), 22.0);
    float maskedIntensity = Intensity * worldMask;
    float signal = edge * (0.11 + scan * 0.025 + spokes * 0.018) * maskedIntensity;

    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 coolGrade = mix(source.rgb, vec3(luminance * 0.82, luminance * 0.96, luminance * 1.10),
                         0.07 * maskedIntensity);
    vec3 blueSignal = vec3(0.12, 0.63, 0.88) * signal;
    vec3 redResponse = vec3(0.52, 0.025, 0.08) * edge * 0.035 *
                       (0.5 + 0.5 * sin(CompassTime * 4.2)) * maskedIntensity;
    fragColor = vec4(coolGrade + blueSignal + redResponse, source.a);
}
