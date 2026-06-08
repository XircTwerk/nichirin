package com.xirc.nichirin.client.aura;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom RenderTypes for the aura system.
 *
 * The key one is {@link #auraTranslucentNoDepthWrite(ResourceLocation)} — same shader, blend,
 * lightmap, and depth TEST as MC's entityTranslucent, but with {@code COLOR_WRITE} so the
 * pass writes colour without touching the depth buffer.
 *
 * Why that matters: the aura disc lives at the entity's centre plane. If it writes depth,
 * the back half of the entity fails the depth test when we re-render it on top, and the
 * disc occludes the player's back side. With depth-WRITE off, depth only carries terrain +
 * the original entity-pass values, so the entity overdraw lands cleanly above the disc.
 */
@Environment(EnvType.CLIENT)
public final class AuraRenderTypes {

    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    private AuraRenderTypes() {}

    public static RenderType auraTranslucentNoDepthWrite(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, AuraRenderTypes::build);
    }

    private static RenderType build(ResourceLocation texture) {
        RenderStateShard.TextureStateShard tex =
                new RenderStateShard.TextureStateShard(texture, false, false);

        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(tex)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)   // <-- the whole point
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(false);

        return RenderType.create(
                "nichirin_aura_translucent_no_depth_write",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true, // sortOnUpload — translucent
                state);
    }
}
