package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.enums.MoveClass;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Base class for all breathing technique attacks.
 * Highly customizable through builder pattern.
 */
@Getter
public abstract class AbstractBreathingAttack<T extends AbstractBreathingAttack<T, A>, A extends IBreathingAttacker<A, ?>> {

    // Timing
    private int cooldown = 1;
    private int windup = 1;
    private int duration = 1;

    // Combat stats
    private float damage = 5.0f;
    private float range = 3.0f;
    private float knockback = 0.4f;
    private int hitStun = 20;

    // Resource management
    private float breathCost = 10.0f;
    private boolean consumeOnHit = false;

    // Particles
    private ParticleOptions primaryParticle = ParticleTypes.CLOUD;
    @Nullable
    private ParticleOptions secondaryParticle;
    private int particleCount = 10;
    private float particleSpread = 0.5f;

    // Sounds
    @Nullable
    private SoundEvent startSound;
    @Nullable
    private SoundEvent hitSound;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    // Effects
    private final Map<MobEffect, MobEffectData> userEffects = new HashMap<>();
    private final Map<MobEffect, MobEffectData> targetEffects = new HashMap<>();

    // Behavior flags
    private boolean holdable = false;
    private boolean piercing = false;
    private boolean areaOfEffect = false;
    private boolean multiHit = false;
    private int maxHits = 1;
    private int maxTargets = 1;

    // Movement
    @Nullable
    private Vec3 userVelocity;
    private boolean lockMovement = false;

    // Metadata
    private Component name = Component.literal("Breathing Technique");
    private Component description = Component.empty();
    private int formNumber = 1;

    // State
    @Setter
    private int currentTick = 0;
    @Setter
    private int hitCount = 0;
    @Setter
    private Set<UUID> hitEntities = new HashSet<>();
    @Setter
    private boolean active = false;
    @Setter
    private MoveClass moveClass;

    // Builder methods for easy customization

