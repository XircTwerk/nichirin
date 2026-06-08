package com.xirc.nichirin.client.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderStateShard;
import org.lwjgl.opengl.GL11;

/**
 * CullStateShard variant that culls FRONT faces instead of the default BACK. Used by the
 * outline render type to implement the classic cel-shader edge silhouette trick:
 *
 *   - Entity is rendered normally first (front-facing surfaces visible).
 *   - Then the same model is rendered slightly upscaled, with FRONT faces culled.
 *   - Only the back-facing surfaces of the expanded model draw — and they're behind the
 *     original surfaces from the camera's perspective.
 *   - Inside the model silhouette: depth test fails (original is closer) → nothing draws.
 *   - At the silhouette edge: no original geometry → expanded back-face passes test → draws.
 *
 * Result: a clean coloured edge around the entity, no flat-color fill over the body.
 */
@Environment(EnvType.CLIENT)
public final class FrontCullStateShard extends RenderStateShard.CullStateShard {
    public FrontCullStateShard() {
        super(true);
    }

    @Override
    public void setupRenderState() {
        RenderSystem.enableCull();
        GL11.glCullFace(GL11.GL_FRONT);
    }

    @Override
    public void clearRenderState() {
        // Restore the default cull face so subsequent draws aren't broken.
        GL11.glCullFace(GL11.GL_BACK);
    }
}
