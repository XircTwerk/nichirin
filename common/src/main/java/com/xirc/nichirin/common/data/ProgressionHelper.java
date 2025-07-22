package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class for easy access to player progression data
 */
public class ProgressionHelper {

    /**
     * Gets progression data for a player
     */
    public static BreathingStyleProgression getProgression(Player player) {
        return PlayerDataProvider.getData(player).getProgression();
    }

    /**
     * Checks if a player has unlocked a breathing style
     */
    public static boolean isStyleUnlocked(Player player, String styleId) {
        return getProgression(player).isStyleUnlocked(styleId);
    }

    /**
     * Unlocks a breathing style for a player (used by unlock handlers)
     */
    public static void unlockStyle(Player player, String styleId) {
        boolean wasAlreadyUnlocked = isStyleUnlocked(player, styleId);

        getProgression(player).unlockStyle(styleId);

        // Trigger advancement if this is a new unlock and it's Thunder Breathing
        if (!wasAlreadyUnlocked && styleId.equals("thunder_breathing") && player instanceof ServerPlayer) {
            NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER.trigger((ServerPlayer) player);
        }

        // Save the data after unlocking
        if (player instanceof ServerPlayer) {
            PlayerDataStorage.savePlayerData((ServerPlayer) player);
        }
    }

    /**
     * Gets the unlock requirement for a breathing style
     */
    public static String getUnlockRequirement(String styleId) {
        // We can call this statically since requirements are the same for all players
        return new BreathingStyleProgression().getUnlockRequirement(styleId);
    }

    /**
     * Records a demon kill for a player
     */
    public static void recordDemonKill(Player player) {
        getProgression(player).addDemonKill();
        // Note: No auto-unlock checking since Thunder Breathing uses lightning strike
    }

    /**
     * Records damage dealt by a player
     */
    public static void recordDamageDealt(Player player, int damage) {
        getProgression(player).addDamageDealt(damage);
    }

    /**
     * Formats a breathing style ID for display
     */
    private static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }
}