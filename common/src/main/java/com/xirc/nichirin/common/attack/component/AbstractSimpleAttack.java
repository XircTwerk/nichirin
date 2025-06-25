package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.enums.MoveClass;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
 * Base class for all physical attacks.
 * Simpler than breathing attacks - no breath cost, uses stamina instead.
 */
@Getter
public abstract class AbstractSimpleAttack<T extends AbstractSimpleAttack<T, A>, A extends IPhysicalAttacker<A, ?>> {

    // Timing
    private int startup;
    private int active;
    private int recovery;

    // Combat stats
    private float damage;
    private float range;
    private float knockback;
    private float hitboxSize;
    private Vec3 hitboxOffset = Vec3.ZERO;
    private int hitStun;

    // Resource management
    private float staminaCost;

    // Effects
    private final Map<MobEffect, MobEffectData> targetEffects = new HashMap<>();
    private final Map<MobEffect, MobEffectData> userEffects = new HashMap<>();

    // Visual/Audio
    @Nullable
    private ParticleOptions hitParticle;
    @Nullable
    private SoundEvent startSound;
    @Nullable
    private SoundEvent hitSound;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    // Animation
    @Nullable
    private ResourceLocation animationId;
    private int animationPriority = 0;

    // Behavior
    private boolean canBackstab = true;
    private boolean unblockable = false;
    private boolean piercing = false;

    // Followup
    @Nullable
    private T followup;
    private int followupWindow = 5; // Ticks after active frames end
    @Nullable
    private ResourceLocation followupAnimation;

    // Metadata
    private Component name = Component.literal("Physical Attack");
    private Component description = Component.empty();

    // State
    @Setter
    private int currentTick = 0;
    @Setter
    private boolean active = false;
    @Setter
    private boolean hitConnected = false;
    @Setter
    private Set<UUID> hitEntities = new HashSet<>();
    @Setter
    private MoveClass moveClass;

    // Builder methods

