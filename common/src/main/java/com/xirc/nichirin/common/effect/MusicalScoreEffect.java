package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Musical Score Status Effect - Tengen's ultimate ability
 * Activates when at 1 heart, provides stat boosts and rhythm bonuses
 * Has a 45-second cooldown system
 */
public class MusicalScoreEffect extends MobEffect {

    // UUIDs for attribute modifiers
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("7E0292F2-9434-48D5-BE48-FBBF08EDF5FF");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("7E0292F2-9434-48D5-BE48-FBBF08EDF600");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("7E0292F2-9434-48D5-BE48-FBBF08EDF601");

    // Cooldown tracking
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int COOLDOWN_DURATION = 900; // 45 seconds in ticks

    // Active Musical Score tracking (since we can't use getPersistentData)
    private static final Map<UUID, MusicalScoreData> activeEffects = new HashMap<>();

    public MusicalScoreEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF5DEB3); // Bone color (musical score paper)

        // Massive stat boosts
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_UUID.toString(),
                2.0, // +200% attack damage
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_UUID.toString(),
                1.0, // +100% movement speed
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ATTACK_SPEED_UUID.toString(),
                1.5, // +150% attack speed
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    /**
     * Data class for tracking Musical Score effects
     */
    private static class MusicalScoreData {
        final float breathingMultiplier;
        final long endTime;

        MusicalScoreData(float breathingMultiplier, long endTime) {
            this.breathingMultiplier = breathingMultiplier;
            this.endTime = endTime;
        }
    }

    /**
     * Check if player can activate Musical Score (not on cooldown)
     */
    public static boolean canActivate(Player player) {
        // Check cooldown
        Long cooldownEnd = playerCooldowns.get(player.getUUID());
        if (cooldownEnd != null) {
            long currentTime = player.level().getGameTime();
            return currentTime >= cooldownEnd;
        }

        return true;
    }

    /**
     * Activate Musical Score effect
     */
    public static boolean activate(Player player) {
        if (!canActivate(player)) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activation failed for {}: Cannot activate (health: {}, cooldown remaining: {})",
                    player.getName().getString(), player.getHealth(), getRemainingCooldownSeconds(player));
            return false;
        }

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activating for {} at {} health",
                player.getName().getString(), player.getHealth());

        // Apply the effect for 5 seconds (100 ticks)
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get(),
                100, // 5 seconds
                0,
                false,
                true,
                true
        ));

        // Set cooldown
        long cooldownEnd = player.level().getGameTime() + COOLDOWN_DURATION;
        playerCooldowns.put(player.getUUID(), cooldownEnd);

        // Store Musical Score data
        long effectEndTime = player.level().getGameTime() + 100; // 5 seconds
        activeEffects.put(player.getUUID(), new MusicalScoreData(3.0f, effectEndTime));

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activated successfully for {}. Cooldown set for {} seconds",
                player.getName().getString(), COOLDOWN_DURATION / 20);

        // Send cooldown display to client
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.xirc.nichirin.common.network.CooldownDisplayPacket.sendToClient(
                    serverPlayer, "Musical Score", COOLDOWN_DURATION
            );
        }

        return true;
    }

    /**
     * Get remaining cooldown time in ticks
     */
    public static int getRemainingCooldown(Player player) {
        Long cooldownEnd = playerCooldowns.get(player.getUUID());
        if (cooldownEnd == null) {
            return 0;
        }

        long currentTime = player.level().getGameTime();
        long remaining = cooldownEnd - currentTime;
        return remaining > 0 ? (int)remaining : 0;
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public static int getRemainingCooldownSeconds(Player player) {
        return getRemainingCooldown(player) / 20;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) {
            return;
        }

        // Heal slightly to maintain the low health threshold
        if (player.getHealth() < 2.0f) {
            player.setHealth(2.0f); // Keep at exactly 1 heart
        }

        // Ensure Musical Score data is tracked
        if (!activeEffects.containsKey(player.getUUID())) {
            long effectEndTime = player.level().getGameTime() + 100; // 5 seconds from now
            activeEffects.put(player.getUUID(), new MusicalScoreData(3.0f, effectEndTime));
        }

        // Restore breath every tick (infinite breath during Musical Score)
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Restore breath to full every tick so attacks don't consume breath
            com.xirc.nichirin.common.util.BreathingManager.restore(player, 1000.0f); // Large amount to ensure full
        }

        // Visual and audio feedback
        if (player.level().getGameTime() % 10 == 0) { // Every 0.5 seconds
            // Play musical note sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.5f, 1.0f + (float)(Math.random() * 0.5));
        }

        // Log every second for debugging
        if (player.level().getGameTime() % 20 == 0) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.debug("Musical Score active for {}: Health={}, Breathing Damage Multiplier=3.0x, Infinite Breath",
                    player.getName().getString(), player.getHealth());
        }
    }

    /**
     * Get breathing damage multiplier for a player (includes rhythm bonus)
     * Returns 1.0 if no Musical Score effect, 3.0+ if active with rhythm bonus
     */
    public static float getBreathingDamageMultiplier(Player player) {
        // Null check first
        if (player == null) {
            return 1.0f;
        }

        // Check if Musical Score breathing buff is active
        MusicalScoreData data = activeEffects.get(player.getUUID());
        if (data != null) {
            long currentTime = player.level().getGameTime();

            if (currentTime < data.endTime) {
                float baseMultiplier = data.breathingMultiplier;

                // Get rhythm timing bonus
                float rhythmMultiplier = getRhythmDamageMultiplier(player);

                return baseMultiplier * rhythmMultiplier;
            } else {
                // Effect expired - clean up
                activeEffects.remove(player.getUUID());
            }
        }

        return 1.0f; // No buff active
    }

    /**
     * Calculate rhythm-based damage multiplier
     */
    private static float getRhythmDamageMultiplier(Player player) {
        // Null check first
        if (player == null) {
            return 1.0f;
        }

        // Calculate server-side rhythm timing (same logic as client)
        long gameTime = player.level().getGameTime();
        float beatCycle = (gameTime % 10.0f) / 10.0f; // 0.0 to 1.0 cycle

        // Calculate distance from center (0.5 = perfect beat)
        float distanceFromCenter = Math.abs(beatCycle - 0.5f);

        // Perfect timing (within 15% of center) - 2x bonus
        if (distanceFromCenter <= 0.15f) {
            return 2.0f;
        }
        // Good timing (within 30% of center) - 1.5x bonus
        else if (distanceFromCenter <= 0.30f) {
            return 1.5f;
        }
        // Off beat - no bonus
        else {
            return 1.0f;
        }
    }

    /**
     * Check if current timing allows no cooldown
     */
    public static boolean allowsNoCooldown(Player player) {
        // Null check first
        if (player == null) {
            return false;
        }

        // Only allow no cooldown if Musical Score is active
        if (!player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return false;
        }

        // Check for perfect timing
        long gameTime = player.level().getGameTime();
        float beatCycle = (gameTime % 10.0f) / 10.0f;
        float distanceFromCenter = Math.abs(beatCycle - 0.5f);

        return distanceFromCenter <= 0.15f; // Perfect timing only
    }

    /**
     * Clean up when effect is removed
     */
    public static void cleanupEffect(LivingEntity entity) {
        if (entity instanceof Player player) {
            // Clear breathing damage multiplier data
            activeEffects.remove(player.getUUID());
        }
    }

    /**
     * Clean up player data on logout or death
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        activeEffects.remove(player.getUUID());
    }

    /**
     * Clear cooldown on death/respawn
     */
    public static void clearCooldownOnDeath(Player player) {
        playerCooldowns.remove(player.getUUID());
        activeEffects.remove(player.getUUID());

        // Also clear from client-side HUD if this is a server player
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Send packet to clear Musical Score cooldown from client HUD
            com.xirc.nichirin.common.network.CooldownDisplayPacket.sendToClient(
                    serverPlayer, "Musical Score", 0 // 0 duration removes the cooldown
            );
        }
    }

    /**
     * Force reset cooldown (for testing or admin commands)
     */
    public static void resetCooldown(Player player) {
        playerCooldowns.remove(player.getUUID());
        activeEffects.remove(player.getUUID());
    }
}