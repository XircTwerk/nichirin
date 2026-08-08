package com.xirc.nichirin.common.attack.moves.breathing.flame;

import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.common.vfx.VfxManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * First Form: Unknowing Fire
 * The user dashes towards the target at tremendous speeds,
 * before unleashing a singular horizontal slash directed at the target's neck for a decapitation.
 */
public class UnknowingFireAttack extends FlameBreathingAttackBase {

    private static final float DASH_DISTANCE = 0.8f;
    private static final int DASH_DURATION = 40;
    private static final float SLASH_WIDTH = 8.0f; // Very wide slash
    private static final float SLASH_DEPTH = 3.0f; // Deep slash

    private boolean dashStarted = false;
    private boolean slashExecuted = false;
    private Vec3 dashDirection;
    @Getter
    @Setter
    private Vec3 startPosition;
    private Vec3 lastDashPos = null;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    // Hit targets ride along with the user until the final slash so they all get caught in the big swing.
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();

    // Invulnerability and fall damage protection
    private boolean wasInvulnerable = false;
    private boolean shouldPreventFallDamage = false;

    public UnknowingFireAttack() {
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        slashExecuted = false;
        lastDashPos = null;
        hitEntities.clear();
        draggedEnemies.clear();

        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();

        // Make user invulnerable during attack
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        // Flame startup sound
        playFlameSound();

        // Create initial flame particles around user

        // Fire charge sound for dash preparation
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Create charging effect
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash after windup
        if (!dashStarted && tickCount == windup + 1) {
            startDash();
            dashStarted = true;
        }

        // Continue dash with constant hitboxes like Flame Tiger
        if (dashStarted && tickCount > windup && tickCount <= windup + DASH_DURATION) {
            continueDash();
        }

        // Execute massive slash at the end of dash
        if (!slashExecuted && tickCount == windup + DASH_DURATION + 1) {
            executeSlash();
            slashExecuted = true;
        }
    }

