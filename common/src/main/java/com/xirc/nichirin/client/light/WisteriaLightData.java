package com.xirc.nichirin.client.light;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WisteriaLightData {
    public static final int MAX_LIGHTS = 24;

    private static final int SCAN_RADIUS = 32;
    private static final int VERTICAL_SCAN_RADIUS = 18;
    private static final float LIGHT_RADIUS = 11.5F;
    private static final float DAY_LIGHT_STRENGTH = 0.46F;
    private static final float NIGHT_LIGHT_STRENGTH = 0.82F;
    private static final int DAY_COLOR = 0xFFD0F2;
    private static final int NIGHT_COLOR = 0xC276FF;

    private static final float[] LIGHTS = new float[MAX_LIGHTS * 4];
    private static int lightCount;
    private static int scanCooldown;
    private static float red = 1.0F;
    private static float green = 0.82F;
    private static float blue = 0.95F;
    private static float strength = DAY_LIGHT_STRENGTH;

    private WisteriaLightData() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            lightCount = 0;
            return;
        }

        updateColor(minecraft.level);

        if (scanCooldown-- > 0) {
            return;
        }

        scanCooldown = 4;
        scanForLights(minecraft.level, minecraft.player.blockPosition());
    }

    public static boolean hasLights() {
        return lightCount > 0;
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
        List<BlockPos> found = new ArrayList<>();
        BlockPos min = center.offset(-SCAN_RADIUS, -VERTICAL_SCAN_RADIUS, -SCAN_RADIUS);
        BlockPos max = center.offset(SCAN_RADIUS, VERTICAL_SCAN_RADIUS, SCAN_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(NichirinBlockRegistry.WISTERIA_LEAVES.get())) {
                found.add(pos.immutable());
            }
        }

        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        lightCount = Math.min(MAX_LIGHTS, found.size());

        for (int i = 0; i < lightCount; i++) {
            BlockPos pos = found.get(i);
            int index = i * 4;
            LIGHTS[index] = pos.getX() + 0.5F;
            LIGHTS[index + 1] = pos.getY() + 0.5F;
            LIGHTS[index + 2] = pos.getZ() + 0.5F;
            LIGHTS[index + 3] = LIGHT_RADIUS;
        }
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
