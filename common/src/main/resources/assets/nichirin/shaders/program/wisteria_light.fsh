#version 150

// Screen-space wisteria colored lighting. Runs when the vanilla core-shader injection is
// unavailable (Sodium replaces the terrain pipeline): reconstructs each pixel's world position
// from the depth buffer and applies the same distance falloff the injected vertex shader uses,
// so wisteria trees cast real colored light on terrain and entities on any render pipeline.

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform mat4 InvViewProj;
uniform vec3 CamPos;
uniform vec4 LightColor; // rgb = wisteria tint, a = strength

uniform vec4 Light0;
uniform vec4 Light1;
uniform vec4 Light2;
uniform vec4 Light3;
uniform vec4 Light4;
uniform vec4 Light5;
uniform vec4 Light6;
uniform vec4 Light7;
uniform vec4 Light8;
uniform vec4 Light9;
uniform vec4 Light10;
uniform vec4 Light11;
uniform vec4 Light12;
uniform vec4 Light13;
uniform vec4 Light14;
uniform vec4 Light15;

in vec2 texCoord;
out vec4 fragColor;

float contrib(vec4 light, vec3 worldPos) {
    if (light.w <= 0.001) return 0.0;
    float falloff = 1.0 - distance(worldPos, light.xyz) / light.w;
    falloff = clamp(falloff, 0.0, 1.0);
    // smoothstep, matching the vanilla injection
    return falloff * falloff * (3.0 - 2.0 * falloff);
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    // Sky: nothing to light
    if (depth >= 0.9999) {
        fragColor = sceneColor;
        return;
    }

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 pos = InvViewProj * ndc;
    vec3 worldPos = CamPos + pos.xyz / pos.w;

    float intensity = 0.0;
    intensity = max(intensity, contrib(Light0, worldPos));
    intensity = max(intensity, contrib(Light1, worldPos));
    intensity = max(intensity, contrib(Light2, worldPos));
    intensity = max(intensity, contrib(Light3, worldPos));
    intensity = max(intensity, contrib(Light4, worldPos));
    intensity = max(intensity, contrib(Light5, worldPos));
    intensity = max(intensity, contrib(Light6, worldPos));
    intensity = max(intensity, contrib(Light7, worldPos));
    intensity = max(intensity, contrib(Light8, worldPos));
    intensity = max(intensity, contrib(Light9, worldPos));
    intensity = max(intensity, contrib(Light10, worldPos));
    intensity = max(intensity, contrib(Light11, worldPos));
    intensity = max(intensity, contrib(Light12, worldPos));
    intensity = max(intensity, contrib(Light13, worldPos));
    intensity = max(intensity, contrib(Light14, worldPos));
    intensity = max(intensity, contrib(Light15, worldPos));
    intensity *= LightColor.a;

    if (intensity <= 0.002) {
        fragColor = sceneColor;
        return;
    }

    // Mostly multiplicative so texture detail survives (like the vertex-color injection),
    // plus a small additive floor so deep shadow still shows the glow.
    vec3 lit = sceneColor.rgb * (1.0 + LightColor.rgb * intensity * 0.9)
             + LightColor.rgb * intensity * 0.06;
    fragColor = vec4(min(lit, vec3(1.0)), sceneColor.a);
}
