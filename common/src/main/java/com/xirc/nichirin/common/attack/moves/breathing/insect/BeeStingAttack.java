package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.common.vfx.VfxIds;

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

// Second Form: Dance of the Bee Sting. Dash through enemies in a straight line, piercing each with light venom stabs.
// Hit targets are carried along with the player for the rest of the dash, so a single sting can chain
// multiple enemies into one impact point instead of leaving them scattered behind.
public class BeeStingAttack extends InsectBreathingAttackBase {

    private static final int DASH_DURATION = 13;

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 lastDashPos;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private int trailEffectCounter = 0;

    @Override
    protected void onStart() {
        dashStarted = false;
        hitEntities.clear();
        draggedEnemies.clear();
        trailEffectCounter = 0;
        lastDashPos = null;
        dashDirection = user.getLookAngle().normalize();
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 0.8f, 1.5f);
        createBeeSwarmEffect();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!dashStarted && tickCount == windup + 1) {
            startDash();
            dashStarted = true;
        }

        if (dashStarted && tickCount > windup && tickCount <= windup + DASH_DURATION) {
            continueDash();
        }
    }

    private void createBeeSwarmEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 0; i < 15; i++) {
            double angle = (i / 15.0) * 2 * Math.PI;
            double radius = 1.2 + Math.sin(angle * 4) * 0.3;
            double height = Math.sin(angle * 2) * 0.8;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;
            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        for (int i = 1; i <= 6; i++) {
            Vec3 dirPos = userPos.add(dashDirection.scale(i * 0.5));
            serverLevel.sendParticles(ParticleTypes.WITCH, dirPos.x, dirPos.y, dirPos.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void startDash() {
        user.setDeltaMovement(dashDirection.scale(dashSpeed));
        user.hurtMarked = true;
        user.hasImpulse = true;
        playInsectVfx(VfxIds.INSECT_BEE_STING,
                user.position().add(0, user.getBbHeight() * 0.5, 0), dashDirection, 1.0f);
        playDashSound();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 1.0f, 2.0f);
        createDashBurst();
    }

    private void continueDash() {
        Vec3 previousCenter = lastDashPos;
        Vec3 currentCenter = user.position().add(0, user.getBbHeight() / 2, 0);
        lastDashPos = currentCenter;

        user.setDeltaMovement(dashDirection.scale(dashSpeed));
        user.hurtMarked = true;
        createBeeTrail();
        trailEffectCounter++;

        List<LivingEntity> dashTargets = previousCenter != null
                ? getTargetsAlongPath(previousCenter, currentCenter, hitboxSize)
                : getTargetsInCustomHitbox(currentCenter, hitboxSize, 2.0, hitboxSize);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                hitTarget(target);
                hitEntities.add(target);
                draggedEnemies.add(target);
                createPierceImpactEffect(target.position());
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }

        dragHitEnemies(user.position());
    }

    /**
     * Drag every hit enemy along the dash — horizontal only, so they don't get launched into
     * the air by Y-axis math, and clamped to a sane speed so a far-away catch doesn't yank them
     * across the world in one tick. Stops applying drag if the user has effectively stopped
     * moving (e.g. the dash ended or got cut short) so they're not stuck eternally orbiting.
     */
    private void dragHitEnemies(Vec3 userFootPos) {
        // Anchor at the user's feet, slightly behind along the dash direction. Using foot Y
        // (not center) keeps the math at the same vertical level the mob already stands at.
        Vec3 horizontalDash = new Vec3(dashDirection.x, 0, dashDirection.z).normalize();
        Vec3 dragAnchor = userFootPos.subtract(horizontalDash.scale(1.2));

        Vec3 userVel = user.getDeltaMovement();
        double userSpeedSqr = userVel.x * userVel.x + userVel.z * userVel.z;
        boolean userMoving = userSpeedSqr > 0.04; // ~0.2 b/t threshold

        draggedEnemies.removeIf(enemy -> !enemy.isAlive() || enemy.isRemoved());
        for (LivingEntity dragged : draggedEnemies) {
            if (!userMoving) {
                // Stop dragging — let gravity take over so they don't keep coasting.
                Vec3 existing = dragged.getDeltaMovement();
                dragged.setDeltaMovement(0, Math.min(existing.y, 0), 0);
                dragged.hurtMarked = true;
                continue;
            }
            Vec3 toAnchor = dragAnchor.subtract(dragged.position());
            // Horizontal-only pull, capped per tick so a 6-block catch doesn't punt them.
            double vx = Math.max(-1.2, Math.min(1.2, toAnchor.x * 0.4));
            double vz = Math.max(-1.2, Math.min(1.2, toAnchor.z * 0.4));
            // Preserve gravity / negative Y so they fall normally, but don't ever push them up.
            double vy = Math.min(0, dragged.getDeltaMovement().y);
            dragged.setDeltaMovement(vx, vy, vz);
            dragged.hurtMarked = true;
            dragged.fallDistance = 0f;
        }
    }

    private List<LivingEntity> getTargetsAlongPath(Vec3 from, Vec3 to, float radius) {
        Vec3 path = to.subtract(from);
        double distance = path.length();
        if (distance < 0.01) return getTargetsInCustomHitbox(to, radius, 2.0, radius);

        Set<LivingEntity> found = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(distance / Math.max(radius * 0.25, 0.1)));
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = from.add(path.scale((double) i / steps));
            found.addAll(getTargetsInCustomHitbox(sample, radius, 2.0, radius));
        }
        return new ArrayList<>(found);
    }

    private void createDashBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        serverLevel.sendParticles(ParticleTypes.WITCH, userPos.x, userPos.y, userPos.z, 20, 0.8, 0.8, 0.8, 0.25);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, userPos.x, userPos.y, userPos.z, 10, 0.5, 0.5, 0.5, 0.15);
        serverLevel.sendParticles(ParticleTypes.PORTAL, userPos.x, userPos.y, userPos.z, 15, 0.6, 0.6, 0.6, 0.2);
    }

    private void createBeeTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), trailPos.x, trailPos.y, trailPos.z, 1, 0.3, 0.3, 0.3, 0.1);
            if ((i & 1) == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), trailPos.x, trailPos.y, trailPos.z, 1, 0.2, 0.2, 0.2, 0.08);
            }
        }

        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 0.8));
            serverLevel.sendParticles(ParticleTypes.WITCH, sidePos.x, sidePos.y, sidePos.z, 2, 0.15, 0.15, 0.15, 0.06);
        }

        if (trailEffectCounter % 5 == 0) {
            createButterflyFlutter(userPos);
        }
    }

    private void createPierceImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z, 8, 0.2, 0.2, 0.2, 0.1);
        serverLevel.sendParticles(ParticleTypes.WITCH, targetPos.x, targetPos.y, targetPos.z, 12, 0.3, 0.3, 0.3, 0.15);
        for (int i = 0; i < 4; i++) {
            double angle = (i / 4.0) * 2 * Math.PI;
            double x = targetPos.x + Math.cos(angle);
            double z = targetPos.z + Math.sin(angle);
            serverLevel.sendParticles(ParticleTypes.PORTAL, x, targetPos.y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public boolean isPiercingAttack() { return true; }

    @Override
    public boolean isDashAttack() { return true; }

    @Override
    protected void onStop() {
        // Release every dragged enemy: zero horizontal velocity so they don't go skidding
        // off when the dash ends, but keep any natural fall velocity intact.
        for (LivingEntity dragged : draggedEnemies) {
            if (dragged.isAlive()) {
                Vec3 existing = dragged.getDeltaMovement();
                dragged.setDeltaMovement(0, Math.min(existing.y, 0), 0);
                dragged.hurtMarked = true;
            }
        }
        user.setDeltaMovement(Vec3.ZERO);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            createButterflyFlutter(userPos);
            serverLevel.sendParticles(ParticleTypes.WITCH, userPos.x, userPos.y, userPos.z, 25, 1.0, 1.0, 1.0, 0.2);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, userPos.x, userPos.y, userPos.z, 15, 0.8, 0.8, 0.8, 0.15);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.6f, 1.0f);

        dashStarted = false;
        lastDashPos = null;
        hitEntities.clear();
        draggedEnemies.clear();
        trailEffectCounter = 0;
    }
}
