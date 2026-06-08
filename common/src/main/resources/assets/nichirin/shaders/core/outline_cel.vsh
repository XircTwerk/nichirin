#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform float OutlineWidth;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    // Cel-shader trick: displace each vertex along its surface normal by OutlineWidth.
    // Adjacent vertices on the same surface stay attached to each other (the surface
    // expands uniformly outward), but separate body parts don't drift apart because
    // their scale is applied per-vertex via the normal rather than uniformly around
    // the entity origin.
    vec3 displaced = Position + normalize(Normal) * OutlineWidth;
    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);
    vertexDistance = fog_distance(ModelViewMat, displaced, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;
}
