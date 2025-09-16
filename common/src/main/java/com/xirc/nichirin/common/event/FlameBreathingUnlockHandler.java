package com.xirc.nichirin.common.event;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles unlocking Flame Breathing when on fire for 15 seconds (300 ticks)
 */
public class FlameBreathingUnlockHandler {

    // Track players currently on fire and their burn duration
    private static final Map<UUID, Integer> burningPlayers = new HashMap<>();
    private static final int REQUIRED_BURN_TICKS = 300; // 15 seconds * 20 ticks/second

    public static void register() {
        // Register server tick event to track burning players
        TickEvent.SERVER_PRE.register(server -> {
            // Process all online players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerBurning(player);
            }
        });
    }

    /**
     * Checks if player is burning and tracks duration
     */
    private static void checkPlayerBurning(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isOnFire()) {
            // Player is burning - increment their burn counter
            int currentTicks = burningPlayers.getOrDefault(playerId, 0);
            currentTicks++;
            burningPlayers.put(playerId, currentTicks);

            // Debug logging every 5 seconds (100 ticks)
            if (currentTicks % 100 == 0) {
                BreathOfNichirin.LOGGER.info("Player {} has been burning for {} ticks ({} seconds)",
                        player.getName().getString(), currentTicks, currentTicks / 20);
            }

            // Check if they've been burning long enough
            if (currentTicks >= REQUIRED_BURN_TICKS) {
                BreathOfNichirin.LOGGER.info("Player {} reached 300 burn ticks, checking unlock...",
                        player.getName().getString());
                checkFlameBreathingUnlock(player);
                // Remove from tracking to prevent multiple unlocks
                burningPlayers.remove(playerId);
            }
        } else {
            // Player is not burning - reset their counter
            if (burningPlayers.containsKey(playerId)) {
                BreathOfNichirin.LOGGER.info("Player {} stopped burning, resetting counter",
                        player.getName().getString());
                burningPlayers.remove(playerId);
            }
        }
    }

    /**
     * Checks if player should unlock Flame Breathing
     */
    private static void checkFlameBreathingUnlock(ServerPlayer player) {
        BreathOfNichirin.LOGGER.info("Checking flame breathing unlock for player {}", player.getName().getString());

        // Check if player already has Flame Breathing unlocked
        if (ProgressionHelper.isStyleUnlocked(player, "flame_breathing")) {
            BreathOfNichirin.LOGGER.info("Player {} already has flame breathing unlocked", player.getName().getString());
            return; // Already unlocked
        }

        BreathOfNichirin.LOGGER.info("Player {} doesn't have flame breathing, unlocking now!", player.getName().getString());
        // Unlock Flame Breathing!
        unlockFlameBreathing(player);
    }

    /**
     * Unlocks Flame Breathing for the player
     */
    private static void unlockFlameBreathing(ServerPlayer player) {
        // Record the unlock in progression system (this handles all advancement triggers)
        ProgressionHelper.unlockStyle(player, "flame_breathing");

        // Set Flame Breathing as the player's style
        PlayerDataProvider.updateAndSync(player, "flame_breathing");

        // Send success message
        player.displayClientMessage(
                Component.literal("🔥 If you are feeling disheartened, that you are somehow not enough. Set your heart ablaze! Flame Breathing unlocked! 🔥")
                        .withStyle(style -> style.withColor(0xFF5500).withBold(true)),
                false
        );

        // Play fire-related sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_AMBIENT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Gets the current burn progress for a player (for UI/debugging)
     */
    public static int getBurnProgress(UUID playerId) {
        return burningPlayers.getOrDefault(playerId, 0);
    }

    /**
     * Gets the required burn duration in ticks
     */
    public static int getRequiredBurnTicks() {
        return REQUIRED_BURN_TICKS;
    }
}