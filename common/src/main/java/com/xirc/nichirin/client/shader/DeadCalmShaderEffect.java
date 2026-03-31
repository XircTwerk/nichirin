package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class DeadCalmShaderEffect extends NichirinPostProcessor {

    private static final int TOTAL_TICKS = 200;
    private static final int FADE_IN_TICKS = 20;
    private static final int FADE_OUT_TICKS = 40;

    private int effectTicks = 0;
    private float intensity = 0f;
    private Vector3f playerPosAtStart = new Vector3f(0, 0, 0);
    private float attackRange = 10.0f;

    private final DeadCalmSkyBoxRenderer skyboxRenderer = new DeadCalmSkyBoxRenderer();
    private final DeadCalmBlockRenderer blockRenderer = new DeadCalmBlockRenderer();

    @Override
    public ResourceLocation getShaderEffectId() {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "dead_calm");
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null || effects.length == 0) {
            System.out.println("ERROR: effects is null or empty!");
            return;
        }

        effectTicks++;

        float progress = Math.min((float) effectTicks / TOTAL_TICKS, 1.0f);

        if (effectTicks <= FADE_IN_TICKS) {
            intensity = (float) effectTicks / FADE_IN_TICKS;
        } else if (effectTicks > TOTAL_TICKS - FADE_OUT_TICKS) {
            intensity = (float) (TOTAL_TICKS - effectTicks) / FADE_OUT_TICKS;
        } else {
            intensity = 1.0f;
        }

        Uniform progressUniform = effects[0].getUniform("Progress");
        if (progressUniform != null) {
            progressUniform.set(progress);
        }

        Uniform intensityUniform = effects[0].getUniform("Intensity");
        if (intensityUniform != null) {
            intensityUniform.set(intensity);
        }

        if (effectTicks >= TOTAL_TICKS) {
            System.out.println("DEBUG: Effect finished after 240 ticks, deactivating");
            setActive(false);
        }
    }

    @Override
    protected void afterProcess() {
        // Render blue blocks after post-processing
        PoseStack poseStack = new PoseStack();
        blockRenderer.render(poseStack);
    }

    public void trigger() {
        trigger(10.0f); // Default range
    }

    public void trigger(float range) {
        // TODO: Dead Calm shader temporarily disabled
        if (true) return;
        System.out.println("DEBUG: Dead Calm shader trigger() called with range=" + range);
        effectTicks = 0;
        intensity = 0.0f;
        attackRange = range;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            playerPosAtStart = mc.player.position().toVector3f();

            // Activate all renderers with the attack range
            skyboxRenderer.activate();
            blockRenderer.activate(mc.player.position(), range);
        }

        setActive(true);
        System.out.println("DEBUG: Dead Calm shader active with " + range + " block range, will run for 240 ticks");
    }

    public DeadCalmSkyBoxRenderer getSkyboxRenderer() {
        return skyboxRenderer;
    }

    public DeadCalmBlockRenderer getBlockRenderer() {
        return blockRenderer;
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (!active) {
            effectTicks = 0;
            intensity = 0.0f;
            skyboxRenderer.deactivate();
            blockRenderer.deactivate();
        }
    }
}