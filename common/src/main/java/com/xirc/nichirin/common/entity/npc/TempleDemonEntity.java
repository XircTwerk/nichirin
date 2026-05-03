package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.client.renderer.entity.dispatcher.TempleDemonDispatcher;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import com.xirc.nichirin.common.system.GrabManager;
import mod.azure.azurelib.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * Temple Demon with full moveset support and smart AI
 * Configurable aggression, damage, and abilities
 */
public class TempleDemonEntity extends DemonNPCEntity {

    public final TempleDemonDispatcher dispatcher;
    public final MoveAnalysis moveAnalysis;

    // Client-side animation state
    private String consumedAttackAnim = ""; // which attack anim we already dispatched this cycle
    private int animCooldownTicks = 0;
    private Boolean lastWasWalking = null; // null = uninitialized, forces idle dispatch on first tick

    // Server-side animation auto-clear countdown
    private int serverAnimTicksRemaining = 0;

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
        this.goalSelector.addGoal(2, new DayShelterGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 32.0f));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> !(entity instanceof TempleDemonEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createDemonAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 50.0)
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
        } else {
            DefaultDemonMoveset.tickEntity(this);
            GrabManager.tick(this);
            tickSunDamage();
        }
    }

    private void tickSunDamage() {
        Level level = this.level();
        if (!level.isDay()) return;
        if (!level.canSeeSky(this.blockPosition())) return;
        // Exposed to direct sunlight — take heavy damage every 4 ticks (5 HP/s)
        if ((level.getGameTime() % 4) == 0) {
            this.hurt(this.damageSources().onFire(), 1.0f);
        }
        this.setSecondsOnFire(4);
    }

    private void updateAnimations() {
        String serverAnim = getCurrentAnimation();

        // Dispatch a new attack animation only if we haven't consumed it yet (or server reset it)
        if (!serverAnim.isEmpty() && (!serverAnim.equals(consumedAttackAnim) || wasAnimationReset())) {
            dispatcher.playAnimation(serverAnim);
            consumedAttackAnim = serverAnim;
            animCooldownTicks = getAnimDurationTicks(serverAnim);
            lastWasWalking = false;
        }

        // Wait while the attack animation plays out
        if (animCooldownTicks > 0) {
            animCooldownTicks--;
            return;
        }

        // Server cleared the anim — allow same anim name to re-trigger next time
        if (serverAnim.isEmpty()) {
            consumedAttackAnim = "";
        }

        // Idle/walk — only re-dispatch when state changes (lastWasWalking == null on first tick)
        boolean walking = moveAnalysis.isMovingHorizontally();
        if (!Boolean.valueOf(walking).equals(lastWasWalking)) {
            if (walking) {
                dispatcher.walk();
            } else {
                dispatcher.idle();
            }
            lastWasWalking = walking;
        }
    }

    @Override
    public void triggerMovesetAnimation(String animationName) {
        super.triggerMovesetAnimation(animationName);
        // Give it a small buffer over the client-side duration so it always fully plays
        serverAnimTicksRemaining = getAnimDurationTicks(animationName) + 4;
    }

    @Override
    protected void handleAnimationTick() {
        if (serverAnimTicksRemaining > 0) {
            serverAnimTicksRemaining--;
            if (serverAnimTicksRemaining == 0) {
                stopAnimation();
            }
        } else {
            // Safety: no countdown set (e.g. from old save data) — clear immediately
            stopAnimation();
        }
    }

    private int getAnimDurationTicks(String animName) {
        return switch (animName) {
            case "demon_slash", "demon_slash_2" -> 10;
            case "demon_gut_punch"              -> 10;
            case "demon_grab"                   -> 10;
            case "demon_high_jump"              -> 15;
            case "demon_stomp"                  -> 12;
            case "demon_bite"                   -> 14;
            case "demon_kick"                   -> 12;
            case "demon_dash_strike"            -> 20;
            default                             -> 20;
        };
    }

    /**
     * AI goal — during daytime, if the demon is exposed to direct sky, seek nearby covered shelter.
     * Does not apply while the demon has an active target (attacking takes priority).
     */
    private static class DayShelterGoal extends Goal {

        private final TempleDemonEntity demon;
        private Vec3 shelterTarget = null;
        private int cooldown = 0;

        public DayShelterGoal(TempleDemonEntity demon) {
            this.demon = demon;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (demon.getTarget() != null) return false;            // Only active during the day and when exposed to open sky
            Level level = demon.level();
            if (!level.isDay()) return false;
            return level.canSeeSky(demon.blockPosition());
        }

        @Override
        public boolean canContinueToUse() {
            if (cooldown-- > 0) return true;
            Level level = demon.level();
            if (!level.isDay() || demon.getTarget() != null) return false;
            return level.canSeeSky(demon.blockPosition());
        }

        @Override
        public void start() {
            cooldown = 100;
            shelterTarget = findShelter();
            if (shelterTarget != null) {
                demon.getNavigation().moveTo(shelterTarget.x, shelterTarget.y, shelterTarget.z, 1.1);
            }
        }

        @Override
        public void tick() {
            if (shelterTarget == null || demon.getNavigation().isDone()) {
                shelterTarget = findShelter();
                if (shelterTarget != null) {
                    demon.getNavigation().moveTo(shelterTarget.x, shelterTarget.y, shelterTarget.z, 1.1);
                }
            }
        }

        @Override
        public void stop() {
            demon.getNavigation().stop();
            shelterTarget = null;
        }

        private Vec3 findShelter() {
            Level level = demon.level();
            // Try random positions nearby, pick one that has a block above it (sheltered)
            for (int i = 0; i < 16; i++) {
                int dx = demon.getRandom().nextIntBetweenInclusive(-20, 20);
                int dz = demon.getRandom().nextIntBetweenInclusive(-20, 20);
                BlockPos surface = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        demon.blockPosition().offset(dx, 0, dz));
                // If the block at this surface isn't sky-exposed (cave entrance, overhang, etc.)
                if (!level.canSeeSky(surface)) {
                    return Vec3.atCenterOf(surface);
                }
            }
            // No surface shelter found — try going one layer underground
            BlockPos under = demon.blockPosition().below(4);
            return Vec3.atCenterOf(under);
        }
    }

    /**
     * Smart AI goal — uses ALL demon moves: left click, right click combos, and wheel moves.
     *
     * Move indices used here:
     *   ATTACK_LEFT  (-3) = Gut Punch     (handleLeftClick)
     *   ATTACK_RIGHT (-1) = Slash combo   (handleRightClick)
     *   0 = Kick
     *   1 = Dashing Strike
     *   2 = Bite
     */
    public static class SmartDemonAttackGoal extends MeleeAttackGoal {

        // Sentinel constants so we can distinguish click attacks from wheel moves
        private static final int ATTACK_LEFT      = -3;
        private static final int ATTACK_RIGHT     = -1;
        private static final int ATTACK_HIGH_JUMP = -99;
        private static final int ATTACK_GRAB      = -5;
        private static final int ATTACK_NONE      = Integer.MIN_VALUE;

        private final TempleDemonEntity demon;

        // Global gate: minimum ticks between any attack
        private int globalCooldown = 0;

        // Per-attack-type cooldowns (ticks)
        private int cooldownLeft      = 0;  // Gut Punch
        private int cooldownRight     = 0;  // Slash
        private int cooldownMove0     = 0;  // Kick
        private int cooldownMove1     = 0;  // Dashing Strike
        private int cooldownMove2     = 0;  // Bite
        private int cooldownMove3     = 0;  // Grab
        private int cooldownHighJump  = 0;  // High Jump

        // Slash combo tracking: if we recently did a slash, try to chain immediately
        private int slashChainWindow = 0;

        // Stomp tracking: set after high jump, cleared when stomp fires
        private boolean pendingStomp = false;
        private int     stompDelay   = 0;   // ticks to wait before stomping

        // Stuck detection
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

            if (globalCooldown   > 0) globalCooldown--;
            if (cooldownLeft     > 0) cooldownLeft--;
            if (cooldownRight    > 0) cooldownRight--;
            if (cooldownMove0    > 0) cooldownMove0--;
            if (cooldownMove1    > 0) cooldownMove1--;
            if (cooldownMove2    > 0) cooldownMove2--;
            if (cooldownMove3    > 0) cooldownMove3--;
            if (cooldownHighJump > 0) cooldownHighJump--;
            if (slashChainWindow > 0) slashChainWindow--;

            // Auto-stomp after high jump: wait until airborne then fire stomp
            if (stompDelay > 0) {
                stompDelay--;
            } else if (pendingStomp && !demon.onGround() && demon.getTarget() != null) {
                demon.getMoveset().handleRightClick(demon, true); // canStompAfterHighJump is set → stomp
                pendingStomp  = false;
                globalCooldown = 20;
            }

            LivingEntity target = demon.getTarget();
            if (target != null && target.isAlive()) {
                demon.getLookControl().setLookAt(target, 30.0F, 30.0F);

                stuckCheckTimer++;
                if (stuckCheckTimer >= 40) {
                    stuckCheckTimer = 0;
                    double currentDistance = demon.distanceToSqr(target);
                    if (Math.abs(currentDistance - lastDistanceToTarget) < 1.0) {
                        if (++timesStuck > 2) {
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
                resetStuck();
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            resetStuck();
        }

        private void resetStuck() {
            timesStuck = 0;
            stuckCheckTimer = 0;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceSquared) {
            if (target == null || !target.isAlive() || demon.getMoveset() == null) return;
            if (!isLookingAtTarget(target)) return;
            if (globalCooldown > 0) return;

            double distance = Math.sqrt(distanceSquared);
            boolean targetMovingAway = isTargetMovingAway(target);
            boolean targetInAir = !target.onGround();
            boolean targetLowHp = target.getHealth() < target.getMaxHealth() * 0.4f;

            // ── Slash combo chain: fire right-click again immediately ──
            if (slashChainWindow > 0 && cooldownRight == 0 && distance <= 5.0) {
                fireRightClick();
                return;
            }

            // ── Pick the best attack for this situation ──
            int chosen = selectAttack(distance, targetMovingAway, targetInAir, targetLowHp);
            if (chosen == ATTACK_NONE) return;

            this.resetAttackCooldown();
            timesStuck = 0;

            switch (chosen) {
                case ATTACK_LEFT      -> fireLeftClick();
                case ATTACK_RIGHT     -> fireRightClick();
                case ATTACK_HIGH_JUMP -> fireHighJump();
                case ATTACK_GRAB      -> fireGrab();
                default               -> fireWheelMove(chosen);
            }
        }

        // ── Execution helpers ──────────────────────────────────────────

        private void fireLeftClick() {
            demon.getMoveset().handleLeftClick(demon);
            cooldownLeft   = 40;
            globalCooldown = 25;
        }

        private void fireRightClick() {
            demon.getMoveset().handleRightClick(demon, false);
            int wasInChain = slashChainWindow;
            if (wasInChain > 0) {
                cooldownRight    = 60;
                slashChainWindow = 0;
            } else {
                cooldownRight    = 15;
                slashChainWindow = 25;
            }
            globalCooldown = 12;
        }

        private void fireHighJump() {
            demon.getMoveset().handleRightClick(demon, true); // sets canStompAfterHighJump
            cooldownHighJump = 160;
            globalCooldown   = 5;
            pendingStomp     = true;
            stompDelay       = 10; // wait 10 ticks to be well airborne
        }

        private void fireGrab() {
            demon.performMovesetMove(3); // index 3 = Grab
            cooldownMove3  = 100; // long cooldown after initiating a grab
            globalCooldown = 15;
        }

        private void fireWheelMove(int moveIndex) {
            demon.performMovesetMove(moveIndex);
            int cd = getWheelMoveCooldown(moveIndex);
            switch (moveIndex) {
                case 0 -> cooldownMove0 = cd;
                case 1 -> cooldownMove1 = cd;
                case 2 -> cooldownMove2 = cd;
                case 3 -> cooldownMove3 = cd;
            }
            globalCooldown = 30;
        }

        private int getWheelMoveCooldown(int index) {
            AbstractMoveset.MoveConfiguration cfg = demon.getMoveset().getMove(index);
            if (cfg != null && cfg.hasCooldown()) return cfg.getCooldown();
            return switch (index) {
                case 0 -> 60;   // Kick
                case 1 -> 80;   // Dashing Strike
                case 2 -> 100;  // Bite
                case 3 -> 80;   // Grab
                default -> 60;
            };
        }

        // ── Attack selection ───────────────────────────────────────────

        /**
         * Returns the attack constant/index to use, or ATTACK_NONE.
         * Priority logic mirrors a real combo fighter:
         *   Very close  → gut punch or bite → slash
         *   Close       → slash → kick → gut punch
         *   Medium      → slash → kick → dash strike
         *   Far         → dash strike → slash
         */
        private int selectAttack(double distance, boolean movingAway, boolean inAir, boolean lowHp) {
            // ── High jump to intercept airborne target ─────────────────────
            if (inAir && distance <= 4.0 && cooldownHighJump == 0 && demon.onGround()) {
                return ATTACK_HIGH_JUMP;
            }

            // ── Very close: grab / gut-punch / bite range ─────────────────
            // Effective ranges: gut-punch 3.0, grab 3.5, bite 3.8
            if (distance <= 3.5) {
                if (cooldownMove3 == 0 && canWheelMove(3))            return ATTACK_GRAB;
                if (lowHp && cooldownMove2 == 0 && canWheelMove(2))   return 2; // Bite
                if (cooldownLeft == 0)                                 return ATTACK_LEFT;
                if (cooldownRight == 0 && distance <= 5.0)            return ATTACK_RIGHT;
                if (cooldownMove0 == 0 && canWheelMove(0))            return 0; // Kick
                if (cooldownMove2 == 0 && canWheelMove(2))            return 2;
            }

            // ── Close: slash + kick range ─────────────────────────────────
            // Effective: slash 5.0, kick 4.5
            if (distance <= 4.5) {
                if (cooldownRight == 0)                                return ATTACK_RIGHT;
                if (cooldownLeft == 0)                                 return ATTACK_LEFT;
                if (cooldownMove0 == 0 && canWheelMove(0))            return 0;
                if (cooldownMove2 == 0 && canWheelMove(2) && distance <= 3.8) return 2;
            }

            // ── Medium: slash or dash-strike ──────────────────────────────
            if (distance <= 5.5) {
                if (movingAway || inAir) {
                    if (cooldownMove1 == 0 && canWheelMove(1))        return 1;
                    if (cooldownHighJump == 0 && demon.onGround())    return ATTACK_HIGH_JUMP;
                }
                if (cooldownRight == 0)                                return ATTACK_RIGHT;
                if (cooldownMove0 == 0 && canWheelMove(0))            return 0;
                if (cooldownMove1 == 0 && canWheelMove(1))            return 1;
            }

            // ── Far: close the gap ─────────────────────────────────────────
            if (distance <= 9.0) {
                if (cooldownMove1 == 0 && canWheelMove(1))            return 1;
                if (cooldownHighJump == 0 && demon.onGround())        return ATTACK_HIGH_JUMP;
            }

            return ATTACK_NONE;
        }

        private boolean canWheelMove(int index) {
            return demon.canUseMove(index);
        }

        // ── Utility ───────────────────────────────────────────────────

        private boolean isLookingAtTarget(LivingEntity target) {
            Vec3 demonLook = demon.getLookAngle();
            Vec3 toTarget = target.position().subtract(demon.position()).normalize();
            return demonLook.dot(toTarget) > 0.6;
        }

        private boolean isTargetMovingAway(LivingEntity target) {
            Vec3 toTarget = target.position().subtract(demon.position()).normalize();
            Vec3 targetVel = target.getDeltaMovement().normalize();
            return toTarget.dot(targetVel) < -0.3;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return 100.0; // Actual range is enforced per-move above
        }

        @Override
        protected int getAttackInterval() {
            return 20;
        }
    }
}