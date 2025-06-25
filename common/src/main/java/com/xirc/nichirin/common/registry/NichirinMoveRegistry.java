package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.moves.BasicSlashAttack;
import com.xirc.nichirin.common.attack.moves.thunder.ChainLightningAttack;
import com.xirc.nichirin.common.attack.moves.thunder.ThunderClapAttack;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry interface for breathing technique moves
 * Simple implementation using a HashMap for cross-platform compatibility
 */
public interface NichirinMoveRegistry {

    // Simple registry using HashMap for cross-platform compatibility
    Map<ResourceLocation, Supplier<AbstractBreathingAttack>> MOVE_SUPPLIERS = new HashMap<>();
    Map<ResourceLocation, AbstractBreathingAttack> MOVE_INSTANCES = new HashMap<>();

    // Register individual moves
    static void registerMoves() {
        register("thunder_clap", ThunderClapAttack::new);
        register("chain_lightning", ChainLightningAttack::new);
    }

    // Registration helper
    static void register(String name, Supplier<AbstractBreathingAttack> supplier) {
        ResourceLocation id = new ResourceLocation(BreathOfNichirin.MOD_ID, name);
        MOVE_SUPPLIERS.put(id, supplier);
        MOVE_INSTANCES.put(id, supplier.get());
    }

    // Helper methods
    static AbstractBreathingAttack getMove(ResourceLocation id) {
        return MOVE_INSTANCES.get(id);
    }

    static AbstractBreathingAttack getMove(String name) {
        return getMove(new ResourceLocation(BreathOfNichirin.MOD_ID, name));
    }

    static ResourceLocation getKey(AbstractBreathingAttack move) {
        return MOVE_INSTANCES.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(move.getClass()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    // Get all registered moves
    static Map<ResourceLocation, AbstractBreathingAttack> getAllMoves() {
        return new HashMap<>(MOVE_INSTANCES);
    }

    // Initialize method to be called in your main mod class
    static void init() {
        registerMoves();
    }

    // Convenience getters for specific moves
    static AbstractBreathingAttack getThunderClap() {
        return getMove("thunder_clap");
    }

    static AbstractBreathingAttack getChainLightning() {
        return getMove("chain_lightning");
    }
}