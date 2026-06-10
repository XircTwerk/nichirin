package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hook called by {@link com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack} on every
 * successful CQC hit. When the attacker has Destructive Death equipped as their BDA and the
 * Shockwave Toggle is on, spawn a small forward shockwave from the impact point. Overdrive flips
 * the tint to red.
 */
public final class DestructiveDeathCqcHook {

    private DestructiveDeathCqcHook() {}

    public static void onCqcHit(LivingEntity attacker, LivingEntity target, Level world) {
        if (world.isClientSide) return;
        if (!(attacker instanceof ServerPlayer sp)) return;
        if (!"destructive_death".equals(MovesetHelper.getDemonMovesetId(sp))) return;
        if (!DestructiveDeathState.isShockwaveEnabled(sp.getUUID())) return;

        Vec3 origin = attacker.position()
                .add(0, attacker.getBbHeight() * 0.55, 0)
                .add(attacker.getLookAngle().scale(0.5));
        Vec3 direction = attacker.getLookAngle().normalize();
        new ShockwaveEntity.Builder()
                .owner(attacker)
                .origin(origin)
                .direction(direction)
                .damage(2.0f)            // chip damage on top of the CQC strike itself
                .speed(0.65f)
                .lifeTicks(18)
                .hitboxRadius(0.95f)
                .pierces(0)
                .red(DestructiveDeathState.isOverdriveEnabled(sp.getUUID()))
                .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
    }
}
