package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Simplified helper class for breathing style data access
 * Removed unused client-side methods since breathing styles are server-controlled
 */
public class MovesetHelper {

    /**
     * Gets the current moveset for a player
     */
    @Nullable
    public static AbstractMoveset getMoveset(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).getMoveset();
    }

    /**
     * Checks if a player has a moveset selected
     */
    public static boolean hasMoveset(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).hasMoveset();
    }

    /**
     * Gets the moveset ID for a player
     */
    @Nullable
    public static String getMovesetId(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).getMovesetId();
    }
}