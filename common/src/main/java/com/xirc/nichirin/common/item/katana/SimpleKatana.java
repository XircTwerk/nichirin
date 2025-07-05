package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moves.SimpleSlashAttack;
import com.xirc.nichirin.common.attack.moves.SimpleSliceAttack;
import com.xirc.nichirin.common.attack.moves.DoubleSlashAttack;
import com.xirc.nichirin.common.attack.moves.RisingSlashAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.util.AnimationUtils;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A simple katana with basic light attacks that consume stamina
 */
public class SimpleKatana extends SwordItem {

    // Track combo state per player
    private static final int COMBO_WINDOW = 20; // ticks to chain attacks

    // Stamina costs
    private static final float LIGHT_ATTACK_STAMINA_COST = 5.0f;
    private static final float SPECIAL_ATTACK_STAMINA_COST = 15.0f;

    // Per-player state tracking
    private final Map<UUID, PlayerAttackState> playerStates = new HashMap<>();

    public SimpleKatana(Properties properties) {
        // Use Iron tier as base, 6 attack damage (3 + 3 from iron tier), -2.4 attack speed
        super(Tiers.IRON, 3, -2.4f, properties);
    }

    /**
     * Makes the katana unbreakable - it will never take damage
     */
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /**
     * Prevents the durability bar from being displayed
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    /**
     * Additional safety - prevents any damage from being applied
     */

    public void setDamage(ItemStack stack, int damage) {
        // Do nothing - prevent any damage from being set
    }

