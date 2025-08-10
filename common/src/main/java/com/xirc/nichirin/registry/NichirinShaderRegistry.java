package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for custom shaders
 */
public interface NichirinShaderRegistry {

    Map<String, ShaderInstance> SHADERS = new HashMap<>();

    ResourceLocation MUSICAL_SCORE_SHADER = new ResourceLocation(BreathOfNichirin.MOD_ID, "shaders/musical_score/musical_score");

    /**
     * Initialize shaders - called during mod initialization
     */
    static void init() {
        BreathOfNichirin.LOGGER.info("Initialized Nichirin shaders");
    }

    /**
     * Register shader instances - called from render system
     */
    static void registerShaders() {
        // Shaders are registered through resource packs
        // The actual shader loading happens in the client mod initializer
    }

    /**
     * Get the musical score shader instance
     */
    static ShaderInstance getMusicalScoreShader() {
        return SHADERS.get("musical_score");
    }

    /**
     * Set the musical score shader instance (called by renderer)
     */
    static void setMusicalScoreShader(ShaderInstance shader) {
        SHADERS.put("musical_score", shader);
    }

    /**
     * Check if musical score shader is available
     */
    static boolean isMusicalScoreShaderAvailable() {
        return SHADERS.containsKey("musical_score") && SHADERS.get("musical_score") != null;
    }

    /**
     * Clear all shader instances
     */
    static void clear() {
        SHADERS.clear();
    }
}