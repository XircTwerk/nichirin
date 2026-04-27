package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Second Form: Dance of the Bee Sting. Dash through enemies in a straight line, piercing each with light venom stabs.
public class BeeStingAttack extends InsectBreathingAttackBase {

    private static final int DASH_DURATION = 13;

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private int trailEffectCounter = 0;

    @Override
    protected void onStart() {
        dashStarted = false;
        hitEntities.clear();
        trailEffectCounter = 0;
        dashDirection = user.getLookAngle().normalize();

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
        playDashSound();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 1.0f, 2.0f);
        createDashBurst();
    }

    private void continueDash() {
        user.setDeltaMovement(dashDirection.scale(dashSpeed));
        user.hurtMarked = true;
        createBeeTrail();
        trailEffectCounter++;

        List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize, 2.0, hitboxSize);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                hitTarget(target);
                hitEntities.add(target);
                target.push(dashDirection.x * knockback * 0.5, 0.1, dashDirection.z * knockback * 0.5);
                createPierceImpactEffect(target.position());
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
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
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), trailPos.x, trailPos.y, trailPos.z, 2, 0.3, 0.3, 0.3, 0.1);
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(), trailPos.x, trailPos.y, trailPos.z, 1, 0.2, 0.2, 0.2, 0.08);
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
        hitEntities.clear();
        trailEffectCounter = 0;
    }
}
