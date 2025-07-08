package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Global registry for all moves across all movesets
 * Flexible system that doesn't care about attack types
 */
public class NichirinMoveRegistry {

    // Map of all registered moves by their full ID (moveset:move)
    private static final Map<ResourceLocation, MoveInfo> GLOBAL_MOVES = new HashMap<>();

    // Map of all registered movesets
    private static final Map<String, AbstractMoveset> MOVESETS = new HashMap<>();

    /**
     * Register a moveset and all its moves
     */
    public static void registerMoveset(AbstractMoveset moveset) {
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
     * Get a specific move by its full ID
     */
    public static MoveInfo getMove(ResourceLocation id) {
        return GLOBAL_MOVES.get(id);
    }

    /**
     * Get a specific move by moveset and move name
     */
    public static MoveInfo getMove(String movesetId, String moveId) {
        return getMove(new ResourceLocation(BreathOfNichirin.MOD_ID, movesetId + "/" + moveId));
    }

    /**
     * Get all moves for a specific moveset
     */
    public static List<MoveInfo> getMovesForMoveset(String movesetId) {
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
    public static AbstractMoveset getMoveset(String movesetId) {
        return MOVESETS.get(movesetId);
    }

    /**
     * Get all registered movesets
     */
    public static Map<String, AbstractMoveset> getAllMovesets() {
        return new HashMap<>(MOVESETS);
    }

    /**
     * Get all registered moves globally
     */
    public static Map<ResourceLocation, MoveInfo> getAllMoves() {
        return new HashMap<>(GLOBAL_MOVES);
    }

    /**
     * Get total number of moves across all movesets
     */
    public static int getTotalMoveCount() {
        return GLOBAL_MOVES.size();
    }

    /**
     * Initialize the registry (call this in your main mod init)
     */
    public static void init() {
        GLOBAL_MOVES.clear();
        MOVESETS.clear();
        BreathOfNichirin.LOGGER.info("Global move registry initialized");
    }

    /**
     * Information about a registered move
     */
    public static class MoveInfo {
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