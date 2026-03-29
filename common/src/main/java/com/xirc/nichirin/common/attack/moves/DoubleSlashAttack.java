package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
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
    private final Map<LivingEntity, Integer> hitCount = new HashMap<>(); // Track how many times each entity has been hit
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

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Reset state
        tickCount = 0;
        hasHit = false;
        hitCooldowns.clear();
        hitCount.clear();
        firstSlashVisual = false;
        secondSlashVisual = false;
        isActive = true;

        // If startup == 0, first slash fires immediately (tick() increments before checking so tickCount==0 is unreachable)
        if (startup == 0) {
            createDiagonalSlashParticles(player, player.level(), true);
            firstSlashVisual = true;
        }

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

                // Track hit count for this entity
                int currentHitCount = hitCount.getOrDefault(target, 0) + 1;
                hitCount.put(target, currentHitCount);

                // Apply knockback ONLY on second hit (no knockback on first hit)
                if (knockback > 0 && currentHitCount >= 2) {
                    Vec3 knockVec = target.position().subtract(user.position()).normalize();
                    target.knockback(knockback, -knockVec.x, -knockVec.z);
                } else if (currentHitCount == 1) {
                }

                // Apply hit stun
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                }

                // Add hit cooldown - entity can be hit again after 6 ticks
                hitCooldowns.put(target, 6);
                hasHit = true;

                // Play hit sound
                if (hitSound != null) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
    }

    private void createDiagonalSlashParticles(Player user, Level world, boolean isFirstDiagonal) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 centre = user.position().add(0, user.getBbHeight() * 0.5, 0)
                .add(user.getLookAngle().scale(range * 0.6));
        Vec3 right = new Vec3(-user.getLookAngle().z, 0, user.getLookAngle().x).normalize();
        // First diagonal: top-right to bottom-left; second: top-left to bottom-right
        double sign = isFirstDiagonal ? 1.0 : -1.0;
        Vec3 start = centre.add(right.scale(sign * 1.2)).add(0, 0.5, 0);
        Vec3 end   = centre.add(right.scale(sign * -1.2)).add(0, -0.5, 0);
        for (double t = 0; t <= 1.0; t += 0.25) {
            Vec3 pos = start.add(end.subtract(start).scale(t));
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                centre.x, centre.y, centre.z, 2, 0.2, 0.2, 0.2, 0.0);
    }

    private void end(Player player) {
        isActive = false;
        hitCooldowns.clear();
        hitCount.clear();
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