    @SuppressWarnings("unchecked")
    public T withTiming(int startup, int active, int recovery) {
        this.startup = startup;
        this.active = active;
        this.recovery = recovery;
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
    public T withHitbox(float size, Vec3 offset) {
        this.hitboxSize = size;
        this.hitboxOffset = offset;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withHitbox(float size, double x, double y, double z) {
        return withHitbox(size, new Vec3(x, y, z));
    }

    @SuppressWarnings("unchecked")
    public T withHitStun(int stun) {
        this.hitStun = stun;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withStaminaCost(float cost) {
        this.staminaCost = cost;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withTargetEffect(MobEffect effect, int duration, int amplifier) {
        targetEffects.put(effect, new MobEffectData(duration, amplifier));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withUserEffect(MobEffect effect, int duration, int amplifier) {
        userEffects.put(effect, new MobEffectData(duration, amplifier));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withParticle(ParticleOptions particle) {
        this.hitParticle = particle;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSounds(SoundEvent start, SoundEvent hit) {
        this.startSound = start;
        this.hitSound = hit;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withAnimation(ResourceLocation animationId, int priority) {
        this.animationId = animationId;
        this.animationPriority = priority;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withAnimation(String animationId, int priority) {
        return withAnimation(new ResourceLocation(animationId), priority);
    }

    @SuppressWarnings("unchecked")
    public T setUnblockable(boolean unblockable) {
        this.unblockable = unblockable;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPiercing(boolean piercing) {
        this.piercing = piercing;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCanBackstab(boolean canBackstab) {
        this.canBackstab = canBackstab;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withFollowup(T followup, ResourceLocation followupAnimation) {
        this.followup = followup;
        this.followupAnimation = followupAnimation;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withFollowupWindow(int window) {
        this.followupWindow = window;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withInfo(Component name, Component description) {
        this.name = name;
        this.description = description;
        return (T) this;
    }

    // Core lifecycle methods

    /**
     * Called when the move is registered to a MoveClass
     */
    public void onRegister(MoveClass moveClass) {
        this.moveClass = moveClass;
        if (followup != null) {
            followup.onRegister(moveClass);
        }
    }

    /**
     * Total duration of the attack
     */
    public int getTotalDuration() {
        return startup + active + recovery;
    }

    /**
     * Checks if the attack can be initiated
     */
    public boolean canStart(A attacker) {
        return attacker.hasStamina(staminaCost);
    }

    /**
     * Starts the attack
     */
    public void start(A attacker) {
        if (!canStart(attacker)) return;

        Player player = attacker.getPlayer();
        Level world = player.level();

        // Consume stamina
        attacker.consumeStamina(staminaCost);

        // Play start sound
        if (startSound != null) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, player.getSoundSource(), soundVolume, soundPitch);
        }

        // Apply user effects
        userEffects.forEach((effect, data) -> {
            player.addEffect(new MobEffectInstance(effect, data.duration, data.amplifier));
        });

        // Reset state
        active = true;
        currentTick = 0;
        hitConnected = false;
        hitEntities.clear();

        onStart(player, world);
    }

    /**
     * Called every tick while the attack is active
     */
    public void tick(A attacker) {
        if (!active) return;

        Player player = attacker.getPlayer();
        Level world = player.level();

        currentTick++;

        // Check if we're in active frames
        if (currentTick > startup && currentTick <= startup + active) {
            // Generate hitbox and check for hits
            performHitCheck(player, world);
        }

        // Check if attack is complete
        if (currentTick >= getTotalDuration()) {
            onEnd(player, world);
            active = false;

            // Check for followup input
            if (followup != null && hitConnected) {
                checkFollowup(attacker);
            }
        }

        onTick(player, world);
    }

    /**
     * Checks for followup input after active frames
     */
    private void checkFollowup(A attacker) {
        // This would need to be implemented with your input system
        // For now, it's a placeholder for the followup window logic
    }

    /**
     * Performs hit detection
     */
    protected void performHitCheck(Player user, Level world) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 hitboxCenter = userPos.add(lookDir.scale(range)).add(hitboxOffset);

        AABB hitbox = new AABB(
                hitboxCenter.x - hitboxSize,
                hitboxCenter.y - hitboxSize,
                hitboxCenter.z - hitboxSize,
                hitboxCenter.x + hitboxSize,
                hitboxCenter.y + hitboxSize,
                hitboxCenter.z + hitboxSize
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() &&
                        (piercing || !hitEntities.contains(entity.getUUID())));

        for (LivingEntity target : targets) {
            if (validateHit(user, target)) {
                hitTarget(user, target);
                hitEntities.add(target.getUUID());
                hitConnected = true;
            }
        }
    }

    /**
     * Validates if a target can be hit
     */
    protected boolean validateHit(Player user, LivingEntity target) {
        return user.hasLineOfSight(target);
    }

    /**
     * Applies damage and effects to a target
     */
    protected void hitTarget(Player user, LivingEntity target) {
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

        // Create hit particles
        if (hitParticle != null) {
            for (int i = 0; i < 5; i++) {
                double offsetX = (target.level().random.nextDouble() - 0.5) * 0.5;
                double offsetY = (target.level().random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (target.level().random.nextDouble() - 0.5) * 0.5;

                target.level().addParticle(hitParticle,
                        target.getX() + offsetX,
                        target.getY() + target.getBbHeight() / 2 + offsetY,
                        target.getZ() + offsetZ,
                        0, 0, 0);
            }
        }

        onHit(user, target);
    }

    // Hook methods for subclasses

    /**
     * Called when the attack starts
     */
    protected void onStart(Player user, Level world) {}

    /**
     * Called every tick during the attack
     */
    protected void onTick(Player user, Level world) {}

    /**
     * Called when the attack ends
     */
    protected void onEnd(Player user, Level world) {}

    /**
     * Called when a target is hit
     */
    protected void onHit(Player user, LivingEntity target) {}

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