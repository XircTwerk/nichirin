package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Form 5: Fast multi-hop zigzag charge with large hitboxes. Drags enemies into a straight finisher.
public class SeaOfCloudsAndHazeAttack extends MistBreathingAttackBase {

    private static final int   ZIGZAG_COUNT   = 5;  // many quick dashes
    private static final int   DASH_DURATION  = 2;   // hit-sampling window per hop
    private static final int   DASH_INTERVAL  = 1;   // ticks between hops
    private static final float ZIGZAG_ANGLE   = 30f; // alternating left/right angle
    private static final float SPEED_FACTOR   = 1.2f; // dashSpeed * this = hop velocity (blocks/tick)

    private int     zigzagsExecuted = 0;
    private int     nextZigzagTick  = 0;
    private boolean finisherExecuted = false;
    private Vec3    baseDirection;
    private Vec3    lastDashPos;

    private final Set<LivingEntity>  caughtEnemies  = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private final Set<LivingEntity>  hitThisHop     = new HashSet<>(); // reset each hop → one hit/hop
    private boolean wasInvulnerable = false;
    private final List<UUID> spawnedClones = new ArrayList<>();

    @Override
    protected void onStart() {
        zigzagsExecuted  = 0;
        nextZigzagTick   = 0;
        finisherExecuted = false;
        lastDashPos      = null;
        caughtEnemies.clear();
        draggedEnemies.clear();
        hitThisHop.clear();
        spawnedClones.clear();

        Vec3 look = user.getLookAngle();
        baseDirection = new Vec3(look.x, 0, look.z).normalize();
        wasInvulnerable = user.isInvulnerable();

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // 1. Trigger next zigzag hop (velocity-based, smooth)
        if (zigzagsExecuted < ZIGZAG_COUNT && tickCount >= windup + nextZigzagTick) {
            executeZigzagDash();
            zigzagsExecuted++;
            nextZigzagTick += DASH_DURATION + DASH_INTERVAL;
        }

        // 2. Sample hits every tick during the current hop's travel window
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            int lastDashStartTick = windup + (zigzagsExecuted - 1) * (DASH_DURATION + DASH_INTERVAL);
            int travelTick = tickCount - lastDashStartTick;
            if (travelTick > 0 && travelTick <= DASH_DURATION) {
                hitEnemiesAlongPath();
            }
        }

        // 3. Finisher once all hops are done
        int finisherStartTick = windup + (ZIGZAG_COUNT * (DASH_DURATION + DASH_INTERVAL)) + 3;
        if (!finisherExecuted && zigzagsExecuted >= ZIGZAG_COUNT && tickCount >= finisherStartTick) {
            executeFinisher();
            finisherExecuted = true;
        }

        // 4. Drag caught enemies between hops
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            continueDragEffect();
        }
    }

    /** Executes a single zigzag hop using velocity (smooth, client-predicted). */
    private void executeZigzagDash() {
        hitThisHop.clear(); // new hop → every enemy can be struck once more

        double angle = (zigzagsExecuted % 2 == 0) ? ZIGZAG_ANGLE : -ZIGZAG_ANGLE;
        Vec3 dir = rotateDirection(baseDirection, angle);
        playMistVfx(VfxIds.SEA_OF_CLOUDS_AND_HAZE,
                user.position().add(0, user.getBbHeight() * 0.35, 0), dir, 0.85f);

        float speed = dashSpeed != null ? dashSpeed * SPEED_FACTOR : 6.0f;
        Vec3 vel = dir.scale(speed);
        user.setDeltaMovement(vel.x, 0.0, vel.z);
        user.hurtMarked = true;
        user.hasImpulse = true;
        user.setInvulnerable(true);
        lastDashPos = user.position().add(0, user.getBbHeight() / 2, 0);

        catchAndDragEnemies();

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f + zigzagsExecuted * 0.08f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();
        List<LivingEntity> pathEnemies = getTargetsInCustomHitbox(userPos, hitboxSize * 1.75, hitboxSize, hitboxSize * 1.75);

        for (LivingEntity enemy : pathEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);

                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        }
    }

    private void continueDragEffect() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 4, 0);

        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 toDrag = userPos.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.15) {
                    // Strong pull (cap 3.5 b/t) with a tiny dead-zone so the enemy is reeled in
                    // right against the player instead of trailing ~half a block behind.
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                    draggedEnemy.hurtMarked = true;
                } else {
                    draggedEnemy.setDeltaMovement(
                            user.getDeltaMovement().x,
                            draggedEnemy.getDeltaMovement().y,
                            user.getDeltaMovement().z
                    );
                }
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void hitEnemiesAlongPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 previousPos = lastDashPos;
        lastDashPos = userPos;

        // Sample along the path between last and current position so fast hops don't skip enemies.
        List<LivingEntity> targets = previousPos != null
                ? getTargetsAlongPath(previousPos, userPos, hitboxSize * 1.5f)
                : getTargetsInCustomHitbox(userPos, hitboxSize * 1.5, hitboxSize, hitboxSize * 1.5);

        for (LivingEntity target : targets) {
            if (!hitThisHop.add(target)) continue; // already struck this hop
            float originalDamage = damage;
            damage = damage * 0.45f;
            hitTargetNoImmunity(target); // bypass i-frames so each hop's hit actually lands
            damage = originalDamage;

            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.12f);
            target.push(lightKnockback.x, 0.04, lightKnockback.z);
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive() && hitThisHop.add(draggedEnemy)) {
                float originalDamage = damage;
                damage = damage * 0.45f;
                hitTargetNoImmunity(draggedEnemy);
                damage = originalDamage;
            }
        }
    }

    private List<LivingEntity> getTargetsAlongPath(Vec3 from, Vec3 to, float radius) {
        Vec3 path = to.subtract(from);
        double distance = path.length();
        if (distance < 0.01) return getTargetsInCustomHitbox(to, radius, hitboxSize, radius);

        Set<LivingEntity> found = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(distance / Math.max(radius * 0.125, 0.05)));
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = from.add(path.scale((double) i / steps));
            found.addAll(getTargetsInCustomHitbox(sample, radius, hitboxSize, radius));
        }
        return new ArrayList<>(found);
    }

    private void executeFinisher() {
        // Short forward burst to sell the final slash
        if (dashSpeed != null) {
            user.setDeltaMovement(baseDirection.scale(dashSpeed * 0.4));
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        playMistVfx(VfxIds.MIST_FINISHER, userPos, baseDirection, range / 4.0f);
        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(userPos, hitboxSize * 1.5f, 2.5, hitboxSize * 1.5f);

        for (LivingEntity target : finisherTargets) {
            hitTarget(target);
            Vec3 finisherKnockback = target.position().subtract(user.position()).normalize();
            target.push(finisherKnockback.x * knockback * 0.7f, 0.35, finisherKnockback.z * knockback * 0.7f);
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 1.5f;
                hitTarget(draggedEnemy);
                damage = originalDamage;

                Vec3 finalKnockback = baseDirection.scale(knockback * 1.2f);
                draggedEnemy.push(finalKnockback.x, 0.4, finalKnockback.z);
            }
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.4f, 0.9f);
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.1f);

        if (world instanceof ServerLevel serverLevel) {
            for (UUID id : spawnedClones) {
                Entity e = serverLevel.getEntity(id);
                if (e != null) e.discard();
            }
        }

        zigzagsExecuted  = 0;
        nextZigzagTick   = 0;
        finisherExecuted = false;
        lastDashPos      = null;
        caughtEnemies.clear();
        draggedEnemies.clear();
        hitThisHop.clear();
        spawnedClones.clear();
    }
}
