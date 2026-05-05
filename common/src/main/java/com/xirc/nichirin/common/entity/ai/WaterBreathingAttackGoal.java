package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal for the Water Breathing Trainer.
 *
 * Movement is tactical:
 *  - Backstep is used as a wind-up for heavy/slow moves. The trainer creates space
 *    then immediately unleashes a charged attack.
 *  - Dash is used to close distance quickly and apply pressure with fast moves.
 *  - The trainer does not move randomly.
 *
 * Move index mapping (WaterBreathingMoveset):
 *  0 – Flowing Dance     (fast, close)
 *  1 – Striking Tide     (360°, close-mid, heavy)
 *  2 – Blessed Rain      (long dash strike, mid-long)
 *  3 – Whirlpool         (multi-hit spin, close, heavy)
 *  4 – Drop Ripple Thrust(thrust, mid)
 *  5 – Waterfall Basin   (large multi-hit, mid)
 *  6 – Splashing Water Flow (zigzag dash, mid)
 *  7 – Constant Flux     (5-hit combo finisher, close-mid, heavy)
 *  8 – Dead Calm         (AoE field, situational)
 */
public class WaterBreathingAttackGoal extends MeleeAttackGoal {

    // --- Cooldowns per move (ticks) ---
    private static final int CD_FLOWING_DANCE        = 50;   // 0
    private static final int CD_STRIKING_TIDE        = 70;   // 1
    private static final int CD_BLESSED_RAIN         = 140;  // 2
    private static final int CD_WHIRLPOOL            = 90;   // 3
    private static final int CD_DROP_RIPPLE_THRUST   = 60;   // 4
    private static final int CD_WATERFALL_BASIN      = 100;  // 5
    private static final int CD_SPLASHING_WATER_FLOW = 80;   // 6
    private static final int CD_CONSTANT_FLUX        = 120;  // 7

    private static final int GLOBAL_COOLDOWN_MIN = 10;
    private static final int GLOBAL_COOLDOWN_MAX = 20;

    // Ticks to wait after a backstep before releasing the queued heavy move.
    // Keeps the trainer mid-air / still backing up while the move starts.
    private static final int BACKSTEP_RELEASE_DELAY = 12;

    private final WaterBreathingTrainerEntity trainer;

    private int globalCooldown      = 0;
    private int[] moveCooldowns     = new int[8];
    private int backstepCooldown    = 0;
    private int doubleJumpCooldown  = 0;

    // Pending heavy move: queued after a backstep so it fires mid-backstep
    private int pendingMoveIndex    = -1;
    private int pendingMoveDelay    = 0;

