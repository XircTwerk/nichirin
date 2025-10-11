package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moves.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simple katana using existing attack classes with combo integration
 */
public class SimpleKatana extends Item {

    private static final int COMBO_WINDOW = 20; // ticks to chain attacks
    private static final float LIGHT_ATTACK_STAMINA_COST = 5.0f;
    private static final float SPECIAL_ATTACK_STAMINA_COST = 15.0f;

    // Base stun durations for different attack types
    private static final int LIGHT_ATTACK_STUN = 5; // ticks
    private static final int COMBO_ATTACK_STUN = 5; // ticks
    private static final int DOUBLE_SLASH_STUN = 7; // ticks
    private static final int RISING_SLASH_STUN = 10; // ticks

    private final Map<UUID, PlayerAttackState> playerStates = new HashMap<>();

    public SimpleKatana(Properties properties) {
        super(properties);
    }

    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public void setDamage(ItemStack stack, int damage) {
        // Do nothing - prevent any damage
    }

    private SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(3, 7, 2)
                .withCooldown(0)
                .withDamage(4.0f)
                .withRange(2.5f)
                .withKnockback(0.3f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(5)
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
                .withHitStun(5)
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
                .withHitStun(7)
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
                .withHitStun(10)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (entity instanceof Player player && isSelected) {
            tick(player);
        }
    }

