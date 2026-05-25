package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

// Base for Water Breathing attacks. All hits apply a slowness effect representing water pressure.
@SuppressWarnings("rawtypes")
public abstract class WaterBreathingAttackBase extends AbstractBreathingAttack<WaterBreathingAttackBase, IBreathingAttacker> {

    private static final int WATER_PARTICLE_COUNT = 20;
    private static final float WATER_PARTICLE_SPREAD = 1.2f;

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        float savedDamage = damage;
        if (user instanceof BaseBreathingTrainerEntity trainer) damage *= trainer.getDifficultyDamageMultiplier();
        super.hitTarget(target);
        damage = savedDamage;
        applyWaterEffect(target);
        createWaterHitParticles(target.position());
        playWaterHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        float savedDamage = damage;
        if (user instanceof BaseBreathingTrainerEntity trainer) damage *= trainer.getDifficultyDamageMultiplier();
        super.hitTargetNoImmunity(target);
        damage = savedDamage;
        applyWaterEffect(target);
        createWaterHitParticles(target.position());
        playWaterHitSound(target.position());
    }

    protected void applyWaterEffect(LivingEntity target) {
        if (target instanceof Player player && player.isCreative()) return;
        int slownessDurationTicks = Math.max(40, (int)(damage * 2));
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                slownessDurationTicks, 0, false, true));
    }

    protected void createWaterHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                25, 0.6, 0.6, 0.6, 0.3);
        serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                12, 0.4, 0.4, 0.4, 0.1);
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                8, 0.3, 0.3, 0.3, 0.1);
    }

    protected void createWaterTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        for (double d = 0; d <= distance; d += 0.5) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    3, 0.2, 0.2, 0.2, 0.1);

            if (d % 1.0 < 0.1) {
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    protected void createWaterCircle(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    protected void createWaterVortex(Vec3 center, float radius, float height, int layers) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int layer = 0; layer < layers; layer++) {
            float layerHeight = (height / layers) * layer;
            float layerRadius = radius * (1.0f - (float)layer / layers * 0.3f);
            int particlesInLayer = Math.max(8, 16 - layer * 2);

            for (int i = 0; i < particlesInLayer; i++) {
                double baseAngle = (2 * Math.PI * i) / particlesInLayer;
                double spiralOffset = layer * 0.8;
                double angle = baseAngle + spiralOffset;

                double x = center.x + Math.cos(angle) * layerRadius;
                double z = center.z + Math.sin(angle) * layerRadius;
                double y = center.y + layerHeight;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                if (layer < layers / 2) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                center.x, center.y, center.z, 12, 0.2, height * 0.5, 0.2, 0.2);
    }

    protected void createWaterExplosion(Vec3 center, float intensity) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        RandomSource random = serverLevel.getRandom();
        int baseParticles = (int)(30 * intensity);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);

        for (int ring = 1; ring <= 3; ring++) {
            float ringRadius = ring * intensity;
            int ringParticles = baseParticles / ring;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                double y = center.y + random.nextDouble() * 3;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.2);
            }
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                center.x, center.y + 1, center.z,
                (int)(60 * intensity), 1.5, 3.0, 1.5, 0.4);

        for (int i = 0; i < (int)(40 * intensity); i++) {
            double x = center.x + (random.nextDouble() - 0.5) * intensity * 4;
            double z = center.z + (random.nextDouble() - 0.5) * intensity * 4;
            double y = center.y + 2 + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 1, 0, -0.5, 0, 0.1);
        }
    }

    protected void createWaterfall(Vec3 start, float width, float height, int streams) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int stream = 0; stream < streams; stream++) {
            double streamOffset = (stream - streams / 2.0) * (width / streams);
            Vec3 streamStart = start.add(streamOffset, height, 0);

            for (int h = 0; h <= (int)height; h++) {
                Vec3 particlePos = streamStart.add(0, -h, 0);

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0, 0.1, 0);

                if (h % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            particlePos.x, particlePos.y, particlePos.z,
                            4, 0.3, 0.2, 0.3, 0.1);
                }
            }

            Vec3 poolCenter = start.add(streamOffset, 0, 0);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    poolCenter.x, poolCenter.y, poolCenter.z,
                    8, width * 0.2, 0.1, width * 0.2, 0.2);
        }
    }

    protected void playWaterSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    protected void playWaterHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    protected void playWaterSlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.1f);
        }
    }

    protected void playWaterFlowSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    protected void playWaterExplosionSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 0.9f);
        }
    }

    public boolean isWhirlpoolAttack() { return false; }
    public boolean isDashAttack() { return hasDash() || hasTeleport(); }
    public boolean isOmnidirectional() { return false; }
    public boolean isPersistentArea() { return false; }
    public boolean hasDefensiveProperties() { return false; }

    public int getPressureDuration() {
        return Math.max(40, (int)(damage * 2));
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();

    @Override
    protected void onStop() {
        super.onStop();
    }
}
