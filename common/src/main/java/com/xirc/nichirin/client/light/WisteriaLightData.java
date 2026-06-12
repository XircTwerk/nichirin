package com.xirc.nichirin.client.light;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WisteriaLightData {
    public static final int MAX_LIGHTS = 24;

    private static final int SCAN_RADIUS = 32;
    private static final int VERTICAL_SCAN_RADIUS = 48;
    // How far each leaf's glow reaches and how strong it is (NOT the scan distance / how close you
    // have to be to the tree — that's SCAN_RADIUS). Bigger radius + strength = the glow spreads
    // further and reads brighter on nearby surfaces.
    private static final float LIGHT_RADIUS = 17.0F;
    private static final float DAY_LIGHT_STRENGTH = 0.75F;
    private static final float NIGHT_LIGHT_STRENGTH = 1.2F;
    // A touch more saturated than before so the pink actually reads against foliage.
    private static final int DAY_COLOR = 0xC8A2FF;
    private static final int NIGHT_COLOR = 0x7E3DCC;

    // Per-tick lerp speed each light's radius eases toward its target (full LIGHT_RADIUS when a
    // leaf is present, 0 when it has left the scan). Low value = "super smooth" grow-in / fade-out
    // instead of the old hard pop. Fade-out is a little slower than fade-in so the glow lingers.
    private static final float FADE_IN_SPEED = 0.085F;
    private static final float FADE_OUT_SPEED = 0.05F;
    private static final float FADE_CULL_EPSILON = 0.05F;

    private static final float[] LIGHTS = new float[MAX_LIGHTS * 4];
    private static int lightCount;
    private static int scanCooldown;

    // Persistent per-leaf light state, keyed by packed block position, so a leaf keeps its current
    // (fading) radius across scans rather than snapping on/off.
    private static final Map<Long, Light> TRACKED = new HashMap<>();

    private static float red = 1.0F;
    private static float green = 0.82F;
    private static float blue = 0.95F;
    private static float strength = DAY_LIGHT_STRENGTH;

    private WisteriaLightData() {
    }

    private static final class Light {
        final float x;
        final float y;
        final float z;
        float currentRadius;
        float targetRadius;

        Light(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            TRACKED.clear();
            lightCount = 0;
            return;
        }

        updateColor(minecraft.level);

        BlockPos center = minecraft.player.blockPosition();
        if (scanCooldown-- <= 0) {
            scanCooldown = 4;
            scanForLights(minecraft.level, center);
        }

        // Advance the fades every tick (not just on scans) so the transition is smooth at 20 TPS,
        // then rebuild the flat upload array from the nearest tracked lights.
        advanceFades();
        rebuildUploadArray(center);
    }

    public static boolean hasLights() {
        return lightCount > 0;
    }

    public static int applyEntityLight(Vec3 worldPos, int packedLight) {
        if (lightCount <= 0) {
            return packedLight;
        }

        float strongest = 0.0F;
        for (int i = 0; i < lightCount; i++) {
            int index = i * 4;
            float radius = LIGHTS[index + 3];
            if (radius <= 0.0F) {
                continue;
            }

            double dx = LIGHTS[index] - worldPos.x;
            double dy = LIGHTS[index + 1] - worldPos.y;
            double dz = LIGHTS[index + 2] - worldPos.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float falloff = 1.0F - distance / radius;
            if (falloff <= 0.0F) {
                continue;
            }

            float smoothed = falloff * falloff * (3.0F - 2.0F * falloff);
            strongest = Math.max(strongest, smoothed * strength);
        }

        if (strongest <= 0.02F) {
            return packedLight;
        }

        int blockLight = (packedLight >> 4) & 0xF;
        int skyLight = (packedLight >> 20) & 0xF;
        int boostedBlockLight = Math.max(blockLight, Math.min(15, Math.round(strongest * 15.0F)));
        return LightTexture.pack(boostedBlockLight, skyLight);
    }

    public static void uploadToShader(int programId) {
        int countLocation = GL20.glGetUniformLocation(programId, "NichirinWisteriaLightCount");
        if (countLocation < 0) {
            return;
        }

        GL20.glUniform1i(countLocation, lightCount);

        int colorLocation = GL20.glGetUniformLocation(programId, "NichirinWisteriaLightColor");
        if (colorLocation >= 0) {
            GL20.glUniform4f(colorLocation, red, green, blue, strength);
        }

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int cameraLocation = GL20.glGetUniformLocation(programId, "NichirinCameraPosition");
        if (cameraLocation >= 0) {
            GL20.glUniform3f(cameraLocation, (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        }

        for (int i = 0; i < lightCount; i++) {
            int index = i * 4;
            int lightLocation = GL20.glGetUniformLocation(programId, "NichirinWisteriaLights[" + i + "]");
            if (lightLocation >= 0) {
                GL20.glUniform4f(
                        lightLocation,
                        LIGHTS[index],
                        LIGHTS[index + 1],
                        LIGHTS[index + 2],
                        LIGHTS[index + 3]);
            }
        }
    }

    private static void scanForLights(Level level, BlockPos center) {
        BlockPos min = center.offset(-SCAN_RADIUS, -VERTICAL_SCAN_RADIUS, -SCAN_RADIUS);
        BlockPos max = center.offset(SCAN_RADIUS, VERTICAL_SCAN_RADIUS, SCAN_RADIUS);

        // Everything currently tracked is assumed gone until the scan re-confirms it; confirmed
        // leaves get their target restored to full radius, the rest ease back to 0 and get culled.
        for (Light light : TRACKED.values()) {
            light.targetRadius = 0.0F;
        }

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(NichirinBlockRegistry.WISTERIA_LEAVES.get())) {
                long key = pos.asLong();
                Light light = TRACKED.get(key);
                if (light == null) {
                    light = new Light(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
                    TRACKED.put(key, light);
                }
                light.targetRadius = LIGHT_RADIUS;
            }
        }
    }

    private static void advanceFades() {
        var iterator = TRACKED.values().iterator();
        while (iterator.hasNext()) {
            Light light = iterator.next();
            float speed = light.targetRadius > light.currentRadius ? FADE_IN_SPEED : FADE_OUT_SPEED;
            light.currentRadius += (light.targetRadius - light.currentRadius) * speed;
            if (light.targetRadius <= 0.0F && light.currentRadius < FADE_CULL_EPSILON) {
                iterator.remove();
            }
        }
    }

    private static void rebuildUploadArray(BlockPos center) {
        float cx = center.getX() + 0.5F;
        float cy = center.getY() + 0.5F;
        float cz = center.getZ() + 0.5F;

        List<Light> sorted = new ArrayList<>(TRACKED.values());
        sorted.sort((a, b) -> Float.compare(distSqr(a, cx, cy, cz), distSqr(b, cx, cy, cz)));

        lightCount = Math.min(MAX_LIGHTS, sorted.size());
        for (int i = 0; i < lightCount; i++) {
            Light light = sorted.get(i);
            int index = i * 4;
            LIGHTS[index] = light.x;
            LIGHTS[index + 1] = light.y;
            LIGHTS[index + 2] = light.z;
            LIGHTS[index + 3] = light.currentRadius;
        }
    }

    private static float distSqr(Light light, float cx, float cy, float cz) {
        float dx = light.x - cx;
        float dy = light.y - cy;
        float dz = light.z - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void updateColor(Level level) {
        float dayAmount = getDayAmount(level.getTimeOfDay(0.0F));
        int color = blendRgb(NIGHT_COLOR, DAY_COLOR, dayAmount);
        red = smooth(red, channel(color, 16) / 255.0F, 0.035F);
        green = smooth(green, channel(color, 8) / 255.0F, 0.035F);
        blue = smooth(blue, channel(color, 0) / 255.0F, 0.035F);
        strength = smooth(strength, lerp(NIGHT_LIGHT_STRENGTH, DAY_LIGHT_STRENGTH, dayAmount), 0.028F);
    }

    private static float getDayAmount(float timeOfDay) {
        float brightness = ((float) Math.cos(timeOfDay * Math.PI * 2.0F) + 1.0F) * 0.5F;
        return brightness * brightness * (3.0F - 2.0F * brightness);
    }

    private static int blendRgb(int from, int to, float amount) {
        int r = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * amount);
        int g = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * amount);
        int b = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static float smooth(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
