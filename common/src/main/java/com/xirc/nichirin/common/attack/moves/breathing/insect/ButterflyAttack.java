package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

// First Form: Dance of the Butterfly — Caprice. Leap upward then dash forward at the aimed direction for a precision venom thrust.
public class ButterflyAttack extends InsectBreathingAttackBase {

    private boolean dashStarted = false;
    private boolean secondDashExecuted = false;
    private Vec3 leapDirection;
    private Vec3 dashDirection;
    private Vec3 lastDashPos;
    private Vec3 startPosition;
    private boolean wasInvulnerable = false;

    @Override
    protected void onStart() {
        dashStarted = false;
        secondDashExecuted = false;
        startPosition = user.position();
        leapDirection = user.getLookAngle().normalize();
        dashDirection = null;
        lastDashPos = null;
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        playInsectSound();
        createButterflyFlutter(user.position().add(0, user.getBbHeight() / 2, 0));
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.5f);
        createLeapChargeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!dashStarted && tickCount == windup + 1) {
            executeInitialLeap();
            dashStarted = true;
        }

        if (dashStarted && !secondDashExecuted && tickCount == windup + 21) {
            // Capture aim direction right before the dash — not at onStart — so the player can steer the leap
            dashDirection = user.getLookAngle().normalize();
            executeForwardDash();
            secondDashExecuted = true;
            lastDashPos = user.position().add(0, user.getBbHeight() / 2, 0);
        }

        if (secondDashExecuted && tickCount > windup + 21 && tickCount <= windup + 35) {
            Vec3 currentDashPos = user.position().add(0, user.getBbHeight() / 2, 0);
            List<LivingEntity> dashTargets = lastDashPos != null
                    ? getTargetsAlongPath(lastDashPos, currentDashPos, 1.0f)
                    : getTargetsInCustomHitbox(currentDashPos, 2.0, 2.5, 2.0);
            lastDashPos = currentDashPos;

            for (LivingEntity dashTarget : dashTargets) {
                executeThrust(dashTarget);
            }
        }
    }

    private List<LivingEntity> getTargetsAlongPath(Vec3 from, Vec3 to, float radius) {
        Vec3 path = to.subtract(from);
        double distance = path.length();
        if (distance < 0.01) return getTargetsInCustomHitbox(to, radius * 2.0, 2.5, radius * 2.0);

        Set<LivingEntity> found = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(distance / Math.max(radius * 0.25, 0.1)));
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = from.add(path.scale((double) i / steps));
            found.addAll(getTargetsInCustomHitbox(sample, radius * 2.0, 2.5, radius * 2.0));
        }
        return new ArrayList<>(found);
    }

    private void createLeapChargeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            double radius = 2.0 + Math.sin(angle * 3) * 0.5;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.5;
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        for (int i = 1; i <= 3; i++) {
            Vec3 leapPreview = userPos.add(leapDirection.scale(i * 0.3)).add(0, i * 0.4, 0);
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    leapPreview.x, leapPreview.y, leapPreview.z, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void executeInitialLeap() {
        Vec3 horizontalDirection = new Vec3(leapDirection.x, 0, leapDirection.z).normalize();
        Vec3 forwardComponent = horizontalDirection.scale(0.1);
        user.setDeltaMovement(forwardComponent.x, 0.8, forwardComponent.z);
        user.hurtMarked = true;
        user.hasImpulse = true;

        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(user));
        }

        playDashSound();
    }

    private void executeForwardDash() {
        float actualDashSpeed = (dashSpeed != null) ? dashSpeed : 3.0f;
        user.setDeltaMovement(dashDirection.scale(actualDashSpeed));
        user.hurtMarked = true;
        user.hasImpulse = true;

        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(user));
        }

        createDashEffect();
        createButterflyTrail(startPosition, user.position());
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    private void executeThrust(LivingEntity thrustTarget) {
        hitTarget(thrustTarget);

        Vec3 thrustDirection = thrustTarget.position().subtract(user.position()).normalize();
        thrustTarget.push(thrustDirection.x * knockback, 0.3, thrustDirection.z * knockback);

        createThrustImpactEffect(thrustTarget);
        world.playSound(null, thrustTarget.getX(), thrustTarget.getY(), thrustTarget.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.8f);
        playPoisonSound(thrustTarget.position());
    }

    private void createDashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.5;
            double speed = 0.3 + random.nextDouble() * 0.2;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + random.nextDouble() * 2;
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), x, y, z, 1, speed, speed, speed, 0.1);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        serverLevel.sendParticles(ParticleTypes.ENCHANT, userPos.x, userPos.y, userPos.z, 25, 1.0, 1.0, 1.0, 0.2);
    }

    private void createButterflyTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        for (double d = 0; d <= distance; d += 0.4) {
            Vec3 particlePos = start.add(normalized.scale(d));
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), particlePos.x, particlePos.y, particlePos.z, 2, 0.3, 0.3, 0.3, 0.1);
            if (d % 0.8 < 0.1) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, particlePos.x, particlePos.y, particlePos.z, 1, 0.05, 0.05, 0.05, 0.03);
            }
        }
    }

    private void createThrustImpactEffect(LivingEntity thrustTarget) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 targetPos = thrustTarget.position().add(0, thrustTarget.getBbHeight() / 2, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z, 15, 0.3, 0.3, 0.3, 0.2);
        createPoisonBurst(targetPos, 1.5f);
        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), targetPos.x, targetPos.y, targetPos.z, 20, 0.5, 0.5, 0.5, 0.25);

        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double x = targetPos.x + Math.cos(angle) * 2.0;
            double z = targetPos.z + Math.sin(angle) * 2.0;
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), x, targetPos.y + 1, z, 2, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public boolean isPrecisionAttack() { return true; }

    @Override
    public boolean isDashAttack() { return true; }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            createButterflyFlutter(userPos);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, userPos.x, userPos.y, userPos.z, 15, 0.8, 0.8, 0.8, 0.15);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 2.0f);

        dashStarted = false;
        secondDashExecuted = false;
        lastDashPos = null;
    }
}
