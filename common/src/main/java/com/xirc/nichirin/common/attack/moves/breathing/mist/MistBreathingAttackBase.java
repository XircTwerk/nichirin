package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Base for all Mist Breathing attacks. Provides shared particle helpers and hit overrides.
@SuppressWarnings("rawtypes")
public abstract class MistBreathingAttackBase extends AbstractBreathingAttack<MistBreathingAttackBase, IBreathingAttacker> {

    protected void createMistParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        var random = serverLevel.getRandom();

        for (int i = 0; i < 12; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 1.5;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        for (int i = 1; i <= 4; i++) {
            Vec3 trailPos = userPos.add(lookDir.scale(i * 0.8));
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.15, 0.15, 0.15, 0.02);
        }
    }

    protected void createMistHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.CLOUD,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                10, 0.4, 0.4, 0.4, 0.05);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                6, 0.3, 0.3, 0.3, 0.15);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                8, 0.3, 0.3, 0.3, 0.05);
    }

    protected void createMistTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for (double d = 0; d <= distance; d += 0.4) {
            Vec3 particlePos = start.add(direction.scale(d));
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.02);
            if (d % 0.8 < 0.1) {
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    protected void createMistCircle(Vec3 center, float radius, int count) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        var random = serverLevel.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 1.5;

            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    protected void createWaterTrailParticles(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                position.x, position.y + 1, position.z, 4, 0.3, 0.3, 0.3, 0.05);
        serverLevel.sendParticles(ParticleTypes.UNDERWATER,
                position.x, position.y + 0.5, position.z, 3, 0.2, 0.2, 0.2, 0.02);
    }

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        createMistHitParticles(target.position());
        playMistHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        createMistHitParticles(target.position());
        playMistHitSound(target.position());
    }

    protected void applyMistBlur(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.blurry(), durationTicks, 0, false, false, false));
        if (target instanceof ServerPlayer player) triggerMistBlur(player);
    }

    private void triggerMistBlur(ServerPlayer player) {
        NichirinPacketRegistry.sendToPlayer(
                new TriggerShaderPacket("com.xirc.nichirin.client.shader.MistBlurShaderEffect", true, 0.85f),
                player);
    }

    protected void playMistSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
    }

    protected void playMistHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5f, 1.6f);
        }
    }

    protected Vec3 rotateDirection(Vec3 direction, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
                direction.x * cos - direction.z * sin,
                direction.y,
                direction.x * sin + direction.z * cos
        ).normalize();
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();
}
