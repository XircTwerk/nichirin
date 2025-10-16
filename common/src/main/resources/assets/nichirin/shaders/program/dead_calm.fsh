#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Progress;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

const vec3 WATER_COLOR = vec3(0.2, 0.5, 0.85);
const vec3 SKY_COLOR = vec3(0.4, 0.7, 1.0);
const vec3 WAVE_COLOR = vec3(0.6, 0.9, 1.0);
const vec3 RIPPLE_COLOR = vec3(0.7, 0.95, 1.0);
const vec3 DROPLET_COLOR = vec3(0.8, 0.95, 1.0);
const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

// Hash function for random
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(269.5, 183.3))) * 43758.5453123);
}

// 2D Noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Fractal noise
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;

    for(int i = 0; i < 6; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

// Voronoi pattern for water cells
vec2 voronoi(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float minDist = 10.0;
    vec2 minPoint = vec2(0.0);

    for(int y = -1; y <= 1; y++) {
        for(int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = hash2(i + neighbor) * vec2(hash(i + neighbor), hash2(i + neighbor));
            vec2 diff = neighbor + point - f;
            float dist = length(diff);

            if(dist < minDist) {
                minDist = dist;
                minPoint = point;
            }
        }
    }

    return vec2(minDist, hash(minPoint));
}

// Generate ripple patterns
float generateRipples(vec2 uv, float time, float density, float speed) {
    float ripples = 0.0;

    // Multiple ripple layers
    for(int i = 0; i < 8; i++) {
        float offset = float(i) * 0.3;
        float frequency = density * (1.0 + float(i) * 0.5);
        float phase = time * speed * (1.0 + float(i) * 0.3);

        vec2 rippleUV = uv * frequency;
        float dist = length(rippleUV);

        float ripple = sin(dist - phase + offset) * 0.5 + 0.5;
        ripple *= exp(-dist * 0.5);
        ripple *= (1.0 - float(i) * 0.1);

        ripples += ripple;
    }

    return ripples / 8.0;
}

// Radial ripple from center
float radialRipple(vec2 uv, vec2 center, float radius, float width, float intensity) {
    float dist = length(uv - center);
    float ripple = smoothstep(width, 0.0, abs(dist - radius));
    return ripple * intensity;
}

// Concentric rings
float concentricRings(vec2 uv, vec2 center, float time, int count) {
    float dist = length(uv - center);
    float rings = 0.0;

    for(int i = 0; i < count; i++) {
        float ringRadius = time - float(i) * 0.1;
        float ring = smoothstep(0.02, 0.0, abs(dist - ringRadius));
        rings += ring * (1.0 - float(i) * 0.15);
    }

    return rings;
}

// Droplet splash pattern
float dropletSplash(vec2 uv, float time, int dropletCount) {
    float splashes = 0.0;

    for(int i = 0; i < dropletCount; i++) {
        vec2 dropletPos = vec2(
        hash(vec2(float(i), time * 0.1)) * 2.0 - 1.0,
        hash(vec2(float(i) + 100.0, time * 0.1)) * 2.0 - 1.0
        );

        float dropletTime = fract(time * 0.5 + float(i) * 0.1);
        float dropletRadius = dropletTime * 0.3;

        float dist = length(uv - dropletPos);
        float splash = smoothstep(0.05, 0.0, abs(dist - dropletRadius));
        splash *= (1.0 - dropletTime);

        splashes += splash;
    }

    return splashes;
}

// Water foam pattern
float waterFoam(vec2 uv, float time) {
    vec2 foamUV = uv * 15.0;
    float foam1 = noise(foamUV + time * 0.5);
    float foam2 = noise(foamUV * 2.0 - time * 0.3);
    float foam3 = noise(foamUV * 0.5 + time * 0.7);

    float foam = foam1 * 0.5 + foam2 * 0.3 + foam3 * 0.2;
    foam = smoothstep(0.6, 0.8, foam);

    return foam;
}

// Caustics pattern
float caustics(vec2 uv, float time) {
    vec2 causticsUV = uv * 10.0;

    float c1 = sin(causticsUV.x * 3.0 + time * 2.0) * cos(causticsUV.y * 3.0 - time * 1.5);
    float c2 = sin((causticsUV.x + causticsUV.y) * 2.0 - time * 3.0);
    float c3 = cos(length(causticsUV) * 5.0 + time * 2.5);

    float caustic = (c1 + c2 + c3) / 3.0;
    caustic = pow(max(caustic, 0.0), 3.0);

    return caustic;
}

// Sparkle particles
float sparkles(vec2 uv, float time) {
    vec2 sparkleUV = uv * 40.0;
    vec2 voronoiResult = voronoi(sparkleUV + time * 0.5);

    float sparkle = smoothstep(0.1, 0.0, voronoiResult.x);
    sparkle *= sin(time * 5.0 + voronoiResult.y * TWO_PI) * 0.5 + 0.5;

    return sparkle;
}

// Wave distortion
vec2 waveDistortion(vec2 uv, float time) {
    float wave1 = sin(uv.x * 10.0 + time * 2.0) * 0.01;
    float wave2 = cos(uv.y * 8.0 - time * 1.5) * 0.01;

    return vec2(wave1, wave2);
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    // Screen position
    vec2 center = vec2(0.5, 0.65);
    vec2 toCenter = texCoord - center;
    float dist = length(toCenter);
    float angle = atan(toCenter.y, toCenter.x);

    // Time
    float time = Progress * 10.0;

    // Expansion
    float currentRadius = Progress * 1.5;
    float inWater = smoothstep(currentRadius + 0.1, currentRadius - 0.1, dist);

    // Apply wave distortion
    vec2 distortedUV = texCoord + waveDistortion(texCoord, time);

    // Main expansion wave - MUCH BRIGHTER
    float mainWave = smoothstep(0.04, 0.0, abs(dist - currentRadius));
    mainWave *= 5.0; // WAY BRIGHTER

    // Multiple ripple rings following the wave - BRIGHTER
    float ripples = 0.0;
    for(int i = 1; i <= 10; i++) {
        float rippleDist = currentRadius - float(i) * 0.06;
        float ripple = smoothstep(0.02, 0.0, abs(dist - rippleDist));
        ripple *= (1.0 - float(i) * 0.08);
        ripples += ripple * 3.0; // MUCH BRIGHTER
    }

    // Concentric rings from center - BRIGHTER
    float rings = concentricRings(texCoord, center, currentRadius, 15);
    rings *= 3.0; // MUCH BRIGHTER

    // Radial waves emanating outward
    float radialWaves = 0.0;
    for(int i = 0; i < 12; i++) {
        float waveAngle = float(i) * (TWO_PI / 12.0);
        float wavePhase = time * 2.0;

        float angleDiff = abs(mod(angle - waveAngle + PI, TWO_PI) - PI);
        float radialWave = smoothstep(0.3, 0.0, angleDiff);
        radialWave *= sin(dist * 20.0 - wavePhase);
        radialWave *= exp(-dist * 2.0);

        radialWaves += radialWave * 0.3;
    }

    // Circular ripples
    float circularRipples = sin(dist * 30.0 - time * 8.0) * 0.5 + 0.5;
    circularRipples *= smoothstep(currentRadius + 0.2, currentRadius, dist);
    circularRipples *= exp(-dist * 1.5);
    circularRipples *= 0.8;

    // Generate animated ripples
    float animatedRipples = generateRipples(texCoord - center, time, 20.0, 3.0);
    animatedRipples *= inWater * 0.7;

    // Droplet splashes
    float droplets = dropletSplash(texCoord - center, time, 20);
    droplets *= 2.0;

    // Water foam
    float foam = waterFoam(distortedUV, time);
    foam *= inWater * 0.5;

    // Caustics
    float causticsPattern = caustics(distortedUV, time);
    causticsPattern *= inWater * 0.8;

    // Sparkles
    float sparkle = sparkles(texCoord, time);
    sparkle *= inWater * 0.6;

    // Secondary wave patterns
    float secondaryWaves = 0.0;
    for(int i = 0; i < 5; i++) {
        float waveFreq = 15.0 + float(i) * 5.0;
        float waveSpeed = 5.0 + float(i) * 2.0;
        float wave = sin(dist * waveFreq - time * waveSpeed) * 0.5 + 0.5;
        wave *= smoothstep(currentRadius, currentRadius - 0.3, dist);
        secondaryWaves += wave * (1.0 - float(i) * 0.15);
    }
    secondaryWaves *= 0.4;

    // Interference patterns
    float interference = sin(dist * 25.0 - time * 6.0) * cos(angle * 8.0 + time * 3.0);
    interference *= inWater * 0.3;

    // Water texture with noise
    vec2 waterUV = distortedUV * 20.0 + time * 0.5;
    float waterNoise = fbm(waterUV) * 0.5;

    // Voronoi cells for water surface
    vec2 voronoiPattern = voronoi(distortedUV * 12.0 + time * 0.3);
    float cells = voronoiPattern.x * 0.3;

    // Build final color
    vec3 finalColor = original.rgb;

    // FORCE SKY BLUE
    if (texCoord.y < 0.5) {
        finalColor = mix(finalColor, SKY_COLOR, Intensity * 0.9);
    }

    // FORCE GROUND BLUE - 100% OPACITY MIX
    if (texCoord.y >= 0.5) {
        vec3 waterCol = WATER_COLOR;
        waterCol += waterNoise * 0.2;
        waterCol += causticsPattern * vec3(0.3, 0.5, 0.7);
        waterCol += cells * vec3(0.1, 0.2, 0.3);
        waterCol += foam * vec3(0.4, 0.6, 0.8);

        // 100% OPACITY - completely replace original color
        finalColor = mix(finalColor, waterCol, inWater * Intensity * 1.0);
    }

    // Add ALL the ripple effects
    finalColor += WAVE_COLOR * mainWave * Intensity;
    finalColor += RIPPLE_COLOR * ripples * Intensity;
    finalColor += RIPPLE_COLOR * rings * Intensity;
    finalColor += vec3(0.5, 0.8, 1.0) * radialWaves * Intensity;
    finalColor += vec3(0.4, 0.7, 0.95) * circularRipples * Intensity;
    finalColor += RIPPLE_COLOR * animatedRipples * Intensity;
    finalColor += DROPLET_COLOR * droplets * Intensity;
    finalColor += vec3(0.6, 0.85, 1.0) * secondaryWaves * Intensity;
    finalColor += vec3(0.5, 0.75, 0.9) * interference * Intensity;
    finalColor += vec3(1.0, 1.0, 1.0) * sparkle * Intensity;

    // Vignette
    float vignette = 1.0 - dist * 0.4;
    finalColor *= mix(1.0, vignette, Intensity * 0.2);

    fragColor = vec4(finalColor, 1.0);
}