package com.xirc.nichirin.client.gui;

import com.xirc.nichirin.common.system.perks.PerkTier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles loading and caching perk icons.
 *
 * <p>Icons live at: {@code textures/icons/perks/{perkId}_{tier}.png}
 * <br>e.g. {@code textures/icons/perks/breath_efficiency_common.png}
 * <br>Falls back to {@code textures/icons/default_perk.png} if not found.</p>
 */
public class PerkIcon {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final String BASE_PATH    = "textures/icons/perks/";
    private static final String DEFAULT_PATH = "textures/icons/default_perk.png";
    private static final String MOD_ID       = "nichirin";

    /**
     * Returns the icon for {@code perkId} at {@code tier}, falling back to the
     * default perk icon if the specific PNG hasn't been painted yet.
     */
    public static ResourceLocation get(String perkId, PerkTier tier) {
        String key = perkId + ":" + tier.name();
        if (CACHE.containsKey(key)) return CACHE.get(key);

        ResourceLocation specific = ResourceLocation.fromNamespaceAndPath(MOD_ID,
                BASE_PATH + perkId + "_" + tier.name().toLowerCase() + ".png");

        ResourceLocation result = exists(specific) ? specific
                : ResourceLocation.fromNamespaceAndPath(MOD_ID, DEFAULT_PATH);

        CACHE.put(key, result);
        return result;
    }

    /** Clears the cache (call on resource pack reload). */
    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean exists(ResourceLocation loc) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}