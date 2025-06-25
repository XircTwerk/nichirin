package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThunderClapAttack extends AbstractBreathingAttack {

    private Vec3 dashStart;
    private Vec3 dashEnd;

    public ThunderClapAttack() {
        withTiming(1, 15, 20)
                .withDamage(12.0f)
                .withRange(10.0f)
                .withKnockback(0.2f)
                .withBreathCost(25.0f)
                .withParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.WAX_OFF, 30)
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.TRIDENT_THUNDER)
                .withSoundPitch(2.0f)
                .withUserEffect(MobEffects.MOVEMENT_SPEED, 100, 2)
                .withTargetEffect(MobEffects.MOVEMENT_SLOWDOWN, 40, 1) // Stun effect
                .setPiercing(true)
                .withForm(3, "Thunder Clap and Flash");
    }

    @Override
    public void onStart(Player user, Level world) {
        super.onStart(user, world);

        // Calculate dash trajectory
        Vec3 look = user.getLookAngle();
        dashStart = user.position();
        dashEnd = dashStart.add(look.scale(getRange()));
    }

    @Override
    protected void perform(Player user, Level world) {
        // Dash forward
        user.teleportTo(dashEnd.x, dashEnd.y, dashEnd.z);

        // Damage all entities in the path
        Vec3 step = dashEnd.subtract(dashStart).normalize();
        for (double d = 0; d < getRange(); d += 0.5) {
            Vec3 pos = dashStart.add(step.scale(d));

            // Check for entities at this position
            world.getEntitiesOfClass(LivingEntity.class,
                            user.getBoundingBox().move(pos.subtract(user.position())).inflate(1),
                            entity -> entity != user && entity.isAlive())
                    .forEach(target -> hitTarget(user, target));

            // Create lightning trail
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 1, pos.z, 0, 0, 0);
        }
    }

    @Override
    public int getWindupPoint() {
        return 0;
    }
}