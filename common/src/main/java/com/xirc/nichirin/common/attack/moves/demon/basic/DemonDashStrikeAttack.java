package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon dashing strike attack - dash 4 blocks forward and deliver devastating punch
 * High damage punch at the end of dash with good range
 */
public class DemonDashStrikeAttack extends AbstractDemonAttack<DemonDashStrikeAttack, IDemonAttacker> {

    private boolean dashExecuted = false;
    private boolean punchExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;

    public DemonDashStrikeAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        dashExecuted = false;
        punchExecuted = false;

        if (user != null) {
            dashDirection = user.getLookAngle().normalize();
            startPosition = user.position();
        }

        // Dash startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || user == null) return;

        // Execute dash after windup
        if (!dashExecuted && tickCount >= windup) {
            executeDash();
            dashExecuted = true;
        }

        // Execute punch at end of dash (during active frames)
        if (dashExecuted && !punchExecuted && tickCount >= windup + 12) { // Punch 12 ticks after dash starts
            executePunch();
            punchExecuted = true;
        }
    }

    private void executeDash() {
        if (user == null || dashDirection == null) return;

        // Apply strong forward momentum for dash
        double dashStrength = 1.8; // Strong dash velocity
        Vec3 dashVelocity = dashDirection.scale(dashStrength);

        // Maintain some vertical component if jumping
        double yVelocity = Math.max(user.getDeltaMovement().y, 0.1);
        user.setDeltaMovement(dashVelocity.x, yVelocity, dashVelocity.z);
        user.hasImpulse = true;
        user.hurtMarked = true;

        // Create dash trail effect
        createDashTrail();

        // Dash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private void executePunch() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 punchPos = userPos.add(dashDirection.scale(2.0)); // Punch 2 blocks in front

        // Create powerful punch effect
        createPunchImpact(punchPos);

        // Hit enemies in front with high damage
        List<LivingEntity> targets = getTargetsInCircle((float)range, 3);

        for (LivingEntity target : targets) {
            // High damage punch
            hitTarget(target);

            // Moderate knockback in dash direction
            Vec3 knockbackDir = dashDirection.normalize();
            Vec3 knockbackForce = knockbackDir.scale(knockback);
            target.setDeltaMovement(target.getDeltaMovement().add(knockbackForce.x, 0.3, knockbackForce.z));
            target.hurtMarked = true;
            target.hasImpulse = true;

            // Impact sound per target
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        // Main punch sound
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.3f, 0.7f);

        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private void createDashTrail() {
    }

    private void createPunchImpact(Vec3 punchPos) {
    }

    @Override
    protected void onStop() {
        dashExecuted = false;
        punchExecuted = false;
        dashDirection = null;
        startPosition = null;
    }
}