    /**
     * SERVER ONLY: Called by MultiplayerInputHandler after validation
     */
    public void performAttack(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!canPerformAttack(player)) {
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            return;
        }

        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
            return;
        }
        if (state.currentDoubleSlash != null && state.currentDoubleSlash.isActive()) {
            return;
        }
        if (state.currentRisingSlash != null && state.currentRisingSlash.isActive()) {
            return;
        }

        // Check if moveset wants to override left-click
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset != null && moveset.handleLeftClick(player)) {
            if (!canPerformAttack(player)) {
                return;
            }
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

        // Find targets in range before executing attack
        List<LivingEntity> targets = findTargetsInRange(player, 2.5f);

        // Execute combo attacks with proper combo integration
        if (isCombo && state.comboCount == 1) {
            state.currentSlash = createLightSlash2();
            state.currentSlash.start(player);
            state.comboCount = 2;
            state.slash2CooldownUntil = currentTime + state.currentSlash.getCooldown();
            NichirinAnimations.playAnimation(player, "sword.slash");

            // Apply combo tracking with damage to all hit targets
            for (LivingEntity target : targets) {
                ComboIntegration.handleKatanaHit(player, target, COMBO_ATTACK_STUN, 5.0f);
            }
        } else {
            state.currentSlash = createLightSlash1();
            state.currentSlash.start(player);
            state.comboCount = 1;
            state.slash1CooldownUntil = currentTime + state.currentSlash.getCooldown();
            NichirinAnimations.playAnimation(player, "sword.slash");

            // Apply combo tracking with damage to all hit targets
            for (LivingEntity target : targets) {
                ComboIntegration.handleKatanaHit(player, target, LIGHT_ATTACK_STUN, 4.0f);
            }
        }

        state.lastAttackTime = currentTime;
    }

    /**
     * SERVER ONLY: Called by MultiplayerInputHandler after validation
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (!canPerformAttack(player)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.BLOCKING.get())) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        PlayerAttackState state = getOrCreatePlayerState(player);

        // Check if any attack is currently active
        if (state.currentSlash != null && state.currentSlash.isActive()) {
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

        // Check if moveset wants to override right-click
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset != null && moveset.handleRightClick(player, isCrouching)) {
            if (!canPerformAttack(player)) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            return InteractionResultHolder.success(getActiveHandItem(player));
        }

        // Default special attacks
        if (!StaminaManager.hasStamina(player, SPECIAL_ATTACK_STAMINA_COST)) {
            player.displayClientMessage(Component.literal("Not enough stamina for special attack!").withStyle(style -> style.withColor(0xFF5555)), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (!StaminaManager.consume(player, SPECIAL_ATTACK_STAMINA_COST)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        // Find targets for special attacks
        List<LivingEntity> targets;

        if (isCrouching) {
            state.currentRisingSlash = createRisingSlashAttack();
            state.currentRisingSlash.start(player);
            state.risingSlashCooldownUntil = currentTime + state.currentRisingSlash.getCooldown();

            if (player instanceof ServerPlayer serverPlayer) {
                PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), "sword.vertical");
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            }
            // Rising slash targets
            targets = findTargetsInRange(player, 2.5f);
            for (LivingEntity target : targets) {
                ComboIntegration.handleKatanaHit(player, target, RISING_SLASH_STUN, 4.0f);
            }

        } else {
            state.currentDoubleSlash = createDoubleSlashAttack();
            state.currentDoubleSlash.start(player);
            state.doubleSlashCooldownUntil = currentTime + state.currentDoubleSlash.getCooldown();

            if (player instanceof ServerPlayer serverPlayer) {
                PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), "sword.doubleslash");
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            }

            // Double slash targets
            targets = findTargetsInRange(player, 2.8f);
            for (LivingEntity target : targets) {
                ComboIntegration.handleKatanaHit(player, target, DOUBLE_SLASH_STUN, 3.5f);
            }
        }

        state.comboCount = 0;
        state.lastAttackTime = 0;

        return InteractionResultHolder.success(getActiveHandItem(player));
    }

    /**
     * Find living entities in range of the player for attacks
     */
    private List<LivingEntity> findTargetsInRange(Player player, float range) {
        AABB searchBox = new AABB(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range
        );

        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        );
    }

    /**
     * Check if this katana instance can perform attacks based on hand priority rules
     */
    private boolean canPerformAttack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainHandKatana = mainHand.getItem() instanceof SimpleKatana;
        boolean offHandKatana = offHand.getItem() instanceof SimpleKatana;

        // Case 1: Dual wielding - only main hand katana can attack (main hand overrides)
        if (mainHandKatana && offHandKatana) {
            return mainHand.getItem() == this;
        }

        // Case 2: Only main hand has katana - can attack
        if (mainHandKatana && !offHandKatana) {
            return mainHand.getItem() == this;
        }

        // Case 3: Only offhand has katana - can attack
        if (!mainHandKatana && offHandKatana) {
            return offHand.getItem() == this;
        }

        // Case 4: No katanas - can't attack
        return false;
    }

    /**
     * Get the item from the currently active hand based on priority rules
     */
    private ItemStack getActiveHandItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainHandKatana = mainHand.getItem() instanceof SimpleKatana;
        boolean offHandKatana = offHand.getItem() instanceof SimpleKatana;

        // When dual wielding, active hand is main hand (main hand overrides)
        if (mainHandKatana && offHandKatana) {
            return mainHand;
        }

        // When only main hand, active hand is main hand
        if (mainHandKatana && !offHandKatana) {
            return mainHand;
        }

        // When only offhand, active hand is offhand
        if (!mainHandKatana && offHandKatana) {
            return offHand;
        }

        // Default fallback
        return mainHand;
    }

    /**
     * CLIENT ONLY: Display visual feedback
     */
    public void displayClientCooldown(Player player) {
        if (!player.level().isClientSide) return;

        PlayerAttackState state = getOrCreatePlayerState(player);
        long currentTime = player.level().getGameTime();
        boolean isCombo = (currentTime - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

        if (isCombo && state.comboCount == 1) {
            CooldownHUD.setCooldown("Slash2", 0);
            NichirinAnimations.playAnimation(player, "sword.slash");
        } else {
            CooldownHUD.setCooldown("Slash1", 0);
            NichirinAnimations.playAnimation(player, "sword.slash");
        }
    }

    public void displayClientRightClickFeedback(Player player, boolean isCrouching) {
        if (!player.level().isClientSide) return;

        // Check if player has a breathing style that will handle this
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset != null) {
            // Don't display default cooldowns - let the breathing system handle it
            return;
        }

        // ALWAYS play animations regardless of breathing style
        if (isCrouching) {
            NichirinAnimations.playAnimation(player, "sword.vertical");
            CooldownHUD.setCooldown("Rising Slash", 25);
        } else {
            NichirinAnimations.playAnimation(player, "sword.doubleslash");
            CooldownHUD.setCooldown("Double Slash", 20);
        }
    }

    public void tick(Player player) {
        PlayerAttackState state = playerStates.get(player.getUUID());
        if (state == null) return;

        if (state.currentSlash != null && state.currentSlash.isActive()) {
            state.currentSlash.tick(player);
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
            boolean doubleSlashInactive = state.currentDoubleSlash == null || !state.currentDoubleSlash.isActive();
            boolean risingSlashInactive = state.currentRisingSlash == null || !state.currentRisingSlash.isActive();
            return slashInactive && doubleSlashInactive && risingSlashInactive && state.comboCount == 0;
        });
    }

    public static class PlayerAttackState {
        long lastAttackTime = 0;
        int comboCount = 0;
        SimpleSlashAttack currentSlash = null;
        DoubleSlashAttack currentDoubleSlash = null;
        RisingSlashAttack currentRisingSlash = null;

        long slash1CooldownUntil = 0;
        long slash2CooldownUntil = 0;
        long doubleSlashCooldownUntil = 0;
        long risingSlashCooldownUntil = 0;
    }
}