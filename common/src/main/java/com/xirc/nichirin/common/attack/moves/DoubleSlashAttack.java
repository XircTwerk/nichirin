package com.xirc.nichirin.common.attack.moves;

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

import java.util.*;

/**
 * Double slash attack that creates an X pattern with two diagonal slashes
 */
public class DoubleSlashAttack {

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
    private final int slashDelay; // Delay between first and second slash visual
    private final SoundEvent startSound;
    private final SoundEvent hitSound;

    // State
    private int tickCount = 0;
    private boolean isActive = false;
    private boolean hasHit = false;
    private final Map<LivingEntity, Integer> hitCooldowns = new HashMap<>(); // Track when entities can be hit again
    private boolean firstSlashVisual = false;
    private boolean secondSlashVisual = false;

    public DoubleSlashAttack(int startup, int active, int recovery, int cooldown, float damage, float range,
                             float knockback, float hitboxSize, Vec3 hitboxOffset, int hitStun, int slashDelay,
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
        this.slashDelay = slashDelay;
        this.startSound = startSound;
        this.hitSound = hitSound;
    }

    /**
     * Builder for easier creation
     */
    public static class Builder {
        private int startup = 4;
        private int active = 16; // Active frames for the entire attack
        private int recovery = 6;
        private int cooldown = 20;
        private float damage = 3.5f; // Damage per hit (targets can be hit twice)
        private float range = 2.8f;
        private float knockback = 0.4f;
        private float hitboxSize = 1.6f;
        private Vec3 hitboxOffset = Vec3.ZERO;
        private int hitStun = 12;
        private int slashDelay = 2; // 2 tick delay between visual slashes
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

        public Builder withSlashDelay(int delay) {
            this.slashDelay = delay;
            return this;
        }

        public Builder withSounds(SoundEvent start, SoundEvent hit) {
            this.startSound = start;
            this.hitSound = hit;
            return this;
        }

        public DoubleSlashAttack build() {
            return new DoubleSlashAttack(startup, active, recovery, cooldown, damage, range, knockback,
                    hitboxSize, hitboxOffset, hitStun, slashDelay, startSound, hitSound);
        }
    }

    public void start(Player player) {
        System.out.println("DEBUG: DoubleSlashAttack start called");

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Reset state
        tickCount = 0;
        hasHit = false;
        hitCooldowns.clear();
        firstSlashVisual = false;
        secondSlashVisual = false;
        isActive = true;

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

        // Update hit cooldowns
        hitCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

        // First slash visual
        if (tickCount == startup && !firstSlashVisual) {
            // Create first diagonal slash particles (top-left to bottom-right)
            createDiagonalSlashParticles(player, player.level(), true);
            firstSlashVisual = true;
        }

        // Second slash visual (2 ticks after first)
        if (tickCount == startup + slashDelay && !secondSlashVisual) {
            // Create second diagonal slash particles (top-right to bottom-left)
            createDiagonalSlashParticles(player, player.level(), false);
            secondSlashVisual = true;

            // Play sound again for second slash
            if (startSound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        startSound, SoundSource.PLAYERS, 1.0f, 1.1f); // Slightly higher pitch
            }
        }

        // Hit detection throughout active frames (single persistent hitbox)
        if (tickCount >= startup && tickCount <= startup + active) {
            performHitDetection(player, player.level());
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

        // Find targets - a single target can be hit multiple times during the active window
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitCooldowns.containsKey(entity));

        if (!targets.isEmpty()) {
            DamageSource damageSource = user.damageSources().playerAttack(user);

            for (LivingEntity target : targets) {
                // Deal damage
                target.hurt(damageSource, damage);

                // Apply knockback - alternating based on which visual phase we're in
                if (knockback > 0) {
                    Vec3 knockVec = target.position().subtract(user.position()).normalize();
                    target.knockback(knockback, -knockVec.x, -knockVec.z);
                }

                // Apply hit stun
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                }

                // Add hit cooldown - entity can be hit again after 6 ticks
                hitCooldowns.put(target, 6);
                hasHit = true;

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

                System.out.println("DEBUG: Double slash hit " + target.getName().getString() + " for " + damage + " damage");
            }
        }
    }

    private void createDiagonalSlashParticles(Player user, Level world, boolean isFirstDiagonal) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        System.out.println("DEBUG: Creating " + (isFirstDiagonal ? "first" : "second") + " diagonal slash particles");

        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.75, 0);
        Vec3 lookDir = user.getLookAngle();

        // Position the X in front of the player
        Vec3 center = userPos.add(lookDir.scale(1.5));

        float size = 1.5f; // Size of the X

        if (isFirstDiagonal) {
            // First diagonal: \ (top-left to bottom-right)
            // Create particles along the \ line
            for (int i = 0; i <= 5; i++) {
                float t = i / 5.0f;
                Vec3 pos = center.add(
                        -size/2 + size * t,  // X: left to right
                        size/2 - size * t,   // Y: top to bottom
                        0                    // Z: straight ahead
                );

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0);
            }
        } else {
            // Second diagonal: / (top-right to bottom-left)
            // Create particles along the / line
            for (int i = 0; i <= 5; i++) {
                float t = i / 5.0f;
                Vec3 pos = center.add(
                        size/2 - size * t,   // X: right to left
                        size/2 - size * t,   // Y: top to bottom
                        0                    // Z: straight ahead
                );

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0);
            }
        }
    }

    private void end(Player player) {
        System.out.println("DEBUG: DoubleSlashAttack ended");
        isActive = false;
        hitCooldowns.clear();
    }

    public boolean isActive() {
        return isActive;
    }

    public int getTotalDuration() {
        return startup + active + recovery;
    }

    public int getCooldown() {
        return cooldown;
    }
}