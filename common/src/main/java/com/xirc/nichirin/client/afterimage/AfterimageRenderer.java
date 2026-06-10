package com.xirc.nichirin.client.afterimage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class AfterimageRenderer {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private record Entry(int entityId, Vec3 from, Vec3 to, long spawnTick, int lifetimeTicks,
                         int copies, float alpha) {
    }

    private AfterimageRenderer() {
    }

    public static void add(int entityId, Vec3 from, Vec3 to, int lifetimeTicks, int copies, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || copies <= 0 || lifetimeTicks <= 0 || alpha <= 0.0F) {
            return;
        }

        // No per-entity dedupe: a single entity can have multiple in-flight trails (e.g. multi-bounce
        // Thunderclap segments). Lifetime fade caps how many ever coexist.
        ENTRIES.add(new Entry(
                entityId,
                from,
                to,
                minecraft.level.getGameTime(),
                Math.min(lifetimeTicks, 40),
                Math.min(copies, 10),
                Math.min(alpha, 0.75F)
        ));
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ENTRIES.isEmpty() || AfterimageRenderState.isRendering()) {
            return;
        }

        long now = minecraft.level.getGameTime();
        ENTRIES.removeIf(entry -> now - entry.spawnTick >= entry.lifetimeTicks);
        if (ENTRIES.isEmpty()) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        AfterimageRenderState.setRendering(true);
        try {
            for (Entry entry : ENTRIES) {
                Entity entity = minecraft.level.getEntity(entry.entityId);
                if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isInvisible()) {
                    continue;
                }
                if (entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) minecraft.getEntityRenderDispatcher()
                        .getRenderer(entity);
                ResourceLocation texture = renderer.getTextureLocation(entity);

                float age = (now - entry.spawnTick + partialTick) / (float) entry.lifetimeTicks;
                float lifetimeFade = Math.max(0.0F, 1.0F - age);

                for (int i = entry.copies; i >= 1; i--) {
                    float pathT = i / (float) (entry.copies + 1);
                    Vec3 pos = entry.from.lerp(entry.to, pathT);
                    float copyFade = i / (float) entry.copies;
                    float alpha = entry.alpha * lifetimeFade * copyFade;
                    if (alpha <= 0.02F) {
                        continue;
                    }

                    int packedLight = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(pos));

                    poseStack.pushPose();
                    minecraft.getEntityRenderDispatcher().render(
                            entity,
                            pos.x - cameraPos.x,
                            pos.y - cameraPos.y,
                            pos.z - cameraPos.z,
                            entity.getYRot(),
                            partialTick,
                            poseStack,
                            new AfterimageBufferSource(buffers, texture, alpha),
                            packedLight
                    );
                    poseStack.popPose();
                }
            }
        } finally {
            AfterimageRenderState.setRendering(false);
            buffers.endBatch();
        }
    }
}
