package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Program;
import com.xirc.nichirin.client.light.WisteriaLightData;

import java.util.Set;

public final class NichirinShaderInjection {
    private static final Set<String> WISTERIA_LIT_SHADERS = Set.of(
            "rendertype_solid",
            "rendertype_cutout",
            "rendertype_cutout_mipped",
            "rendertype_entity_solid",
            "rendertype_entity_cutout",
            "rendertype_entity_cutout_no_cull",
            "rendertype_entity_cutout_no_cull_z_offset",
            "rendertype_entity_translucent",
            "rendertype_entity_translucent_cull",
            "rendertype_entity_smooth_cutout",
            "rendertype_entity_decal",
            "rendertype_entity_no_outline");

    private NichirinShaderInjection() {
    }

    public static String transform(Program.Type type, String shaderName, String source) {
        if (type != Program.Type.VERTEX || !WISTERIA_LIT_SHADERS.contains(shaderName)) {
            return source;
        }

        return injectWisteriaLighting(source);
    }

    public static boolean isWisteriaLitShader(String shaderName) {
        return WISTERIA_LIT_SHADERS.contains(shaderName);
    }

    private static String injectWisteriaLighting(String source) {
        if (source.contains("NichirinWisteriaLightCount")) {
            return source;
        }

        String declarations = """

                uniform int NichirinWisteriaLightCount;
                uniform vec4 NichirinWisteriaLights[%d];
                uniform vec4 NichirinWisteriaLightColor;
                uniform vec3 NichirinCameraPosition;

                vec3 nichirin_wisteria_light(vec3 worldPos) {
                    vec3 lightColor = vec3(0.0);
                    for (int i = 0; i < %d; i++) {
                        if (i >= NichirinWisteriaLightCount) {
                            break;
                        }

                        vec4 light = NichirinWisteriaLights[i];
                        float falloff = 1.0 - distance(worldPos, light.xyz) / light.w;
                        float intensity = smoothstep(0.0, 1.0, clamp(falloff, 0.0, 1.0));
                        lightColor = max(lightColor, NichirinWisteriaLightColor.rgb * NichirinWisteriaLightColor.a * intensity);
                    }
                    return lightColor;
                }
                """.formatted(WisteriaLightData.MAX_LIGHTS, WisteriaLightData.MAX_LIGHTS);

        source = source.replace("out float vertexDistance;", declarations + "\nout float vertexDistance;");
        return source.replace(
                "vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);",
                """
                        vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
                            vertexColor.rgb += nichirin_wisteria_light(pos + NichirinCameraPosition) * vertexColor.a;
                            vertexColor.rgb = min(vertexColor.rgb, vec3(1.35));""");
    }
}
