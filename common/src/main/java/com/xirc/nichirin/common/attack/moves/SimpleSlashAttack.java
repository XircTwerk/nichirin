package com.xirc.nichirin.common.attack.moves;

import lombok.Getter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple slash attack that doesn't depend on the complex attack system
 */
public class SimpleSlashAttack {

    // Configuration
    private final int startup;
    private final int active;
    private final int recovery;
    private final int cooldown;
    private final float damage;
    private final float range;
    private final float knockback;
    private final float hitboxSize;
    private final Vec3 hitboxOffset;
    private final int hitStun;
    private final SoundEvent startSound;
    private final SoundEvent hitSound;

    // State
    private int tickCount = 0;
    @Getter
    private boolean isActive = false;
    private boolean hasHit = false;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public SimpleSlashAttack(int startup, int active, int recovery, int cooldown, float damage, float range,
                             float knockback, float hitboxSize, Vec3 hitboxOffset, int hitStun,
                             SoundEvent startSound, SoundEvent hitSound) {
        this.startup = startup;
        this.active = active;
        this.recovery = recovery;
        this.cooldown = cooldown;
        this.damage = damage;
        this.range = range;
        this.knockback = knockback;
        this.hitboxSize = hitboxSize;
        this.hitboxOffset = hitboxOffset;
        this.hitStun = hitStun;
        this.startSound = startSound;
        this.hitSound = hitSound;
    }

    /**
     * Builder for easier creation
     */
    public static class Builder {
        private int startup = 3;
        private int active = 13;
        private int recovery = 4;
        private int cooldown = 10;
        private float damage = 4.0f;
        private float range = 2.5f;
        private float knockback = 0.3f;
        private float hitboxSize = 1.5f;
        private Vec3 hitboxOffset = Vec3.ZERO;
        private int hitStun = 15;
        private SoundEvent startSound = null;
        private SoundEvent hitSound = null;

        public Builder withTiming(int startup, int active, int recovery) {
            this.startup = startup;
            this.active = active;
            this.recovery = recovery;
            return this;
        }

        public Builder withCooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder withDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder withRange(float range) {
            this.range = range;
            return this;
        }

        public Builder withKnockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public Builder withHitbox(float size, Vec3 offset) {
            this.hitboxSize = size;
            this.hitboxOffset = offset;
            return this;
        }

        public Builder withHitStun(int hitStun) {
            this.hitStun = hitStun;
            return this;
        }

        public Builder withSounds(SoundEvent start, SoundEvent hit) {
            this.startSound = start;
            this.hitSound = hit;
            return this;
        }

        public SimpleSlashAttack build() {
            return new SimpleSlashAttack(startup, active, recovery, cooldown, damage, range, knockback,
                    hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public void start(Player player) {

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Reset state
        tickCount = 0;
        hasHit = false;
        hitEntities.clear();
        isActive = true;

        // Create slash particles
        createSlashParticles(player, player.level());

        // Play start sound
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public void tick(Player player) {
        if (!isActive) return;

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        tickCount++;

        // Check if we're in the active frames
        if (tickCount >= startup && tickCount <= startup + active) {
            // Perform hit detection
            if (!hasHit) {
                performHitDetection(player, player.level());
            }
        }

        // Check if attack is complete
        if (tickCount >= getTotalDuration()) {
            end(player);
        }
    }

    private void performHitDetection(Player user, Level world) {

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 hitboxCenter = userPos.add(lookDir.scale(range)).add(hitboxOffset);

        // Create hitbox
        AABB hitbox = new AABB(
                hitboxCenter.x - hitboxSize,
                hitboxCenter.y - hitboxSize,
                hitboxCenter.z - hitboxSize,
                hitboxCenter.x + hitboxSize,
                hitboxCenter.y + hitboxSize,
                hitboxCenter.z + hitboxSize
        );

        // Find targets
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        if (!targets.isEmpty()) {
            hasHit = true;
            DamageSource damageSource = user.damageSources().playerAttack(user);

            for (LivingEntity target : targets) {
                // Deal damage
                target.hurt(damageSource, damage);

                // Apply knockback
                if (knockback > 0) {
                    Vec3 knockVec = target.position().subtract(user.position()).normalize();
                    target.knockback(knockback, -knockVec.x, -knockVec.z);
                }

                // Apply hit stun
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                }

                // Add to hit list
                hitEntities.add(target);

                // Create hit particles
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                            10, 0.2, 0.2, 0.2, 0.1);
                }

                // Play hit sound
                if (hitSound != null) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
                }

            }
        }
    }

    private void createSlashParticles(Player user, Level world) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        double radius = range;
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.75, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create arc of particles
        for (int i = -30; i <= 30; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 offset = lookDir.yRot((float)angle).scale(radius);
            Vec3 particlePos = userPos.add(offset);

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0);
        }
    }

    private void end(Player player) {
        isActive = false;
        hitEntities.clear();
    }

    public int getTotalDuration() {
        return startup + active + recovery;
    }

    public int getCooldown() {
        return cooldown;
    }
}