package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

// Eleventh Form: Dead Calm. Establishes a persistent field that auto-slashes all enemies who enter or remain in range.
public class DeadCalmAttack extends WaterBreathingAttackBase {

    private boolean fieldActive = false;
    private Vec3 fieldCenter;
    private Vec3 frozenPos;
    private final Set<LivingEntity> entitiesInField = new HashSet<>();
    private final Set<LivingEntity> recentlyTriggered = new HashSet<>();
    private int calmTicks = 0;

    @Override
    protected void onStart() {
        fieldActive = false;
        fieldCenter = null;
        entitiesInField.clear();
        recentlyTriggered.clear();
        calmTicks = 0;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.5f);

        createCalmWaterGathering();

        if (!world.isClientSide) {
            sendShaderPacketToNearbyPlayers();
        }
    }

    private void sendShaderPacketToNearbyPlayers() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(userPos) < 10000) {
                NichirinPacketRegistry.sendToPlayer(
                        new TriggerShaderPacket(
                                "com.xirc.nichirin.client.shader.DeadCalmShaderEffect",
                                true
                        ),
                        player
                );
            }
        }
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!fieldActive && tickCount == windup + 1) {
            establishCalmField();
            fieldActive = true;
        }

        if (fieldActive && tickCount > windup && tickCount < windup + duration) {
            calmTicks++;
            maintainCalmField();
        }
    }

    private void createCalmWaterGathering() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int ring = 1; ring <= 12; ring++) {
            float radius = ring * 1.2f;
            int particlesInRing = 8 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = userPos.x + Math.cos(angle) * radius;
                double z = userPos.z + Math.sin(angle) * radius;
                double y = userPos.y + Math.sin(angle * 4) * 0.1;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.01);

                if (ring <= 2 && i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }
        }
    }

    private void establishCalmField() {
        frozenPos = user.position();

        Vec3 lookDir = user.getLookAngle();
        fieldCenter = user.position().add(lookDir.scale(2.0));

        createFieldEstablishmentEffect();

        world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    private void maintainCalmField() {
        if (fieldCenter == null || frozenPos == null) return;

        // Anchor the user in place â€” teleport overrides client-side prediction
        if (user instanceof ServerPlayer sp) {
            sp.teleportTo(frozenPos.x, frozenPos.y, frozenPos.z);
        } else {
            user.absMoveTo(frozenPos.x, frozenPos.y, frozenPos.z, user.getYRot(), user.getXRot());
        }
        user.setDeltaMovement(Vec3.ZERO);

        createPersistentFieldEffect();
        updateEntitiesInField();
        triggerAutoSlashes();

        if (calmTicks % 40 == 0) {
            world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.4f, 0.7f);
        }
    }

    private void updateEntitiesInField() {
        AABB fieldArea = new AABB(
                fieldCenter.x - range, fieldCenter.y - 2, fieldCenter.z - range,
                fieldCenter.x + range, fieldCenter.y + 4, fieldCenter.z + range
        );

        List<LivingEntity> currentEntities = world.getEntitiesOfClass(LivingEntity.class, fieldArea,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());

        Set<LivingEntity> newEntrants = new HashSet<>(currentEntities);
        newEntrants.removeAll(entitiesInField);

        for (LivingEntity newEntrant : newEntrants) {
            if (!recentlyTriggered.contains(newEntrant)) {
                triggerSlashSequence(newEntrant);
                recentlyTriggered.add(newEntrant);
            }
        }

        entitiesInField.clear();
        entitiesInField.addAll(currentEntities);

        if (calmTicks % 10 == 0) {
            recentlyTriggered.clear();
        }
    }

    private void triggerAutoSlashes() {
        if (calmTicks % 2 == 0) {
            for (LivingEntity entity : entitiesInField) {
                if (!recentlyTriggered.contains(entity)) {
                    triggerSlashSequence(entity);
                    recentlyTriggered.add(entity);
                }
            }
        }
    }

    private void triggerSlashSequence(LivingEntity target) {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

        for (int slash = 0; slash < 3; slash++) {
            scheduleSlash(target, targetPos, slash * 4);
        }

        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private void scheduleSlash(LivingEntity target, Vec3 targetPos, int delay) {
        hitTargetNoImmunity(target);

        createAutoSlashEffect(targetPos, delay);

        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6f, 1.5f + delay * 0.1f);
    }

    private void createFieldEstablishmentEffect() {
        if (!(world instanceof ServerLevel serverLevel) || fieldCenter == null) return;

        for (int ring = 1; ring <= 5; ring++) {
            float ringRadius = ring * (range / 5);
            int particlesInRing = 12 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = fieldCenter.x + Math.cos(angle) * ringRadius;
                double z = fieldCenter.z + Math.sin(angle) * ringRadius;
                double y = fieldCenter.y + 0.5;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                if (ring == 5) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                fieldCenter.x, fieldCenter.y + 1, fieldCenter.z,
                20, 0.5, 2.0, 0.5, 0.2);
    }

    private void createPersistentFieldEffect() {
        if (!(world instanceof ServerLevel serverLevel) || fieldCenter == null) return;

        int boundaryParticles = 24;
        for (int i = 0; i < boundaryParticles; i++) {
            double angle = (2 * Math.PI * i) / boundaryParticles + calmTicks * 0.02;
            double x = fieldCenter.x + Math.cos(angle) * range * 0.9;
            double z = fieldCenter.z + Math.sin(angle) * range * 0.9;
            double y = fieldCenter.y + Math.sin(angle * 3) * 0.2;

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
        }

        for (int i = 0; i < 6; i++) {
            double x = fieldCenter.x + (Math.random() - 0.5) * range * 1.5;
            double z = fieldCenter.z + (Math.random() - 0.5) * range * 1.5;
            double y = fieldCenter.y;

            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                fieldCenter.x, fieldCenter.y + 0.5, fieldCenter.z,
                4, 0.3, 0.3, 0.3, 0.08);
    }

    private void createAutoSlashEffect(Vec3 slashPos, int slashIndex) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int i = -2; i <= 2; i++) {
            double angle = i * 30 + slashIndex * 60;
            double radians = Math.toRadians(angle);

            Vec3 slashDir = new Vec3(Math.cos(radians), 0, Math.sin(radians));
            Vec3 particlePos = slashPos.add(slashDir.scale(1.5));

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    4, 0.3, 0.3, 0.3, 0.15);

            if (Math.abs(i) <= 1) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.15, 0.15, 0.15, 0.1);
            }
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                slashPos.x, slashPos.y, slashPos.z,
                8, 0.4, 0.4, 0.4, 0.2);
    }

    @Override
    public boolean isPersistentArea() {
        return true;
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel serverLevel && fieldCenter != null) {
            for (int ring = 5; ring >= 1; ring--) {
                float ringRadius = ring * (range / 5);
                int particlesInRing = 8 * ring;

                for (int i = 0; i < particlesInRing; i++) {
                    double angle = (2 * Math.PI * i) / particlesInRing;
                    double x = fieldCenter.x + Math.cos(angle) * ringRadius;
                    double z = fieldCenter.z + Math.sin(angle) * ringRadius;
                    double y = fieldCenter.y + 0.5;

                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 3, 0.3, 0.3, 0.3, 0.15);
                }
            }

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    fieldCenter.x, fieldCenter.y + 1, fieldCenter.z,
                    30, 1.0, 2.0, 1.0, 0.3);
        }

        world.playSound(null, fieldCenter != null ? fieldCenter.x : user.getX(),
                fieldCenter != null ? fieldCenter.y : user.getY(),
                fieldCenter != null ? fieldCenter.z : user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);

        entitiesInField.clear();
        recentlyTriggered.clear();
        fieldActive = false;
        fieldCenter = null;
        calmTicks = 0;
    }
}
