package com.xirc.nichirin.common.item.gun;

import com.xirc.nichirin.common.attack.moveset.DefaultGunMoveset;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.system.GrabManager;
import com.xirc.nichirin.common.util.ItemStackData;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Genya's double-barrel gun.
 *
 * <p>Unlike katanas this weapon cannot block — right-click fires directly through {@link #use}
 * instead of starting a guard, so the attack inputs work without holding block. All combat logic
 * lives in {@link DefaultGunMoveset}; this class is a routing/state layer.</p>
 *
 * <ul>
 *   <li>Left-click — single shot (routed via the gun input packet)</li>
 *   <li>Right-click — double shot</li>
 *   <li>Crouch + Right-click — reload</li>
 *   <li>Wheel/hotkey 0 — Gun Bash, 1 — Grab</li>
 * </ul>
 */
public class GenyaDB extends Item {

    public static final int MAX_AMMO = 2;
    private static final String AMMO_KEY = "genya_ammo";
    /** Per-stack render flag: true while a demon holds the gun (shows the flesh bone). */
    public static final String DEMON_RENDER_KEY = "genya_demon_render";

    public GenyaDB(Properties properties) {
        super(properties);
    }

    public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }

    /** Bullets currently in the chamber (a fresh gun starts loaded). */
    public static int getAmmo(ItemStack stack) {
        return ItemStackData.has(stack, AMMO_KEY) ? ItemStackData.get(stack).getInt(AMMO_KEY) : MAX_AMMO;
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        int clamped = Math.max(0, Math.min(MAX_AMMO, ammo));
        ItemStackData.update(stack, tag -> tag.putInt(AMMO_KEY, clamped));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        // Drive the active grab (pin + stun + auto-release/launch) while the gun is held.
        if (isSelected) {
            if (GrabManager.isGrabbing(player)) {
                GrabManager.tick(player);
            }
            // Complete a pending reload once its timer elapses (bullets are consumed here).
            DefaultGunMoveset.tickReload(player, stack);
            // Keep the demon render flag in sync so the flesh bone shows only for demons.
            boolean demon = DemonManager.isDemon(player);
            if (demon != ItemStackData.get(stack).getBoolean(DEMON_RENDER_KEY)) {
                ItemStackData.update(stack, tag -> tag.putBoolean(DEMON_RENDER_KEY, demon));
            }
        } else {
            // Swapped off the gun — a reload in progress is canceled, and any grab is dropped.
            DefaultGunMoveset.cancelReload(player);
            if (GrabManager.isGrabbing(player)) {
                GrabManager.releaseGrab(player, false);
            }
        }
    }

    /**
     * Right-click reloads the chamber (no blocking, no crouch needed).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            DefaultGunMoveset.INSTANCE.reloadGun(player);
        }
        return InteractionResultHolder.success(stack);
    }

    /** SERVER ONLY: fire {@code barrels} barrels (tap = 1, hold = 2), invoked by the gun input packet. */
    public void performShoot(Player player, int barrels) {
        if (player.level().isClientSide) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        DefaultGunMoveset.INSTANCE.fire(player, barrels);
    }

    /** SERVER ONLY: wheel / hotkey moves (0 = Gun Bash, 1 = Grab). */
    public void performWheelMove(Player player, int moveIndex) {
        if (player.level().isClientSide) return;
        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        DefaultGunMoveset.INSTANCE.performMove(player, moveIndex);
    }
}
