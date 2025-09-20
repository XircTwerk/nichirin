package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.moveset.*;
import com.xirc.nichirin.common.attack.moveset.breathing.*;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Global registry for all moves across all movesets
 * Flexible system that doesn't care about attack types
 */
public interface NichirinMoveRegistry {

    // Map of all registered moves by their full ID (moveset:move)
    Map<ResourceLocation, MoveInfo> GLOBAL_MOVES = new HashMap<>();

    // Map of all registered movesets
    Map<String, AbstractMoveset> MOVESETS = new HashMap<>();

    /**
     * Register a moveset and all its moves
     */
    static void registerMoveset(AbstractMoveset moveset) {
        MOVESETS.put(moveset.getMovesetId(), moveset);

        // Register each move globally
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null) {
                ResourceLocation moveId = new ResourceLocation(BreathOfNichirin.MOD_ID,
                        moveset.getMovesetId() + "/" + config.getMoveId());

                MoveInfo info = new MoveInfo(
                        moveset.getMovesetId(),
                        config.getMoveId(),
                        config.getDisplayName(),
                        i,
                        moveset
                );

                GLOBAL_MOVES.put(moveId, info);
            }
        }

        BreathOfNichirin.LOGGER.info("Registered moveset '{}' with {} moves",
                moveset.getDisplayName(), moveset.getMoveCount());
    }

    /**
     * Auto-register all breathing style movesets
     */
    private static void registerAllMovesets() {
        BreathOfNichirin.LOGGER.info("Auto-registering breathing style movesets...");

        // Register all movesets here
        registerMoveset(new ThunderBreathingMoveset());
        registerMoveset(new FlameBreathingMoveset());
        registerMoveset(new InsectBreathingMoveset());
        registerMoveset(new SoundBreathingMoveset());
        registerMoveset(new WaterBreathingMoveset());
        registerMoveset(new DefaultDemonMoveset());
        BreathOfNichirin.LOGGER.info("Auto-registered {} movesets with {} total moves",
                MOVESETS.size(), GLOBAL_MOVES.size());
    }

    /**
     * Get a specific move by its full ID
     */
    static MoveInfo getMove(ResourceLocation id) {
        return GLOBAL_MOVES.get(id);
    }

    /**
     * Get a specific move by moveset and move name
     */
    static MoveInfo getMove(String movesetId, String moveId) {
        return getMove(new ResourceLocation(BreathOfNichirin.MOD_ID, movesetId + "/" + moveId));
    }

    /**
     * Get all moves for a specific moveset
     */
    static List<MoveInfo> getMovesForMoveset(String movesetId) {
        List<MoveInfo> moves = new ArrayList<>();
        for (MoveInfo info : GLOBAL_MOVES.values()) {
            if (info.movesetId.equals(movesetId)) {
                moves.add(info);
            }
        }
        return moves;
    }

    /**
     * Get a moveset by ID
     */
    static AbstractMoveset getMoveset(String movesetId) {
        return MOVESETS.get(movesetId);
    }

    /**
     * Get all registered movesets
     */
    static Map<String, AbstractMoveset> getAllMovesets() {
        return new HashMap<>(MOVESETS);
    }

    /**
     * Get all registered moves globally
     */
    static Map<ResourceLocation, MoveInfo> getAllMoves() {
        return new HashMap<>(GLOBAL_MOVES);
    }

    /**
     * Get total number of moves across all movesets
     */
    static int getTotalMoveCount() {
        return GLOBAL_MOVES.size();
    }

    /**
     * Initialize the registry and auto-register all movesets
     */
    static void init() {
        GLOBAL_MOVES.clear();
        MOVESETS.clear();
        BreathOfNichirin.LOGGER.info("Global move registry initialized");

        // Auto-register all movesets
        registerAllMovesets();
    }

    /**
     * Information about a registered move
     */
    class MoveInfo {
        public final String movesetId;
        public final String moveId;
        public final String displayName;
        public final int index;
        public final AbstractMoveset moveset;

        MoveInfo(String movesetId, String moveId, String displayName, int index, AbstractMoveset moveset) {
            this.movesetId = movesetId;
            this.moveId = moveId;
            this.displayName = displayName;
            this.index = index;
            this.moveset = moveset;
        }

        /**
         * Execute this move for a player
         */
        public void execute(net.minecraft.world.entity.player.Player player) {
            moveset.performMove(player, index);
        }

        @Override
        public String toString() {
            return movesetId + ":" + moveId + " (" + displayName + ")";
        }
    }
}