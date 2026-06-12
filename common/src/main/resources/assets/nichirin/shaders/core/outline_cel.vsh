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
    vec4 viewPosition = ModelViewMat * vec4(displaced, 1.0);
    gl_Position = ProjMat * viewPosition;
    // Push outline slightly back in NDC depth so it can never z-fight with the original
    // model where the displaced surface lands within float precision of the original.
    // gl_Position is in clip space; NDC.z = gl_Position.z / gl_Position.w, so multiplying
    // the bias by w gives a constant NDC-space offset of ~0.0008 (back of the model).
    gl_Position.z += gl_Position.w * 0.0008;
    vertexDistance = fog_distance(viewPosition.xyz, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;
}