    /**
     * Creates the first light slash attack (M1)
     */
    private SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(3, 7, 2)   // startup, active, recovery (total: 12 ticks)
                .withCooldown(0)       // No cooldown
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
                .withTiming(2, 10, 3)  // Faster startup, shorter overall
                .withCooldown(0)       // No cooldown
                .withDamage(5.0f)      // Slightly more damage
                .withRange(2.5f)
                .withKnockback(0.5f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Creates the double slash X attack for right click
     */
    private DoubleSlashAttack createDoubleSlashAttack() {
        return new DoubleSlashAttack.Builder()
                .withTiming(4, 16, 6)  // startup, active (for both slashes), recovery
                .withCooldown(20)
                .withDamage(3.5f)      // Per hit
                .withRange(2.8f)
                .withKnockback(0.4f)
                .withHitbox(1.6f, new Vec3(0, 0, 1.0))
                .withHitStun(12)
                .withSlashDelay(2)     // 2 tick delay between visual slashes
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    /**
     * Creates the rising slash attack for crouch + right click
     */
    private RisingSlashAttack createRisingSlashAttack() {
        return new RisingSlashAttack.Builder()
                .withTiming(5, 10, 8)
                .withCooldown(25)
                .withDamage(4.0f)      // Same as basic slash
                .withRange(2.5f)
                .withLaunchPower(1.5f) // Launch enemies 1.5 blocks up
                .withKnockback(0.2f)
                .withHitbox(1.5f, new Vec3(0, 0.5, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }

    /**
     * Called every tick while the item is in a player's inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Only tick for players holding the katana
        if (entity instanceof Player player && isSelected) {
            tick(player);
        }
    }

    /**
     * Called from client to display feedback for right click attacks
     */
    public void displayClientRightClickFeedback(Player player, boolean isCrouching) {
        if (!player.level().isClientSide) return;

        if (isCrouching) {
            AnimationUtils.playAnimation(player, "rising_slash");
            CooldownHUD.setCooldown("Rising Slash", 25);
        } else {
            AnimationUtils.playAnimation(player, "double_slash");
            CooldownHUD.setCooldown("Double Slash", 20);
        }
    }

    /**
     * Called from client to display cooldown when attacking
     */
    public void displayClientCooldown(Player player) {
        if (!player.level().isClientSide) return;

        PlayerAttackState state = getOrCreatePlayerState(player);
        long currentTime = player.level().getGameTime();
        boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

        if (isCombo && state.comboCount == 1) {
            CooldownHUD.setCooldown("Slash2", 0);
            AnimationUtils.playAnimation(player, "light_slash2");
        } else {
            CooldownHUD.setCooldown("Slash1", 0);
            AnimationUtils.playAnimation(player, "light_slash1");
        }
    }

    /**
     * Called when the player left-clicks with this item (M1)
     */
    public void performAttack(Player player) {
        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            return;
        }
        if (state.currentSlice != null && state.currentSlice.isActive()) {
            return;
        }

        // Only process attack logic on server side
        if (!player.level().isClientSide) {
            // NEW: Check for moveset first
            AbstractMoveset moveset = BreathingStyleHelper.getMoveset(player);

            if (moveset != null && moveset.hasMove(MoveInputType.BASIC)) {
                // For now, just show that we detected the moveset
                player.displayClientMessage(
                        Component.literal("Using " + moveset.getDisplayName() + " (Coming Soon!)").withStyle(style -> style.withColor(0x55FFFF)),
                        true
                );
                // Continue with default attack for now
            }

            // Original attack logic
            // Check stamina FIRST before any other logic
            if (!StaminaManager.hasStamina(player, LIGHT_ATTACK_STAMINA_COST)) {
                // Send feedback to player about insufficient stamina
                player.displayClientMessage(Component.literal("Not enough stamina!").withStyle(style -> style.withColor(0xFF5555)), true);
                // Play sound effect for feedback
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
                return;
            }

            long currentTime = player.level().getGameTime();

            // Determine which slash to use based on combo
            boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

            // Check server-side cooldowns
            if (!isCombo && currentTime < state.slash1CooldownUntil) {
                return;
            }
            if (isCombo && currentTime < state.slash2CooldownUntil) {
                return;
            }

            // Consume stamina BEFORE starting the attack
            if (!StaminaManager.consume(player, LIGHT_ATTACK_STAMINA_COST)) {
                // This shouldn't happen since we checked above, but just in case
                return;
            }

            // Select and start appropriate slash attack
            if (isCombo && state.comboCount == 1) {
                // Second slash in combo
                state.currentSlash = createLightSlash2();
                state.currentSlash.start(player);
                state.comboCount = 2;
                state.slash2CooldownUntil = currentTime + state.currentSlash.getCooldown();

                // Play animation server-side (will sync to clients)
                AnimationUtils.playAnimation(player, "light_slash2");

                // Store for client sync
                state.lastAnimationName = "light_slash2";
                state.lastCooldownName = "Slash2";
                state.lastCooldownDuration = state.currentSlash.getCooldown();
            } else {
                // First slash or reset combo
                state.currentSlash = createLightSlash1();
                state.currentSlash.start(player);
                state.comboCount = 1;
                state.slash1CooldownUntil = currentTime + state.currentSlash.getCooldown();

                // Play animation server-side (will sync to clients)
                AnimationUtils.playAnimation(player, "light_slash1");

                // Store for client sync
                state.lastAnimationName = "light_slash1";
                state.lastCooldownName = "Slash1";
                state.lastCooldownDuration = state.currentSlash.getCooldown();
            }

            // Update timing
            state.lastAttackTime = currentTime;
        }
    }

    /**
     * Handle right-click (M2) - double slash or rising slash based on crouch
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (state.currentSlice != null && state.currentSlice.isActive()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (state.currentDoubleSlash != null && state.currentDoubleSlash.isActive()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (state.currentRisingSlash != null && state.currentRisingSlash.isActive()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        // Determine which attack based on crouch state
        boolean isCrouching = player.isCrouching();
        long currentTime = level.getGameTime();

        // Check cooldowns FIRST on both client and server to prevent any action
        if (isCrouching && currentTime < state.risingSlashCooldownUntil) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (!isCrouching && currentTime < state.doubleSlashCooldownUntil) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        // Only proceed with attack logic on server
        if (!level.isClientSide) {
            // NEW: Check for moveset
            AbstractMoveset moveset = BreathingStyleHelper.getMoveset(player);

            if (moveset != null) {
                MoveInputType inputType = isCrouching ? MoveInputType.SPECIAL2 : MoveInputType.SPECIAL1;
                if (moveset.hasMove(inputType)) {
                    player.displayClientMessage(
                            Component.literal("Using " + moveset.getDisplayName() + " - " + inputType.name() + " (Coming Soon!)").withStyle(style -> style.withColor(0x55FFFF)),
                            true
                    );
                }
            }

            // Original special attack logic
            // Check stamina BEFORE starting any attack
            if (!StaminaManager.hasStamina(player, SPECIAL_ATTACK_STAMINA_COST)) {
                // Send feedback to player about insufficient stamina
                player.displayClientMessage(Component.literal("Not enough stamina for special attack!").withStyle(style -> style.withColor(0xFF5555)), true);
                // Play sound effect for feedback
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            // Consume stamina BEFORE starting the attack
            if (!StaminaManager.consume(player, SPECIAL_ATTACK_STAMINA_COST)) {
                // This shouldn't happen since we checked above, but just in case
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            if (isCrouching) {
                // Crouch + Right Click = Rising Slash
                // Start rising slash attack on server
                state.currentRisingSlash = createRisingSlashAttack();
                state.currentRisingSlash.start(player);

                // Set server-side cooldown
                state.risingSlashCooldownUntil = currentTime + state.currentRisingSlash.getCooldown();
            } else {
                // Normal Right Click = Double Slash (X attack)
                // Start double slash attack on server
                state.currentDoubleSlash = createDoubleSlashAttack();
                state.currentDoubleSlash.start(player);

                // Set server-side cooldown
                state.doubleSlashCooldownUntil = currentTime + state.currentDoubleSlash.getCooldown();
            }

            // Reset combo when using special attacks
            state.comboCount = 0;
            state.lastAttackTime = 0;
        }

        // Handle client-side effects only if not on cooldown
        if (level.isClientSide) {
            // Check if player has enough stamina for visual feedback
            if (!StaminaManager.hasStamina(player, SPECIAL_ATTACK_STAMINA_COST)) {
                // Don't play animations if no stamina
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            if (isCrouching) {
                // Play animation on client for rising slash
                AnimationUtils.playAnimation(player, "rising_slash");

                // Set cooldown display on client
                CooldownHUD.setCooldown("Rising Slash", 25);
            } else {
                // Play animation on client for double slash
                AnimationUtils.playAnimation(player, "double_slash");

                // Set cooldown display on client
                CooldownHUD.setCooldown("Double Slash", 20);
            }
        }

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
        if (state.currentDoubleSlash != null && state.currentDoubleSlash.isActive()) {
            state.currentDoubleSlash.tick(player);
        }
        if (state.currentRisingSlash != null && state.currentRisingSlash.isActive()) {
            state.currentRisingSlash.tick(player);
        }

        // Reset combo if window expired
        long currentTime = player.level().getGameTime();
        if (currentTime - state.lastAttackTime > COMBO_WINDOW) {
            if (state.comboCount > 0) {
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
    public PlayerAttackState getOrCreatePlayerState(Player player) {
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
            boolean doubleSlashInactive = state.currentDoubleSlash == null || !state.currentDoubleSlash.isActive();
            boolean risingSlashInactive = state.currentRisingSlash == null || !state.currentRisingSlash.isActive();
            return slashInactive && sliceInactive && doubleSlashInactive && risingSlashInactive && state.comboCount == 0;
        });
    }

    /**
     * Per-player attack state
     */
    public static class PlayerAttackState {
        long lastAttackTime = 0;
        int comboCount = 0;
        SimpleSlashAttack currentSlash = null;
        SimpleSliceAttack currentSlice = null;  // Added missing field
        DoubleSlashAttack currentDoubleSlash = null;
        RisingSlashAttack currentRisingSlash = null;

        // Server-side cooldown tracking
        long slash1CooldownUntil = 0;
        long slash2CooldownUntil = 0;
        long doubleSlashCooldownUntil = 0;
        long risingSlashCooldownUntil = 0;

        // Client synchronization data
        String lastAnimationName = null;
        String lastCooldownName = null;
        int lastCooldownDuration = 0;
    }
}