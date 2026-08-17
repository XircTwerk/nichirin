package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hook called by {@link AbstractCqcAttack} on every
 * successful CQC hit. When the attacker has Destructive Death equipped as their BDA and the
 * Shockwave Toggle is on, spawn a small forward shockwave from the impact point. Overdrive flips
 * the tint to red.
 */
public final class DestructiveDeathCqcHook {

    private DestructiveDeathCqcHook() {}

    /** Player Overdrive berserker sustain: health leeched per CQC hit (1.0f = half a heart). */
    private static final float OVERDRIVE_LIFESTEAL = 1.0f;

    public static void onCqcHit(LivingEntity attacker, LivingEntity target, Level world) {
        if (world.isClientSide) return;

        boolean shockwaveEnabled;
        boolean overdrive;
        if (attacker instanceof IDestructiveDeathHost host) {
            // NPC host (Akaza) — state lives on the entity. Akaza's Overdrive is its own boss enrage
            // (regen + adaptation, handled on the entity), so it keeps the plain toggle-gated shockwave.
            shockwaveEnabled = host.ddShockwaveEnabled();
            overdrive = host.ddOverdriveActive();
        } else if (attacker instanceof ServerPlayer sp) {
            if (!"destructive_death".equals(MovesetHelper.getDemonMovesetId(sp))) return;
            overdrive = DestructiveDeathState.isOverdriveEnabled(sp.getUUID(), world.getGameTime());
            // Player Overdrive is a berserker state: shockwaves fire on EVERY CQC hit regardless of the
            // Shockwave Toggle, and each hit leeches a sliver of health to reward staying aggressive.
            shockwaveEnabled = overdrive || DestructiveDeathState.isShockwaveEnabled(sp.getUUID());
            if (overdrive && attacker.getHealth() < attacker.getMaxHealth()) {
                attacker.heal(OVERDRIVE_LIFESTEAL);
            }
        } else {
            return;
        }
        if (!shockwaveEnabled) return;

        Vec3 origin = attacker.position()
                .add(0, attacker.getBbHeight() * 0.55, 0)
                .add(attacker.getLookAngle().scale(0.5));
        Vec3 direction = attacker.getLookAngle().normalize();
        new ShockwaveEntity.Builder()
                .owner(attacker)
                .origin(origin)
                .direction(direction)
                // Overdrive shockwaves hit harder and punch through a couple of targets so they chain.
                .damage(overdrive ? 3.5f : 2.0f)
                .speed(overdrive ? 2.1f : 1.8f)
                .lifeTicks(18)
                .hitboxRadius(overdrive ? 1.1f : 0.95f)
                .pierces(overdrive ? 2 : 0)
                .destructiveDeathCqcDamage()
                .red(overdrive)
                .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
    }
}
