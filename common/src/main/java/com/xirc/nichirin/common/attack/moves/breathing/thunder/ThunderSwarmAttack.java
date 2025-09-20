package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom; // ✅ Thread-safe alternative

/**
 * Third Form: Thunder Swarm
 * Launches 4 sword slashes forward that travel in sequence
 * Each slash creates lightning on impact or when hitting ground/expiring
 *
 * All configuration comes from the moveset builder.
 */
public class ThunderSwarmAttack extends ThunderBreathingAttackBase {

    private static final double SLASH_SPEED = 1.2; // Blocks per tick
    private static final int MAX_TRAVEL_DISTANCE = 16; // Blocks before expiring
    private static final int SLASH_LAUNCH_INTERVAL = 3; // Ticks between launching each slash

    private final List<ProjectileSlash> activeSlashes = new ArrayList<>();
    private int slashesLaunched = 0;
    private int launchTimer = 0;
    // ✅ REMOVED: private final Random random = new Random();

    public ThunderSwarmAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        // Reset state
        activeSlashes.clear();
        slashesLaunched = 0;
        launchTimer = 0;

        // Initial thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        launchTimer++;

        // Launch slashes one after another (but immediately on first tick)
        if ((launchTimer == 1 || launchTimer % SLASH_LAUNCH_INTERVAL == 0) && slashesLaunched < 4) {
            launchSlash();
            slashesLaunched++;
        }

        // Update all active slashes - move them forward
        List<ProjectileSlash> toRemove = new ArrayList<>();
        for (ProjectileSlash slash : activeSlashes) {
            if (!moveSlashForward(slash)) {
                toRemove.add(slash);
            }
        }

        // Remove expired slashes
        activeSlashes.removeAll(toRemove);

        // Stop attack when all slashes are launched and expired
        if (slashesLaunched >= 4 && activeSlashes.isEmpty()) {
            stop();
        }
    }

    private void launchSlash() {
        // Calculate launch direction (straight forward from player's look direction)
        Vec3 lookDirection = user.getLookAngle();

        // Starting position (slightly in front of player)
        Vec3 startPos = user.position().add(0, user.getEyeHeight() * 0.8, 0).add(lookDirection.scale(1.5));

        // Create projectile slash - all travel in same direction
        ProjectileSlash slash = new ProjectileSlash(
                startPos,
                lookDirection.normalize().scale(SLASH_SPEED),
                0 // Distance traveled starts at 0
        );

        activeSlashes.add(slash);

        // Launch sound - ✅ FIXED: Use ThreadLocalRandom instead of instance field
        world.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.8f, 1.5f + ThreadLocalRandom.current().nextFloat() * 0.3f);

    }

    /**
     * Move a slash forward and handle its interactions
     * @return false if slash should be removed
     */
    private boolean moveSlashForward(ProjectileSlash slash) {
        // Store old position for collision checking
        Vec3 oldPos = slash.position;

        // Move the slash forward
        slash.position = slash.position.add(slash.velocity);
        slash.distanceTraveled += slash.velocity.length();

        // Create visual slash effect at current position
        createSlashVisuals(slash.position);

        // Check for entity hits FIRST (before ground)
        List<LivingEntity> nearbyEntities = getTargetsInCustomHitbox(slash.position, 1.8, 1.8, 1.8);
        for (LivingEntity target : nearbyEntities) {

            // Hit the target WITHOUT immunity frames (so all 4 slashes can hit same target)
            hitTargetNoImmunity(target);

            // Hit particles (NO LIGHTNING for entity hits)
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.2);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        slash.position.x, slash.position.y, slash.position.z,
                        3, 0.3, 0.3, 0.3, 0.1);
            }

            return false; // Remove slash after hitting entity (no lightning)
        }

        // Check for ground/block collision
        BlockHitResult blockHit = world.clip(new ClipContext(
                oldPos, slash.position,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                user
        ));

        if (blockHit.getType() == HitResult.Type.BLOCK) {

            // Create lightning bolt at ground impact
            createLightningBolt(Vec3.atCenterOf(blockHit.getBlockPos()).add(0, 1, 0));

            return false; // Remove slash after hitting ground
        }

        // Check max distance - create lightning if expires without hitting anything
        if (slash.distanceTraveled >= MAX_TRAVEL_DISTANCE) {

            // Create lightning bolt at expiration point (didn't hit anything)
            createLightningBolt(slash.position);

            return false; // Remove slash after max distance
        }

        return true; // Continue slash
    }

    /**
     * Create visual effects for the flying slash at its current position
     */
    private void createSlashVisuals(Vec3 position) {
        if (world instanceof ServerLevel serverLevel) {
            // Main slash visual - sweep attack particle
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    position.x, position.y, position.z,
                    1, 0.2, 0.2, 0.2, 0.05);

            // Electric trail effect
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    position.x, position.y, position.z,
                    3, 0.1, 0.1, 0.1, 0.02);

            // Add some crit particles for extra slash effect
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    position.x, position.y, position.z,
                    2, 0.15, 0.15, 0.15, 0.1);
        }
    }

    /**
     * Create a lightning bolt at the specified position
     */
    private void createLightningBolt(Vec3 position) {
        if (world.isClientSide) return;

        // Find ground level for lightning
        BlockPos groundPos = new BlockPos((int)position.x, (int)position.y, (int)position.z);

        // Search downward for solid ground (up to 10 blocks)
        for (int i = 0; i < 10; i++) {
            BlockPos checkPos = groundPos.below(i);
            BlockState blockState = world.getBlockState(checkPos);

            if (!blockState.isAir() && blockState.getBlock() != Blocks.WATER) {
                groundPos = checkPos.above(); // Lightning strikes one block above ground
                break;
            }
        }

        // Create lightning bolt
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning != null) {
            lightning.moveTo(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            lightning.setCause((net.minecraft.server.level.ServerPlayer) user);
            world.addFreshEntity(lightning);

        }

        // Extra lightning particles
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    groundPos.getX() + 0.5, groundPos.getY() + 2, groundPos.getZ() + 0.5,
                    8, 0.3, 1.0, 0.3, 0.1);
        }

        // Lightning sound
        world.playSound(null, groundPos.getX(), groundPos.getY(), groundPos.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Rotate a vector around the Y-axis by the given angle
     */
    private Vec3 rotateVectorY(Vec3 vector, double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        return new Vec3(
                vector.x * cos - vector.z * sin,
                vector.y,
                vector.x * sin + vector.z * cos
        );
    }

    @Override
    protected void onStop() {
        activeSlashes.clear();
    }

    /**
     * Represents a single projectile slash
     */
    private static class ProjectileSlash {
        Vec3 position;
        Vec3 velocity;
        double distanceTraveled;

        ProjectileSlash(Vec3 position, Vec3 velocity, double distanceTraveled) {
            this.position = position;
            this.velocity = velocity;
            this.distanceTraveled = distanceTraveled;
        }
    }
}