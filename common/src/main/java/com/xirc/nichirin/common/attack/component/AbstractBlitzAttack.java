package com.xirc.nichirin.common.attack.component;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Abstract blitz attack that generates multiple hitboxes at specified intervals.
 * Each hitbox can have its own position, size, and properties.
 */
@Getter
public abstract class AbstractBlitzAttack extends AbstractBreathingAttack {

    // Hitbox generation
    private int hitboxInterval = 2; // Ticks between hitbox generation
    private int maxHitboxes = 5;
    private float hitboxSize = 2.0f;
    private float hitboxOffset = 1.0f; // Distance from user
    private boolean persistentHitboxes = false; // Whether hitboxes stay active
    private int hitboxDuration = 5; // How long each hitbox lasts if persistent

    // Hitbox behavior
    private boolean chainHitboxes = false; // Whether hitboxes connect in a chain
    private boolean expandingHitboxes = false; // Whether hitboxes grow over time
    private float expansionRate = 0.1f;

    // Visual/Audio per hitbox
    @Nullable
    private ParticleOptions hitboxParticle = ParticleTypes.SWEEP_ATTACK;
    @Nullable
    private SoundEvent hitboxSound = SoundEvents.PLAYER_ATTACK_SWEEP;

    // Tracking
    private final List<ActiveHitbox> activeHitboxes = new ArrayList<>();
    @Setter
    private int hitboxesGenerated = 0;

    public AbstractBlitzAttack() {
        super();
        // Set default multi-hit behavior
        setMultiHit(5);
    }

    // Override parent methods to return AbstractBlitzAttack for chaining

    @Override
    public AbstractBlitzAttack withTiming(int cooldown, int windup, int duration) {
        super.withTiming(cooldown, windup, duration);
        return this;
    }

    @Override
    public AbstractBlitzAttack withDamage(float damage) {
        super.withDamage(damage);
        return this;
    }

    @Override
    public AbstractBlitzAttack withBreathCost(float cost) {
        super.withBreathCost(cost);
        return this;
    }

    @Override
    public AbstractBlitzAttack withForm(int number, String name) {
        super.withForm(number, name);
        return this;
    }

    // Builder methods specific to blitz attacks

    public AbstractBlitzAttack withHitboxTiming(int interval, int maxHitboxes) {
        this.hitboxInterval = interval;
        this.maxHitboxes = maxHitboxes;
        setMultiHit(maxHitboxes);
        return this;
    }

    public AbstractBlitzAttack withHitboxProperties(float size, float offset) {
        this.hitboxSize = size;
        this.hitboxOffset = offset;
        return this;
    }

    public AbstractBlitzAttack withPersistentHitboxes(int duration) {
        this.persistentHitboxes = true;
        this.hitboxDuration = duration;
        return this;
    }

    public AbstractBlitzAttack withChainedHitboxes() {
        this.chainHitboxes = true;
        return this;
    }

    public AbstractBlitzAttack withExpandingHitboxes(float rate) {
        this.expandingHitboxes = true;
        this.expansionRate = rate;
        return this;
    }

    public AbstractBlitzAttack withHitboxEffects(ParticleOptions particle, SoundEvent sound) {
        this.hitboxParticle = particle;
        this.hitboxSound = sound;
        return this;
    }

    // Core methods

    @Override
    public void onStart(Player user, Level world) {
        super.onStart(user, world);
        activeHitboxes.clear();
        hitboxesGenerated = 0;
    }

    @Override
    protected boolean shouldPerform() {
        // Generate hitboxes at intervals
        if (getCurrentTick() >= getWindup() && hitboxesGenerated < maxHitboxes) {
            return (getCurrentTick() - getWindup()) % hitboxInterval == 0;
        }
        return false;
    }

