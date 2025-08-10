#version 150

uniform sampler2D Sampler0; // Main texture
uniform sampler2D Sampler1; // Depth texture
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec2 ScreenSize;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 worldPos;

out vec4 fragColor;

// Musical score color (bone/parchment color)
const vec3 SCORE_COLOR = vec3(0.961, 0.871, 0.702); // #F5DEB3

void main() {
    // Get the original color
    vec4 originalColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    // Calculate musical score overlay
    vec2 screenPos = gl_FragCoord.xy / ScreenSize;

    // Create staff lines effect
    float staffLines = 0.0;
    for (int i = 0; i < 5; i++) {
        float lineY = 0.3 + float(i) * 0.1;
        float lineDist = abs(screenPos.y - lineY);
        staffLines += smoothstep(0.002, 0.001, lineDist);
    }

    // Create musical notes effect (floating sine waves)
    float noteEffect = 0.0;
    for (int i = 0; i < 3; i++) {
        float noteX = 0.2 + float(i) * 0.3 + sin(GameTime * 0.05 + float(i)) * 0.1;
        float noteY = 0.4 + sin(GameTime * 0.03 + float(i) * 2.0) * 0.2;
        float noteDist = distance(screenPos, vec2(noteX, noteY));
        noteEffect += smoothstep(0.02, 0.01, noteDist);
    }

    // Combine effects
    float musicalOverlay = staffLines + noteEffect * 0.5;

    // Apply bone color tint
    vec3 tintedColor = mix(originalColor.rgb, SCORE_COLOR, 0.3);

    // Add musical score overlay
    vec3 finalColor = mix(tintedColor, SCORE_COLOR, musicalOverlay * 0.4);

    // Slight pulsing effect
    float pulse = 0.8 + 0.2 * sin(GameTime * 0.1);
    finalColor *= pulse;

    fragColor = vec4(finalColor, originalColor.a);
}