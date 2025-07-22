package com.xirc.nichirin.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles loading and caching move icons for breathing styles
 * Icons are loaded from: textures/icons/{breathing_style}/{move_name}.png
 */
public class MoveIcon {

    private static final Map<String, ResourceLocation> ICON_CACHE = new HashMap<>();
    private static final String BASE_PATH = "textures/icons/";
    private static final String DEFAULT_ICON_PATH = "textures/icons/default_move.png";

    /**
     * Gets the icon ResourceLocation for a specific move
     * @param breathingStyle The breathing style ID (e.g., "thunder_breathing")
     * @param moveName The move ID (e.g., "rice_spirit")
     * @return ResourceLocation for the icon, or default icon if not found
     */
    public static ResourceLocation getIcon(String breathingStyle, String moveName) {
        String cacheKey = breathingStyle + ":" + moveName;

        // Check cache first
        if (ICON_CACHE.containsKey(cacheKey)) {
            return ICON_CACHE.get(cacheKey);
        }

        // Build the path: textures/icons/thunder_breathing/rice_spirit.png
        String iconPath = BASE_PATH + breathingStyle + "/" + moveName + ".png";
        ResourceLocation iconLocation = new ResourceLocation("nichirin", iconPath);

        // Check if the texture exists
        if (textureExists(iconLocation)) {
            ICON_CACHE.put(cacheKey, iconLocation);
            return iconLocation;
        }

        // Fall back to default icon
        ResourceLocation defaultIcon = new ResourceLocation("nichirin", DEFAULT_ICON_PATH);
        ICON_CACHE.put(cacheKey, defaultIcon);
        return defaultIcon;
    }

    /**
     * Gets the icon ResourceLocation for a move configuration
     * Uses the moveset ID and move ID from the configuration
     */
    public static ResourceLocation getIconFromConfig(String movesetId, String moveId) {
        return getIcon(movesetId, moveId);
    }

    /**
     * Pre-loads all icons for a breathing style
     * Useful for preventing lag when opening GUIs
     * @param breathingStyle The breathing style to preload icons for
     * @param moveNames Array of move names to preload
     */
    public static void preloadIcons(String breathingStyle, String... moveNames) {
        for (String moveName : moveNames) {
            getIcon(breathingStyle, moveName);
        }
    }

    /**
     * Clears the icon cache
     * Useful for resource pack reloads
     */
    public static void clearCache() {
        ICON_CACHE.clear();
    }

    /**
     * Checks if a texture exists in the resource manager
     */
    private static boolean textureExists(ResourceLocation location) {
        try {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            // Try to get the texture - if it doesn't exist, this will return false
            return Minecraft.getInstance().getResourceManager()
                    .getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the default icon when no specific icon is found
     */
    public static ResourceLocation getDefaultIcon() {
        return new ResourceLocation("nichirin", DEFAULT_ICON_PATH);
    }

    /**
     * Formats a move name for file path usage
     * Converts spaces to underscores and makes lowercase
     */
    public static String formatMoveNameForPath(String moveName) {
        return moveName.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * Gets icon with automatic move name formatting
     * Useful when move display names have spaces or special characters
     */
    public static ResourceLocation getIconFormatted(String breathingStyle, String moveDisplayName) {
        String formattedMoveName = formatMoveNameForPath(moveDisplayName);
        return getIcon(breathingStyle, formattedMoveName);
    }

    /**
     * Debug method to list all cached icons
     */
    public static void debugPrintCache() {
        System.out.println("=== Move Icon Cache ===");
        for (Map.Entry<String, ResourceLocation> entry : ICON_CACHE.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println("=====================");
    }
}