    @Override
    protected void perform(Player user, Level world) {
        // Generate a new hitbox
        Vec3 hitboxPos = calculateHitboxPosition(user, hitboxesGenerated);
        float size = calculateHitboxSize(hitboxesGenerated);

        ActiveHitbox hitbox = new ActiveHitbox(
                hitboxPos,
                size,
                hitboxesGenerated,
                getCurrentTick()
        );

        activeHitboxes.add(hitbox);
        hitboxesGenerated++;

        // Play effects for new hitbox
        if (hitboxParticle != null) {
            createHitboxParticles(world, hitbox);
        }
        if (hitboxSound != null) {
            world.playSound(null, hitbox.position.x, hitbox.position.y, hitbox.position.z,
                    hitboxSound, user.getSoundSource(), getSoundVolume(), getSoundPitch());
        }

        // Immediately check for hits with the new hitbox
        checkHitbox(user, world, hitbox);
    }

    @Override
    public boolean onTick(Player user, Level world) {
        // Update existing hitboxes
        Iterator<ActiveHitbox> it = activeHitboxes.iterator();
        while (it.hasNext()) {
            ActiveHitbox hitbox = it.next();
            hitbox.age++;

            // Remove expired hitboxes
            if (!persistentHitboxes || hitbox.age > hitboxDuration) {
                it.remove();
                continue;
            }

            // Update hitbox properties
            if (expandingHitboxes) {
                hitbox.size += expansionRate;
            }

            // Check for hits with persistent hitboxes
            if (persistentHitboxes) {
                checkHitbox(user, world, hitbox);
            }

            // Update visuals
            createHitboxParticles(world, hitbox);
        }

        // Continue parent tick logic
        return super.onTick(user, world);
    }

    /**
     * Calculate where the next hitbox should be positioned
     */
    protected abstract Vec3 calculateHitboxPosition(Player user, int index);

    /**
     * Calculate the size of the hitbox at the given index
     */
    protected float calculateHitboxSize(int index) {
        return hitboxSize;
    }

    /**
     * Check for entities hit by a specific hitbox
     */
    protected void checkHitbox(Player user, Level world, ActiveHitbox hitbox) {
        AABB bounds = new AABB(
                hitbox.position.x - hitbox.size,
                hitbox.position.y - hitbox.size,
                hitbox.position.z - hitbox.size,
                hitbox.position.x + hitbox.size,
                hitbox.position.y + hitbox.size,
                hitbox.position.z + hitbox.size
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != user && entity.isAlive() && !hitbox.hitEntities.contains(entity.getUUID()));

        for (LivingEntity target : targets) {
            // Custom hit validation
            if (validateHit(user, target, hitbox)) {
                hitTarget(user, target);
                hitbox.hitEntities.add(target.getUUID());

                // Apply hitbox-specific effects
                applyHitboxEffects(user, target, hitbox);
            }
        }
    }

    /**
     * Validate if a target should be hit by a hitbox
     */
    protected boolean validateHit(Player user, LivingEntity target, ActiveHitbox hitbox) {
        return true;
    }

    /**
     * Apply additional effects when a hitbox hits a target
     */
    protected void applyHitboxEffects(Player user, LivingEntity target, ActiveHitbox hitbox) {
        // Override for custom effects per hitbox
    }

    /**
     * Create visual effects for a hitbox
     */
    protected void createHitboxParticles(Level world, ActiveHitbox hitbox) {
        if (hitboxParticle == null) return;

        // Create particle ring around hitbox
        int particles = (int)(hitbox.size * 8);
        for (int i = 0; i < particles; i++) {
            double angle = (i / (double)particles) * Math.PI * 2;
            double x = hitbox.position.x + Math.cos(angle) * hitbox.size;
            double z = hitbox.position.z + Math.sin(angle) * hitbox.size;

            world.addParticle(hitboxParticle, x, hitbox.position.y, z, 0, 0, 0);
        }
    }

    /**
     * Get all currently active hitboxes
     */
    public List<ActiveHitbox> getActiveHitboxes() {
        return new ArrayList<>(activeHitboxes);
    }

    /**
     * Represents an active hitbox in the world
     */
    @Getter
    public static class ActiveHitbox {
        private final Vec3 position;
        private float size;
        private final int index;
        private final int createdAt;
        private int age = 0;
        private final Set<UUID> hitEntities = new HashSet<>();

        public ActiveHitbox(Vec3 position, float size, int index, int createdAt) {
            this.position = position;
            this.size = size;
            this.index = index;
            this.createdAt = createdAt;
        }
    }
}