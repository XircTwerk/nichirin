package com.xirc.nichirin.common.attack.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Rising slash attack that launches enemies into the air
 */
public class RisingSlashAttack {

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
    private final float launchPower; // How high to launch enemies
    private final SoundEvent startSound;
    private final SoundEvent hitSound;

    // State
    private int tickCount = 0;
    private boolean isActive = false;
    private boolean hasHit = false;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public RisingSlashAttack(int startup, int active, int recovery, int cooldown, float damage, float range,
                             float knockback, float hitboxSize, Vec3 hitboxOffset, int hitStun, float launchPower,
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
        this.launchPower = launchPower;
        this.startSound = startSound;
        this.hitSound = hitSound;
    }

    /**
     * Builder for easier creation
     */
    public static class Builder {
        private int startup = 5;
        private int active = 10;
        private int recovery = 8;
        private int cooldown = 25;
        private float damage = 6.0f;
        private float range = 2.5f;
        private float knockback = 0.2f;
        private float hitboxSize = 1.5f;
        private Vec3 hitboxOffset = new Vec3(0, 0.5, 0); // Slightly higher hitbox
        private int hitStun = 20;
        private float launchPower = 1.2f; // Default launch power (blocks)
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

        public Builder withLaunchPower(float launchPower) {
            this.launchPower = launchPower;
            return this;
        }

        public Builder withSounds(SoundEvent start, SoundEvent hit) {
            this.startSound = start;
            this.hitSound = hit;
            return this;
        }

        public RisingSlashAttack build() {
            return new RisingSlashAttack(startup, active, recovery, cooldown, damage, range, knockback,
                    hitboxSize, hitboxOffset, hitStun, launchPower, startSound, hitSound);
        }
    }

    public void start(Player player) {
        System.out.println("DEBUG: RisingSlashAttack start called");

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Reset state
        tickCount = 0;
        hasHit = false;
        hitEntities.clear();
        isActive = true;

        // Create rising particles
        createRisingParticles(player, player.level());

        // Play start sound
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 0.8f); // Lower pitch for power
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
        System.out.println("DEBUG: Performing rising slash hit detection");

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

        System.out.println("DEBUG: Hitbox center: " + hitboxCenter);
        System.out.println("DEBUG: Launch power: " + launchPower);

        // Find targets
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        System.out.println("DEBUG: Found " + targets.size() + " potential targets");

        if (!targets.isEmpty()) {
            hasHit = true;
            DamageSource damageSource = user.damageSources().playerAttack(user);

            for (LivingEntity target : targets) {
                // Launch the target
                launchTarget(target, user, damageSource);

                // Add to hit list
                hitEntities.add(target);

                // Create hit particles - upward stream
                if (world instanceof ServerLevel serverLevel) {
                    createHitParticles(serverLevel, target);
                }

                // Play hit sound
                if (hitSound != null) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            hitSound, SoundSource.PLAYERS, 1.0f, 0.8f);
                }
            }
        }
    }

    private void launchTarget(LivingEntity target, Player user, DamageSource damageSource) {
        // Debug current state
        System.out.println("DEBUG: Target state before launch - onGround: " + target.onGround()
                + ", inWater: " + target.isInWater()
                + ", noGravity: " + target.isNoGravity()
                + ", current Y velocity: " + target.getDeltaMovement().y);

        // Clear any existing velocity
        target.setDeltaMovement(Vec3.ZERO);

        // Lift slightly off ground to ensure launch works
        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
            System.out.println("DEBUG: Lifted target off ground");
        }

        // Calculate launch velocity
        Vec3 launchVelocity = new Vec3(0, launchPower, 0);

        // Add horizontal knockback if needed
        if (knockback > 0) {
            Vec3 knockDirection = target.position().subtract(user.position()).normalize();
            launchVelocity = launchVelocity.add(
                    knockDirection.x * knockback,
                    0,
                    knockDirection.z * knockback
            );
        }

        // Apply the velocity
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true; // Forces velocity sync
        target.hasImpulse = true; // Ensures the velocity is applied

        // Force sync for players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
            System.out.println("DEBUG: Forced velocity sync for player");
        }

        // Apply hit stun BEFORE damage to prevent damage knockback interference
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;
        }

        // Apply damage AFTER velocity
        target.hurt(damageSource, damage);

        System.out.println("DEBUG: Launched " + target.getName().getString()
                + " with velocity: " + launchVelocity);

        // Debug check - schedule a delayed velocity check
        if (user.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                Vec3 delayedVelocity = target.getDeltaMovement();
                System.out.println("DEBUG: Velocity 1 tick later - Y: " + delayedVelocity.y
                        + ", onGround: " + target.onGround());
            });
        }
    }

    private void createHitParticles(ServerLevel serverLevel, LivingEntity target) {
        // Impact particles
        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.3, 0.3, 0.1);

        // Rising particles
        for (int i = 0; i < 5; i++) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + (i * 0.3), target.getZ(),
                    3, 0.1, 0.1, 0.1, 0.05);
        }

        // Add some sweep particles around the target
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0, 0, 0, 0);

        System.out.println("DEBUG: Created rising hit particles for " + target.getName().getString());
    }

    private void createRisingParticles(Player user, Level world) {
        if (!(world instanceof ServerLevel serverLevel)) {
            System.out.println("DEBUG: Not server level, skipping particles");
            return;
        }

        System.out.println("DEBUG: Creating rising slash particles");

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Position the slash in front of the player
        Vec3 slashBase = userPos.add(lookDir.scale(range * 0.6));

        float slashHeight = 3.0f; // Height of the vertical slash
        float slashWidth = 0.8f;  // Width of the slash for some visual thickness

        // Create vertical line of particles going straight up
        for (int i = 0; i <= 8; i++) {
            float progress = i / 8.0f;

            // Main vertical line
            Vec3 particlePos = slashBase.add(0, progress * slashHeight, 0);

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0);

            // Add some width to the slash with side particles
            if (i % 2 == 0) {
                Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

                // Left side particle
                Vec3 leftPos = particlePos.add(rightDir.scale(-slashWidth/2));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        leftPos.x, leftPos.y, leftPos.z,
                        1, 0, 0, 0, 0);

                // Right side particle
                Vec3 rightPos = particlePos.add(rightDir.scale(slashWidth/2));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        rightPos.x, rightPos.y, rightPos.z,
                        1, 0, 0, 0, 0);
            }

            // Add some cloud particles for effect at key points
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0.1, 0.1, 0.01);
            }
        }

        System.out.println("DEBUG: Created rising slash particle line going straight up");
    }

    private void end(Player player) {
        System.out.println("DEBUG: RisingSlashAttack ended");
        isActive = false;
        hitEntities.clear();
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

    public float getLaunchPower() {
        return launchPower;
    }
}