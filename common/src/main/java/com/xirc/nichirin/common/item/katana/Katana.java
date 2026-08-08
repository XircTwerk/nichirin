package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.attack.moveset.DualKatanaMoveset;
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
public class Katana extends Item {

    public Katana(Properties properties) {
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
        DualKatanaMoveset.tick(player);
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
        // Otherwise fall through to the equipment-appropriate neutral slash combo.
        if (moveset != null && moveset.handleLeftClick(player)) return;
        DualKatanaMoveset.neutralMovesetFor(player).handleLeftClick(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Right-click is the BLOCK button now (handled client-side in BlockingInputHandler). The
        // right-click SPECIAL moved to block + left-click, which routes through performSpecial(...)
        // via the katana input packet. The vanilla use() path must therefore do NOTHING, otherwise
        // a plain right-click would also fire the special on top of starting a block.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * SERVER ONLY: performs the right-click special move (the old right-click / crouch-right-click
     * behaviour). Invoked only from the katana input packet when the player does block + left click.
     */
    public void performSpecial(Player player, boolean isCrouching) {
        if (player.level().isClientSide) return;
        if (!canPerformAttack(player)) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (SheathingManager.isSelectedKatanaSheathed(player)) return;

        // If the breathing moveset claims the right-click (returns true) we're done.
        // Otherwise fall through to the equipment-appropriate neutral special.
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset != null && moveset.handleRightClick(player, isCrouching)) {
            return;
        }
        DualKatanaMoveset.neutralMovesetFor(player).handleRightClick(player, isCrouching);
    }

    // Wheel moves (0 = Check, 1 = Overhead, 2 = Thrust)
    // Called by MoveHotkeyPacket when the player has no breathing style.
    public void performWheelMove(Player player, int moveIndex) {
        if (player.level().isClientSide()) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (player.hasEffect(NichirinEffectRegistry.blocking())) return;
        if (SheathingManager.isSelectedKatanaSheathed(player)) return;

        DualKatanaMoveset.neutralMovesetFor(player).performMove(player, moveIndex);
    }

    /** CLIENT ONLY: Update the cooldown HUD and play the attack animation. */
    public void displayClientCooldown(Player player) {
        if (!player.level().isClientSide) return;
        if (MovesetHelper.getBreathingMoveset(player) != null) return;
        boolean dualWielding = DualKatanaMoveset.isDualWielding(player);
        int comboCount = dualWielding
                ? DualKatanaMoveset.getComboCount(player)
                : DefaultKatanaMoveset.getOrCreateState(player).comboCount;

        if (comboCount == 1) {
            CooldownHUD.setCooldown("Slash2", 0);
        } else {
            CooldownHUD.setCooldown("Slash1", 0);
        }
        // Animation is played via server broadcast — don't play locally to avoid stutter
    }

    /**
     * The right-click special's cooldown HUD is now sent authoritatively by the server (via
     * {@code MoveExecutor}/{@code ServerCooldownManager}) using the actual move it resolved. The old
     * client-side guess here duplicated the bar — and, on a crouch-state mismatch, showed a SECOND,
     * wrong move on cooldown — so it's intentionally a no-op.
     */
    public void displayClientRightClickFeedback(Player player, boolean isCrouching) {
    }

    /**
     * Returns true only if this katana instance is the one that should be handling attacks
     * (enforces main-hand priority when dual-wielding).
     */
    private boolean canPerformAttack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();

        boolean mainIsKatana = mainHand.getItem() instanceof Katana;
        boolean offIsKatana  = offHand.getItem()  instanceof Katana;

        if (mainIsKatana && offIsKatana)  return mainHand.getItem() == this;
        if (mainIsKatana)                 return mainHand.getItem() == this;
        if (offIsKatana)                  return offHand.getItem()  == this;
        return false;
    }

    private ItemStack getActiveHandItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();
        boolean mainIsKatana = mainHand.getItem() instanceof Katana;
        boolean offIsKatana  = offHand.getItem()  instanceof Katana;
        if (mainIsKatana && offIsKatana) return mainHand;
        if (mainIsKatana)                return mainHand;
        if (offIsKatana)                 return offHand;
        return mainHand;
    }
}
