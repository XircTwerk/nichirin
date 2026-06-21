package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Leg Type — Flying Planet Thousand Wheels: upward sweep that launches target + user. Compass-
 * tracked targets get a stronger lift so the chain into BSCA reads cleanly.
 */
public class CqcFlyingPlanetThousandWheelsAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public CqcFlyingPlanetThousandWheelsAttack() {
        super("flying_planet_thousand_wheels");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 1, 0.0, 0.0);
        // Pure-vertical self-lift — zero out horizontal so the user follows the target straight up
        // without sliding sideways. Anyone holding W mid-swing won't drift.
        user.setDeltaMovement(0, 0.65, 0);
        user.hurtMarked = true;
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.1f);
        hitBurst(world, target, ParticleTypes.CRIT, 12, 0.25, 0.2, 0.25, 0.15);

        // Pure-vertical launch on the target — no horizontal carry. Compass-tracked targets get
        // a bigger lift so they can be chained into BSCA more cleanly.
        float lift = compassBuffsTarget(user, target) ? 1.2f : 0.8f;
        target.setDeltaMovement(0, lift, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 1, false, false, true));

        if (isShockwaveEnabled(user)) {
            // Vertical-knock shockwave so an entity passing through it also gets lifted, not
            // pushed sideways. Direction stays look-aligned so it travels forward visually.
            new ShockwaveEntity.Builder()
                    .owner(user)
                    .origin(user.position().add(0, user.getBbHeight() * 0.55, 0)
                            .add(user.getLookAngle().scale(0.6)))
                    .direction(user.getLookAngle().normalize())
                    .damage(damage * 0.4f)
                    .knockback(0.65f)
                    .noHorizontalPush(true)
                    .speed(1.6f)
                    .lifeTicks(16)
                    .hitboxRadius(1.0f)
                    .pierces(0)
                    .red(isOverdriveActive(user))
                    .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
        }
    }
}
