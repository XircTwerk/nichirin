package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Base class for Thunder Breathing attacks
 * Simplified to avoid the IBreathingAttacker constraint
 */
public abstract class ThunderBreathingAttackBase {

    // Configuration
    protected float damage = 10.0f;
    protected float range = 5.0f;
    protected float knockback = 0.3f;
    protected float breathCost = 15.0f;
    protected int hitStun = 20; // 1 second
    protected float hitboxSize = 2.0f; // Size 2 hitbox as specified

    // Timing
    @Getter
    protected int cooldown = 40;
    protected int windup = 5;
    protected int duration = 20;

    // Getters
    // State
    @Getter
    protected boolean isActive = false;
    protected int tickCount = 0;
    protected Player user;
    protected Level world;

    /**
     * Start the attack
     */
    public void start(Player user, Level world) {
        this.user = user;
        this.world = world;
        this.tickCount = 0;

        // Check breath cost BEFORE marking as active
        if (!BreathingManager.consume(user, breathCost)) {
            user.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            // DON'T set isActive = true if we don't have enough breath
            return;
        }

        // Only mark as active if we successfully consumed breath
        this.isActive = true;
        onStart();
    }

    /**
     * Tick the attack
     */
    public void tick() {
        if (!isActive || user == null || world == null) return;

        tickCount++;

        // Check if we're past windup
        if (tickCount > windup) {
            perform();
        }

        // Check if attack is complete
        if (tickCount >= windup + duration) {
            stop();
        }
    }

    /**
     * Stop the attack
     */
    public void stop() {
        isActive = false;
        onStop();
    }

    /**
     * Apply damage and effects to a target
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        target.hurt(source, damage);

        // Apply shocked effect using our custom effect
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.SHOCKED.get(), // Use our custom shocked effect
                hitStun,
                0, // Amplifier 0 (effect only has 1 level)
                false, // Ambient
                true   // Show particles
        ));

        // Apply knockback
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }
    }

    /**
     * Special hit method that removes immunity frames - for ThunderClapFlash
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Store current invulnerability time
        int oldInvulTime = target.invulnerableTime;

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        target.hurt(source, damage);

        // Don't restore old invulnerability - let it start fresh from this hit

        // Apply shocked effect using our custom effect
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.SHOCKED.get(),
                hitStun,
                0,
                false,
                true
        ));

        // Apply knockback
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }
    }

    /**
     * Get entities in a hitbox
     */
    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        AABB hitbox = new AABB(
                center.x - hitboxSize/2, center.y - hitboxSize/2, center.z - hitboxSize/2,
                center.x + hitboxSize/2, center.y + hitboxSize/2, center.z + hitboxSize/2
        );

        return world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());
    }

    /**
     * Called when attack starts
     */
    protected abstract void onStart();

    /**
     * Called every tick during the attack
     */
    protected abstract void perform();

    /**
     * Called when attack ends
     */
    protected void onStop() {
        // Override if needed
    }

    // Builder-style configuration methods
    public ThunderBreathingAttackBase withDamage(float damage) {
        this.damage = damage;
        return this;
    }

    public ThunderBreathingAttackBase withRange(float range) {
        this.range = range;
        return this;
    }

    public ThunderBreathingAttackBase withBreathCost(float cost) {
        this.breathCost = cost;
        return this;
    }

    public ThunderBreathingAttackBase withTiming(int cooldown, int windup, int duration) {
        this.cooldown = cooldown;
        this.windup = windup;
        this.duration = duration;
        return this;
    }

    public ThunderBreathingAttackBase withKnockback(float knockback) {
        this.knockback = knockback;
        return this;
    }

    public ThunderBreathingAttackBase withHitStun(int stun) {
        this.hitStun = stun;
        return this;
    }

    public ThunderBreathingAttackBase withHitboxSize(float size) {
        this.hitboxSize = size;
        return this;
    }

    public int getTotalDuration() { return windup + duration; }
}