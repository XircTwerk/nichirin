package com.xirc.nichirin.client.outline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Outline render types. Two variants used in combination depending on the instance config:
 *
 *   EDGE                — cull FRONT + LEQUAL depth test. The expanded-model back-faces
 *                         draw only at the silhouette edges where the entity is *visible*.
 *                         Produces the cel-shader edge effect. ALWAYS used.
 *
 *   BEHIND_WALLS_FILL   — cull BACK + GREATER depth test. The UN-expanded model draws only
 *                         at pixels where the outline depth is BEHIND the framebuffer
 *                         depth — i.e. only where the entity is occluded by walls or other
 *                         geometry. ONLY used when seeThroughWalls=true. Combined with EDGE
 *                         it gives: edge outline when visible, silhouette fill through walls.
 *
 * Both write colour only (no depth write) so they don't interfere with later passes.
 */
@Environment(EnvType.CLIENT)
public final class OutlineRenderTypes {

    private static RenderType EDGE;
    private static RenderType BEHIND_WALLS_FILL;

    private OutlineRenderTypes() {}

    /** Edge outline using the cull-front back-face trick. Use for both modes. */
    public static RenderType edge(ResourceLocation texture) {
        if (EDGE == null) EDGE = buildEdge(texture);
        return EDGE;
    }

    /** Filled silhouette behind walls. Use only when seeThroughWalls=true. */
    public static RenderType behindWallsFill(ResourceLocation texture) {
        if (BEHIND_WALLS_FILL == null) BEHIND_WALLS_FILL = buildBehindWalls(texture);
        return BEHIND_WALLS_FILL;
    }

    private static RenderType buildEdge(ResourceLocation texture) {
        RenderStateShard.TextureStateShard tex =
                new RenderStateShard.TextureStateShard(texture, false, false);

        // Custom shader does normal-displacement on the GPU. Front faces are culled so only
        // the back-facing surfaces of the displaced mesh draw — they form a clean edge ring
        // behind the original model, exactly the cel-shader silhouette look. The shader
        // displaces along each vertex's NORMAL rather than scaling around the entity origin,
        // so body parts don't drift apart and the head doesn't pop out.
        RenderStateShard.ShaderStateShard shaderShard =
                new RenderStateShard.ShaderStateShard(() -> {
                    // Push the current outline width into the shader's uniform every draw.
                    OutlineShaderHolder.uploadWidth();
                    return OutlineShaderHolder.getShader();
                });

        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(shaderShard)
                .setTextureState(tex)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(new FrontCullStateShard())
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(false);
        return RenderType.create(
                "nichirin_outline_edge",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                state);
    }

    private static RenderType buildBehindWalls(ResourceLocation texture) {
        RenderStateShard.TextureStateShard tex =
                new RenderStateShard.TextureStateShard(texture, false, false);
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(tex)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                // Normal back-face culling — we want the front-facing surfaces of the regular
                // (un-expanded) model. With GREATER depth test these draw only where they're
                // behind the framebuffer depth, i.e. behind walls / in-front geometry.
                .setCullState(RenderStateShard.CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setDepthTestState(RenderStateShard.GREATER_DEPTH_TEST)
                .createCompositeState(false);
        return RenderType.create(
                "nichirin_outline_behind_walls_fill",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                state);
    }
}
