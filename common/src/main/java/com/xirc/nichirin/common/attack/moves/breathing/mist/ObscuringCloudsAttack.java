package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Seventh Form: Obscuring Clouds — Muichiro Tokito's personal creation.
 * The user becomes invisible and pulses a large hitbox around themselves every few ticks,
 * hitting all enemies in range repeatedly for 5 seconds. Water particles trail from the
 * player throughout, and the big hitbox punishes anyone who stays close.
 */
public class ObscuringCloudsAttack extends MistBreathingAttackBase {

    private static final int HIT_INTERVAL = 6; // pulse every 6 ticks

    @Override
    protected void onStart() {
        // Apply invisibility for the full duration
        user.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY, windup + duration + 10, 0, false, false, false));

        // Startup: dissolve into mist
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    40, 1.2, 1.0, 1.2, 0.04);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z,
                    30, 1.0, 0.8, 1.0, 0.03);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Water particle trail every tick — marks the invisible player's position subtly
        createWaterTrailParticles(userPos);

        // Ambient mist cloud while invisible
        if (tickCount % 3 == 0) {
            emitAmbientMist(userPos);
        }

        // Pulse a large hitbox on the player every HIT_INTERVAL ticks
        if ((tickCount - windup) % HIT_INTERVAL == 0) {
            pulseHitbox(userPos);
        }
    }

    private void pulseHitbox(Vec3 userPos) {
        // Large hitbox centered on the player — range is the 8-block radius from moveset
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                userPos, range * 2.0, user.getBbHeight() + 1.0, range * 2.0);

        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target); // pulses repeatedly, so bypass immunity frames
            createWaterTrailParticles(target.position());
        }

        // Pulse visual — brief expanding mist ring from player
        createMistCircle(userPos, range * 0.6f, 14);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    private void emitAmbientMist(Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        var random = serverLevel.getRandom();
        for (int i = 0; i < 4; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 3.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 3.0;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    userPos.x + offsetX, userPos.y + offsetY, userPos.z + offsetZ,
                    1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    @Override
    protected void onStop() {
        // Reappear from mist
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    50, 1.5, 1.2, 1.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z,
                    30, 1.0, 0.8, 1.0, 0.05);
        }

        user.removeEffect(MobEffects.INVISIBILITY);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
