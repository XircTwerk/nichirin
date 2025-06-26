package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.common.attack.moves.SimpleSlashAttack;
import com.xirc.nichirin.common.util.AnimationUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A simple katana with basic light attacks
 */
public class SimpleKatana extends SwordItem {

    // Track combo state per player
    private static final int COMBO_WINDOW = 20; // ticks to chain attacks
    private static final int LIGHT_ATTACK_COOLDOWN = 10; // ticks between attacks

    // Per-player state tracking
    private final Map<UUID, PlayerAttackState> playerStates = new HashMap<>();

    public SimpleKatana(Properties properties) {
        // Use Iron tier as base, 6 attack damage (3 + 3 from iron tier), -2.4 attack speed
        super(Tiers.IRON, 3, -2.4f, properties);
        System.out.println("DEBUG: SimpleKatana created");
    }

    /**
     * Creates the first light slash attack
     */
    private SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(3, 13, 4)  // startup, active, recovery
                .withDamage(4.0f)
                .withRange(2.5f)
                .withKnockback(0.3f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(15)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Creates the second light slash attack for combos
     */
    private SimpleSlashAttack createLightSlash2() {
        return new SimpleSlashAttack.Builder()
                .withTiming(2, 14, 4)  // Faster startup
                .withDamage(5.0f)
                .withRange(2.5f)
                .withKnockback(0.5f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Called when the player left-clicks with this item
     */
    public void performAttack(Player player) {
        System.out.println("DEBUG: performAttack called for player: " + player.getName().getString());

        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check cooldown
        long currentTime = player.level().getGameTime();
        if (currentTime - state.lastAttackInitiated < LIGHT_ATTACK_COOLDOWN) {
            System.out.println("DEBUG: Attack on cooldown");
            return;
        }

        // Check if any attack is currently active
        if (state.currentAttack != null && state.currentAttack.isActive()) {
            System.out.println("DEBUG: Attack already active");
            return;
        }

        // Determine which attack to use based on combo
        boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

        // Select and start appropriate attack
        if (isCombo && state.comboCount == 1) {
            // Second attack in combo - create new instance
            state.currentAttack = createLightSlash2();
            AnimationUtils.playAnimation(player, "light_slash_2");
            state.comboCount = 2;
        } else {
            // First attack or reset combo - create new instance
            state.currentAttack = createLightSlash1();
            AnimationUtils.playAnimation(player, "light_slash_1");
            state.comboCount = 1;
        }

        // Start the attack
        state.currentAttack.start(player);

        // Update timing
        state.lastAttackTime = currentTime;
        state.lastAttackInitiated = currentTime;

        System.out.println("DEBUG: Started attack " + state.comboCount + " of combo");
    }

    /**
     * Called every tick to update active attacks
     */
    public void tick(Player player) {
        PlayerAttackState state = playerStates.get(player.getUUID());
        if (state == null) return;

        // Update active attack
        if (state.currentAttack != null && state.currentAttack.isActive()) {
            state.currentAttack.tick(player);
        }

        // Reset combo if window expired
        long currentTime = player.level().getGameTime();
        if (currentTime - state.lastAttackTime > COMBO_WINDOW) {
            if (state.comboCount > 0) {
                System.out.println("DEBUG: Combo window expired, resetting");
                state.comboCount = 0;
            }
        }

        // Clean up old states periodically
        if (currentTime % 100 == 0) {
            cleanupOldStates();
        }
    }

    /**
     * Gets or creates player state
     */
    private PlayerAttackState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerAttackState());
    }

    /**
     * Cleans up states for players who are no longer online
     */
    private void cleanupOldStates() {
        playerStates.entrySet().removeIf(entry -> {
            // In a real implementation, check if player is still online
            // For now, just remove if attack is not active
            PlayerAttackState state = entry.getValue();
            return state.currentAttack == null || !state.currentAttack.isActive();
        });
    }

    /**
     * Per-player attack state
     */
    private static class PlayerAttackState {
        long lastAttackTime = 0;
        int comboCount = 0;
        long lastAttackInitiated = 0;
        SimpleSlashAttack currentAttack = null;
    }
}