package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.util.AnimationUtils;
import com.xirc.nichirin.common.util.StaminaManager;
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
 * THE katana - handles both basic attacks and breathing styles
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

    // Create attack methods
    private SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(3, 7, 2)
                .withCooldown(0)
                .withDamage(4.0f)
                .withRange(2.5f)
                .withKnockback(0.3f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(15)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private SimpleSlashAttack createLightSlash2() {
        return new SimpleSlashAttack.Builder()
                .withTiming(2, 10, 3)
                .withCooldown(0)
                .withDamage(5.0f)
                .withRange(2.5f)
                .withKnockback(0.5f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private DoubleSlashAttack createDoubleSlashAttack() {
        return new DoubleSlashAttack.Builder()
                .withTiming(4, 16, 6)
                .withCooldown(20)
                .withDamage(3.5f)
                .withRange(2.8f)
                .withKnockback(0.4f)
                .withHitbox(1.6f, new Vec3(0, 0, 1.0))
                .withHitStun(12)
                .withSlashDelay(2)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private RisingSlashAttack createRisingSlashAttack() {
        return new RisingSlashAttack.Builder()
                .withTiming(5, 10, 8)
                .withCooldown(25)
                .withDamage(4.0f)
                .withRange(2.5f)
                .withLaunchPower(1.5f)
                .withKnockback(0.2f)
                .withHitbox(1.5f, new Vec3(0, 0.5, 1.0))
                .withHitStun(20)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (entity instanceof Player player && isSelected) {
            tick(player);

            // Also tick any active moveset attacks
            MoveExecutor.tickAttacks(player);
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
            // Check if moveset wants to override left-click
            AbstractMoveset moveset = BreathingStyleHelper.getMoveset(player);
            if (moveset != null && moveset.handleLeftClick(player)) {
                return; // Moveset handled it
            }

            // Default left-click behavior
            if (!StaminaManager.hasStamina(player, LIGHT_ATTACK_STAMINA_COST)) {
                player.displayClientMessage(Component.literal("Not enough stamina!").withStyle(style -> style.withColor(0xFF5555)), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
                return;
            }

            long currentTime = player.level().getGameTime();
            boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

            if (!isCombo && currentTime < state.slash1CooldownUntil) {
                return;
            }
            if (isCombo && currentTime < state.slash2CooldownUntil) {
                return;
            }

            if (!StaminaManager.consume(player, LIGHT_ATTACK_STAMINA_COST)) {
                return;
            }

            // Execute default combo attacks
            if (isCombo && state.comboCount == 1) {
                state.currentSlash = createLightSlash2();
                state.currentSlash.start(player);
                state.comboCount = 2;
                state.slash2CooldownUntil = currentTime + state.currentSlash.getCooldown();
                AnimationUtils.playAnimation(player, "light_slash2");
            } else {
                state.currentSlash = createLightSlash1();
                state.currentSlash.start(player);
                state.comboCount = 1;
                state.slash1CooldownUntil = currentTime + state.currentSlash.getCooldown();
                AnimationUtils.playAnimation(player, "light_slash1");
            }

            state.lastAttackTime = currentTime;
        }
    }

    /**
     * Handle right-click (M2) - special moves or default attacks
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

        boolean isCrouching = player.isCrouching();
        long currentTime = level.getGameTime();

        if (isCrouching && currentTime < state.risingSlashCooldownUntil) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (!isCrouching && currentTime < state.doubleSlashCooldownUntil) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        AbstractMoveset moveset = null;
        boolean hasBreathingStyle = false;

        if (!level.isClientSide) {
            // Check if moveset wants to override right-click
            moveset = BreathingStyleHelper.getMoveset(player);
            hasBreathingStyle = (moveset != null);

            if (moveset != null && moveset.handleRightClick(player, isCrouching)) {
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            // No moveset or didn't override - use default special attacks
            if (!StaminaManager.hasStamina(player, SPECIAL_ATTACK_STAMINA_COST)) {
                player.displayClientMessage(Component.literal("Not enough stamina for special attack!").withStyle(style -> style.withColor(0xFF5555)), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            if (!StaminaManager.consume(player, SPECIAL_ATTACK_STAMINA_COST)) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            if (isCrouching) {
                state.currentRisingSlash = createRisingSlashAttack();
                state.currentRisingSlash.start(player);
                state.risingSlashCooldownUntil = currentTime + state.currentRisingSlash.getCooldown();
            } else {
                state.currentDoubleSlash = createDoubleSlashAttack();
                state.currentDoubleSlash.start(player);
                state.doubleSlashCooldownUntil = currentTime + state.currentDoubleSlash.getCooldown();
            }

            state.comboCount = 0;
            state.lastAttackTime = 0;
        }

        // Client-side effects for default attacks - ONLY if no breathing style
        if (level.isClientSide) {
            // Check breathing style on client side too
            moveset = BreathingStyleHelper.getMoveset(player);
            hasBreathingStyle = (moveset != null);

            // Only show default cooldowns if NO breathing style
            if (!hasBreathingStyle) {
                if (!StaminaManager.hasStamina(player, SPECIAL_ATTACK_STAMINA_COST)) {
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                }

                if (isCrouching) {
                    AnimationUtils.playAnimation(player, "rising_slash");
                    CooldownHUD.setCooldown("Rising Slash", 25);
                } else {
                    AnimationUtils.playAnimation(player, "double_slash");
                    CooldownHUD.setCooldown("Double Slash", 20);
                }
            }
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public void displayClientRightClickFeedback(Player player, boolean isCrouching) {
        if (!player.level().isClientSide) return;

        // Check if player has a breathing style that will handle this
        AbstractMoveset moveset = BreathingStyleHelper.getMoveset(player);
        if (moveset != null) {
            // Don't display default cooldowns - let the breathing system handle it
            return;
        }

        // Only show default cooldowns if no breathing style
        if (isCrouching) {
            AnimationUtils.playAnimation(player, "rising_slash");
            CooldownHUD.setCooldown("Rising Slash", 25);
        } else {
            AnimationUtils.playAnimation(player, "double_slash");
            CooldownHUD.setCooldown("Double Slash", 20);
        }
    }

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

    public void tick(Player player) {
        PlayerAttackState state = playerStates.get(player.getUUID());
        if (state == null) return;

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

        long currentTime = player.level().getGameTime();
        if (currentTime - state.lastAttackTime > COMBO_WINDOW) {
            if (state.comboCount > 0) {
                state.comboCount = 0;
            }
        }

        if (currentTime % 100 == 0) {
            cleanupOldStates();
        }
    }

    public PlayerAttackState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerAttackState());
    }

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

    public static class PlayerAttackState {
        long lastAttackTime = 0;
        int comboCount = 0;
        SimpleSlashAttack currentSlash = null;
        SimpleSliceAttack currentSlice = null;
        DoubleSlashAttack currentDoubleSlash = null;
        RisingSlashAttack currentRisingSlash = null;

        long slash1CooldownUntil = 0;
        long slash2CooldownUntil = 0;
        long doubleSlashCooldownUntil = 0;
        long risingSlashCooldownUntil = 0;

        String lastAnimationName = null;
        String lastCooldownName = null;
        int lastCooldownDuration = 0;
    }
}