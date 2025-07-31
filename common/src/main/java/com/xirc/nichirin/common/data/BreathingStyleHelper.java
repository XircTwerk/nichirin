package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.BreathingStyleSyncPacket;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for easy access to breathing style data
 * No changes needed - this works through PlayerDataProvider which now uses PlayerData internally
 */
public class BreathingStyleHelper {

    /**
     * Gets the current moveset for a player
     */
    @Nullable
    public static AbstractMoveset getMoveset(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).getMoveset();
    }

    /**
     * Sets the moveset for a player (client-side, sends request to server)
     */
    public static void setMoveset(Player player, @Nullable AbstractMoveset moveset) {
        if (player.level().isClientSide) {
            // Send request to server
            BreathingStyleSyncPacket.requestStyleChange(moveset != null ? moveset.getMovesetId() : null);
        } else {
            // Direct server-side update
            PlayerDataProvider.getBreathingStyleData(player).setMoveset(moveset);
        }
    }

    /**
     * Sets the moveset by ID for a player
     */
    public static void setMovesetId(Player player, @Nullable String movesetId) {
        if (player.level().isClientSide) {
            // Send request to server
            BreathingStyleSyncPacket.requestStyleChange(movesetId);
        } else {
            // Direct server-side update
            PlayerDataProvider.getBreathingStyleData(player).setMovesetId(movesetId);
        }
    }

    /**
     * Checks if a player has a moveset selected
     */
    public static boolean hasMoveset(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).hasMoveset();
    }

    /**
     * Clears the moveset for a player
     */
    public static void clearMoveset(Player player) {
        if (player.level().isClientSide) {
            // Send request to server
            BreathingStyleSyncPacket.requestStyleChange(null);
        } else {
            // Direct server-side update
            PlayerDataProvider.getBreathingStyleData(player).clearMoveset();
        }
    }

    /**
     * Gets the moveset ID for a player
     */
    @Nullable
    public static String getMovesetId(Player player) {
        return PlayerDataProvider.getBreathingStyleData(player).getMovesetId();
    }
}