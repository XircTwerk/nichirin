package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Katana item.
 *
 * <p>All attack logic lives in {@link DefaultKatanaMoveset} (default) or in a
 * breathing-style {@link AbstractMoveset} (if the player has one assigned).
 * This class is purely a routing/delegation layer.</p>
 */
public class SimpleKatana extends Item {

    public SimpleKatana(Properties properties) {
        super(properties);
    }

    public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    public void setDamage(ItemStack stack, int damage) { /* immutable */ }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player && isSelected) {
            tick(player);
        }
    }

    public void tick(Player player) {
        // Always tick so active default-katana attacks finish their lifecycle,
        // even when a breathing style is equipped (M1 falls back to the slash combo).
        DefaultKatanaMoveset.tick(player);
    }

    /**
     * SERVER ONLY: Called by MultiplayerInputHandler after network validation.
     * Routes to breathing moveset left-click if available, otherwise falls back to
     * the default slash combo from {@link DefaultKatanaMoveset}.
     */
    public void performAttack(Player player) {
        if (player.level().isClientSide) return;
        if (player.isSpectator()) return;
        if (!canPerformAttack(player)) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (player.hasEffect(NichirinEffectRegistry.blocking())) return;
        if (SheathingManager.isSelectedKatanaSheathed(player)) return;

        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        // If the breathing moveset claims the hit (returns true) we're done.
        // Otherwise fall through to the default slash combo.
        if (moveset != null && moveset.handleLeftClick(player)) return;
        DefaultKatanaMoveset.INSTANCE.handleLeftClick(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!canPerformAttack(player)) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player.hasEffect(NichirinEffectRegistry.stunned()))
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player.hasEffect(NichirinEffectRegistry.blocking()))
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (SheathingManager.isSelectedKatanaSheathed(player))
            return InteractionResultHolder.pass(player.getItemInHand(hand));

        boolean isCrouching = player.isShiftKeyDown() || player.isCrouching();

        // If the breathing moveset claims the right-click (returns true) we're done.
        // Otherwise fall through to the default double-slash / rising-slash.
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset != null && moveset.handleRightClick(player, isCrouching)) {
            return InteractionResultHolder.consume(getActiveHandItem(player));
        }

        DefaultKatanaMoveset.INSTANCE.handleRightClick(player, isCrouching);
        return InteractionResultHolder.consume(getActiveHandItem(player));
    }

    // Wheel moves (0 = Check, 1 = Overhead, 2 = Thrust)
    // Called by MoveHotkeyPacket when the player has no breathing style.
    public void performWheelMove(Player player, int moveIndex) {
        if (player.level().isClientSide()) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (player.hasEffect(NichirinEffectRegistry.blocking())) return;
        if (SheathingManager.isSelectedKatanaSheathed(player)) return;

        DefaultKatanaMoveset.INSTANCE.performMove(player, moveIndex);
    }

    /** CLIENT ONLY: Update the cooldown HUD and play the attack animation. */
    public void displayClientCooldown(Player player) {
        if (!player.level().isClientSide) return;
        DefaultKatanaMoveset.KatanaState state = DefaultKatanaMoveset.getOrCreateState(player);
        long now = player.level().getGameTime();
        boolean isCombo = (now - state.lastAttackTime) <= 20 && state.comboCount > 0;

        if (isCombo && state.comboCount == 1) {
            CooldownHUD.setCooldown("Slash2", 0);
        } else {
            CooldownHUD.setCooldown("Slash1", 0);
        }
        // Animation is played via server broadcast — don't play locally to avoid stutter
    }

    /** CLIENT ONLY: Update the cooldown HUD for right-click attacks. */
    public void displayClientRightClickFeedback(Player player, boolean isCrouching) {
        if (!player.level().isClientSide) return;
        // Breathing movesets handle their own HUD
        if (MovesetHelper.getBreathingMoveset(player) != null) return;

        if (isCrouching) {
            CooldownHUD.setCooldown("Rising Slash", 25);
        } else {
            CooldownHUD.setCooldown("Double Slash", 20);
        }
    }

    /**
     * Returns true only if this katana instance is the one that should be handling attacks
     * (enforces main-hand priority when dual-wielding).
     */
    private boolean canPerformAttack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();

        boolean mainIsKatana = mainHand.getItem() instanceof SimpleKatana;
        boolean offIsKatana  = offHand.getItem()  instanceof SimpleKatana;

        if (mainIsKatana && offIsKatana)  return mainHand.getItem() == this;
        if (mainIsKatana)                 return mainHand.getItem() == this;
        if (offIsKatana)                  return offHand.getItem()  == this;
        return false;
    }

    private ItemStack getActiveHandItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();
        boolean mainIsKatana = mainHand.getItem() instanceof SimpleKatana;
        boolean offIsKatana  = offHand.getItem()  instanceof SimpleKatana;
        if (mainIsKatana && offIsKatana) return mainHand;
        if (mainIsKatana)                return mainHand;
        if (offIsKatana)                 return offHand;
        return mainHand;
    }
}