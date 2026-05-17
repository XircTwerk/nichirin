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

    /**
     * Initialize shaders - called during mod initialization
     */
    static void init() {
    }

    /**
     * Register shader instances - called from render system
     */
    static void registerShaders() {
        // Shaders are registered through resource packs
        // The actual shader loading happens in the client mod initializer
    }

    /**
     * Clear all shader instances
     */
    static void clear() {
        SHADERS.clear();
    }
}