    @SuppressWarnings("unchecked")
    public T withTiming(int cooldown, int windup, int duration) {
        this.cooldown = cooldown;
        this.windup = windup;
        this.duration = duration;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withDamage(float damage) {
        this.damage = damage;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withRange(float range) {
        this.range = range;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withKnockback(float knockback) {
        this.knockback = knockback;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withHitStun(int stun) {
        this.hitStun = stun;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withBreathCost(float cost) {
        this.breathCost = cost;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withParticle(ParticleOptions particle) {
        this.primaryParticle = particle;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withParticles(ParticleOptions primary, ParticleOptions secondary, int count) {
        this.primaryParticle = primary;
        this.secondaryParticle = secondary;
        this.particleCount = count;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSound(SoundEvent sound) {
        this.startSound = sound;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSounds(SoundEvent start, SoundEvent hit) {
        this.startSound = start;
        this.hitSound = hit;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSoundPitch(float pitch) {
        this.soundPitch = pitch;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withUserEffect(MobEffect effect, int duration, int amplifier) {
        userEffects.put(effect, new MobEffectData(duration, amplifier));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withTargetEffect(MobEffect effect, int duration, int amplifier) {
        targetEffects.put(effect, new MobEffectData(duration, amplifier));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withUserVelocity(Vec3 velocity) {
        this.userVelocity = velocity;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withUserVelocity(double x, double y, double z) {
        this.userVelocity = new Vec3(x, y, z);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setHoldable(boolean holdable) {
        this.holdable = holdable;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPiercing(boolean piercing) {
        this.piercing = piercing;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAreaOfEffect(boolean aoe, int maxTargets) {
        this.areaOfEffect = aoe;
        this.maxTargets = maxTargets;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setMultiHit(int hits) {
        this.multiHit = hits > 1;
        this.maxHits = hits;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withForm(int number, String name) {
        this.formNumber = number;
        this.name = Component.translatable("nichirin.form." + number, name);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T lockMovement(boolean lock) {
        this.lockMovement = lock;
        return (T) this;
    }

    // Core lifecycle methods

    /**
     * Called when the move is registered to a MoveClass
     */
    public void onRegister(MoveClass moveClass) {
        this.moveClass = moveClass;
    }

    /**
     * Called every tick by the move map
     */
    public void tick(A attacker) {
        if (active) {
            Player player = attacker.getPlayer();
            Level world = player.level();

            if (!onTick(player, world)) {
                onEnd(player, world);
                active = false;
            }
        }
    }

    /**
     * Starts the breathing attack
     */
    public void start(A attacker) {
        if (active) return;

        Player player = attacker.getPlayer();
        Level world = player.level();

        active = true;
        onStart(player, world);
    }

    /**
     * Stops the breathing attack
     */
    public void stop(A attacker) {
        if (!active) return;

        Player player = attacker.getPlayer();
        Level world = player.level();

        onEnd(player, world);
        active = false;
    }

    /**
     * Called when technique starts
     */
    public void onStart(Player user, Level world) {
        // Play sound
        if (startSound != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    startSound, user.getSoundSource(), soundVolume, soundPitch);
        }

        // Apply user effects
        userEffects.forEach((effect, data) -> {
            user.addEffect(new MobEffectInstance(effect, data.duration, data.amplifier));
        });

        // Apply velocity
        if (userVelocity != null) {
            Vec3 look = user.getLookAngle();
            Vec3 velocity = new Vec3(
                    look.x * userVelocity.x,
                    userVelocity.y,
                    look.z * userVelocity.z
            );
            user.setDeltaMovement(velocity);
        }

        // Reset state
        currentTick = 0;
        hitCount = 0;
        hitEntities.clear();
    }

    /**
     * Called every tick while active
     * @return true to continue, false to end
     */
    public boolean onTick(Player user, Level world) {
        currentTick++;

        // Check if we should perform the attack
        if (shouldPerform()) {
            perform(user, world);
        }

        // Create particles
        createParticles(user, world);

        // Lock movement if configured
        if (lockMovement) {
            user.setDeltaMovement(Vec3.ZERO);
        }

        return currentTick < duration;
    }

    /**
     * Called when technique ends
     */
    public void onEnd(Player user, Level world) {
        // Override for cleanup
    }

    /**
     * Determines when to perform the attack
     */
    protected boolean shouldPerform() {
        if (multiHit) {
            // For multi-hit attacks, space them out
            int interval = duration / maxHits;
            return currentTick % interval == windup && hitCount < maxHits;
        }
        // For single hit, perform after windup
        return currentTick == windup;
    }

    /**
     * Performs the actual attack
     */
    protected abstract void perform(Player user, Level world);

    /**
     * Applies damage and effects to a target
     */
    protected void hitTarget(Player user, LivingEntity target) {
        // Check if already hit (for non-multi-hit attacks)
        if (!multiHit && hitEntities.contains(target.getUUID())) {
            return;
        }

        // Deal damage
        DamageSource source = user.damageSources().playerAttack(user);
        target.hurt(source, damage);

        // Apply knockback
        if (knockback > 0) {
            Vec3 knockVec = target.position().subtract(user.position()).normalize();
            target.knockback(knockback, -knockVec.x, -knockVec.z);
        }

        // Apply stun
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;
        }

        // Apply target effects
        targetEffects.forEach((effect, data) -> {
            target.addEffect(new MobEffectInstance(effect, data.duration, data.amplifier));
        });

        // Play hit sound
        if (hitSound != null) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, target.getSoundSource(), soundVolume, soundPitch);
        }

        // Track hit
        hitEntities.add(target.getUUID());
        hitCount++;
    }

    /**
     * Creates particle effects
     */
    protected void createParticles(Player user, Level world) {
        for (int i = 0; i < particleCount; i++) {
            double x = user.getX() + (world.random.nextDouble() - 0.5) * particleSpread;
            double y = user.getY() + user.getBbHeight() / 2;
            double z = user.getZ() + (world.random.nextDouble() - 0.5) * particleSpread;

            world.addParticle(primaryParticle, x, y, z, 0, 0.1, 0);

            if (secondaryParticle != null && world.random.nextBoolean()) {
                world.addParticle(secondaryParticle, x, y, z, 0, 0.05, 0);
            }
        }
    }

    /**
     * Gets entities in range
     */
    protected List<LivingEntity> getTargetsInRange(Player user, Level world) {
        return world.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(range),
                entity -> entity != user && entity.isAlive() && user.hasLineOfSight(entity));
    }

    /**
     * Helper class for effect data
     */
    private static class MobEffectData {
        final int duration;
        final int amplifier;

        MobEffectData(int duration, int amplifier) {
            this.duration = duration;
            this.amplifier = amplifier;
        }
    }
}