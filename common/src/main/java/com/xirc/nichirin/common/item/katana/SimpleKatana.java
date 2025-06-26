package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moves.SimpleSlashAttack;
import com.xirc.nichirin.common.attack.moves.SimpleSliceAttack;
import com.xirc.nichirin.common.util.AnimationUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
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

    // Per-player state tracking
    private final Map<UUID, PlayerAttackState> playerStates = new HashMap<>();

    public SimpleKatana(Properties properties) {
        // Use Iron tier as base, 6 attack damage (3 + 3 from iron tier), -2.4 attack speed
        super(Tiers.IRON, 3, -2.4f, properties);
        System.out.println("DEBUG: SimpleKatana created");
    }

    /**
     * Creates the first light slash attack (M1)
     */
    private SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(3, 13, 4)  // startup, active, recovery
                .withCooldown(10)      // 10 tick cooldown
                .withDamage(4.0f)
                .withRange(2.5f)
                .withKnockback(0.3f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(15)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Creates the second light slash attack for combos (M1 followup)
     */
    private SimpleSlashAttack createLightSlash2() {
        return new SimpleSlashAttack.Builder()
                .withTiming(2, 14, 4)  // Faster startup
                .withCooldown(15)      // Slightly longer cooldown
                .withDamage(5.0f)      // Slightly more damage
                .withRange(2.5f)
                .withKnockback(0.5f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Creates the slice attack for right click (M2)
     */
    private SimpleSliceAttack createSliceAttack() {
        return new SimpleSliceAttack.Builder()
                .withTiming(5, 15, 6)  // Slower but more powerful
                .withCooldown(20)      // Longer cooldown
                .withDamage(7.0f)      // Higher damage
                .withRange(3.0f)       // Slightly longer range
                .withKnockback(0.8f)
                .withHitbox(2.0f, new Vec3(0, 0, 1.0))
                .withHitStun(25)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }

    /**
     * Called when the player left-clicks with this item (M1)
     */
    public void performAttack(Player player) {
        System.out.println("DEBUG: performAttack (M1) called for player: " + player.getName().getString());

        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            System.out.println("DEBUG: Attack already active");
            return;
        }
        if (state.currentSlice != null && state.currentSlice.isActive()) {
            System.out.println("DEBUG: Attack already active");
            return;
        }

        long currentTime = player.level().getGameTime();

        // Determine which slash to use based on combo
        boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

        // Check cooldowns
        if (!isCombo && CooldownHUD.isOnCooldown("Slash1")) {
            System.out.println("DEBUG: Slash1 on cooldown");
            return;
        }
        if (isCombo && CooldownHUD.isOnCooldown("Slash2")) {
            System.out.println("DEBUG: Slash2 on cooldown");
            return;
        }

        // Select and start appropriate slash attack
        if (isCombo && state.comboCount == 1) {
            // Second slash in combo - create new instance
            state.currentSlash = createLightSlash2();
            AnimationUtils.playAnimation(player, "light_slash_2");
            state.currentSlash.start(player);
            state.comboCount = 2;

            // Set cooldown on client
            if (player.level().isClientSide()) {
                CooldownHUD.setCooldown("Slash2", state.currentSlash.getCooldown());
            }
        } else {
            // First slash or reset combo - create new instance
            state.currentSlash = createLightSlash1();
            AnimationUtils.playAnimation(player, "light_slash_1");
            state.currentSlash.start(player);
            state.comboCount = 1;

            // Set cooldown on client
            if (player.level().isClientSide()) {
                CooldownHUD.setCooldown("Slash1", state.currentSlash.getCooldown());
            }
        }

        // Update timing
        state.lastAttackTime = currentTime;

        System.out.println("DEBUG: Started slash " + state.comboCount + " of combo");
    }

    /**
     * Handle right-click (M2) - slice attack
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        System.out.println("DEBUG: use (M2) called for player: " + player.getName().getString());

        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            System.out.println("DEBUG: Slash attack already active");
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (state.currentSlice != null && state.currentSlice.isActive()) {
            System.out.println("DEBUG: Slice attack already active");
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        // Check cooldown
        if (CooldownHUD.isOnCooldown("Slice")) {
            System.out.println("DEBUG: Slice on cooldown");
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        // Start slice attack
        state.currentSlice = createSliceAttack();
        AnimationUtils.playAnimation(player, "heavy_slice"); // Different animation for slice
        state.currentSlice.start(player);

        // Set cooldown on client
        if (player.level().isClientSide()) {
            CooldownHUD.setCooldown("Slice", state.currentSlice.getCooldown());
        }

        // Reset combo when using slice
        state.comboCount = 0;
        state.lastAttackTime = 0;

        System.out.println("DEBUG: Started slice attack");

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /**
     * Called every tick to update active attacks
     */
    public void tick(Player player) {
        PlayerAttackState state = playerStates.get(player.getUUID());
        if (state == null) return;

        // Update active attacks
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            state.currentSlash.tick(player);
        }
        if (state.currentSlice != null && state.currentSlice.isActive()) {
            state.currentSlice.tick(player);
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
            PlayerAttackState state = entry.getValue();
            boolean slashInactive = state.currentSlash == null || !state.currentSlash.isActive();
            boolean sliceInactive = state.currentSlice == null || !state.currentSlice.isActive();
            return slashInactive && sliceInactive && state.comboCount == 0;
        });
    }

    /**
     * Per-player attack state
     */
    private static class PlayerAttackState {
        long lastAttackTime = 0;
        int comboCount = 0;
        SimpleSlashAttack currentSlash = null;
        SimpleSliceAttack currentSlice = null;
    }
}