package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
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

// Fourth Form: Dance of the Centipede. Zigzag dash (22.5° / -22.5° / 22.5°) drags caught enemies into a straight finisher.
public class CentipedeAttack extends InsectBreathingAttackBase {

    private static final int ZIGZAG_COUNT = 4;
    private static final int DASH_DURATION = 10;
    private static final int DASH_INTERVAL = 10;

    private int zigzagsExecuted = 0;
    private int nextZigzagTick = 0;
    private boolean finisherExecuted = false;
    private Vec3 baseDirection;
    private Vec3 lastDashPos;
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();

    private boolean wasInvulnerable = false;

    @Override
    protected void onStart() {
        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        lastDashPos = null;
        caughtEnemies.clear();
        draggedEnemies.clear();

        Vec3 look = user.getLookAngle();
        baseDirection = new Vec3(look.x, 0, look.z).normalize();
        wasInvulnerable = user.isInvulnerable();
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
        createCentipedeCoilEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (zigzagsExecuted < ZIGZAG_COUNT && tickCount >= windup + nextZigzagTick) {
            executeZigzagDash();
            zigzagsExecuted++;
            nextZigzagTick += DASH_DURATION + DASH_INTERVAL;
        }

        // Sample hitboxes on every tick during each dash's travel window
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            int lastDashStartTick = windup + (zigzagsExecuted - 1) * (DASH_DURATION + DASH_INTERVAL);
            int travelTick = tickCount - lastDashStartTick;
            if (travelTick > 0 && travelTick <= DASH_DURATION) {
                hitEnemiesAlongPath();
            }
        }

        if (!finisherExecuted && zigzagsExecuted >= ZIGZAG_COUNT &&
                tickCount >= windup + (ZIGZAG_COUNT * (DASH_DURATION + DASH_INTERVAL)) + 5) {
            executeFinisher();
            finisherExecuted = true;
        }

