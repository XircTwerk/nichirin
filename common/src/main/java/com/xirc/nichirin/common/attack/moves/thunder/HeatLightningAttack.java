package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fifth Form: Heat Lightning
 * Auto-targets the closest entity you're looking at with an upward slash
 * Then strikes airborne targets with lightning
 *
 * All configuration now comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class HeatLightningAttack extends ThunderBreathingAttackBase {

    private static final double MAX_TARGET_RANGE = 15.0; // Fixed range for targeting

    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final Map<LivingEntity, Integer> launchedEntities = new HashMap<>(); // Track with tick count
    private final Set<LivingEntity> struckByLightning = new HashSet<>();
    private float launchPower = 1.5f;
    private LivingEntity targetedEntity = null;

    public HeatLightningAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hitEntities.clear();
        launchedEntities.clear();
        struckByLightning.clear();
        targetedEntity = null;

        // Find the closest entity in range
        targetedEntity = findClosestEntity();

        if (targetedEntity == null) {
            System.out.println("DEBUG: Heat Lightning - No entity in range, attack will miss");
        } else {
            System.out.println("DEBUG: Heat Lightning - Auto-targeted: " + targetedEntity.getName().getString());
        }

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

        // Check for airborne targets continuously
        checkAndStrikeAirborneTargets();
    }

    /**
     * Find the closest entity within range (no look angle requirement)
     */
    private LivingEntity findClosestEntity() {
        Vec3 playerPos = user.getEyePosition();

        // Search for entities within fixed range
        AABB searchBox = new AABB(
                user.getX() - MAX_TARGET_RANGE, user.getY() - MAX_TARGET_RANGE, user.getZ() - MAX_TARGET_RANGE,
                user.getX() + MAX_TARGET_RANGE, user.getY() + MAX_TARGET_RANGE, user.getZ() + MAX_TARGET_RANGE
        );

        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // Find the closest entity (no line of sight or angle requirements)
        LivingEntity closestEntity = nearbyEntities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(user)))
                .orElse(null);

        if (closestEntity != null) {
            double distance = Math.sqrt(closestEntity.distanceToSqr(user));
            System.out.println("DEBUG: Heat Lightning - Closest entity at distance " +
                    String.format("%.2f", distance) + " blocks");
        }

        return closestEntity;
    }

    private void performRisingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // If we have a targeted entity, create slash effect toward them
        Vec3 slashDirection = lookDir;
        Vec3 slashBase = userPos.add(lookDir.scale(range * 0.3));

        if (targetedEntity != null) {
            // Aim the slash toward the targeted entity
            Vec3 toTarget = targetedEntity.position().subtract(userPos).normalize();
            slashDirection = toTarget;
            slashBase = userPos.add(toTarget.scale(2.0)); // Closer to ensure hit
        }

        // Create visual effect - vertical slash
        if (world instanceof ServerLevel serverLevel) {
            float slashHeight = 4.0f;

            // Vertical line of particles toward target
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

            // Extra particles connecting to target
            if (targetedEntity != null) {
                // Line of particles from player to target
                Vec3 playerEye = user.getEyePosition();
                Vec3 targetPos = targetedEntity.getEyePosition();
                int steps = 10;

                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    Vec3 particlePos = playerEye.lerp(targetPos, t);

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            3, 0.15, 0.15, 0.15, 0.05);
                }
            }
        }

        // Thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.5f);

        // Hit the targeted entity if we have one
        if (targetedEntity != null && targetedEntity.isAlive()) {
            // Create armor-bypassing damage source (using configured damage)
            DamageSource armorPiercingSource = user.damageSources().magic();
            targetedEntity.hurt(armorPiercingSource, damage);

            // Launch the target
            launchTarget(targetedEntity);

            hitEntities.add(targetedEntity);
            launchedEntities.put(targetedEntity, tickCount);

            System.out.println("DEBUG: Heat Lightning - Hit and launched " + targetedEntity.getName().getString());
        } else {
            System.out.println("DEBUG: Heat Lightning - No target to hit");
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

        System.out.println("DEBUG: Heat Lightning - Launched " + target.getName().getString() + " with velocity " + launchVelocity);
    }

    private void checkAndStrikeAirborneTargets() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Check all launched entities
        Set<LivingEntity> toRemove = new HashSet<>();

        for (Map.Entry<LivingEntity, Integer> entry : launchedEntities.entrySet()) {
            LivingEntity target = entry.getKey();
            int launchTick = entry.getValue();

            // Wait at least 5 ticks after launch to ensure they're airborne
            if (tickCount - launchTick < 5) continue;

            if (!target.isAlive()) {
                toRemove.add(target);
                continue;
            }

            // Check if target is airborne and hasn't been struck yet
            boolean isAirborne = !target.onGround() || target.getDeltaMovement().y > 0.1;

            if (isAirborne && !struckByLightning.contains(target)) {
                // Strike with lightning
                strikeWithLightning(serverLevel, target);
                struckByLightning.add(target);
                toRemove.add(target);
            }

            // Remove if they've been in the air too long without being struck (safety)
            if (tickCount - launchTick > 40) {
                toRemove.add(target);
            }
        }

        // Clean up struck targets
        for (LivingEntity entity : toRemove) {
            launchedEntities.remove(entity);
        }
    }

    private void strikeWithLightning(ServerLevel serverLevel, LivingEntity target) {
        // Create lightning bolt at target
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning != null) {
            lightning.moveTo(target.position());
            lightning.setCause((ServerPlayer) user); // Remove setVisualOnly to make it deal damage
            serverLevel.addFreshEntity(lightning);
        }

        // Extra damage while airborne (magic damage to bypass armor - using configured damage)
        DamageSource source = user.damageSources().magic();
        target.hurt(source, damage * 0.5f);

        // NOW apply the shocked effect after lightning hits (using configured hitStun)
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.SHOCKED.get(),
                hitStun,
                0,
                false,
                true
        ));

        // Lightning particles
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY(), target.getZ(),
                40, 0.8, 0.8, 0.8, 0.3);

        // Thunder sound
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.0f);

        System.out.println("DEBUG: Heat Lightning - Lightning struck " + target.getName().getString());
    }

    @Override
    protected void onStop() {
        hitEntities.clear();
        launchedEntities.clear();
        struckByLightning.clear();
        targetedEntity = null;
    }
}