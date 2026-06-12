package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Form 6: Jump + horizontal charge, slashing everything in the path. Ends with a massive vertical AoE.
public class LunarDispersingMistAttack extends MistBreathingAttackBase {

    private boolean launched = false;
    private boolean finisherExecuted = false;
    private Vec3 dashDirection;
    private final Set<LivingEntity> hitDuringFlight = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private int slashCount = 0;

    @Override
    protected void onStart() {
        launched = false;
        finisherExecuted = false;
        hitDuringFlight.clear();
        draggedEnemies.clear();
        slashCount = 0;
        Vec3 look = user.getLookAngle();
        dashDirection = new Vec3(look.x, 0, look.z).normalize();
    }

    @Override
    protected void onActiveStart() {
        createMistParticles();

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!launched && tickCount == windup + 1) {
            launchAerialCharge();
            launched = true;
        }

        if (!launched) return;

        sustainFlight();

        // Slash hitbox pulses every 4 ticks during mid-flight
        int ticksSinceWindup = tickCount - windup;
        if (ticksSinceWindup >= 2 && ticksSinceWindup <= duration - 6 && ticksSinceWindup % 4 == 0) {
            performMidFlightSlash();
        }

        // Final vertical slash near end of attack
        if (!finisherExecuted && tickCount >= windup + duration - 4) {
            executeVerticalFinisher();
            finisherExecuted = true;
        }

        createFlightTrail();
    }

    private void launchAerialCharge() {
        float jumpStrength = 0.9f;
        float horizontalSpeed = dashSpeed != null ? dashSpeed / Math.max(duration, 1) * 10.0f : 0.5f;

        user.setDeltaMovement(
                dashDirection.x * horizontalSpeed,
                jumpStrength,
                dashDirection.z * horizontalSpeed
        );
        user.hurtMarked = true;
        user.hasImpulse = true;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, 0.5, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    20, 0.8, 0.3, 0.8, 0.04);
        }
    }

    private void sustainFlight() {
        if (dashSpeed != null && !finisherExecuted) {
            Vec3 current = user.getDeltaMovement();
            float horizontalSpeed = dashSpeed / Math.max(duration, 1) * 7.0f;
            user.setDeltaMovement(
                    dashDirection.x * horizontalSpeed,
                    current.y,
                    dashDirection.z * horizontalSpeed
            );
            user.hurtMarked = true;
        }
    }

    private void performMidFlightSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        List<LivingEntity> targets = getTargetsInCustomHitbox(
                userPos, hitboxSize * 2.0, user.getBbHeight() + 1.5, hitboxSize * 2.0);

        for (LivingEntity target : targets) {
            if (!hitDuringFlight.contains(target)) {
                hitTarget(target);
                hitDuringFlight.add(target);
                if (!draggedEnemies.contains(target)) draggedEnemies.add(target);
            } else {
                float originalDamage = damage;
                damage = damage * 0.5f;
                hitTargetNoImmunity(target);
                damage = originalDamage;
            }
        }

        // Drag caught enemies along the flight path
        Vec3 dragAnchor = user.position().add(0, user.getBbHeight() / 4, 0);
        for (LivingEntity dragged : new ArrayList<>(draggedEnemies)) {
            if (!dragged.isAlive()) { draggedEnemies.remove(dragged); continue; }
            Vec3 toDrag = dragAnchor.subtract(dragged.position());
            double dist = toDrag.length();
            if (dist > 0.5) {
                dragged.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                dragged.hurtMarked = true;
            } else {
                dragged.setDeltaMovement(
                        user.getDeltaMovement().x,
                        dragged.getDeltaMovement().y,
                        user.getDeltaMovement().z
                );
            }
        }

        slashCount++;
        float pitch = 1.1f + slashCount * 0.05f;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, pitch);

        // Slash arc particles radiating outward from the player
        if (world instanceof ServerLevel serverLevel) {
            for (int i = -4; i <= 4; i++) {
                double angle = Math.toRadians(i * 15);
                Vec3 slashDir = dashDirection.yRot((float) angle);
                Vec3 particlePos = userPos.add(slashDir.scale(hitboxSize * 1.5));
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        3, 0.05, 0.05, 0.05, 0.03);
            }
        }
    }

    private void executeVerticalFinisher() {
        Vec3 current = user.getDeltaMovement();
        user.setDeltaMovement(current.x * 0.2, current.y, current.z * 0.2);

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(
                userPos, hitboxSize * 3.0, user.getBbHeight() + 2.0, hitboxSize * 3.0);

        for (LivingEntity target : finisherTargets) {
            float originalDamage = damage;
            damage = damage * 2.5f;
            hitTarget(target);
            damage = originalDamage;

            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            target.push(knockbackDir.x * knockback * 1.2f, 0.4, knockbackDir.z * knockback * 1.2f);
            createMistHitParticles(target.position());
        }

        createMistCircle(userPos, hitboxSize * 2.5f, 32);
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, userPos.x, userPos.y, userPos.z,
                    40, 2.0, 1.5, 2.0, 0.05);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, userPos.x, userPos.y, userPos.z,
                    20, 1.5, 1.5, 1.5, 0.15);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.3f, 0.8f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
    }

    private void createFlightTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 1; i <= 5; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.6));
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    trailPos.x, trailPos.y, trailPos.z, 2, 0.2, 0.2, 0.2, 0.02);
        }

        if (tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    userPos.x, userPos.y, userPos.z, 3, 0.4, 0.4, 0.4, 0.03);
        }
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);
        hitDuringFlight.clear();
        draggedEnemies.clear();
        launched = false;
        finisherExecuted = false;
        slashCount = 0;
    }
}