package com.xirc.nichirin.common.entity.animal;

import com.xirc.nichirin.client.renderer.entity.dispatcher.BoarEntityDispatcher;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import mod.azure.azurelib.util.MoveAnalysis;
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

    public final BoarEntityDispatcher dispatcher;
    public final MoveAnalysis moveAnalysis;
    private int lastAmbientTick = 0;

    public BoarEntity(EntityType<? extends BoarEntity> entityType, Level level) {
        super(entityType, level);
        this.setTame(false);
        this.dispatcher = new BoarEntityDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
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

        if (this.level().isClientSide) {
            moveAnalysis.update();
            updateAnimations();
        }

        if (!this.level().isClientSide) {
            checkEnragedState();
            handleAttackTiming();
            handleSittingBehavior();
            handleAmbientBehavior();
            updateMovementState();
            forceTargetLook();
        }
    }

    private void updateAnimations() {
        int attackType = getAttackType();

        // Attack animations (highest priority)
        if (attackType != ATTACK_NONE) {
            switch (attackType) {
                case ATTACK_JUMP_HIT:
                    dispatcher.jumpHit();
                    return;
                case ATTACK_POUNCE:
                    dispatcher.pounce();
                    return;
                case ATTACK_CHARGE:
                    dispatcher.runningFast();
                    return;
            }
        }

        // Sitting animations
        if (isOrderedToSit()) {
            if (shouldLayDown()) {
                dispatcher.layDown();
            } else {
                dispatcher.sit();
            }
            return;
        }

        // Movement animations
        if (moveAnalysis.isMovingHorizontally()) {
            int movementState = getMovementState();
            switch (movementState) {
                case MOVEMENT_RUNNING_FAST:
                    dispatcher.runningFast();
                    break;
                case MOVEMENT_RUNNING:
                    dispatcher.running();
                    break;
                default:
                    dispatcher.walking();
                    break;
            }
        } else {
            dispatcher.idle();
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
        if (this.isOrderedToSit() && this.isTame()) {
            int sitTicks = this.entityData.get(SIT_TICKS);
            sitTicks++;
            this.entityData.set(SIT_TICKS, sitTicks);
        } else {
            this.entityData.set(SIT_TICKS, 0);
        }
    }

    private void handleAmbientBehavior() {
        if (this.tickCount - lastAmbientTick > 100 + this.random.nextInt(100)) {
            if (this.random.nextFloat() < 0.3f) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.RAVAGER_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.2f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            this.getX(), this.getY(), this.getZ(),
                            5, 0.3, 0.1, 0.3, 0.1);
                }
            }
            lastAmbientTick = this.tickCount;
        }
    }

    private void updateMovementState() {
        LivingEntity target = this.getTarget();

        if (target != null) {
            int chaseTicks = this.entityData.get(CHASE_TICKS);
            chaseTicks++;
            this.entityData.set(CHASE_TICKS, chaseTicks);

            if (chaseTicks >= 200) {
                setMovementState(MOVEMENT_RUNNING_FAST);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
                this.getNavigation().setSpeedModifier(1.3);
            } else if (chaseTicks >= 100) {
                setMovementState(MOVEMENT_RUNNING);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
                this.getNavigation().setSpeedModifier(1.2);
            } else {
                setMovementState(MOVEMENT_WALKING);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
                this.getNavigation().setSpeedModifier(1.0);
            }
        } else {
            this.entityData.set(CHASE_TICKS, 0);
            setMovementState(MOVEMENT_WALKING);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
            this.getNavigation().setSpeedModifier(1.0);
        }
    }

    private void forceTargetLook() {
        LivingEntity target = this.getTarget();
        if (target != null && !this.isOrderedToSit()) {
            double deltaX = target.getX() - this.getX();
            double deltaZ = target.getZ() - this.getZ();
            double deltaY = target.getY() + target.getEyeHeight() - (this.getY() + this.getEyeHeight());

            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float yaw = (float)(Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float)(-(Math.atan2(deltaY, distance) * (180.0 / Math.PI)));

            this.setYRot(this.rotlerp(this.getYRot(), yaw, 30.0F));
            this.setXRot(this.rotlerp(this.getXRot(), pitch, 30.0F));
            this.yHeadRot = this.getYRot();
            this.yBodyRot = this.getYRot();
        }
    }

    private float rotlerp(float current, float target, float speed) {
        float delta = target - current;
        while (delta > 180.0F) {
            delta -= 360.0F;
        }
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        return current + delta * (speed / 180.0F);
    }

    private void executeJumpAttack() {
        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.4, 0));
        damageNearbyEntities(10.0f);
    }

    private void executePounceAttack() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(1.0).add(0, 0.2, 0));

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0f, 1.2f);
        }

        damageNearbyEntities(12.0f);
    }

    private void executeChargeAttack() {
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.5, 0.1, 0.5, 0.1);
        }

        damageNearbyEntitiesWithKnockback(8.0f);
    }

    private void damageNearbyEntities(float damage) {
        AABB damageArea = this.getBoundingBox().inflate(2.0);
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && entity != this.getOwner() && this.canAttack(entity));

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof BoarEntity otherBoar && this.isTame() && otherBoar.isTame()) {
                UUID myOwner = this.getOwnerUUID();
                UUID otherOwner = otherBoar.getOwnerUUID();
                if (myOwner != null && myOwner.equals(otherOwner)) {
                    continue;
                }
            }

            float finalDamage = isEnraged() ? damage * 1.5f : damage;
            entity.hurt(this.level().damageSources().mobAttack(this), finalDamage);

            entity.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    STUN_DURATION,
                    0,
                    false,
                    true
            ));
        }
    }

    private void damageNearbyEntitiesWithKnockback(float damage) {
        AABB damageArea = this.getBoundingBox().inflate(2.0);
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && entity != this.getOwner() && this.canAttack(entity));

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof BoarEntity otherBoar && this.isTame() && otherBoar.isTame()) {
                UUID myOwner = this.getOwnerUUID();
                UUID otherOwner = otherBoar.getOwnerUUID();
                if (myOwner != null && myOwner.equals(otherOwner)) {
                    continue;
                }
            }

            float finalDamage = isEnraged() ? damage * 1.5f : damage;
            entity.hurt(this.level().damageSources().mobAttack(this), finalDamage);

            Vec3 direction = entity.position().subtract(this.position()).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(CHARGE_KNOCKBACK)));

            entity.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    STUN_DURATION,
                    0,
                    false,
                    true
            ));
        }
    }

    public void startAttack(int attackType) {
        this.entityData.set(ATTACK_TYPE, attackType);
        this.entityData.set(ATTACK_TICK, 0);
    }

    private void resetAttack() {
        this.entityData.set(ATTACK_TYPE, ATTACK_NONE);
        this.entityData.set(ATTACK_TICK, 0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();

        if (!this.isTame()) {
            if (this.getTarget() != null) {
                return InteractionResult.PASS;
            }

            if (item == Items.RED_MUSHROOM || item == Items.BROWN_MUSHROOM) {
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                if (this.random.nextFloat() < 0.25f) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.heal(20.0f);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                                7, 0.5, 0.5, 0.5, 0.1);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                                3, 0.3, 0.3, 0.3, 0.05);
                    }
                    return InteractionResult.CONSUME;
                }
            } else if (item == Items.BEETROOT) {
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                if (this.random.nextFloat() < 0.20f) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.heal(20.0f);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                                7, 0.5, 0.5, 0.5, 0.1);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                                3, 0.3, 0.3, 0.3, 0.05);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        } else if (this.isOwnedBy(player)) {
            if ((item == Items.RED_MUSHROOM || item == Items.BROWN_MUSHROOM) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                this.heal(4.0f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                            5, 0.5, 0.5, 0.5, 0.1);
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                            3, 0.3, 0.3, 0.3, 0.05);
                }
                return InteractionResult.SUCCESS;
            }

            if (player.isShiftKeyDown()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean hurt = super.hurt(damageSource, amount);

        if (hurt && this.getHealth() < this.getMaxHealth() * 0.3f) {
            if (this.getTarget() != null) {
                this.getNavigation().stop();
                Vec3 retreatDirection = this.position().subtract(this.getTarget().position()).normalize();
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

    private static class BoarAttackGoal extends MeleeAttackGoal {
        private final BoarEntity boar;
        private int attackCooldown = 0;

        public BoarAttackGoal(BoarEntity boar) {
            super(boar, 1.0D, true);
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            // Don't attack if already attacking
            return super.canUse() && this.boar.getAttackType() == ATTACK_NONE;
        }

        @Override
        public boolean canContinueToUse() {
            // Stop goal if currently attacking
            if (this.boar.getAttackType() != ATTACK_NONE) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            if (attackCooldown > 0) {
                attackCooldown--;
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distance) {
            // Only attack if cooldown is ready and not currently attacking
            if (attackCooldown > 0 || this.boar.getAttackType() != ATTACK_NONE) {
                return;
            }

            if (distance <= this.getAttackReachSqr(target) && this.getTicksUntilNextAttack() <= 0) {
                int movementState = this.boar.getMovementState();

                switch (movementState) {
                    case MOVEMENT_RUNNING:
                        this.boar.startAttack(ATTACK_JUMP_HIT);
                        attackCooldown = 25; // Cooldown after jump hit
                        break;
                    case MOVEMENT_RUNNING_FAST:
                        this.boar.startAttack(ATTACK_POUNCE);
                        attackCooldown = 35; // Cooldown after pounce
                        break;
                    default:
                        super.checkAndPerformAttack(target, distance);
                        attackCooldown = 15; // Cooldown after normal attack
                        return;
                }

                this.resetAttackCooldown();
            }
        }
    }
}