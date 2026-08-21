package com.xirc.nichirin.common.item.gyomei;

import net.minecraft.world.item.Item;

/**
 * Gyomei's Nichirin ball-and-chain (axe + flail). Simply holding it in the main hand activates the
 * physics simulation ({@code GyomeiWeaponManager}) — the rendered item is the axe (the held end), and
 * the chain + heavy flail trail from it entirely through physics. M1 / M2 / crouch-M2 are its attacks.
 */
public class GyomeiWeapon extends Item {
    public GyomeiWeapon(Properties properties) {
        super(properties);
    }
}
