package com.xirc.nichirin.common.entity;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class BoarEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOVEMENT_STATE = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TICK = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TYPE = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIT_TICKS = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHASE_TICKS = SynchedEntityData.defineId(BoarEntity.class, EntityDataSerializers.INT);

    private static final int MOVEMENT_WALKING = 0;
    private static final int MOVEMENT_RUNNING = 1;
    private static final int MOVEMENT_RUNNING_FAST = 2;

    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_JUMP_HIT = 1;
    private static final int ATTACK_POUNCE = 2;
    private static final int ATTACK_CHARGE = 3;

    private static final int JUMP_HIT_TICK = 8;
    private static final int POUNCE_DURATION = 30;
    private static final int CHARGE_DURATION = 40;
    private static final int SIT_DURATION = 1200;
    private static final int STUN_DURATION = 10;
    private static final double GROUP_DETECTION_RADIUS = 50.0;
    private static final double CHARGE_KNOCKBACK = 2.5;

    private int lastAmbientTick = 0;

    public BoarEntity(EntityType<? extends BoarEntity> entityType, Level level) {
        super(entityType, level);
        this.setTame(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ENRAGED, false);
        this.entityData.define(MOVEMENT_STATE, MOVEMENT_WALKING);
        this.entityData.define(ATTACK_TICK, 0);
        this.entityData.define(ATTACK_TYPE, ATTACK_NONE);
        this.entityData.define(SIT_TICKS, 0);
        this.entityData.define(CHASE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2D,
                Ingredient.of(Items.RED_MUSHROOM, Items.BROWN_MUSHROOM, Items.BEETROOT), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new BoarAttackGoal(this));
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (super.canUse() && this.mob.getLastHurtByMob() instanceof BoarEntity otherBoar) {
                    if (BoarEntity.this.isTame() && otherBoar.isTame()) {
                        UUID myOwner = BoarEntity.this.getOwnerUUID();
                        UUID otherOwner = otherBoar.getOwnerUUID();
                        if (myOwner != null && myOwner.equals(otherOwner)) {
                            return false;
                        }
                    }
                }
                return super.canUse();
            }
        });
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (player) -> !this.isTame() && this.getTarget() != null));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            checkEnragedState();
            handleAttackTiming();
            handleSittingBehavior();
            handleAmbientBehavior();
            updateMovementState();
            forceTargetLook();
        }
    }

    private void checkEnragedState() {
        if (!this.isTame()) {
            AABB searchArea = new AABB(
                    this.getX() - GROUP_DETECTION_RADIUS, this.getY() - 10, this.getZ() - GROUP_DETECTION_RADIUS,
                    this.getX() + GROUP_DETECTION_RADIUS, this.getY() + 10, this.getZ() + GROUP_DETECTION_RADIUS
            );

            List<BoarEntity> nearbyBoars = this.level().getEntitiesOfClass(BoarEntity.class, searchArea,
                    boar -> boar != this && boar.isAlive() && !boar.isTame());

            boolean shouldBeEnraged = nearbyBoars.isEmpty();
            setEnraged(shouldBeEnraged);
        }
    }

    private void handleAttackTiming() {
        int attackType = this.entityData.get(ATTACK_TYPE);
        if (attackType == ATTACK_NONE) return;

        int attackTick = this.entityData.get(ATTACK_TICK);
        attackTick++;
        this.entityData.set(ATTACK_TICK, attackTick);

        switch (attackType) {
            case ATTACK_JUMP_HIT:
                if (attackTick == JUMP_HIT_TICK) {
                    executeJumpAttack();
                } else if (attackTick >= 20) {
                    resetAttack();
                }
                break;
            case ATTACK_POUNCE:
                if (attackTick == 10) {
                    executePounceAttack();
                } else if (attackTick >= POUNCE_DURATION) {
                    resetAttack();
                }
                break;
            case ATTACK_CHARGE:
                executeChargeAttack();
                if (attackTick >= CHARGE_DURATION) {
                    resetAttack();
                }
                break;
        }
    }

    private void handleSittingBehavior() {
        if (this.isOrderedToSit()) {
            int sitTicks = this.entityData.get(SIT_TICKS);
            this.entityData.set(SIT_TICKS, sitTicks + 1);
        } else {
            this.entityData.set(SIT_TICKS, 0);
        }
    }

    private void handleAmbientBehavior() {
        if (this.tickCount - lastAmbientTick >= 100) {
            if (this.random.nextFloat() < 0.1f) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PIG_AMBIENT, SoundSource.NEUTRAL, 0.8f, 0.8f + this.random.nextFloat() * 0.4f);
            }
            lastAmbientTick = this.tickCount;
        }
    }

    private void updateMovementState() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            int chaseTicks = this.entityData.get(CHASE_TICKS);
            this.entityData.set(CHASE_TICKS, chaseTicks + 1);

            double distance = this.distanceToSqr(target);

            if (chaseTicks > 60 && distance > 16.0) {
                setMovementState(MOVEMENT_RUNNING_FAST);
            } else if (distance > 9.0) {
                setMovementState(MOVEMENT_RUNNING);
            } else {
                setMovementState(MOVEMENT_WALKING);
            }
        } else {
            this.entityData.set(CHASE_TICKS, 0);
            setMovementState(MOVEMENT_WALKING);
        }
    }

    private void forceTargetLook() {
        if (this.getTarget() != null) {
            this.getLookControl().setLookAt(this.getTarget(), 30.0F, 30.0F);
        }
    }

    public void startAttack(int attackType) {
        this.entityData.set(ATTACK_TYPE, attackType);
        this.entityData.set(ATTACK_TICK, 0);

        if (this.level().isClientSide) return;

        switch (attackType) {
            case ATTACK_CHARGE:
                LivingEntity target = this.getTarget();
                if (target != null) {
                    Vec3 direction = target.position().subtract(this.position()).normalize();
                    this.setDeltaMovement(direction.scale(1.2).add(0, 0.3, 0));
                }

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 1.0f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            this.getX(), this.getY() + 1, this.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1);
                }
                break;
        }
    }

    private void executeJumpAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        Vec3 jumpDirection = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(jumpDirection.scale(0.8).add(0, 0.4, 0));

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0f, 1.2f);

        AABB damageArea = this.getBoundingBox().inflate(1.5);
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && entity.isAlive() && (entity instanceof Player || entity instanceof Monster));

        for (LivingEntity entity : nearbyEntities) {
            if (entity == this.getOwner()) continue;

            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
            entity.hurt(this.level().damageSources().mobAttack(this), damage);

            Vec3 knockback = entity.position().subtract(this.position()).normalize().scale(0.8);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.3, knockback.z));

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private void executePounceAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        Vec3 pounceDirection = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(pounceDirection.scale(1.5).add(0, 0.6, 0));

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.2f, 0.9f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(),
                    5, 0, 0, 0, 0);
        }
    }

    private void executeChargeAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        Vec3 chargeDirection = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(chargeDirection.scale(0.6));

        AABB chargeArea = this.getBoundingBox().inflate(1.2);
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, chargeArea,
                entity -> entity != this && entity.isAlive() && entity != this.getOwner());

        for (LivingEntity entity : nearbyEntities) {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0f;
            entity.hurt(this.level().damageSources().mobAttack(this), damage);

            Vec3 knockback = entity.position().subtract(this.position()).normalize().scale(CHARGE_KNOCKBACK);
            entity.setDeltaMovement(knockback.add(0, 0.5, 0));

            if (entity instanceof Player player) {
                player.addEffect(new MobEffectInstance(NichirinEffectRegistry.SHOCKED.get(), STUN_DURATION, 0));
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        15, 0.5, 0.5, 0.5, 0.1);
            }
        }

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.3, 0.1, 0.3, 0.02);
        }
    }

    private void resetAttack() {
        this.entityData.set(ATTACK_TYPE, ATTACK_NONE);
        this.entityData.set(ATTACK_TICK, 0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (this.level().isClientSide) {
            return itemStack.is(Items.RED_MUSHROOM) || itemStack.is(Items.BROWN_MUSHROOM)
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (itemStack.is(Items.RED_MUSHROOM) || itemStack.is(Items.BROWN_MUSHROOM)) {
            if (!this.isTame()) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);

                    this.level().broadcastEntityEvent(this, (byte) 7);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + this.getBbHeight() + 0.5, this.getZ(),
                                7, 0.5, 0.5, 0.5, 0);
                    }
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }

                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                return InteractionResult.SUCCESS;
            } else if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                this.heal(5.0F);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + this.getBbHeight() + 0.5, this.getZ(),
                            5, 0.3, 0.3, 0.3, 0);
                }

                return InteractionResult.SUCCESS;
            }
        }

        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(itemStack)) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.navigation.stop();
            this.setTarget(null);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        }

        boolean hurt = super.hurt(damageSource, amount);

        if (hurt && !this.level().isClientSide) {
            int chaseTicks = this.entityData.get(CHASE_TICKS);
            if (chaseTicks > 120) {
                int attackType = this.entityData.get(ATTACK_TYPE);
                if (attackType == ATTACK_NONE && this.random.nextFloat() < 0.3f) {
                    startAttack(ATTACK_CHARGE);
                }
            }

            if (this.getHealth() < this.getMaxHealth() * 0.3f && this.random.nextFloat() < 0.4f) {
                Vec3 retreatDirection = this.position().subtract(damageSource.getSourcePosition()).normalize();
                this.getNavigation().moveTo(
                        this.getX() + retreatDirection.x * 10,
                        this.getY(),
                        this.getZ() + retreatDirection.z * 10,
                        1.5
                );
            }
        }

        return hurt;
    }

    // Getters and setters
    public boolean isEnraged() {
        return this.entityData.get(ENRAGED);
    }

    public void setEnraged(boolean enraged) {
        boolean currentEnraged = this.entityData.get(ENRAGED);
        this.entityData.set(ENRAGED, enraged);

        if (enraged && !currentEnraged && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    this.getX(), this.getY() + this.getEyeHeight(), this.getZ(),
                    5, 0.3, 0.1, 0.3, 0.1);
        }
    }

    public int getMovementState() {
        return this.entityData.get(MOVEMENT_STATE);
    }

    public void setMovementState(int state) {
        this.entityData.set(MOVEMENT_STATE, state);
    }

    public int getAttackType() {
        return this.entityData.get(ATTACK_TYPE);
    }

    public int getSitTicks() {
        return this.entityData.get(SIT_TICKS);
    }

    public boolean shouldLayDown() {
        return this.isOrderedToSit() && this.getSitTicks() > SIT_DURATION;
    }

    public int getChaseTicks() {
        return this.entityData.get(CHASE_TICKS);
    }

    public float getTailAngle() {
        float healthPercentage = this.getHealth() / this.getMaxHealth();
        return -40.0f + (60.0f * healthPercentage);
    }

    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob otherParent) {
        BoarEntity baby = new BoarEntity((EntityType<? extends BoarEntity>) this.getType(), level);
        UUID uuid = this.getOwnerUUID();
        if (uuid != null) {
            baby.setOwnerUUID(uuid);
            baby.setTame(true);
        }
        return baby;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.RED_MUSHROOM) || stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.BEETROOT);
    }

    // NBT Data
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Enraged", this.isEnraged());
        compound.putInt("MovementState", this.getMovementState());
        compound.putInt("AttackType", this.getAttackType());
        compound.putInt("AttackTick", this.entityData.get(ATTACK_TICK));
        compound.putInt("SitTicks", this.getSitTicks());
        compound.putInt("ChaseTicks", this.getChaseTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setEnraged(compound.getBoolean("Enraged"));
        this.setMovementState(compound.getInt("MovementState"));
        this.entityData.set(ATTACK_TYPE, compound.getInt("AttackType"));
        this.entityData.set(ATTACK_TICK, compound.getInt("AttackTick"));
        this.entityData.set(SIT_TICKS, compound.getInt("SitTicks"));
        this.entityData.set(CHASE_TICKS, compound.getInt("ChaseTicks"));
    }

    // Custom Attack Goal
    private static class BoarAttackGoal extends MeleeAttackGoal {
        private final BoarEntity boar;

        public BoarAttackGoal(BoarEntity boar) {
            super(boar, 1.0D, true);
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.boar.getAttackType() == ATTACK_NONE;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distance) {
            if (distance <= this.getAttackReachSqr(target) && this.getTicksUntilNextAttack() <= 0) {
                int movementState = this.boar.getMovementState();

                switch (movementState) {
                    case MOVEMENT_RUNNING:
                        this.boar.startAttack(ATTACK_JUMP_HIT);
                        break;
                    case MOVEMENT_RUNNING_FAST:
                        this.boar.startAttack(ATTACK_POUNCE);
                        break;
                    default:
                        super.checkAndPerformAttack(target, distance);
                        return;
                }

                this.resetAttackCooldown();
            }
        }
    }
}