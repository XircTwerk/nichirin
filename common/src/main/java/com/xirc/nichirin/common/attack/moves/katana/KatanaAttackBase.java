package com.xirc.nichirin.common.attack.moves.katana;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinDamageHandler;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.common.vfx.VfxManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared component-driven foundation for the single-katana neutral moveset. */
@SuppressWarnings("rawtypes")
public abstract class KatanaAttackBase
        extends AbstractBreathingAttack<KatanaAttackBase, IBreathingAttacker> {

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide || getHitEntities().contains(target.getUUID())) return;
        damageTarget(target, false);
        getHitEntities().add(target.getUUID());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        damageTarget(target, true);
    }

    private void damageTarget(LivingEntity target, boolean bypassImmunity) {
        if (bypassImmunity) {
            target.invulnerableTime = 0;
            target.hurtTime = 0;
        }
        boolean damaged = NichirinDamageHandler.hurt(target, NichirinDamageSources.blade(user), damage);
        if (damaged && user instanceof net.minecraft.world.entity.player.Player player) {
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage, comboSequence);
        }
        applyHitStun(target);
        applyKnockback(target);
        setHitCount(getHitCount() + 1);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    protected void playSwing(float pitch) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, pitch);
    }

    protected void playVfx(ResourceLocation id, float forwardOffset, float heightFactor, float scale) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 direction = user.getLookAngle();
        Vec3 origin = user.position().add(0.0, user.getBbHeight() * heightFactor, 0.0)
                .add(direction.scale(forwardOffset));
        VfxManager.playAttached(serverLevel, user, id, origin, direction, scale);
    }
}
