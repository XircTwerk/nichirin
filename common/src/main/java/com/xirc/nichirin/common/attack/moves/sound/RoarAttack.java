package com.xirc.nichirin.common.attack.moves.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * First Form: Roar
 * The user lifts their twin swords above their head and slams them down with great force,
 * causing a loud sound resembling thunder as a result.
 *
 * Mechanics:
 * - Short-range slam (4–5 block radius on impact)
 * - Creates an AOE explosion with knockback
 * - Brief stun (0.5s) on hit
 * - Moderate damage, fast startup
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RoarAttack extends SoundBreathingAttackBase {

    private boolean hasExecuted = false;
    private boolean swordsRaised = false;

    public RoarAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        swordsRaised = false;

        // Initial sword raising sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Create initial particle buildup
        createSoundParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Raise swords during windup
        if (!swordsRaised && tickCount <= windup) {
            if (tickCount % 3 == 0) { // Every 3 ticks during windup
                createSwordRaisingEffect();
            }

            if (tickCount == windup) {
                swordsRaised = true;
                // Sound buildup reaching peak
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }

        // Execute the slam once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            executeRoarSlam();
            hasExecuted = true;
        }
    }

    private void createSwordRaisingEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Particles gathering above the user as swords are raised
        for (int i = 0; i < 5; i++) {
            double height = 2.0 + (tickCount / (double)windup) * 2.0; // Rising effect
            Vec3 particlePos = userPos.add(
                    (Math.random() - 0.5) * 0.5,
                    height,
                    (Math.random() - 0.5) * 0.5
            );

            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void executeRoarSlam() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 slamPoint = userPos.add(user.getLookAngle().scale(1.5)).add(0, -1, 0);

        // Create massive impact effect
        createRoarExplosion(slamPoint);

        // Hit all enemies in AOE radius
        List<LivingEntity> targets = getTargetsInCustomHitbox(slamPoint, range, 2.0, range);

        for (LivingEntity target : targets) {
            hitTarget(target);

            // Strong knockback away from slam point
            Vec3 knockbackDir = target.position().subtract(slamPoint).normalize();
            Vec3 strongKnockback = knockbackDir.scale(knockback * 1.5);
            target.push(strongKnockback.x, 0.3, strongKnockback.z);
            target.hurtMarked = true;

            // Extra particle burst at each hit target
            createSoundHitParticles(target.position());
        }

        // Thunder-like slam sound
        world.playSound(null, slamPoint.x, slamPoint.y, slamPoint.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.7f);

        // Impact sound
        world.playSound(null, slamPoint.x, slamPoint.y, slamPoint.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.8f);
    }

    /**
     * Create the massive roar explosion effect with more boom boom
     */
    private void createRoarExplosion(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Multiple central explosions for more boom boom
        for (int i = 0; i < 3; i++) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    center.x + (Math.random() - 0.5) * 2,
                    center.y + 1 + (Math.random() - 0.5) * 2,
                    center.z + (Math.random() - 0.5) * 2,
                    1, 0, 0, 0, 0);
        }

        // Large shockwave ring - much bigger
        createSoundShockwave(center, range, 64); // More particles for bigger boom

        // Massive upward particle burst - much more spread out and more particles
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                center.x, center.y + 1, center.z,
                30, range * 2.0, 6.0, range * 2.0, 0.5); // Much bigger spread and more particles

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                center.x, center.y + 0.5, center.z,
                25, range * 1.8, 5.0, range * 1.8, 0.4); // Much bigger spread

        // Flash effects - bigger and more
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                center.x, center.y + 1, center.z,
                20, range * 1.2, 4.0, range * 1.2, 0.3); // Much bigger spread

        serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                center.x, center.y + 1, center.z,
                20, range * 1.2, 4.0, range * 1.2, 0.3); // Much bigger spread

        // Multiple ground impact rings for more boom boom
        for (int ring = 1; ring <= 4; ring++) {
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * 2 * Math.PI;
                double radius = range * 0.3 * ring; // Multiple rings
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;

                serverLevel.sendParticles(ParticleTypes.POOF,
                        x, center.y, z,
                        3, 0.3, 0.1, 0.3, 0.1);
            }
        }

        // Sonic boom effect - multiple for more boom boom
        for (int i = 0; i < 5; i++) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    center.x + (Math.random() - 0.5) * 4,
                    center.y + 1 + (Math.random() - 0.5) * 4,
                    center.z + (Math.random() - 0.5) * 4,
                    1, 1.0, 1.0, 1.0, 0);
        }

        // Extra lava particles for more boom boom
        serverLevel.sendParticles(ParticleTypes.LAVA,
                center.x, center.y + 1, center.z,
                15, range * 0.8, 3.0, range * 0.8, 0.2);
    }

    @Override
    public boolean isShockwaveAttack() {
        return true; // This creates a shockwave
    }

    @Override
    protected void onStop() {
        // Reset state
        hasExecuted = false;
        swordsRaised = false;

        // Final echo sound
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 0.9f);
        }
    }
}