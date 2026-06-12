#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Read the entity texture alpha so the outline respects model cutouts (transparent
    // pixels in the original entity texture won't get a coloured outline overlay).
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;

    vec4 col = vertexColor * ColorModulator;
    if (col.a <= 0.0) discard;
    fragColor = linear_fog(col, vertexDistance, FogStart, FogEnd, FogColor);
}
