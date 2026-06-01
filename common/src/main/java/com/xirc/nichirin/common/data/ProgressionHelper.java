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
    public static MovesetProgression getProgression(Player player) {
        return PlayerDataProvider.getData(player).getProgression();
    }

    /**
     * Checks if a player has unlocked a moveset
     */
    public static boolean isMovesetUnlocked(Player player, String movesetId) {
        return getProgression(player).isMovesetUnlocked(movesetId);
    }

    /**
     * Checks if a player has unlocked any moveset
     */
    public static boolean hasAnyMoveset(Player player) {
        return getProgression(player).hasAnyMoveset();
    }

    /**
     * Unlocks a moveset for a player (used by unlock handlers)
     */
    public static void unlockMoveset(Player player, String movesetId) {
        boolean wasAlreadyUnlocked = isMovesetUnlocked(player, movesetId);
        boolean wasFirstMoveset = !hasAnyMoveset(player);

        getProgression(player).unlockMoveset(movesetId);

        // Trigger advancements if this is a new unlock and player is on server
        if (!wasAlreadyUnlocked && player instanceof ServerPlayer serverPlayer) {
            // Trigger specific moveset advancement
            switch (movesetId) {
                case "thunder_breathing" -> {
                    if (NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "flame_breathing" -> {
                    if (NichirinCriteriaTriggers.FLAME_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.FLAME_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "insect_breathing" -> {
                    if (NichirinCriteriaTriggers.INSECT_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.INSECT_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "sound_breathing" -> {
                    if (NichirinCriteriaTriggers.SOUND_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.SOUND_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "water_breathing" -> {
                    if (NichirinCriteriaTriggers.WATER_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.WATER_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "beast_breathing" -> {
                    if (NichirinCriteriaTriggers.BEAST_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.BEAST_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
                case "mist_breathing" -> {
                    if (NichirinCriteriaTriggers.MIST_BREATHING_TRIGGER != null) {
                        NichirinCriteriaTriggers.MIST_BREATHING_TRIGGER.trigger(serverPlayer);
                    }
                }
            }

            // Trigger First Technique advancement if this was their first moveset
            if (wasFirstMoveset && NichirinCriteriaTriggers.FIRST_BREATH_TRIGGER != null) {
                NichirinCriteriaTriggers.FIRST_BREATH_TRIGGER.trigger(serverPlayer);
            }
        }

        // Save the data after unlocking
        if (player instanceof ServerPlayer) {
            PlayerDataStorage.savePlayerData((ServerPlayer) player);
        }
    }

    /**
     * Gets the unlock requirement for a moveset
     */
    public static String getUnlockRequirement(String movesetId) {
        // We can call this statically since requirements are the same for all players
        return new MovesetProgression().getUnlockRequirement(movesetId);
    }

    /**
     * Records a demon kill for a player
     */
    public static void recordDemonKill(Player player) {
        getProgression(player).addDemonKill();
        // Note: No auto-unlock checking since movesets have specific unlock requirements
    }

    /**
     * Records damage dealt by a player
     */
    public static void recordDamageDealt(Player player, int damage) {
        getProgression(player).addDamageDealt(damage);
    }

    // Legacy methods for backwards compatibility

    /**
     * @deprecated Use isMovesetUnlocked instead
     */
    @Deprecated
    public static boolean isStyleUnlocked(Player player, String styleId) {
        return isMovesetUnlocked(player, styleId);
    }

    /**
     * @deprecated Use hasAnyMoveset instead
     */
    @Deprecated
    public static boolean hasAnyBreathingStyle(Player player) {
        return hasAnyMoveset(player);
    }

    /**
     * @deprecated Use unlockMoveset instead
     */
    @Deprecated
    public static void unlockStyle(Player player, String styleId) {
        unlockMoveset(player, styleId);
    }
}