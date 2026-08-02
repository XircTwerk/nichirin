package com.xirc.nichirin.common.attack.moves.dual;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinDamageHandler;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Shared component-based foundation for neutral dual-katana attacks. */
@SuppressWarnings("rawtypes")
public abstract class DualKatanaAttackBase
        extends AbstractBreathingAttack<DualKatanaAttackBase, IBreathingAttacker> {

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide || getHitEntities().contains(target.getUUID())) return;

        boolean damaged = NichirinDamageHandler.hurt(target, NichirinDamageSources.blade(user), damage);
        if (damaged && user instanceof net.minecraft.world.entity.player.Player player) {
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        applyHitStun(target);
        applyKnockback(target);
        getHitEntities().add(target.getUUID());
        setHitCount(getHitCount() + 1);
        playHitSound(target, 1.0f);
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        target.invulnerableTime = 0;
        target.hurtTime = 0;
        boolean damaged = NichirinDamageHandler.hurt(target, NichirinDamageSources.blade(user), damage);
        if (damaged && user instanceof net.minecraft.world.entity.player.Player player) {
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        applyHitStun(target);
        applyKnockback(target);
        setHitCount(getHitCount() + 1);
        playHitSound(target, 1.0f);
    }

    protected void playSwingSound(float pitch) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, pitch);
    }

    protected void playHitSound(LivingEntity target, float pitch) {
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, pitch);
    }
}
