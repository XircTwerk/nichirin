package com.xirc.nichirin.client.renderer.entity.layer;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.awt.Color;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produces the red Overdrive variant of Akaza's blue emissive textures (lines / eyes) entirely in
 * code — no baked red asset. The blue source is loaded once, hue-rotated per pixel into a
 * {@link DynamicTexture}, registered, and cached. On any failure it falls back to the blue texture
 * so the glow degrades gracefully instead of crashing the render.
 */
@Environment(EnvType.CLIENT)
public final class AkazaEmissiveTextures {

    /** +0.4 turns (144°): the ~216° blue lines land on red while keeping their shading. */
    private static final float HUE_SHIFT = 0.4f;

    private static final Map<ResourceLocation, ResourceLocation> RED_CACHE = new ConcurrentHashMap<>();

    private AkazaEmissiveTextures() {}

    public static ResourceLocation redVariant(ResourceLocation blue) {
        ResourceLocation cached = RED_CACHE.get(blue);
        if (cached != null) return cached;

        ResourceLocation red = ResourceLocation.fromNamespaceAndPath(
                blue.getNamespace(),
                blue.getPath().replace(".png", "") + "_overdrive.png");
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> res = mc.getResourceManager().getResource(blue);
            if (res.isEmpty()) {
                RED_CACHE.put(blue, blue);
                return blue;
            }
            NativeImage image;
            try (InputStream in = res.get().open()) {
                image = NativeImage.read(in);
            }
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setPixelRGBA(x, y, shiftPixel(image.getPixelRGBA(x, y)));
                }
            }
            mc.getTextureManager().register(red, new DynamicTexture(image));
            RED_CACHE.put(blue, red);
            return red;
        } catch (Exception e) {
            RED_CACHE.put(blue, blue); // graceful fallback: keep the blue glow
            return blue;
        }
    }

    /** Hue-rotates one pixel. NativeImage packs colors as ABGR (0xAABBGGRR). */
    private static int shiftPixel(int abgr) {
        int a = (abgr >>> 24) & 0xFF;
        if (a == 0) return abgr; // transparent — leave as-is
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        int rgb = Color.HSBtoRGB((hsb[0] + HUE_SHIFT) % 1.0f, hsb[1], hsb[2]);
        int nr = (rgb >> 16) & 0xFF;
        int ng = (rgb >> 8) & 0xFF;
        int nb = rgb & 0xFF;
        return (a << 24) | (nb << 16) | (ng << 8) | nr;
    }
}
