package com.xirc.nichirin.common.entity.projectile;

import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FlashBombEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> IS_ARMED = SynchedEntityData.defineId(FlashBombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TIMEOUT_TIMER = SynchedEntityData.defineId(FlashBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE_TIMER = SynchedEntityData.defineId(FlashBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EXPLOSION_COUNT = SynchedEntityData.defineId(FlashBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EXPLOSION_DELAY = SynchedEntityData.defineId(FlashBombEntity.class, EntityDataSerializers.INT);

    private static final int MAX_TIMEOUT = 40; // 2 seconds before auto-explode
    private static final int FUSE_TIME = 20; // 1 second after being triggered

    public FlashBombEntity(EntityType<? extends FlashBombEntity> entityType, Level level) {
        super(entityType, level);
        // Make the flash bomb heavier (falls faster)
        this.setDeltaMovement(this.getDeltaMovement().scale(1.0).add(0, -0.1, 0));
    }

    public FlashBombEntity(Level level, LivingEntity shooter) {
        super(NichirinEntityRegistry.FLASH_BOMB.get(), shooter, level);
        // Make the flash bomb heavier (falls faster)
        this.setDeltaMovement(this.getDeltaMovement().scale(1.0).add(0, -0.1, 0));
    }

    @Override
    protected Item getDefaultItem() {
        return com.xirc.nichirin.registry.NichirinItemRegistry.FLASH_BOMB.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ARMED, false);
        this.entityData.define(TIMEOUT_TIMER, MAX_TIMEOUT);
        this.entityData.define(FUSE_TIMER, FUSE_TIME);
        this.entityData.define(EXPLOSION_COUNT, 0);
        this.entityData.define(EXPLOSION_DELAY, 0);
    }

    public boolean isArmed() {
        return this.entityData.get(IS_ARMED);
    }

    public void setArmed(boolean armed) {
        this.entityData.set(IS_ARMED, armed);
    }

    public int getTimeoutTimer() {
        return this.entityData.get(TIMEOUT_TIMER);
    }

    public void setTimeoutTimer(int timer) {
        this.entityData.set(TIMEOUT_TIMER, timer);
    }

    public int getFuseTimer() {
        return this.entityData.get(FUSE_TIMER);
    }

    public void setFuseTimer(int timer) {
        this.entityData.set(FUSE_TIMER, timer);
    }

    public int getExplosionCount() {
        return this.entityData.get(EXPLOSION_COUNT);
    }

    public void setExplosionCount(int count) {
        this.entityData.set(EXPLOSION_COUNT, count);
    }

    public int getExplosionDelay() {
        return this.entityData.get(EXPLOSION_DELAY);
    }

    public void setExplosionDelay(int delay) {
        this.entityData.set(EXPLOSION_DELAY, delay);
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            // Hit something (ground, wall, etc.) - explode immediately
            System.out.println("Flash bomb hit something, exploding immediately");
            setExplosionCount(1);
            setExplosionDelay(0);
            // Stop all movement when it hits
            this.setDeltaMovement(0, 0, 0);
        }
        // Don't call super.onHit() to prevent the projectile from being removed
    }

    @Override
    public void tick() {
        // Call super.tick() to maintain normal projectile physics (gravity, movement, etc.)
        if (getExplosionCount() == 0) {
            super.tick();
            // Apply additional downward force to make it heavier/fall faster
            if (!this.level().isClientSide) {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x, motion.y - 0.02, motion.z); // Extra gravity
            }
        } else {
            // Once exploding, stop normal physics and just update position for rendering
            this.baseTick();
        }

        if (!this.level().isClientSide) {
            // Only use timeout if we haven't started exploding yet
            if (getExplosionCount() == 0) {
                // Count down timeout timer as backup
                int currentTimeout = getTimeoutTimer();
                if (currentTimeout <= 0) {
                    // Timeout reached - start explosion
                    System.out.println("Flash bomb timeout reached, exploding");
                    setExplosionCount(1);
                    setExplosionDelay(0);
                } else {
                    setTimeoutTimer(currentTimeout - 1);
                }
            }

            // Handle explosion sequence
            if (getExplosionCount() > 0 && getExplosionCount() <= 3) {
                int currentDelay = getExplosionDelay();
                if (currentDelay <= 0) {
                    // Time for next explosion
                    System.out.println("Creating explosion #" + getExplosionCount());
                    createFlashExplosion(this.position(), (ServerLevel) this.level(), getExplosionCount());

                    if (getExplosionCount() >= 3) {
                        // All explosions done
                        this.discard();
                    } else {
                        // Set up next explosion
                        setExplosionCount(getExplosionCount() + 1);
                        setExplosionDelay(3); // 3 ticks until next explosion
                    }
                } else {
                    // Count down delay
                    setExplosionDelay(currentDelay - 1);
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && getExplosionCount() == 0) {
            // Player interacted - explode immediately
            System.out.println("Player interacted with flash bomb, exploding immediately");
            setExplosionCount(1);
            setExplosionDelay(0);

            // Play interaction sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.5F);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return getExplosionCount() == 0; // Can only interact when not exploding
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        if (!this.level().isClientSide && getExplosionCount() == 0 && damageSource.getEntity() instanceof Player) {
            // Player hit the bomb - explode immediately
            System.out.println("Player hit flash bomb, exploding immediately");
            setExplosionCount(1);
            setExplosionDelay(0);

            // Play interaction sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.5F);

            return true;
        }
        return super.hurt(damageSource, amount);
    }

    private void armBomb() {
        setArmed(true);
        setFuseTimer(FUSE_TIME);

        // Play arming sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void createFlashExplosion(Vec3 position, ServerLevel serverLevel, int explosionNumber) {
        System.out.println("Creating flash explosion #" + explosionNumber + " at " + position);
        final double DAMAGE_RADIUS = 3.0; // 3 block radius for damage

        // Play explosion sound
        serverLevel.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.0F);

        // Create flash particles
        createFlashParticles(position, serverLevel);

        // Apply damage to nearby entities
        AABB damageArea = new AABB(
                position.x - DAMAGE_RADIUS, position.y - DAMAGE_RADIUS, position.z - DAMAGE_RADIUS,
                position.x + DAMAGE_RADIUS, position.y + DAMAGE_RADIUS, position.z + DAMAGE_RADIUS
        );

        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, damageArea);
        for (LivingEntity entity : entities) {
            double distance = Math.sqrt(
                    Math.pow(entity.getX() - position.x, 2) +
                            Math.pow(entity.getY() - position.y, 2) +
                            Math.pow(entity.getZ() - position.z, 2)
            );
            if (distance <= DAMAGE_RADIUS) {
                // Calculate damage based on distance (closer = more damage)
                float maxDamage = 6.0f;
                float damage = (float) (maxDamage * (1.0 - (distance / DAMAGE_RADIUS)));
                damage = Math.max(damage, 2.0f);

                // Apply explosion damage WITHOUT immunity frames
                entity.invulnerableTime = 0; // Remove immunity frames
                entity.hurt(entity.damageSources().explosion(null, this.getOwner()), damage);

                // Add knockback effect
                double knockbackStrength = 1.5;
                double deltaX = entity.getX() - position.x;
                double deltaY = entity.getY() - position.y;
                double deltaZ = entity.getZ() - position.z;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (length > 0) {
                    deltaX /= length;
                    deltaY /= length;
                    deltaZ /= length;

                    entity.setDeltaMovement(
                            entity.getDeltaMovement().add(
                                    deltaX * knockbackStrength,
                                    Math.max(deltaY * knockbackStrength, 0.4), // Minimum upward knockback
                                    deltaZ * knockbackStrength
                            )
                    );
                }
            }
        }
    }

    private void createFlashParticles(Vec3 position, ServerLevel serverLevel) {
        // Central shockwave
        for (int i = 0; i < 5; i++) {
            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    position.x, position.y, position.z,
                    1, 0, 0, 0, 0);
        }

        // Flash1 particles - bright initial flash
        for (int i = 0; i < 30; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 4.0;
            double offsetY = (serverLevel.random.nextDouble() - 0.5) * 4.0;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 4.0;

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                    1, 0, 0, 0, 0);
        }

        // Flash2 particles - secondary flash effect
        for (int i = 0; i < 20; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 6.0;
            double offsetY = (serverLevel.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 6.0;

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                    1, 0, 0, 0, 0);
        }

        // Additional explosion particles for extra impact
        for (int i = 0; i < 15; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 3.0;
            double offsetY = (serverLevel.random.nextDouble() - 0.5) * 3.0;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 3.0;

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Armed", isArmed());
        compound.putInt("TimeoutTimer", getTimeoutTimer());
        compound.putInt("FuseTimer", getFuseTimer());
        compound.putInt("ExplosionCount", getExplosionCount());
        compound.putInt("ExplosionDelay", getExplosionDelay());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setArmed(compound.getBoolean("Armed"));
        setTimeoutTimer(compound.getInt("TimeoutTimer"));
        setFuseTimer(compound.getInt("FuseTimer"));
        setExplosionCount(compound.getInt("ExplosionCount"));
        setExplosionDelay(compound.getInt("ExplosionDelay"));
    }
}