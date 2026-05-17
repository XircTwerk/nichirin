package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.DuelDifficulty;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.TrainerMode;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal for the Water Breathing Trainer.
 *
 * Pacing: after using a move, globalCooldown = move's (windup+duration) from frame data
 * + a difficulty-based padding. Hard adds almost nothing (immediate follow-up pressure).
 * Easy adds enough that the full moveset cooldown has expired by the time the AI acts again,
 * so the move is guaranteed to be ready and Easy actually attacks.
 *
 * All difficulties can use all moves — difficulty only controls timing and think pauses.
 * Damage reduction is handled by getDifficultyDamageMultiplier() in WaterBreathingAttackBase.
 *
 * Move indices (WaterBreathingMoveset):
 *  0 – Flowing Dance        windup=15, duration=60, cooldown=240
 *  1 – Striking Tide        windup=12, duration=40, cooldown=360
 *  2 – Blessed Rain         windup=9,  duration=25, cooldown=500
 *  3 – Whirlpool            windup=14, duration=70, cooldown=380
 *  4 – Drop Ripple Thrust   windup=10, duration=35, cooldown=300
 *  5 – Waterfall Basin      windup=16, duration=60, cooldown=400
 *  6 – Splashing Water Flow windup=10, duration=40, cooldown=380
 *  7 – Constant Flux        windup=20, duration=80, cooldown=420
 */
public class WaterBreathingAttackGoal extends MeleeAttackGoal {

    // Extra ticks added AFTER a move's (windup+duration) before the next decision.
    // Easy: short gap — trainer still fights properly, just deals reduced damage.
    // Hard: 0-3 ticks — re-evaluates the instant the animation ends, chains immediately.
    private static final int PADDING_EASY_MIN   =  20;
    private static final int PADDING_EASY_MAX   =  50;
    private static final int PADDING_MED_MIN    =  15;
    private static final int PADDING_MED_MAX    =  35;
    private static final int PADDING_HARD_MIN   =   0;
    private static final int PADDING_HARD_MAX   =   3;

    // Backstep release delay — ticks into the backstep before the heavy fires
    private static final int BACKSTEP_RELEASE_DELAY = 12;

    // Chance per decision cycle to spontaneously hesitate ("think pause")
    private static final float THINK_CHANCE_EASY   = 0.04f;
    private static final float THINK_CHANCE_MED    = 0.06f;
    private static final float THINK_CHANCE_HARD   = 0.02f;

    private final WaterBreathingTrainerEntity trainer;

    private int globalCooldown      = 0;
    private int backstepCooldown    = 0;
    private int doubleJumpCooldown  = 0;
    private int thinkPauseTicks     = 0;

    private int pendingMoveIndex    = -1;
    private int pendingMoveDelay    = 0;

    private int    stuckCheckTimer  = 0;
    private double lastDistSq       = 0;
    private int    timesStuck       = 0;

