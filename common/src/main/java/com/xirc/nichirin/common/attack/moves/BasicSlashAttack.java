package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.attack.component.AbstractSimpleAttack;
import com.xirc.nichirin.common.attack.component.IPhysicalAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Basic slash attack for all sword weapons.
 * Simple horizontal slash with optional followup.
 */
public class BasicSlashAttack<A extends IPhysicalAttacker<A, ?>> extends AbstractSimpleAttack<BasicSlashAttack<A>, A> {

    // State tracking
    private int tickCount = 0;
    private boolean hasHit = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    public BasicSlashAttack() {
        withInfo(
                Component.literal("Basic Slash"),
                Component.literal("A simple horizontal sword slash")
        );

        // Default particle for slashes
        withParticle(ParticleTypes.SWEEP_ATTACK);
    }

    @Override
    protected boolean canStart(A attacker) {
        // Check if player has enough stamina
        return attacker.getPlayer() != null && !isActive();
    }

    @Override
    protected void onStart(Player user, Level world) {
        System.out.println("DEBUG: BasicSlashAttack onStart called");

        // Reset state
        tickCount = 0;
        hasHit = false;
        hitEntities.clear();

        // Create slash particles
        createSlashParticles(user, world);

        // Play start sound if configured
        if (getStartSound() != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    getStartSound(), SoundSource.PLAYERS, getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    protected void onTick(Player user, Level world) {
        tickCount++;

        // Check if we're in the active frames
        if (tickCount >= getStartup() && tickCount <= getStartup() + getActiveFrames()) {
            // Perform hit detection
            if (!hasHit) {
                performHitDetection(user, world);
            }
        }

        // Check if attack is complete
        if (tickCount >= getTotalDuration()) {
            onEnd(user, world);
            setActive(false);
        }
    }

    private void performHitDetection(Player user, Level world) {
        System.out.println("DEBUG: Performing hit detection");

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 hitboxCenter = userPos.add(lookDir.scale(getRange())).add(getHitboxOffset());

        // Create hitbox
        AABB hitbox = new AABB(
                hitboxCenter.x - getHitboxSize(),
                hitboxCenter.y - getHitboxSize(),
                hitboxCenter.z - getHitboxSize(),
                hitboxCenter.x + getHitboxSize(),
                hitboxCenter.y + getHitboxSize(),
                hitboxCenter.z + getHitboxSize()
        );

        // Find targets
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        if (!targets.isEmpty()) {
            hasHit = true;
            DamageSource damageSource = user.damageSources().playerAttack(user);

            for (LivingEntity target : targets) {
                // Deal damage
                target.hurt(damageSource, getDamage());

                // Apply knockback
                if (getKnockback() > 0) {
                    Vec3 knockVec = target.position().subtract(user.position()).normalize();
                    target.knockback(getKnockback(), -knockVec.x, -knockVec.z);
                }

                // Apply hit stun
                if (getHitStun() > 0) {
                    target.invulnerableTime = getHitStun();
                }

                // Add to hit list
                hitEntities.add(target);

                // Call hit hook
                onHit(user, target);

                // Create hit particles
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                            10, 0.2, 0.2, 0.2, 0.1);
                }

                // Play hit sound
                if (getHitSound() != null) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            getHitSound(), SoundSource.PLAYERS, getSoundVolume(), getSoundPitch());
                }
            }
        }
    }

    @Override
    protected void onHit(Player user, LivingEntity target) {
        System.out.println("DEBUG: Hit " + target.getName().getString());
        // Target effects are handled internally by AbstractSimpleAttack
    }

    @Override
    protected void onEnd(Player user, Level world) {
        System.out.println("DEBUG: BasicSlashAttack ended");
        // Cleanup
        hitEntities.clear();
    }

    @Override
    public boolean canStart(Player physAttacker) {
        return physAttacker != null && !isActive();
    }

    /**
     * Public tick method to be called from the katana
     */
    public void tick(Player player) {
        if (isActive() && player != null) {
            onTick(player, player.level());
        }
    }

    /**
     * Creates visual slash effect
     */
    private void createSlashParticles(Player user, Level world) {
        if (world.isClientSide() || !(world instanceof ServerLevel serverLevel)) return;

        double radius = getRange();
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

    @SuppressWarnings("unchecked")
    protected BasicSlashAttack<A> getThis() {
        return this;
    }

    /**
     * Creates a copy of this attack
     */
    public BasicSlashAttack<A> copy() {
        BasicSlashAttack<A> copy = new BasicSlashAttack<>();

        // Copy all the properties
        copy.withTiming(getStartup(), getActiveFrames(), getRecovery())
                .withDamage(getDamage())
                .withRange(getRange())
                .withKnockback(getKnockback())
                .withHitbox(getHitboxSize(), getHitboxOffset())
                .withHitStun(getHitStun())
                .withStaminaCost(getStaminaCost())
                .withInfo(getName(), getDescription());

        // Copy sounds
        if (getStartSound() != null) {
            copy.withSounds(getStartSound(), getHitSound());
        }

        // Copy particle
        if (getHitParticle() != null) {
            copy.withParticle(getHitParticle());
        }

        return copy;
    }
}