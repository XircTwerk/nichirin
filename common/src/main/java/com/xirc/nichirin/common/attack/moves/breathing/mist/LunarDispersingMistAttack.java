package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.vfx.VfxIds;
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

        // Slash hitbox pulses every 2 ticks during mid-flight.
        int ticksSinceWindup = tickCount - windup;
        if (ticksSinceWindup >= 2 && ticksSinceWindup <= duration - 6 && ticksSinceWindup % 2 == 0) {
            performMidFlightSlash();
        }

        // Final vertical slash near end of attack
        if (!finisherExecuted && tickCount >= windup + duration - 4) {
            executeVerticalFinisher();
            finisherExecuted = true;
        }

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
        playMistVfx(VfxIds.LUNAR_DISPERSING_MIST,
                user.position().add(0, user.getBbHeight() * 0.45, 0), dashDirection, 1.0f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);

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

    }

    private void executeVerticalFinisher() {
        Vec3 current = user.getDeltaMovement();
        user.setDeltaMovement(current.x * 0.2, current.y, current.z * 0.2);

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        playMistVfx(VfxIds.MIST_FINISHER, userPos, dashDirection, hitboxSize / 3.0f);

        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(
                userPos, hitboxSize * 3.0, user.getBbHeight() + 2.0, hitboxSize * 3.0);

        for (LivingEntity target : finisherTargets) {
            float originalDamage = damage;
            damage = damage * 2.5f;
            hitTarget(target);
            damage = originalDamage;

            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            target.push(knockbackDir.x * knockback * 1.2f, 0.4, knockbackDir.z * knockback * 1.2f);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.3f, 0.8f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
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
