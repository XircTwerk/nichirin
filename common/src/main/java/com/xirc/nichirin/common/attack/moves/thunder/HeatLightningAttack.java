package com.xirc.nichirin.common.attack.moves.thunder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fifth Form: Heat Lightning
 * Upward slash that ignores armor and summons lightning on airborne targets
 */
public class HeatLightningAttack extends ThunderBreathingAttackBase {

    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final Set<LivingEntity> launchedEntities = new HashSet<>();
    private float launchPower = 1.5f;

    public HeatLightningAttack() {
        withTiming(60, 8, 20) // cooldown, windup, duration
                .withDamage(18.0f)
                .withRange(12.0f)
                .withKnockback(0.0f) // No horizontal knockback, just launch
                .withBreathCost(25.0f)
                .withHitStun(20);
    }

    @Override
    protected void onStart() {
        hitEntities.clear();
        launchedEntities.clear();

        // Rising slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Perform the rising slash on first tick
        if (tickCount == windup + 1) {
            performRisingSlash();
        }

        // Check for airborne targets to strike with lightning
        if (tickCount % 5 == 0) {
            strikeAirborneTargets();
        }
    }

    private void performRisingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create visual effect - vertical slash
        if (world instanceof ServerLevel serverLevel) {
            Vec3 slashBase = userPos.add(lookDir.scale(range * 0.5));
            float slashHeight = 4.0f;

            // Vertical line of particles
            for (int i = 0; i <= 10; i++) {
                float progress = i / 10.0f;
                Vec3 particlePos = slashBase.add(0, progress * slashHeight, 0);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);

                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            5, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        // Thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.5f);

        // Create hitbox for the upward slash
        Vec3 hitboxCenter = userPos.add(lookDir.scale(range / 2));
        AABB hitbox = new AABB(
                hitboxCenter.x - hitboxSize,
                hitboxCenter.y - 1,
                hitboxCenter.z - hitboxSize,
                hitboxCenter.x + hitboxSize,
                hitboxCenter.y + 4, // Tall hitbox for upward slash
                hitboxCenter.z + hitboxSize
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        for (LivingEntity target : targets) {
            // Create armor-bypassing damage source
            DamageSource armorPiercingSource = createArmorBypassingDamage(user.damageSources().playerAttack(user));
            target.hurt(armorPiercingSource, damage);

            // Apply shocked effect
            hitTarget(target);

            // Launch the target
            launchTarget(target);

            hitEntities.add(target);
            launchedEntities.add(target);
        }
    }

    private void launchTarget(LivingEntity target) {
        // Clear existing velocity
        target.setDeltaMovement(Vec3.ZERO);

        // Lift off ground
        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
        }

        // Apply upward velocity
        Vec3 launchVelocity = new Vec3(0, launchPower, 0);
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // Force sync for players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    private void strikeAirborneTargets() {
        ServerLevel serverLevel = (ServerLevel) world;

        for (LivingEntity target : launchedEntities) {
            if (target.isAlive() && !target.onGround() && target.getY() > target.yo) {
                // Create lightning bolt at target
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.moveTo(target.position());
                    lightning.setVisualOnly(true);
                    serverLevel.addFreshEntity(lightning);
                }

                // Extra damage while airborne
                DamageSource source = createArmorBypassingDamage(user.damageSources().playerAttack(user));
                target.hurt(source, damage * 0.5f);

                // Lightning particles
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY(), target.getZ(),
                        30, 0.5, 0.5, 0.5, 0.2);

                // Thunder sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.5f);

                // Remove from launched list after striking
                launchedEntities.remove(target);
            }
        }
    }

    /**
     * Creates a damage source that bypasses armor by using reflection or alternative methods
     * Since bypassArmor() doesn't exist, we'll create a custom approach
     */
    private DamageSource createArmorBypassingDamage(DamageSource originalSource) {
        // Option 1: Try to use magic damage (typically bypasses armor)
        try {
            return user.damageSources().magic();
        } catch (Exception e) {
            // Option 2: Use the original source and apply extra damage to compensate
            return originalSource;
        }
    }

    @Override
    protected void onStop() {
        hitEntities.clear();
        launchedEntities.clear();
    }
}