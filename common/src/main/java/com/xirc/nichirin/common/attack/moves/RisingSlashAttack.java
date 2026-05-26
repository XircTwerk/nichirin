package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import lombok.Getter;
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
    @Getter
    private final int cooldown;
    private final float damage;
    private final float range;
    private final float knockback;
    private final float hitboxSize;
    private final Vec3 hitboxOffset;
    private final int hitStun;
    @Getter
    private final float launchPower; // How high to launch enemies
    private final SoundEvent startSound;
    private final SoundEvent hitSound;

    // State
    private int tickCount = 0;
    @Getter
    private boolean isActive = false;
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
        private float damage = 0.0f;
        private float range = 2.5f;
        private float knockback = 0.2f;
        private float hitboxSize = 2.0f;
        private Vec3 hitboxOffset = new Vec3(0, 0.5, 0); // Slightly higher hitbox
        private int hitStun = 20;
        private float launchPower = 0.8f; // Default launch power (blocks)
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

    public void start(LivingEntity player) {
        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Reset state
        tickCount = 0;
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

    public void tick(LivingEntity player) {
        if (!isActive) return;

        // Only run on server side
        if (player.level().isClientSide()) {
            return;
        }

        tickCount++;

        // Check if we're in the active frames — detect every tick so all enemies in range get launched
        if (tickCount >= startup && tickCount <= startup + active) {
            performHitDetection(player, player.level());
        }

        // Check if attack is complete
        if (tickCount >= getTotalDuration()) {
            end(player);
        }
    }

    private void performHitDetection(LivingEntity user, Level world) {
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
            DamageSource damageSource = user instanceof Player p
                    ? user.damageSources().playerAttack(p)
                    : user.damageSources().mobAttack(user);

            for (LivingEntity target : targets) {
                // Launch the target
                launchTarget(target, user, damageSource);

                // Add to hit list
                hitEntities.add(target);

                // Play hit sound
                if (hitSound != null) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            hitSound, SoundSource.PLAYERS, 1.0f, 0.8f);
                }
            }
        }
    }

    private void launchTarget(LivingEntity target, LivingEntity user, DamageSource damageSource) {
        // Apply damage first so vanilla knockback fires before we overwrite velocity
        target.hurt(damageSource, damage);

        // Lift slightly off ground to ensure launch works
        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
        }

        // Calculate launch velocity — purely upward + slight horizontal scatter
        Vec3 launchVelocity = new Vec3(0, launchPower, 0);
        if (knockback > 0) {
            Vec3 knockDirection = target.position().subtract(user.position()).normalize();
            launchVelocity = launchVelocity.add(
                    knockDirection.x * knockback,
                    0,
                    knockDirection.z * knockback
            );
        }

        // Overwrite velocity AFTER damage so nothing can clobber our launch
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // Apply hit stun after damage so it doesn't block our own hurt() call
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;
        }

        // Force sync for players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    private void createRisingParticles(LivingEntity user, Level world) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 base = user.position().add(0, user.getBbHeight() * 0.3, 0)
                .add(user.getLookAngle().scale(range * 0.5));
        // Upward streak of sparks
        for (int i = 0; i < 5; i++) {
            Vec3 pos = base.add(0, i * 0.35, 0);
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    pos.x, pos.y, pos.z, 2, 0.1, 0.05, 0.1, 0.0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                base.x, base.y + 0.8, base.z, 3, 0.2, 0.2, 0.2, 0.0);
    }

    private void end(LivingEntity player) {
        isActive = false;
        hitEntities.clear();
    }

    public void stop() {
        isActive = false;
        hitEntities.clear();
    }

    public int getTotalDuration() {
        return startup + active + recovery;
    }
}
