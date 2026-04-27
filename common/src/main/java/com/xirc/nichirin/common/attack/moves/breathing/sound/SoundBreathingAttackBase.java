package com.xirc.nichirin.common.attack.moves.breathing.sound;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Base for Sound Breathing attacks. All hits stun, build a combo counter, and trigger shockwave explosions every 4th hit.
@SuppressWarnings("rawtypes")
public abstract class SoundBreathingAttackBase extends AbstractBreathingAttack<SoundBreathingAttackBase, IBreathingAttacker> {

    private static final int DEFAULT_STUN_DURATION = 10;
    private static final int SOUND_PARTICLE_COUNT = 8;
    private static final float SOUND_PARTICLE_SPREAD = 3.0f;
    private static final float COMBO_DAMAGE_BOOST = 0.05f;
    private static final float COMBO_SPEED_BOOST = 0.05f;

    private static final Map<UUID, Integer> playerComboCount = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final int COMBO_TIMEOUT = 100;

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        applyComboBonus();

        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (knockback > 0) {
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        if (!getHitEntities().contains(target.getUUID())) {
            getHitEntities().add(target.getUUID());
            setHitCount(getHitCount() + 1);
        }

        // Stun is applied after knockback so knockback velocity isn't immediately nullified
        Objects.requireNonNull(world.getServer()).execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, 2, false, true, true));
            }
        });

        incrementCombo();
        createSoundHitParticles(target.position());
        checkComboExplosion(target.position());
        playSoundHitEffects(target.position());
    }

    protected void hitTargetWithDelayedExplosion(LivingEntity target) {
        if (world.isClientSide) return;

        applyComboBonus();

        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (knockback > 0) {
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        if (!getHitEntities().contains(target.getUUID())) {
            getHitEntities().add(target.getUUID());
            setHitCount(getHitCount() + 1);
        }

        Objects.requireNonNull(world.getServer()).execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, 2, false, true, true));
            }
        });

        incrementCombo();
        checkComboExplosion(target.position());
        playSoundHitEffects(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        applyComboBonus();

        target.invulnerableTime = 0;
        target.hurtTime = 0;

        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (knockback > 0) {
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        Objects.requireNonNull(world.getServer()).execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, 2, false, true, true));
            }
        });

        incrementCombo();
        createSoundHitParticles(target.position());
        checkComboExplosion(target.position());
        playSoundHitEffects(target.position());
        setHitCount(getHitCount() + 1);
    }

    private void applyComboBonus() {
        if (user == null) return;
        int comboCount = getComboCount();
        if (comboCount > 0) {
            float damageMultiplier = 1.0f + Math.min(comboCount * COMBO_DAMAGE_BOOST, 1.0f);
            damage = damage * damageMultiplier;
            float speedMultiplier = 1.0f + Math.min(comboCount * COMBO_SPEED_BOOST, 0.5f);
            windup = Math.max(1, (int)(windup / speedMultiplier));
            duration = Math.max(1, (int)(duration / speedMultiplier));
        }
    }

    protected void applyStunEffect(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return;

        Objects.requireNonNull(world.getServer()).execute(() -> {
            if (target.isAlive()) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        DEFAULT_STUN_DURATION, 5, false, false));
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                        DEFAULT_STUN_DURATION, 5, false, false));
            }
        });
    }

    private void incrementCombo() {
        if (user == null) return;
        UUID playerId = user.getUUID();
        long currentTime = world.getGameTime();
        Long lastHit = lastHitTime.get(playerId);
        if (lastHit != null && currentTime - lastHit > COMBO_TIMEOUT) {
            playerComboCount.put(playerId, 0);
        }
        int currentCombo = playerComboCount.getOrDefault(playerId, 0);
        playerComboCount.put(playerId, currentCombo + 1);
        lastHitTime.put(playerId, currentTime);
    }

    private int getComboCount() {
        if (user == null) return 0;
        return playerComboCount.getOrDefault(user.getUUID(), 0);
    }

    private void checkComboExplosion(Vec3 position) {
        int comboCount = getComboCount();
        if (comboCount % 4 == 0 && comboCount > 0) {
            createComboExplosion(position);
        }
    }

    protected void applyDisorientedEffect(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return;
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.xirc.nichirin.registry.NichirinEffectRegistry.DISORIENTED.get(),
                30, 0, false, true, true));
    }

    protected void createSoundParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < SOUND_PARTICLE_COUNT; i++) {
            double offsetX = (random.nextDouble() - 0.5) * SOUND_PARTICLE_SPREAD;
            double offsetY = random.nextDouble() * SOUND_PARTICLE_SPREAD;
            double offsetZ = (random.nextDouble() - 0.5) * SOUND_PARTICLE_SPREAD;
            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            if (i % 6 == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 1) {
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 2) {
                serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 3) {
                serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 4) {
                serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 5) {
                serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            } else {
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    protected void createSoundHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                hitPosition.x, hitPosition.y + 1, hitPosition.z, 2, 1.0, 1.0, 1.0, 0.2);
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z, 2, 0.8, 0.8, 0.8, 0.1);
        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z, 2, 0.5, 0.5, 0.5, 0.1);
    }

    protected void createComboExplosion(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                center.x, center.y + 1, center.z, 6, 6.0, 3.0, 6.0, 0.3);
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                center.x, center.y + 1, center.z, 4, 5.0, 2.5, 5.0, 0.2);
        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                center.x, center.y + 1, center.z, 4, 4.0, 2.0, 4.0, 0.15);
        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                center.x, center.y + 1, center.z, 4, 4.0, 2.0, 4.0, 0.15);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 1, center.z, 1, 0.5, 0.5, 0.5, 0);

        java.util.List<LivingEntity> nearbyTargets = world.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center.subtract(3, 3, 3), center.add(3, 3, 3)),
                entity -> entity != user && entity.isAlive());

        for (LivingEntity target : nearbyTargets) {
            target.hurt(world.damageSources().explosion(null, user), damage * 0.3f);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    DEFAULT_STUN_DURATION, 5, false, false));
        }

        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    protected void createSoundTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        for (double d = 0; d <= distance; d += 0.3) {
            Vec3 particlePos = start.add(normalized.scale(d));
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    particlePos.x, particlePos.y, particlePos.z, 2, 0.1, 0.1, 0.1, 0.05);
            if (d % 1.0 < 0.1) {
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    protected void createSoundShockwave(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y;

            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    x, y, z, 3, 0.1, 0.1, 0.1, 0.1);
            if (i % 2 == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, y + 0.5, z, 1, 0.05, 0.1, 0.05, 0.05);
            }
        }
    }

    protected void playSoundEffects() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    protected void playSoundHitEffects(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }

    public static void resetCombo(UUID playerId) {
        playerComboCount.remove(playerId);
        lastHitTime.remove(playerId);
    }

    public boolean isShockwaveAttack() { return false; }
    public boolean isDashAttack() { return hasDash() || hasTeleport(); }
    public boolean isOmnidirectional() { return false; }
    public int getStunDuration() { return DEFAULT_STUN_DURATION; }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();

    @Override
    protected void onStop() {
        super.onStop();
    }
}
