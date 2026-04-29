package com.xirc.nichirin.common.entity.projectile;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thrown katana projectile for Beast Breathing's Eleventh Fang.
 * Spins forward, pierces entities, sticks to blocks, expires after 10 seconds.
 */
public class ThrownKatanaEntity extends Entity {

    private static final int MAX_LIFE_TICKS = 200; // 10 seconds
    private static final double HIT_RADIUS = 0.4;
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(ThrownKatanaEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifeTicks = 0;
    private boolean stuck = false;
    private float damage = 20.0f;
    private int hitStun = 20;
    private Entity owner;
    private final Set<UUID> hitEntities = new HashSet<>();

    public ThrownKatanaEntity(EntityType<? extends ThrownKatanaEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public ThrownKatanaEntity(EntityType<? extends ThrownKatanaEntity> entityType, Level level,
                               LivingEntity owner, float damage, int hitStun) {
        this(entityType, level);
        this.owner = owner;
        this.damage = damage;
        this.hitStun = hitStun;
    }

    public boolean isStuck() {
        return entityData.get(STUCK);
    }

    @Override
    public void defineSynchedData() {
        entityData.define(STUCK, false);
    }

    @Override
    public void tick() {
        super.tick();

        lifeTicks++;
        if (lifeTicks >= MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        if (stuck) {
            return;
        }

        Vec3 motion = getDeltaMovement();

        // Move forward
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        // Point toward travel direction
        double horizDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Mth.atan2(-motion.y, horizDist) * (180.0 / Math.PI)));

        // Check block collision
        if (!this.level().noCollision(this, this.getBoundingBox().move(motion))) {
            // Raycast to exact surface, then back off so tip sits at the surface
            // (blade tip is 31/16 blocks from model origin along travel direction)
            BlockHitResult hit = level().clip(new ClipContext(
                    position(), position().add(motion),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 dir = motion.normalize();
                Vec3 surface = hit.getLocation();
                setPos(surface.x - dir.x * (31.0 / 16.0),
                       surface.y - dir.y * (31.0 / 16.0),
                       surface.z - dir.z * (31.0 / 16.0));
            }
            stuck = true;
            entityData.set(STUCK, true);
            setDeltaMovement(Vec3.ZERO);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ARROW_HIT, SoundSource.NEUTRAL, 1.0f, 1.2f);
            return;
        }

        // Pierce entities
        if (!this.level().isClientSide) {
            hitNearbyEntities();
        }

        if (level() instanceof ServerLevel sl && lifeTicks % 3 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void hitNearbyEntities() {
        AABB hitBox = this.getBoundingBox().inflate(HIT_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != owner && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : targets) {
            DamageSource source;
            if (owner instanceof Player player) {
                source = target.damageSources().playerAttack(player);
            } else {
                source = target.damageSources().generic();
            }

            boolean hurt = target.hurt(source, damage);
            if (hurt) {
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                    target.addEffect(new MobEffectInstance(
                            NichirinEffectRegistry.STUNNED.get(), hitStun, 1, false, false, true));
                }
                Vec3 direction = getDeltaMovement().normalize();
                target.push(direction.x * 0.3, 0.1, direction.z * 0.3);

                hitEntities.add(target.getUUID()); // Pierce: track but don't stop
                level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("LifeTicks");
        stuck = tag.getBoolean("Stuck");
        damage = tag.getFloat("Damage");
        hitStun = tag.getInt("HitStun");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTicks", lifeTicks);
        tag.putBoolean("Stuck", stuck);
        tag.putFloat("Damage", damage);
        tag.putInt("HitStun", hitStun);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
