package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

// Base for Insect Breathing attacks. All hits apply stackable venom; invulnerability is granted for the full attack duration.
@SuppressWarnings("rawtypes")
public abstract class InsectBreathingAttackBase extends AbstractBreathingAttack<InsectBreathingAttackBase, IBreathingAttacker> {

    private static final int DEFAULT_POISON_DURATION = 200;
    private static final int INSECT_PARTICLE_COUNT = 12;
    private static final float INSECT_PARTICLE_SPREAD = 1.0f;

    private int originalInvulnerableTime = 0;

    @Override
    protected void onStart() {
        originalInvulnerableTime = user.invulnerableTime;
        user.invulnerableTime = windup + duration;
        onStartInsectAttack();
    }

    @Override
    protected void onStop() {
        user.invulnerableTime = originalInvulnerableTime;
        onStopInsectAttack();
        super.onStop();
    }

    protected void onStartInsectAttack() {}

    protected void onStopInsectAttack() {}

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        applyPoisonEffect(target);
        createInsectHitParticles(target.position());
        playInsectHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        applyPoisonEffect(target);
        createInsectHitParticles(target.position());
        playInsectHitSound(target.position());
    }

    protected void applyPoisonEffect(LivingEntity target) {
        if (target instanceof Player player && player.isCreative()) {
            return;
        }

        Holder<MobEffect> venomEffect =
                NichirinEffectRegistry.venom();

        MobEffectInstance existingVenom = target.getEffect(venomEffect);

        int newAmplifier = 0;
        int baseDuration = Math.max(DEFAULT_POISON_DURATION, (int)(damage * 3));
        int newDuration = baseDuration;

        if (existingVenom != null) {
            newAmplifier = Math.min(existingVenom.getAmplifier() + 1, 99);
            newDuration = baseDuration;
        }

        target.addEffect(new MobEffectInstance(venomEffect, newDuration, newAmplifier, false, true));
    }

    protected void createInsectParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < INSECT_PARTICLE_COUNT; i++) {
            double offsetX = (random.nextDouble() - 0.5) * INSECT_PARTICLE_SPREAD;
            double offsetY = random.nextDouble() * INSECT_PARTICLE_SPREAD;
            double offsetZ = (random.nextDouble() - 0.5) * INSECT_PARTICLE_SPREAD;

            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.05);

            if (random.nextBoolean()) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        for (int i = 1; i <= 4; i++) {
            Vec3 trailPos = userPos.add(lookDir.scale(i * 0.6));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.1, 0.1, 0.1, 0.08);
        }
    }

    protected void createInsectHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.WITCH,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                15, 0.4, 0.4, 0.4, 0.2);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                10, 0.3, 0.3, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                8, 0.2, 0.2, 0.2, 0.1);
    }

    protected void createInsectTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        for (double d = 0; d <= distance; d += 0.4) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.08);

            if (d % 0.8 < 0.1) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.03);
            }
        }
    }

    protected void createInsectSwarm(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 1.5;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);

            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    protected void createPoisonBurst(Vec3 center, float intensity) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        RandomSource random = serverLevel.getRandom();
        int baseParticles = (int)(15 * intensity);

        serverLevel.sendParticles(ParticleTypes.WITCH,
                center.x, center.y + 1, center.z,
                baseParticles, 0.5, 0.5, 0.5, 0.2);

        for (int ring = 1; ring <= 2; ring++) {
            float ringRadius = ring * intensity;
            int ringParticles = baseParticles / ring;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                double y = center.y + random.nextDouble() * 2;

                serverLevel.sendParticles(ParticleTypes.WITCH,
                        x, y, z, 2, 0.2, 0.2, 0.2, 0.15);

                if (i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL,
                            x, y, z, 1, 0.1, 0.1, 0.1, 0.08);
                }
            }
        }

        serverLevel.sendParticles(ParticleTypes.WITCH,
                center.x, center.y + 1, center.z,
                (int)(25 * intensity), 0.8, 1.5, 0.8, 0.25);
    }

    protected void createButterflyFlutter(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 0.8 + Math.sin(angle * 3) * 0.2;

            double x = position.x + Math.cos(angle) * radius;
            double z = position.z + Math.sin(angle) * radius;
            double y = position.y + 0.5 + Math.sin(angle * 2) * 0.3;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    protected void playInsectSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    protected void playInsectHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    protected void playPoisonSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7f, 1.3f);
        }
    }

    protected void playDashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 2.0f);
        }
    }

    public boolean isPoisonAttack() { return true; }
    public boolean isPrecisionAttack() { return false; }
    public boolean isDashAttack() { return hasDash() || hasTeleport(); }
    public boolean isPiercingAttack() { return false; }
    public boolean hasInvincibilityFrames() { return true; }

    public int getPoisonDuration() {
        return Math.max(DEFAULT_POISON_DURATION, (int)(damage * 3));
    }

    public int getPoisonAmplifier() {
        return Math.min(2, Math.max(0, (int)(damage / 8.0f)));
    }

    @Override
    protected abstract void perform();
}