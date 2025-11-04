package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.client.renderer.entity.dispatcher.TempleDemonDispatcher;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import mod.azure.azurelib.util.MoveAnalysis;
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

/**
 * Temple Demon with full moveset support and smart AI
 * Configurable aggression, damage, and abilities
 */
public class TempleDemonEntity extends DemonNPCEntity {

    public final TempleDemonDispatcher dispatcher;
    public final MoveAnalysis moveAnalysis;

    public TempleDemonEntity(EntityType<? extends DemonNPCEntity> entityType, Level level) {
        super(entityType, level);
        this.setDemonType("temple_demon");
        this.dispatcher = new TempleDemonDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);

        // Assign the demon moveset
        this.setMoveset(new DefaultDemonMoveset());

        // Configure Temple Demon properties (FULLY CUSTOMIZABLE)
        this.maxBloodPoints = 15; // More blood than default
        this.maxBreathGauge = 150.0f; // More breath for abilities
        this.aggression = 0.9f; // 90% aggressive - very aggressive
        this.damageMultiplier = 1.25f; // 25% more damage
        this.attackSpeedMultiplier = 1.0f; // Normal attack speed
        this.moveSpeedMultiplier = 1.0f; // Normal movement speed
        this.canRegenBlood = true; // Can regenerate blood
        this.bloodRegenMultiplier = 1.5f; // 50% faster blood regen
        this.breathRegenMultiplier = 2.5f; // 150% faster breath regen

        // Blacklist certain moves (example: disable move index 1 - Dashing Strike for balance)
        // this.blacklistedMoves.add(1); // Uncomment to disable Dashing Strike
    }

    @Override
    protected String getDefaultDemonType() {
        return "temple_demon";
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SmartDemonAttackGoal(this, 1.2, true));
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
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 100.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 12.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 8.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 32.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            moveAnalysis.update();
            updateAnimations();
        }
    }

    private void updateAnimations() {
        String currentAnim = getCurrentAnimation();

        if (!currentAnim.isEmpty()) {
            dispatcher.playAnimation(currentAnim);
            return;
        }

        if (moveAnalysis.isMovingHorizontally()) {
            dispatcher.walk();
        } else {
            dispatcher.idle();
        }
    }

    /**
     * Smart AI goal that uses moveset moves with intelligent attack selection
     */
    public static class SmartDemonAttackGoal extends MeleeAttackGoal {
        private final TempleDemonEntity demon;
        private int attackCooldown = 0;
        private int ticksSinceLastAttack = 0;
        private int stuckCheckTimer = 0;
        private double lastDistanceToTarget = 0;
        private int timesStuck = 0;

        public SmartDemonAttackGoal(TempleDemonEntity demon, double speedModifier, boolean followingTargetEvenIfNotSeen) {
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

                // Stuck detection
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

            // Check if looking at target
            if (!isLookingAtTarget(target)) {
                return;
            }

            // Attack cooldown with aggression modifier
            int baseCooldown = 60; // 3 seconds base
            int adjustedCooldown = (int) (baseCooldown * (1.0f - demon.getAggression() * 0.5f));

            if (attackCooldown <= 0 && ticksSinceLastAttack >= adjustedCooldown) {
                this.resetAttackCooldown();

                // Select smart attack based on moveset
                int moveIndex = selectSmartMovesetAttack(target, distance);

                if (moveIndex != -1) {
                    // Perform the moveset move
                    demon.performMovesetMove(moveIndex);

                    attackCooldown = adjustedCooldown;
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

        /**
         * SMART ATTACK SELECTION: Uses moveset configuration to pick best move
         * Checks range, hitbox, breath cost, cooldown, and blacklist
         */
        private int selectSmartMovesetAttack(LivingEntity target, double distance) {
            if (demon.getMoveset() == null) {
                return -1;
            }

            Vec3 targetVelocity = target.getDeltaMovement();
            boolean targetMoving = targetVelocity.horizontalDistanceSqr() > 0.01;
            boolean targetInAir = !target.onGround();

            // Get move count from moveset
            int moveCount = demon.getMoveset().getMoveCount();

            // Try moves in priority order based on situation
            int[] priorityOrder = determinePriorityOrder(target, distance, targetMoving, targetInAir);

            for (int moveIndex : priorityOrder) {
                if (moveIndex >= moveCount) continue; // Skip invalid indices

                // Check if can use this move
                if (!demon.canUseMove(moveIndex)) {
                    continue; // Skip if blacklisted, on cooldown, or not enough breath
                }

                AbstractMoveset.MoveConfiguration config = demon.getMoveset().getMove(moveIndex);
                if (config == null) continue;

                // Check if target is within range + hitbox
                float moveRange = config.getRangeOrDefault(3.0f);
                float hitboxSize = config.getHitboxSizeOrDefault(2.0f);
                float totalReach = moveRange + hitboxSize;

                if (distance <= totalReach) {
                    // This move can hit! Return it
                    return moveIndex;
                }
            }

            // No suitable move found
            return -1;
        }

        /**
         * Determine priority order of moves based on situation
         * Returns move indices in order of priority
         */
        private int[] determinePriorityOrder(LivingEntity target, double distance, boolean targetMoving, boolean targetInAir) {
            // Default move indices for DefaultDemonMoveset:
            // 0 = Kick
            // 1 = Dashing Strike
            // 2 = Bite

            if (distance <= 2.5) {
                // Close range - prioritize kick and bite
                if (target.getHealth() < target.getMaxHealth() * 0.4f) {
                    return new int[]{2, 0, 1}; // Bite for healing, then kick, then dash
                }
                return new int[]{0, 2, 1}; // Kick first, bite second, dash last
            } else if (distance <= 5.0) {
                // Medium range
                if (targetInAir) {
                    return new int[]{1, 0, 2}; // Dash strike to close gap, kick, bite
                }
                if (targetMoving && isTargetMovingAway(target)) {
                    return new int[]{1, 0, 2}; // Dash strike to chase
                }
                return new int[]{0, 1, 2}; // Kick, dash, bite
            } else {
                // Long range - prioritize gap closers
                return new int[]{1, 0, 2}; // Dashing strike, kick, bite
            }
        }

        private boolean isTargetMovingAway(LivingEntity target) {
            Vec3 toTarget = target.position().subtract(demon.position()).normalize();
            Vec3 targetVelocity = target.getDeltaMovement().normalize();
            return toTarget.dot(targetVelocity) < -0.3;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            // Large reach for checking, actual range is handled by moveset configs
            return 100.0;
        }

        @Override
        protected int getAttackInterval() {
            // Base interval, modified by aggression in checkAndPerformAttack
            return 60;
        }
    }
}