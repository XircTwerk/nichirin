package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for moveset data access
 * Supports both breathing techniques and demon arts
 */
public class MovesetHelper {

    /**
     * Gets the current moveset for a player (breathing or demon)
     */
    @Nullable
    public static AbstractMoveset getMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).getMoveset();
    }

    /**
     * Checks if a player has any moveset selected (breathing or demon)
     */
    public static boolean hasMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasMoveset();
    }

    /**
     * Gets the moveset ID for a player
     */
    @Nullable
    public static String getMovesetId(Player player) {
        return PlayerDataProvider.getMovesetData(player).getMovesetId();
    }

    // Breathing-specific methods

    /**
     * Gets the current breathing moveset for a player
     */
    @Nullable
    public static AbstractMoveset getBreathingMoveset(Player player) {
        AbstractMoveset moveset = getMoveset(player);
        return (moveset != null && moveset.isBreathingMoveset()) ? moveset : null;
    }

    /**
     * Checks if a player has a breathing technique selected
     */
    public static boolean hasBreathingMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasBreathingMoveset();
    }

    /**
     * Gets the breathing moveset ID for a player
     */
    @Nullable
    public static String getBreathingMovesetId(Player player) {
        AbstractMoveset moveset = getBreathingMoveset(player);
        return moveset != null ? moveset.getMovesetId() : null;
    }

    // Demon art-specific methods

    /**
     * Gets the current demon art moveset for a player
     */
    @Nullable
    public static AbstractMoveset getDemonMoveset(Player player) {
        AbstractMoveset moveset = getMoveset(player);
        return (moveset != null && moveset.isDemonMoveset()) ? moveset : null;
    }

    /**
     * Checks if a player has a demon art selected
     */
    public static boolean hasDemonMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasDemonMoveset();
    }

    /**
     * Gets the demon art moveset ID for a player
     */
    @Nullable
    public static String getDemonMovesetId(Player player) {
        AbstractMoveset moveset = getDemonMoveset(player);
        return moveset != null ? moveset.getMovesetId() : null;
    }

    // Legacy methods for backwards compatibility

    /**
     * @deprecated Use getMovesetData() instead
     */
    @Deprecated
    public static MovesetData getBreathingStyleData(Player player) {
        return PlayerDataProvider.getMovesetData(player);
    }

    /**
     * @deprecated Use hasBreathingMoveset() instead
     */
    @Deprecated
    public static boolean hasBreathingStyle(Player player) {
        return hasBreathingMoveset(player);
    }

    /**
     * @deprecated Use getBreathingMovesetId() instead
     */
    @Deprecated
    public static String getBreathingStyleId(Player player) {
        return getBreathingMovesetId(player);
    }
}