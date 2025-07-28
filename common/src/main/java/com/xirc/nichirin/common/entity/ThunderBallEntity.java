package com.xirc.nichirin.common.entity;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.*;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.architectury.networking.NetworkManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Thunder Ball Entity - Travels forward for 6 seconds, continuously damaging nearby entities
 * and striking with lightning every 2 seconds (3 times total)
 */
public class ThunderBallEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<Integer> LIFE_TICKS = SynchedEntityData.defineId(ThunderBallEntity.class, EntityDataSerializers.INT);
    private static final int MAX_LIFE_TICKS = 120; // 6 seconds at 20 TPS - EXACTLY 120 ticks
    private static final int LIGHTNING_INTERVAL = 40; // 2 seconds between lightning strikes
    private static final double TRAVEL_SPEED = 0.2; // 4 blocks per second (4/20 = 0.2 per tick)
    private static final double DAMAGE_RADIUS = 4.0;

    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    private final Set<LivingEntity> damagedEntities = new HashSet<>();
    private int lastLightningTick = 0;
    private int lightningCount = 0;
    private float damage = 2.0f; // 1 heart of damage
    private int hitStun = 20; // 1 second of shocked effect
    private Entity owner;

    public ThunderBallEntity(EntityType<? extends ThunderBallEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.entityData.set(LIFE_TICKS, 0);
    }

    public ThunderBallEntity(EntityType<? extends ThunderBallEntity> entityType, Level level, LivingEntity owner, float damage, int hitStun) {
        this(entityType, level);
        this.owner = owner;
        this.damage = damage;
        this.hitStun = hitStun;

        // Set initial position and velocity
        this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight(), owner.getZ());
        Vec3 lookDirection = owner.getLookAngle();
        this.setDeltaMovement(lookDirection.scale(TRAVEL_SPEED));
    }

    @Override
    public void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            int currentLifeTicks = this.entityData.get(LIFE_TICKS);

            // STRICT 120 tick lifespan - disappear immediately at 120 ticks
            if (currentLifeTicks >= MAX_LIFE_TICKS) {
                this.discard(); // Disappear, not die
                return;
            }

            // Update life ticks
            this.entityData.set(LIFE_TICKS, currentLifeTicks + 1);

            // Move forward at constant speed
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

            // Damage nearby entities continuously
            damageNearbyEntities();

            // Lightning strikes every 2 seconds
            if (currentLifeTicks - lastLightningTick >= LIGHTNING_INTERVAL && lightningCount < 3) {
                performLightningStrike();
                lastLightningTick = currentLifeTicks;
                lightningCount++;
            }

            // Create visual effects
            createVisualEffects();
        }

        // Play continuous thunder ball sound
        if (this.tickCount % 20 == 0) { // Every second
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    NicirinSoundRegistry.THUNDER_BALL.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        }
    }

    private void damageNearbyEntities() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        AABB damageArea = new AABB(
                this.getX() - DAMAGE_RADIUS, this.getY() - DAMAGE_RADIUS, this.getZ() - DAMAGE_RADIUS,
                this.getX() + DAMAGE_RADIUS, this.getY() + DAMAGE_RADIUS, this.getZ() + DAMAGE_RADIUS
        );

        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this.owner && entity.isAlive() &&
                        entity.position().distanceTo(this.position()) <= DAMAGE_RADIUS);

        for (LivingEntity entity : nearbyEntities) {
            // Only damage each entity once every 20 ticks (1 second) to prevent spam
            if (!damagedEntities.contains(entity) || this.tickCount % 20 == 0) {
                // Create damage source
                DamageSource damageSource = this.owner != null ?
                        this.level().damageSources().magic() :
                        this.level().damageSources().generic();

                // Apply damage
                entity.hurt(damageSource, damage);

                // Apply shocked effect
                entity.addEffect(new MobEffectInstance(
                        NichirinEffectRegistry.SHOCKED.get(),
                        hitStun,
                        0,
                        false,
                        true
                ));

                damagedEntities.add(entity);

                // Create damage particles around the entity
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Clear damaged entities set periodically to allow re-damage
        if (this.tickCount % 20 == 0) {
            damagedEntities.clear();
        }
    }

    private void performLightningStrike() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // Strike at random positions around the ball, but on the ground
        for (int i = 0; i < 2; i++) { // 2 lightning bolts per strike
            double offsetX = (this.random.nextDouble() - 0.5) * DAMAGE_RADIUS * 1.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * DAMAGE_RADIUS * 1.5;

            // Find the ground level for lightning strike
            int groundY = this.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    new net.minecraft.core.BlockPos((int)(this.getX() + offsetX), (int)this.getY(), (int)(this.getZ() + offsetZ))
            ).getY();

            Vec3 strikePos = new Vec3(this.getX() + offsetX, groundY, this.getZ() + offsetZ);

            // Create lightning bolt at ground level
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
            if (lightning != null) {
                lightning.moveTo(strikePos.x, strikePos.y, strikePos.z);
                lightning.setVisualOnly(true); // Visual only, we handle damage ourselves
                serverLevel.addFreshEntity(lightning);
            }

            // Create extra particles at strike location
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    strikePos.x, strikePos.y, strikePos.z,
                    30, 1.0, 2.0, 1.0, 0.3);
        }
    }

    private void createVisualEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // Continuous electric particles around the ball
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(), this.getZ(),
                5, 0.5, 0.5, 0.5, 0.1);

        // Occasional larger sparks
        if (this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        return false; // Thunder ball cannot be damaged
    }

    // Prevent command summoning by making the entity non-summonable
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
        compound.putInt("LightningCount", this.lightningCount);
        compound.putInt("LastLightningTick", this.lastLightningTick);
        compound.putFloat("Damage", this.damage);
        compound.putInt("HitStun", this.hitStun);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        this.entityData.set(LIFE_TICKS, compound.getInt("LifeTicks"));
        this.lightningCount = compound.getInt("LightningCount");
        this.lastLightningTick = compound.getInt("LastLightningTick");
        this.damage = compound.getFloat("Damage");
        this.hitStun = compound.getInt("HitStun");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    // AzureLib Animation Implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate)
                .setAnimationSpeed(1.0)); // Ensure normal animation speed
    }

    private PlayState predicate(AnimationState<ThunderBallEntity> state) {
        // Play the spin animation while alive - ensure it's always playing
        if (this.isAlive()) {
            state.setAnimation(RawAnimation.begin().then("spin", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object entity) {
        return RenderUtils.getCurrentTick();
    }

    @Override
    public boolean shouldPlayAnimsWhileGamePaused() {
        return false;
    }
}