    // Stuck detection
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
        return trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    public void tick() {
        super.tick();

        // Tick down all cooldowns
        if (globalCooldown   > 0) globalCooldown--;
        if (backstepCooldown > 0) backstepCooldown--;
        if (doubleJumpCooldown > 0) doubleJumpCooldown--;
        if (pendingMoveDelay > 0) pendingMoveDelay--;
        for (int i = 0; i < moveCooldowns.length; i++) {
            if (moveCooldowns[i] > 0) moveCooldowns[i]--;
        }

        LivingEntity target = trainer.getTarget();
        if (target == null || !target.isAlive()) return;

        trainer.getLookControl().setLookAt(target, 30f, 30f);

        double distSq = trainer.distanceToSqr(target);

        // Stuck detection — nudge navigation if the trainer hasn't moved
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

        // Fire pending heavy move once the delay expires (set up after a backstep)
        if (pendingMoveIndex >= 0 && pendingMoveDelay == 0) {
            int idx = pendingMoveIndex;
            pendingMoveIndex = -1;
            if (trainer.getMoveset() != null && trainer.canUseMove(idx)) {
                trainer.performMovesetMove(idx);
                moveCooldowns[idx] = getCooldownForMove(idx);
                globalCooldown = randomGlobalCooldown();
            }
            return;
        }

        if (globalCooldown > 0 || pendingMoveIndex >= 0) return;

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

        // --- Tactical decision tree ---

        if (distSq < 4.0 * 4.0) {
            // Very close — decide between a fast attack or backstep-into-heavy
            boolean heavyReady = canUseAnyHeavyMove();
            if (heavyReady && backstepCooldown == 0 && trainer.getRandom().nextInt(3) != 0) {
                // Backstep to create space, then release a heavy move
                queueHeavyMoveAfterBackstep(distSq);
            } else {
                // Fast attack: Flowing Dance or Drop Ripple Thrust
                tryUseMove(0, CD_FLOWING_DANCE);
            }

        } else if (distSq < 8.0 * 8.0) {
            // Close-mid range — Striking Tide, Water Wheel setup, or Flowing Dance
            if (canDoMove(1) && trainer.getRandom().nextBoolean()) {
                // Striking Tide — 360° sweep; backstep slightly first
                queueMoveAfterBackstep(1, CD_STRIKING_TIDE);
            } else if (canDoMove(4)) {
                tryUseMove(4, CD_DROP_RIPPLE_THRUST);
            } else if (canDoMove(0)) {
                tryUseMove(0, CD_FLOWING_DANCE);
            } else {
                // Apply pressure with a dash to stay in melee
                applyDashPressure(target, distSq);
            }

        } else if (distSq < 16.0 * 16.0) {
            // Mid range — ranged / area moves, or dash into melee
            if (canDoMove(5) && trainer.getRandom().nextBoolean()) {
                tryUseMove(5, CD_WATERFALL_BASIN);
            } else if (canDoMove(6)) {
                // Splashing Water Flow zigzag dash — pure pressure
                tryUseMove(6, CD_SPLASHING_WATER_FLOW);
            } else if (canDoMove(2)) {
                // Blessed Rain — long dash strike, no backstep needed
                tryUseMove(2, CD_BLESSED_RAIN);
            } else {
                applyDashPressure(target, distSq);
            }

        } else {
            // Long range — dash to close or Blessed Rain
            if (canDoMove(2) && trainer.getRandom().nextInt(3) == 0) {
                tryUseMove(2, CD_BLESSED_RAIN);
            } else {
                applyDashPressure(target, distSq);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean canDoMove(int idx) {
        return moveCooldowns[idx] == 0 && trainer.canUseMove(idx);
    }

    private boolean canUseAnyHeavyMove() {
        // Heavy moves: Whirlpool (3), Striking Tide (1), Constant Flux (7), Blessed Rain (2)
        return canDoMove(3) || canDoMove(1) || canDoMove(7) || canDoMove(2);
    }

    private void tryUseMove(int idx, int cd) {
        if (!canDoMove(idx)) return;
        trainer.performMovesetMove(idx);
        moveCooldowns[idx] = cd;
        globalCooldown = randomGlobalCooldown();
    }

    /** Backstep then queue a move to fire after BACKSTEP_RELEASE_DELAY ticks. */
    private void queueMoveAfterBackstep(int moveIdx, int cd) {
        EntityMovement.applyBackstep(trainer);
        backstepCooldown    = 60;
        pendingMoveIndex    = moveIdx;
        pendingMoveDelay    = BACKSTEP_RELEASE_DELAY;
        moveCooldowns[moveIdx] = cd;
    }

    /** Choose the best available heavy move and backstep into it. */
    private void queueHeavyMoveAfterBackstep(double distSq) {
        // Prefer Constant Flux when very close, then Whirlpool, Striking Tide, Blessed Rain
        if (canDoMove(7)) {
            queueMoveAfterBackstep(7, CD_CONSTANT_FLUX);
        } else if (canDoMove(3)) {
            queueMoveAfterBackstep(3, CD_WHIRLPOOL);
        } else if (canDoMove(1)) {
            queueMoveAfterBackstep(1, CD_STRIKING_TIDE);
        } else if (canDoMove(2)) {
            queueMoveAfterBackstep(2, CD_BLESSED_RAIN);
        }
    }

    /** Dash toward the target to close distance, readying a quick follow-up. */
    private void applyDashPressure(LivingEntity target, double distSq) {
        Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
        EntityMovement.applyDash(trainer, toTarget);
        globalCooldown = 12;
    }

    private int getCooldownForMove(int idx) {
        return switch (idx) {
            case 0 -> CD_FLOWING_DANCE;
            case 1 -> CD_STRIKING_TIDE;
            case 2 -> CD_BLESSED_RAIN;
            case 3 -> CD_WHIRLPOOL;
            case 4 -> CD_DROP_RIPPLE_THRUST;
            case 5 -> CD_WATERFALL_BASIN;
            case 6 -> CD_SPLASHING_WATER_FLOW;
            case 7 -> CD_CONSTANT_FLUX;
            default -> 60;
        };
    }

    private int randomGlobalCooldown() {
        return GLOBAL_COOLDOWN_MIN + trainer.getRandom().nextInt(GLOBAL_COOLDOWN_MAX - GLOBAL_COOLDOWN_MIN);
    }
}