    public WaterBreathingAttackGoal(WaterBreathingTrainerEntity trainer, double speedModifier, boolean followEvenIfNotSeen) {
        super(trainer, speedModifier, followEvenIfNotSeen);
        this.trainer = trainer;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        TrainerMode mode = trainer.getMode();
        return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE)
                && trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        TrainerMode mode = trainer.getMode();
        return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE)
                && trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    public void tick() {
        super.tick();

        if (globalCooldown     > 0) globalCooldown--;
        if (backstepCooldown   > 0) backstepCooldown--;
        if (doubleJumpCooldown > 0) doubleJumpCooldown--;
        if (pendingMoveDelay   > 0) pendingMoveDelay--;
        if (thinkPauseTicks    > 0) thinkPauseTicks--;

        LivingEntity target = trainer.getTarget();
        if (target == null || !target.isAlive()) return;

        trainer.getLookControl().setLookAt(target, 360f, 360f);

        double distSq = trainer.distanceToSqr(target);

        // Stuck detection
        stuckCheckTimer++;
        if (stuckCheckTimer >= 40) {
            stuckCheckTimer = 0;
            if (Math.abs(distSq - lastDistSq) < 1.0) {
                if (++timesStuck > 2) {
                    trainer.getNavigation().stop();
                    trainer.getNavigation().moveTo(target, 1.3);
                    timesStuck = 0;
                }
            } else {
                timesStuck = 0;
            }
            lastDistSq = distSq;
        }

        // Fire pending heavy move after backstep wind-up
        if (pendingMoveIndex >= 0 && pendingMoveDelay == 0) {
            int idx = pendingMoveIndex;
            pendingMoveIndex = -1;
            if (trainer.getMoveset() != null && trainer.canUseMove(idx)) {
                snapToFaceTarget();
                trainer.performMovesetMove(idx);
                globalCooldown = cooldownAfterMove(idx);
            }
            return;
        }

        if (thinkPauseTicks > 0 || pendingMoveIndex >= 0) return;

        // First-blow phase: trainer stands ready and lets the player strike first.
        // Just approaches to melee range and faces them — no attacks.
        if (trainer.isWaitingForFirstBlow()) {
            if (distSq > 3.5 * 3.5) {
                trainer.getNavigation().moveTo(target, 0.9);
            } else {
                trainer.getNavigation().stop();
            }
            return;
        }

        // While counting down, approach player
        if (globalCooldown > 0) {
            if (distSq > 4.0 * 4.0) {
                double approachSpeed = trainer.getDuelDifficulty() == DuelDifficulty.HARD ? 1.4 : 1.1;
                trainer.getNavigation().moveTo(target, approachSpeed);
            }
            return;
        }

        // Air dodge toward target while airborne
        if (!trainer.onGround() && doubleJumpCooldown == 0) {
            Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
            EntityMovement.applyAirDodge(trainer, toTarget);
            doubleJumpCooldown = 40;
        }

        // Double jump if target is elevated
        if (trainer.onGround() && target.getY() > trainer.getY() + 2.0 && trainer.canDoubleJump()) {
            trainer.markDoubleJumped();
            EntityMovement.applyDoubleJump(trainer, target.position().subtract(trainer.position()));
        }

        if (trainer.getMoveset() == null) return;

        // Spontaneous hesitation beat
        if (trainer.getRandom().nextFloat() < thinkChance()) {
            thinkPauseTicks = 8 + trainer.getRandom().nextInt(14);
            if (trainer.getRandom().nextBoolean() && distSq > 3.0 * 3.0 && backstepCooldown == 0) {
                EntityMovement.applyBackstep(trainer);
                backstepCooldown = 30;
            }
            return;
        }

        decideAction(target, distSq);
    }

    // Decision tree — no difficulty gating on move availability.
    // Difficulty only affects timing (cooldowns) and think-pause frequency
    private void decideAction(LivingEntity target, double distSq) {
        if (distSq < 4.0 * 4.0) {
            // Close — backstep into heavy, or fast jab
            if (canUseAnyHeavy() && backstepCooldown == 0 && trainer.getRandom().nextInt(3) != 0) {
                queueHeavyAfterBackstep();
            } else if (canDo(0)) useMove(0);
            else if (canDo(4)) useMove(4);
            else applyDash(target);

        } else if (distSq < 8.0 * 8.0) {
            // Close-mid
            if (canDo(1) && trainer.getRandom().nextBoolean()) queueAfterBackstep(1);
            else if (canDo(4)) useMove(4);
            else if (canDo(0)) useMove(0);
            else applyDash(target);

        } else if (distSq < 16.0 * 16.0) {
            // Mid
            if (canDo(5) && trainer.getRandom().nextBoolean()) useMove(5);
            else if (canDo(6)) useMove(6);
            else if (canDo(2)) useMove(2);
            else applyDash(target);

        } else {
            // Long
            if (canDo(2) && trainer.getRandom().nextInt(3) == 0) useMove(2);
            else applyDash(target);
        }
    }

    // Helpers
    private boolean canDo(int idx) {
        return trainer.canUseMove(idx);
    }

    private boolean canUseAnyHeavy() {
        return canDo(3) || canDo(1) || canDo(7) || canDo(2);
    }

    private void useMove(int idx) {
        if (!canDo(idx)) return;
        snapToFaceTarget();
        trainer.performMovesetMove(idx);
        globalCooldown = cooldownAfterMove(idx);
    }

    /** Instantly aligns trainer's yRot/yBodyRot to face the current target so hitboxes land. */
    private void snapToFaceTarget() {
        LivingEntity target = trainer.getTarget();
        if (target == null) return;
        double dx = target.getX() - trainer.getX();
        double dz = target.getZ() - trainer.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        trainer.setYRot(yaw);
        trainer.yRotO = yaw;
        trainer.setYBodyRot(yaw);
        trainer.setYHeadRot(yaw);
    }

    private void queueAfterBackstep(int idx) {
        EntityMovement.applyBackstep(trainer);
        backstepCooldown = 60;
        pendingMoveIndex = idx;
        pendingMoveDelay = BACKSTEP_RELEASE_DELAY;
    }

    private void queueHeavyAfterBackstep() {
        if      (canDo(7)) queueAfterBackstep(7);
        else if (canDo(3)) queueAfterBackstep(3);
        else if (canDo(1)) queueAfterBackstep(1);
        else if (canDo(2)) queueAfterBackstep(2);
    }

    private void applyDash(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
        EntityMovement.applyDash(trainer, toTarget);
        globalCooldown = 14;
    }

    /**
     * Global cooldown after firing move idx.
     *
     * Easy:   moveset CD + padding — guarantees move IS ready when AI acts again.
     *         Trainer attacks slowly but reliably.
     * Medium: animation (windup+duration) + small padding. Move may not be ready;
     *         trainer dashes until it is.
     * Hard:   barely more than animation time. Trainer applies constant dash pressure
     *         and attacks the moment a move becomes available.
     */
    private int cooldownAfterMove(int idx) {
        MoveConfiguration cfg = trainer.getMoveset() != null ? trainer.getMoveset().getMove(idx) : null;
        if (cfg == null) return 40;

        int animBase   = cfg.getWindupOrDefault(10) + cfg.getDurationOrDefault(20);
        int movesetCd  = cfg.getCooldownOrDefault(120);

        return switch (trainer.getDuelDifficulty()) {
            case EASY   -> movesetCd  + PADDING_EASY_MIN + trainer.getRandom().nextInt(PADDING_EASY_MAX - PADDING_EASY_MIN);
            case MEDIUM -> animBase   + PADDING_MED_MIN  + trainer.getRandom().nextInt(PADDING_MED_MAX  - PADDING_MED_MIN);
            case HARD   -> animBase   + PADDING_HARD_MIN + trainer.getRandom().nextInt(PADDING_HARD_MAX - PADDING_HARD_MIN);
        };
    }

    private float thinkChance() {
        return switch (trainer.getDuelDifficulty()) {
            case EASY   -> THINK_CHANCE_EASY;
            case MEDIUM -> THINK_CHANCE_MED;
            case HARD   -> 0f; // Hard never hesitates
        };
    }
}