    private void createDashChargeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create intense flame buildup around user
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.0 + Math.sin(angle * 3) * 0.3; // Wavy circle

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.random() * 2.0;
}

        // Upward flame spiral
        for (int i = 0; i < 15; i++) {
            double height = i * 0.2;
            double angle = i * 0.5;
            double radius = 0.8;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;
}
    }

    private void startDash() {
        playFlameVfxAt(VfxIds.UNKNOWING_FIRE, user.position(), dashDirection, 1.15f);
        // Set user velocity for dash - using Flame Tiger's method
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE * 6);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash start sound - more intense
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 1.8f);

        // Create initial dash burst
    }

    private void continueDash() {
        // Maintain dash velocity
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE * 6);
        Vec3 prevPos = lastDashPos;
        lastDashPos = user.position().add(0, user.getBbHeight() / 2, 0);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create continuous intense trail

        // Sweep hitbox from last position to current to eliminate gaps from high-speed movement
        List<LivingEntity> dashTargets;
        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);
        if (prevPos != null) {
            dashTargets = getTargetsInLine(prevPos, center, hitboxSize * 1.5);
        } else {
            dashTargets = getTargetsInCustomHitbox(center, hitboxSize * 1.5f, hitboxSize * 1.25f, hitboxSize * 1.5f);
        }

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                // Very light dash damage (20% of full damage) - RESPECTS IMMUNITY FRAMES
                float originalDamage = damage;
                hitTarget(target); // Respects immunity frames
                damage = originalDamage;

                hitEntities.add(target);
                draggedEnemies.add(target);

                // Create impact particles
            }
        }

        dragHitEnemies();
    }

    /**
     * Drag every hit enemy toward where the final slash will land — horizontal only so they
     * don't get launched into the air, capped per-tick so a far-away catch doesn't yank them
     * across the map, and only while the user is actually still moving. The moment the dash
     * ends or the user stops, the pull stops too.
     */
    private void dragHitEnemies() {
        Vec3 horizontalDash = new Vec3(dashDirection.x, 0, dashDirection.z).normalize();
        // Anchor in FRONT of the user (at foot level) so caught mobs pile up where the slash hits.
        Vec3 dragAnchor = user.position().add(horizontalDash.scale(1.0));

        Vec3 userVel = user.getDeltaMovement();
        double userSpeedSqr = userVel.x * userVel.x + userVel.z * userVel.z;
        boolean userMoving = userSpeedSqr > 0.04;

        draggedEnemies.removeIf(enemy -> !enemy.isAlive() || enemy.isRemoved());
        for (LivingEntity dragged : draggedEnemies) {
            if (!userMoving) {
                Vec3 existing = dragged.getDeltaMovement();
                dragged.setDeltaMovement(0, Math.min(existing.y, 0), 0);
                dragged.hurtMarked = true;
                continue;
            }
            Vec3 toAnchor = dragAnchor.subtract(dragged.position());
            double vx = Math.max(-1.2, Math.min(1.2, toAnchor.x * 0.4));
            double vz = Math.max(-1.2, Math.min(1.2, toAnchor.z * 0.4));
            double vy = Math.min(0, dragged.getDeltaMovement().y);
            dragged.setDeltaMovement(vx, vy, vz);
            dragged.hurtMarked = true;
            dragged.fallDistance = 0f;
        }
    }

    private void executeSlash() {
        // Create MASSIVE horizontal slash effect

        // Hit all enemies in the massive slash area - RESPECTS IMMUNITY FRAMES
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create multiple hitboxes for the wide slash
        for (int i = -8; i <= 8; i++) {
            double offset = i * (SLASH_WIDTH / 16.0);
            Vec3 slashCenter = userPos.add(lookDir.scale(SLASH_DEPTH / 2)).add(rightDir.scale(offset));

            List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, hitboxSize * 1.5, hitboxSize);

            for (LivingEntity target : targets) {
                // Full power slash damage - RESPECTS IMMUNITY FRAMES
                hitTarget(target);

                // Strong sideways knockback based on position
                Vec3 slashKnockback = rightDir.scale(knockback * (i > 0 ? 1 : -1) * 1.5);
                slashKnockback = slashKnockback.add(lookDir.scale(knockback * 0.5));
                target.push(slashKnockback.x, 0.4, slashKnockback.z);

                // Create impact particles

                // Individual hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }

        // Main slash sound - very dramatic
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.8f);
    }

    private void createDashBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Explosion of flames at dash start
}

    private void createIntenseDashTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Dense flame trail behind user
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));
}

        // Side flames during dash
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 1.0));
}
    }

    private void createDashImpactParticles(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Flame burst at impact during dash
}

    private void createMassiveSlashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create MASSIVE horizontal arc of flame particles
        for (int i = -16; i <= 16; i++) { // Much wider slash
            double offset = i * (SLASH_WIDTH / 32.0);

            for (int depth = 0; depth < 6; depth++) { // Multiple depth layers
                Vec3 slashPos = userPos
                        .add(lookDir.scale(1.0 + depth * 0.5))
                        .add(rightDir.scale(offset));

                // Main flame particles - more intense in center
                int particleCount = Math.max(1, 6 - Math.abs(i) / 3);
if (Math.abs(i) <= 8) {
}

                // Crit particles for cutting effect
                if (Math.abs(i) <= 4 && depth < 3) {
}
            }
        }

        // Create vertical flame walls at the edges
        for (int side = -1; side <= 1; side += 2) {
            Vec3 edgePos = userPos.add(rightDir.scale(side * SLASH_WIDTH / 2));

            for (int height = 0; height < 5; height++) {
                Vec3 wallPos = edgePos.add(0, height * 0.5, 0);
}
        }

        // Massive flame explosion at slash center
        Vec3 slashCenter = userPos.add(lookDir.scale(SLASH_DEPTH / 2));
// Smoke cloud from the massive slash
}

    private void createSlashImpactParticles(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Intense flame burst at impact
}

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Release every dragged enemy: zero horizontal drag, preserve gravity, so they don't
        // skid forever after the dash ends.
        for (LivingEntity dragged : draggedEnemies) {
            if (dragged.isAlive()) {
                Vec3 existing = dragged.getDeltaMovement();
                dragged.setDeltaMovement(0, Math.min(existing.y, 0), 0);
                dragged.hurtMarked = true;
            }
        }
        // Restore original invulnerability state
        user.setInvulnerable(wasInvulnerable);

        // Set flag to prevent fall damage on next landing
        shouldPreventFallDamage = true;

        // If user is already on ground, reset fall distance to prevent immediate fall damage
        if (user.onGround()) {
            user.resetFallDistance();
        }

        // Reset user velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Final dramatic flame explosion
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Keep the terminal boom in world space so it does not follow the user afterward.
            VfxManager.playOwned(serverLevel, user, VfxIds.UNKNOWING_FIRE_IMPACT,
                    userPos, dashDirection, 1.15f);
        }

        // Final dramatic sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.7f);

        dashStarted = false;
        slashExecuted = false;
        lastDashPos = null;
        hitEntities.clear();
        draggedEnemies.clear();
    }

    @Override
    public void tick() {
        super.tick();

        // Check if we should prevent fall damage and user just landed
        if (shouldPreventFallDamage && user.onGround() && user.fallDistance > 0) {
            user.resetFallDistance(); // Prevent fall damage
            shouldPreventFallDamage = false; // Reset flag after use
        }
    }

}
