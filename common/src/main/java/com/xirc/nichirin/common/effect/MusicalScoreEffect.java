package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Musical Score Status Effect - Tengen's ultimate ability
 * Activates when at 1 heart
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
                0.5, // +100% movement speed
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
     * Check if player can activate Musical Score (not on cooldown and at low health)
     */
    public static boolean canActivate(Player player) {
        // Check health requirement (1 heart = 2 health points)
        if (player.getHealth() > 2.0f) {
            return false;
        }

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
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) {
            return;
        }

        // Heal slightly to maintain the low health threshold
        if (player.getHealth() < 2.0f) {
            player.setHealth(2.0f); // Keep at exactly 1 heart
        }

        // Visual and audio feedback
        if (player.level().getGameTime() % 10 == 0) { // Every 0.5 seconds
            // Play musical note sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.5f, 1.0f + (float)(Math.random() * 0.5));
        }
    }
    /**
     * Clean up player data on logout
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
    }

    /**
     * Force reset cooldown (for testing or admin commands)
     */
    public static void resetCooldown(Player player) {
        playerCooldowns.remove(player.getUUID());
    }
}