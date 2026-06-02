package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeadCalmSkyBoxRenderer {
    private static final int TOTAL_TICKS = 240;

    private boolean active = false;
    private int ticks = 0;
    private float alpha = 0.0f;
    private Vec3 playerPosAtStart = Vec3.ZERO;
    private final List<WaterDroplet> droplets = new ArrayList<>();
    private final List<GroundRipple> ripples = new ArrayList<>();
    private final List<StartupWave> startupWaves = new ArrayList<>();
    private final Random random = new Random();

    private static final float SKY_R = 0.3f;
    private static final float SKY_G = 0.6f;
    private static final float SKY_B = 0.9f;

    public void activate() {
        active = true;
        ticks = 0;
        alpha = 0.0f;
        droplets.clear();
        ripples.clear();
        startupWaves.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            playerPosAtStart = mc.player.position();
        }

        // Create initial MASSIVE radial waves on startup
        for (int i = 0; i < 15; i++) {
            startupWaves.add(new StartupWave(playerPosAtStart, i));
        }

        // Create HUGE ground-level ripples immediately
        for (int i = 0; i < 10; i++) {
            GroundRipple bigRipple = new GroundRipple(random, playerPosAtStart, i * 2.0f);
            bigRipple.expansionSpeed = 2.0f; // Faster expansion
            bigRipple.maxLifetime = 80; // Last longer
            ripples.add(bigRipple);
        }

        System.out.println("DEBUG: Skybox activated with " + startupWaves.size() + " startup waves + " + ripples.size() + " ground ripples at position " + playerPosAtStart);
    }

    public void deactivate() {
        active = false;
        ticks = 0;
        alpha = 0.0f;
        droplets.clear();
        ripples.clear();
        startupWaves.clear();
        System.out.println("DEBUG: Skybox deactivated!");
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (!active) return;

        ticks++;

        if (ticks >= TOTAL_TICKS) {
            deactivate();
            return;
        }

        float progress = (float) ticks / TOTAL_TICKS;

        if (ticks <= 20) {
            alpha = (float) ticks / 20.0f;
        } else if (ticks > TOTAL_TICKS - 40) {
            alpha = (float) (TOTAL_TICKS - ticks) / 40.0f;
        } else {
            alpha = 1.0f;
        }

        float maxRadius = 50.0f;
        float currentRadius = progress * maxRadius;

        // Update startup waves
        for (StartupWave wave : startupWaves) {
            wave.update();
        }
        startupWaves.removeIf(StartupWave::isDead);

        // Update droplets
        for (WaterDroplet droplet : droplets) {
            droplet.update();
        }

        if (random.nextFloat() < 0.8f && droplets.size() < 300) {
            droplets.add(new WaterDroplet(random, playerPosAtStart, currentRadius));
        }

        droplets.removeIf(WaterDroplet::isDead);

        // Update ripples
        for (GroundRipple ripple : ripples) {
            ripple.update();
        }

        // Spawn ground ripples in expanded area
        if (ticks % 5 == 0 && ripples.size() < 20) {
            ripples.add(new GroundRipple(random, playerPosAtStart, currentRadius));
        }

        ripples.removeIf(GroundRipple::isDead);

        // Spawn ripples where droplets land
        if (ticks % 3 == 0 && ripples.size() < 40) {
            // Create ripples at random positions (simulating droplet impacts)
            float angle = random.nextFloat() * Mth.TWO_PI;
            float distance = random.nextFloat() * currentRadius;
            Vec3 ripplePos = new Vec3(
                    playerPosAtStart.x + Math.cos(angle) * distance,
                    playerPosAtStart.y,
                    playerPosAtStart.z + Math.sin(angle) * distance
            );
            ripples.add(new GroundRipple(random, ripplePos, 0));
        }
    }

    public void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
        if (!active) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        renderBlueSky(poseStack);
        renderStartupWaves(poseStack);
        renderGroundRipples(poseStack);
        renderDroplets(poseStack);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderBlueSky(PoseStack poseStack) {
        Tesselator tesselator = Tesselator.getInstance();

        poseStack.pushPose();

        for (int face = 0; face < 6; face++) {
            poseStack.pushPose();

            if (face == 1) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            } else if (face == 2) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            } else if (face == 3) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            } else if (face == 4) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            } else if (face == 5) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }

            Matrix4f matrix = poseStack.last().pose();

            float brightness = face == 1 ? 1.2f : (face == 2 ? 0.6f : 1.0f);
            float r = SKY_R * brightness;
            float g = SKY_G * brightness;
            float b = SKY_B * brightness;

            float finalAlpha = Math.max(alpha * 0.9f, 0.3f);

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(matrix, -2000.0F, -2000.0F, -2000.0F).setColor(r, g, b, finalAlpha);
            buffer.addVertex(matrix, -2000.0F, -2000.0F, 2000.0F).setColor(r, g, b, finalAlpha);
            buffer.addVertex(matrix, 2000.0F, -2000.0F, 2000.0F).setColor(r, g, b, finalAlpha);
            buffer.addVertex(matrix, 2000.0F, -2000.0F, -2000.0F).setColor(r, g, b, finalAlpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderStartupWaves(PoseStack poseStack) {
        if (startupWaves.isEmpty()) return;

        Tesselator tesselator = Tesselator.getInstance();

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        for (StartupWave wave : startupWaves) {
            poseStack.pushPose();

            poseStack.translate(
                    wave.centerX - camera.x,
                    wave.centerY - camera.y + 0.1f,
                    wave.centerZ - camera.z
            );

            Matrix4f matrix = poseStack.last().pose();

            float r = wave.radius;
            float a = wave.alpha * alpha;

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            int segments = 64;
            for (int i = 0; i <= segments; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segments);
                float x = Mth.cos(angle) * r;
                float z = Mth.sin(angle) * r;
                buffer.addVertex(matrix, x, 0, z).setColor(0.6f, 0.9f, 1.0f, a * 1.5f);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());

            poseStack.popPose();
        }
    }

    private void renderGroundRipples(PoseStack poseStack) {
        if (alpha < 0.1f) return;

        Tesselator tesselator = Tesselator.getInstance();

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        for (GroundRipple ripple : ripples) {
            poseStack.pushPose();

            poseStack.translate(
                    ripple.x - camera.x,
                    ripple.y - camera.y + 0.05f,
                    ripple.z - camera.z
            );

            Matrix4f matrix = poseStack.last().pose();

            float r = ripple.radius;
            float a = ripple.alpha * alpha;

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            int segments = 32;
            for (int i = 0; i <= segments; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segments);
                float x = Mth.cos(angle) * r;
                float z = Mth.sin(angle) * r;
                buffer.addVertex(matrix, x, 0, z).setColor(0.5f, 0.8f, 1.0f, a);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());

            poseStack.popPose();
        }
    }

    private void renderDroplets(PoseStack poseStack) {
        if (alpha < 0.1f) return;

        Tesselator tesselator = Tesselator.getInstance();

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (WaterDroplet droplet : droplets) {
            poseStack.pushPose();

            poseStack.translate(
                    droplet.x - camera.x,
                    droplet.y - camera.y,
                    droplet.z - camera.z
            );

            Matrix4f matrix = poseStack.last().pose();
            float size = droplet.size;
            float a = droplet.alpha * alpha;

            buffer.addVertex(matrix, -size, -size, 0).setColor(0.7f, 0.9f, 1.0f, a);
            buffer.addVertex(matrix, -size, size, 0).setColor(0.7f, 0.9f, 1.0f, a);
            buffer.addVertex(matrix, size, size, 0).setColor(0.7f, 0.9f, 1.0f, a);
            buffer.addVertex(matrix, size, -size, 0).setColor(0.7f, 0.9f, 1.0f, a);

            poseStack.popPose();
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    // STARTUP WAVE - Big radial waves on activation
    private static class StartupWave {
        float centerX, centerY, centerZ;
        float radius;
        float alpha;
        int lifetime;
        int maxLifetime;
        float expansionSpeed;

        StartupWave(Vec3 center, int index) {
            centerX = (float) center.x;
            centerY = (float) center.y;
            centerZ = (float) center.z;

            radius = index * 5.0f; // Staggered start
            alpha = 1.0f;
            lifetime = 0;
            maxLifetime = 100;
            expansionSpeed = 1.5f; // Fast expansion
        }

        void update() {
            radius += expansionSpeed;
            lifetime++;

            float progress = (float) lifetime / maxLifetime;
            alpha = 1.0f - progress;
        }

        boolean isDead() {
            return lifetime >= maxLifetime || alpha <= 0.0f;
        }
    }

    private static class WaterDroplet {
        float x, y, z;
        float size;
        float speed;
        float alpha;
        int lifetime;
        int maxLifetime;

        WaterDroplet(Random random, Vec3 playerPos, float currentRadius) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float distance = random.nextFloat() * currentRadius;

            x = (float) (playerPos.x + Mth.cos(angle) * distance);
            z = (float) (playerPos.z + Mth.sin(angle) * distance);
            y = (float) playerPos.y + 20.0f + random.nextFloat() * 30.0f;

            size = 0.1f + random.nextFloat() * 0.15f;
            speed = 0.3f + random.nextFloat() * 0.4f;
            alpha = 0.7f + random.nextFloat() * 0.3f;
            lifetime = 0;
            maxLifetime = 100 + random.nextInt(80);
        }

        void update() {
            y -= speed;
            lifetime++;

            float progress = (float) lifetime / maxLifetime;
            if (progress > 0.7f) {
                alpha = (1.0f - ((progress - 0.7f) / 0.3f)) * (0.7f + 0.3f);
            }
        }

        boolean isDead() {
            return lifetime >= maxLifetime || y < -10.0f;
        }
    }

    private static class GroundRipple {
        float x, y, z;
        float radius;
        float alpha;
        int lifetime;
        int maxLifetime;
        float expansionSpeed;

        GroundRipple(Random random, Vec3 pos, float startRadius) {
            x = (float) pos.x;
            z = (float) pos.z;
            y = (float) pos.y + 0.1f;

            radius = startRadius;
            alpha = 1.0f;
            lifetime = 0;
            maxLifetime = 40 + random.nextInt(30);
            expansionSpeed = 0.3f + random.nextFloat() * 0.3f;
        }

        void update() {
            radius += expansionSpeed;
            lifetime++;

            float progress = (float) lifetime / maxLifetime;
            alpha = 1.0f - progress;
        }

        boolean isDead() {
            return lifetime >= maxLifetime;
        }
    }
}