        if (zigzagsExecuted > 0 && !finisherExecuted) {
            continueDragEffect();
        }
    }

    private void executeZigzagDash() {
        Vec3 look = user.getLookAngle();
        Vec3 aimedDirection = new Vec3(look.x, 0, look.z);
        if (aimedDirection.lengthSqr() > 0.001) {
            baseDirection = aimedDirection.normalize();
        }

        // Alternating zigzag: +22.5° / -22.5° / +22.5° / -22.5° ...
        double offsetDegrees = (zigzagsExecuted % 2 == 0) ? 22.5 : -22.5;
        Vec3 zigzagDirection = rotateDirection(baseDirection, offsetDegrees);

        Vec3 dashVelocity = zigzagDirection.scale(dashSpeed * 0.67);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;
        user.setInvulnerable(true);
        lastDashPos = user.position().add(0, user.getBbHeight() / 2, 0);

        catchAndDragEnemies();
        createZigzagTrail(zigzagsExecuted);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f + zigzagsExecuted * 0.3f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private Vec3 rotateDirection(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double newX = direction.x * cos - direction.z * sin;
        double newZ = direction.x * sin + direction.z * cos;
        return new Vec3(newX, direction.y, newZ).normalize();
    }

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();
        List<LivingEntity> pathEnemies = getTargetsInCustomHitbox(userPos, hitboxSize, hitboxSize * 0.67, hitboxSize);

        for (LivingEntity enemy : pathEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);
                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.8f, 1.5f);
                createCatchEffect(enemy.position());
            }
        }
    }

    private void continueDragEffect() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 4, 0);
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 toDrag = userPos.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.5) {
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                    draggedEnemy.hurtMarked = true;
                } else {
                    draggedEnemy.setDeltaMovement(
                            user.getDeltaMovement().x,
                            draggedEnemy.getDeltaMovement().y,
                            user.getDeltaMovement().z
                    );
                }
                createDragTrailEffect(draggedEnemy.position());
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void hitEnemiesAlongPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 previousPos = lastDashPos;
        lastDashPos = userPos;
        List<LivingEntity> targets = previousPos != null
                ? getTargetsAlongPath(previousPos, userPos, hitboxSize * 0.84f)
                : getTargetsInCustomHitbox(userPos, hitboxSize * 0.84, hitboxSize * 0.5, hitboxSize * 0.84);

        for (LivingEntity target : targets) {
            float originalDamage = damage;
            damage = damage * 0.4f;
            hitTarget(target);
            damage = originalDamage;

            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.1);
            target.push(lightKnockback.x, 0.033, lightKnockback.z);
            createZigzagImpactEffect(target.position());
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 0.4f;
                hitTarget(draggedEnemy);
                damage = originalDamage;
                createZigzagImpactEffect(draggedEnemy.position());
            }
        }
    }

    private List<LivingEntity> getTargetsAlongPath(Vec3 from, Vec3 to, float radius) {
        Vec3 path = to.subtract(from);
        double distance = path.length();
        if (distance < 0.01) return getTargetsInCustomHitbox(to, radius, hitboxSize * 0.5, radius);

        Set<LivingEntity> found = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(distance / Math.max(radius * 0.25, 0.1)));
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = from.add(path.scale((double) i / steps));
            found.addAll(getTargetsInCustomHitbox(sample, radius, hitboxSize * 0.5, radius));
        }
        return new ArrayList<>(found);
    }

    private void executeFinisher() {
        Vec3 look = user.getLookAngle();
        Vec3 aimedDirection = new Vec3(look.x, 0, look.z);
        if (aimedDirection.lengthSqr() > 0.001) {
            baseDirection = aimedDirection.normalize();
        }
        Vec3 finisherVelocity = baseDirection.scale(dashSpeed * 0.83);
        user.setDeltaMovement(finisherVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize * 0.83, 1.33, hitboxSize * 0.83);

        for (LivingEntity target : finisherTargets) {
            hitTarget(target);
            Vec3 finisherKnockback = target.position().subtract(user.position()).normalize();
            target.push(finisherKnockback.x * knockback * 0.5, 0.27, finisherKnockback.z * knockback * 0.5);
            createFinisherImpactEffect(target.position());
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 1.5f;
                hitTarget(draggedEnemy);
                damage = originalDamage;
                Vec3 finalKnockback = baseDirection.scale(knockback * 1.0);
                draggedEnemy.push(finalKnockback.x, 0.33, finalKnockback.z);
                createFinisherImpactEffect(draggedEnemy.position());
            }
        }

        createVenomBurst();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.8f);
        playPoisonSound(user.position());
    }

    private void createCentipedeCoilEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 0; i < 25; i++) {
            double progress = i / 25.0;
            double angle = progress * 6 * Math.PI; // Multiple rotations
            double radius = 0.67 * (1.0 - progress); // 1/3 of 2.0
            double height = progress * 0.83; // 1/3 of 2.5

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    private void createZigzagTrail(int zigzagNumber) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(baseDirection.scale(i * 0.13));
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    trailPos.x, trailPos.y, trailPos.z, 3, 0.2, 0.2, 0.2, 0.1);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    trailPos.x, trailPos.y, trailPos.z, 2, 0.15, 0.15, 0.15, 0.08);
        }

        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                userPos.x, userPos.y, userPos.z, 8, 0.5, 0.5, 0.5, 0.1);
    }

    private void createCatchEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                enemyPos.x, enemyPos.y + 1, enemyPos.z, 10, 0.5, 0.5, 0.5, 0.2);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                enemyPos.x, enemyPos.y + 1, enemyPos.z, 8, 0.4, 0.4, 0.4, 0.15);
    }

    private void createDragTrailEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                enemyPos.x, enemyPos.y + 0.5, enemyPos.z, 2, 0.3, 0.3, 0.3, 0.1);
        if (tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    enemyPos.x, enemyPos.y, enemyPos.z, 1, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void createZigzagImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z, 5, 0.2, 0.2, 0.2, 0.1);
        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                targetPos.x, targetPos.y, targetPos.z, 8, 0.3, 0.3, 0.3, 0.12);
    }

    private void createFinisherImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z, 20, 0.6, 0.6, 0.6, 0.3);
        createPoisonBurst(targetPos, 0.67f);
        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                targetPos.x, targetPos.y, targetPos.z, 25, 0.8, 0.8, 0.8, 0.4);
    }

    private void createVenomBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                userPos.x, userPos.y, userPos.z, 60, 0.83, 0.83, 0.83, 0.17);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                userPos.x, userPos.y, userPos.z, 40, 0.67, 0.67, 0.67, 0.13);

        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 1.67;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y;

            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    x, y, z, 8, 0.2, 0.4, 0.2, 0.1);
        }
    }

    @Override
    public boolean hasInvincibilityFrames() {
        return true;
    }

    @Override
    public boolean isDashAttack() {
        return true;
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);
        createVenomBurst();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_DEATH, SoundSource.PLAYERS, 1.0f, 1.2f);
        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        lastDashPos = null;
        caughtEnemies.clear();
        draggedEnemies.clear();
    }
}