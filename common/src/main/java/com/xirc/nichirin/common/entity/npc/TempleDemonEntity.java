package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.network.s2c.NPCAnimationPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.util.AzureLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TempleDemonEntity extends DemonNPCEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);

    private NPCPlayerWrapper playerWrapper;
    private DefaultDemonMoveset demonMoveset;

    private static final Map<UUID, Map<String, Long>> npcCooldowns = new HashMap<>();
    private static final Map<UUID, String> lastUsedAttack = new HashMap<>();

    // Add synced data for current animation
    private static final EntityDataAccessor<String> CURRENT_ANIM =
            SynchedEntityData.defineId(TempleDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ANIM_START_TICK =
            SynchedEntityData.defineId(TempleDemonEntity.class, EntityDataSerializers.INT);

    public TempleDemonEntity(EntityType<? extends DemonNPCEntity> entityType, Level level) {
        super(entityType, level);
        this.setDemonType("temple");

        if (!level.isClientSide) {
            initializeAttackSystem();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_ANIM, "");
        this.entityData.define(ANIM_START_TICK, 0);
    }

    public String getCurrentPlayerAnimation() {
        return this.entityData.get(CURRENT_ANIM);
    }

    public int getAnimationStartTick() {
        return this.entityData.get(ANIM_START_TICK);
    }

    private void setCurrentPlayerAnimation(String animName) {
        this.entityData.set(CURRENT_ANIM, animName);
        this.entityData.set(ANIM_START_TICK, this.tickCount);
    }

    @Override
    protected String getDefaultDemonType() {
        return "temple";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, state -> null));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void initializeAttackSystem() {
        this.playerWrapper = new NPCPlayerWrapper(this);
        this.demonMoveset = new DefaultDemonMoveset();

        MovesetData data = PlayerDataProvider.getMovesetData(playerWrapper);
        data.setDemonMovesetId("default_demon");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DemonMeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0f));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> !(entity instanceof TempleDemonEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createDemonAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 80.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 10.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 6.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 32.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    private boolean isOnNPCCooldown(String attackType) {
        Map<String, Long> cooldowns = npcCooldowns.get(this.getUUID());
        if (cooldowns == null) return false;

        Long cooldownEnd = cooldowns.get(attackType);
        if (cooldownEnd == null) return false;

        return this.level().getGameTime() < cooldownEnd;
    }

    private void setNPCCooldown(String attackType, int ticks) {
        Map<String, Long> cooldowns = npcCooldowns.computeIfAbsent(this.getUUID(), k -> new HashMap<>());
        cooldowns.put(attackType, this.level().getGameTime() + ticks);
    }

    public void performDemonAttack(String attackType) {
        if (playerWrapper == null || demonMoveset == null) return;

        if (!this.level().isClientSide) {
            if (isOnNPCCooldown(attackType)) return;

            playerWrapper.syncWithNPC();
            setNPCCooldown(attackType, 3);

            switch (attackType) {
                case "gut_punch" -> demonMoveset.handleLeftClick(playerWrapper);
                case "slash" -> demonMoveset.handleRightClick(playerWrapper, false);
                case "kick" -> demonMoveset.performMove(playerWrapper, 0);
                case "dashing_strike" -> demonMoveset.performMove(playerWrapper, 1);
                case "bite" -> demonMoveset.performMove(playerWrapper, 2);
                case "high_jump" -> demonMoveset.handleRightClick(playerWrapper, true);
            }

            String animationName = mapAttackToAnimation(attackType);

            // Set animation on entity (syncs to clients)
            setAnimation(animationName, 1.0f);
            setCurrentPlayerAnimation(animationName);

            // Send packet to nearby players for PlayerAnimator
            NPCAnimationPacket packet = NPCAnimationPacket.playAnimation(this.getId(), animationName);
            this.level().players().stream()
                    .filter(player -> player.distanceToSqr(this) < 64 * 64)
                    .forEach(player -> {
                        if (player instanceof ServerPlayer serverPlayer) {
                            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
                        }
                    });

            lastUsedAttack.put(this.getUUID(), attackType);

            System.out.println("Server: Performed attack " + attackType + " -> animation " + animationName);
        }
    }

    private String mapAttackToAnimation(String attackType) {
        return switch (attackType) {
            case "gut_punch" -> "demon_gut_punch";
            case "slash" -> "demon_slash";
            case "kick" -> "demon_kick";
            case "dashing_strike" -> "demon_dash_strike";
            case "bite" -> "demon_bite";
            case "high_jump" -> "demon_high_jump";
            default -> attackType;
        };
    }

    @Override
    protected void tickDemonSystems() {
        if (playerWrapper != null) {
            playerWrapper.syncWithNPC();
            DefaultDemonMoveset.tickPlayer(playerWrapper);
            MoveExecutor.tickAttacks(playerWrapper);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            if (playerWrapper != null) {
                DefaultDemonMoveset.cleanupPlayer(playerWrapper);
                MoveExecutor.clearAttacks(playerWrapper);
            }
            npcCooldowns.remove(this.getUUID());
            lastUsedAttack.remove(this.getUUID());
        }

        super.remove(reason);
    }

    public static class DemonMeleeAttackGoal extends MeleeAttackGoal {
        private final TempleDemonEntity demon;
        private int attackCooldown = 0;
        private int ticksSinceLastAttack = 0;
        private int stuckCheckTimer = 0;
        private double lastDistanceToTarget = 0;
        private int timesStuck = 0;

        public DemonMeleeAttackGoal(TempleDemonEntity demon, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(demon, speedModifier, followingTargetEvenIfNotSeen);
            this.demon = demon;
        }

        @Override
        public void tick() {
            super.tick();

            if (attackCooldown > 0) {
                attackCooldown--;
            }
            ticksSinceLastAttack++;

            LivingEntity target = demon.getTarget();
            if (target != null && target.isAlive()) {
                demon.getLookControl().setLookAt(target, 30.0F, 30.0F);

                stuckCheckTimer++;
                if (stuckCheckTimer >= 40) {
                    stuckCheckTimer = 0;
                    double currentDistance = demon.distanceToSqr(target);

                    if (Math.abs(currentDistance - lastDistanceToTarget) < 1.0) {
                        timesStuck++;

                        if (timesStuck > 2) {
                            demon.getNavigation().stop();
                            demon.getNavigation().moveTo(target, 1.2);
                            timesStuck = 0;
                        }
                    } else {
                        timesStuck = 0;
                    }

                    lastDistanceToTarget = currentDistance;
                }
            }
        }

        @Override
        public boolean canUse() {
            LivingEntity target = demon.getTarget();
            return target != null && target.isAlive() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = demon.getTarget();
            if (target == null || !target.isAlive()) {
                timesStuck = 0;
                stuckCheckTimer = 0;
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            timesStuck = 0;
            stuckCheckTimer = 0;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceSquared) {
            if (target == null || !target.isAlive()) {
                return;
            }

            double distance = Math.sqrt(distanceSquared);

            if (!isLookingAtTarget(target)) {
                return;
            }

            if (attackCooldown <= 0 && ticksSinceLastAttack >= 3) {
                this.resetAttackCooldown();

                String attack = selectSmartAttack(target, distance);
                if (attack != null) {
                    demon.performDemonAttack(attack);
                    attackCooldown = 3;
                    ticksSinceLastAttack = 0;
                    timesStuck = 0;
                }
            }
        }

        private boolean isLookingAtTarget(LivingEntity target) {
            Vec3 demonLook = demon.getLookAngle();
            Vec3 toTarget = target.position().subtract(demon.position()).normalize();
            double dotProduct = demonLook.dot(toTarget);
            return dotProduct > 0.7;
        }

        private String selectSmartAttack(LivingEntity target, double distance) {
            Vec3 targetVelocity = target.getDeltaMovement();
            boolean targetMoving = targetVelocity.horizontalDistanceSqr() > 0.01;
            boolean targetInAir = !target.onGround();

            String lastAttack = lastUsedAttack.get(demon.getUUID());

            if (distance <= 2.5) {
                if (targetMoving && isTargetMovingAway(target)) {
                    return "dashing_strike";
                }
                if (target.getHealth() < target.getMaxHealth() * 0.3f && demon.getHealth() < demon.getMaxHealth() * 0.7f) {
                    return "bite";
                }
                if ("gut_punch".equals(lastAttack)) {
                    return "slash";
                } else if ("slash".equals(lastAttack)) {
                    return "gut_punch";
                }
                return demon.random.nextBoolean() ? "gut_punch" : "slash";
            }
            else if (distance <= 5.0) {
                if (targetInAir) {
                    return "high_jump";
                }
                if (!targetMoving && !"kick".equals(lastAttack)) {
                    return "kick";
                }
                if ("slash".equals(lastAttack)) {
                    return "dashing_strike";
                } else if ("dashing_strike".equals(lastAttack)) {
                    return "slash";
                }
                return demon.random.nextBoolean() ? "slash" : "dashing_strike";
            }
            else if (distance <= 10.0) {
                return "dashing_strike";
            }
            else if (distance <= 15.0) {
                return "high_jump";
            }

            return null;
        }

        private boolean isTargetMovingAway(LivingEntity target) {
            Vec3 toTarget = target.position().subtract(demon.position()).normalize();
            Vec3 targetVelocity = target.getDeltaMovement().normalize();
            return toTarget.dot(targetVelocity) < -0.3;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return 100.0;
        }

        @Override
        protected int getAttackInterval() {
            return 3;
        }
    }
}