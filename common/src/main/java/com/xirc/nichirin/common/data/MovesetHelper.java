package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for moveset data access
 * Enhanced to support dual moveset system (breathing techniques AND demon arts simultaneously)
 */
public class MovesetHelper {

    /**
     * Gets the current breathing moveset for a player
     */
    @Nullable
    public static AbstractMoveset getBreathingMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).getBreathingMoveset();
    }

    /**
     * Gets the current demon moveset for a player
     */
    @Nullable
    public static AbstractMoveset getDemonMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).getDemonMoveset();
    }

    /**
     * Gets the current neutral fighting style moveset for a player.
     */
    @Nullable
    public static AbstractMoveset getFightingMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).getFightingMoveset();
    }

    /**
     * Gets the primary moveset for a player (breathing takes precedence, then demon)
     * This maintains backwards compatibility with existing code
     */
    @Nullable
    public static AbstractMoveset getMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).getMoveset();
    }

    /**
     * Checks if a player has any moveset selected (breathing OR demon)
     */
    public static boolean hasMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasMoveset();
    }

    /**
     * Checks if a player has a breathing technique selected
     */
    public static boolean hasBreathingMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasBreathingMoveset();
    }

    /**
     * Checks if a player has a demon art selected
     */
    public static boolean hasDemonMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasDemonMoveset();
    }

    /**
     * Checks if a player has a neutral fighting style selected.
     */
    public static boolean hasFightingMoveset(Player player) {
        return PlayerDataProvider.getMovesetData(player).hasFightingMoveset();
    }

    /**
     * Gets the breathing moveset ID for a player
     */
    @Nullable
    public static String getBreathingMovesetId(Player player) {
        return PlayerDataProvider.getMovesetData(player).getBreathingMovesetId();
    }

    /**
     * Gets the demon moveset ID for a player
     */
    @Nullable
    public static String getDemonMovesetId(Player player) {
        return PlayerDataProvider.getMovesetData(player).getDemonMovesetId();
    }

    /**
     * Gets the fighting style moveset ID for a player.
     */
    @Nullable
    public static String getFightingMovesetId(Player player) {
        return PlayerDataProvider.getMovesetData(player).getFightingMovesetId();
    }

    /**
     * Gets the primary moveset ID for a player (backwards compatibility)
     */
    @Nullable
    public static String getMovesetId(Player player) {
        return PlayerDataProvider.getMovesetData(player).getMovesetId();
    }

    /**
     * Sets a breathing moveset for a player
     */
    public static void setBreathingMoveset(Player player, @Nullable String movesetId) {
        PlayerDataProvider.getMovesetData(player).setBreathingMovesetId(movesetId);
    }

    /**
     * Sets a demon moveset for a player
     */
    public static void setDemonMoveset(Player player, @Nullable String movesetId) {
        PlayerDataProvider.getMovesetData(player).setDemonMovesetId(movesetId);
    }

    /**
     * Sets a neutral fighting style for a player.
     */
    public static void setFightingMoveset(Player player, @Nullable String movesetId) {
        PlayerDataProvider.getMovesetData(player).setFightingMovesetId(movesetId);
    }

    /**
     * Sets a moveset for a player (backwards compatibility - determines type automatically)
     */
    public static void setMoveset(Player player, @Nullable String movesetId) {
        PlayerDataProvider.getMovesetData(player).setMovesetId(movesetId);
    }

    /**
     * Clears all movesets for a player
     */
    public static void clearMovesets(Player player) {
        PlayerDataProvider.getMovesetData(player).clearMovesets();
    }

    /**
     * Clears only the breathing moveset for a player
     */
    public static void clearBreathingMoveset(Player player) {
        PlayerDataProvider.getMovesetData(player).clearBreathingMoveset();
    }

    /**
     * Clears only the demon moveset for a player
     */
    public static void clearDemonMoveset(Player player) {
        PlayerDataProvider.getMovesetData(player).clearDemonMoveset();
    }

    /**
     * Clears only the fighting style for a player.
     */
    public static void clearFightingMoveset(Player player) {
        PlayerDataProvider.getMovesetData(player).clearFightingMoveset();
    }

    /**
     * Gets the moveset data object for a player
     */
    public static MovesetData getMovesetData(Player player) {
        return PlayerDataProvider.getMovesetData(player);
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
