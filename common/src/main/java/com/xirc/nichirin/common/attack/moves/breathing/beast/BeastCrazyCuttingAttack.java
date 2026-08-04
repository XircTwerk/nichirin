package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Fifth Fang: Crazy Cutting. Levitates the user and slashes omnidirectionally every 3 ticks.
public class BeastCrazyCuttingAttack extends BeastBreathingAttackBase {

    private boolean levitationApplied = false;

    @Override
    protected void onStart() {
        levitationApplied = false;
        user.addEffect(new MobEffectInstance(MobEffects.LEVITATION, windup + duration + 5, 1, false, false, false));
        levitationApplied = true;
    }

    @Override
    protected void onActiveStart() {
        playBeastVfx(VfxIds.BEAST_CRAZY_CUTTING,
                user.position().add(0, user.getBbHeight() * 0.45, 0), user.getLookAngle(), range / 4.0f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);

        if (tickCount % 3 == 0) {
            List<LivingEntity> targets = getTargetsInCircle(range, 12);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.0f + (tickCount * 0.02f));
        }
    }

    @Override
    protected void onStop() {
        if (levitationApplied && user.hasEffect(MobEffects.LEVITATION)) {
            user.removeEffect(MobEffects.LEVITATION);
        }
        levitationApplied = false;
        user.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, false));
    }
}
