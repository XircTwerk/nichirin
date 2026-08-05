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
 * Builds Akaza's emissive glow textures in code from his blue source textures — no baked assets.
 *
 * <p>The base variant preserves the authored texture colors at full brightness. The Overdrive
 * variant hue-rotates blue → red without changing the source brightness. Results are
 * cached; on any failure it falls back to the untouched source so the glow degrades gracefully.</p>
 */
@Environment(EnvType.CLIENT)
public final class AkazaEmissiveTextures {

    /** +0.4 turns (144°): the ~216° blue lines land on red while keeping their shading. */
    private static final float HUE_SHIFT = 0.4f;
    /** Preserve the source brightness when promoting authored pixels to emissive. */
    private static final float BRIGHTNESS = 1.0f;

    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private AkazaEmissiveTextures() {}

    /**
     * Returns the authored source normally or a red hue-shifted copy in Overdrive.
     */
    public static ResourceLocation processed(ResourceLocation source, boolean overdrive) {
        if (!overdrive) return source;
        String key = source + (overdrive ? "#od" : "#base");
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) return cached;

        ResourceLocation out = ResourceLocation.fromNamespaceAndPath(
                source.getNamespace(),
                source.getPath().replace(".png", "") + (overdrive ? "_glow_od.png" : "_glow.png"));
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> res = mc.getResourceManager().getResource(source);
            if (res.isEmpty()) {
                CACHE.put(key, source);
                return source;
            }
            NativeImage image;
            try (InputStream in = res.get().open()) {
                image = NativeImage.read(in);
            }
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setPixelRGBA(x, y, process(image.getPixelRGBA(x, y), overdrive));
                }
            }
            mc.getTextureManager().register(out, new DynamicTexture(image));
            CACHE.put(key, out);
            return out;
        } catch (Exception e) {
            CACHE.put(key, source); // graceful fallback
            return source;
        }
    }

    /** Darkens (and, for overdrive, hue-rotates) one pixel. NativeImage packs colors as ABGR. */
    private static int process(int abgr, boolean overdrive) {
        int a = (abgr >>> 24) & 0xFF;
        if (a == 0) return abgr; // transparent — leave as-is
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = overdrive ? (hsb[0] + HUE_SHIFT) % 1.0f : hsb[0];
        float brightness = hsb[2] * BRIGHTNESS;
        int rgb = Color.HSBtoRGB(hue, hsb[1], brightness);

        int nr = (rgb >> 16) & 0xFF;
        int ng = (rgb >> 8) & 0xFF;
        int nb = rgb & 0xFF;
        return (a << 24) | (nb << 16) | (ng << 8) | nr;
    }
}
