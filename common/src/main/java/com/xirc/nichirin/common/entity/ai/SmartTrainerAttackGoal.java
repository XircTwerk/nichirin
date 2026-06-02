package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.TrainerMode;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified, aggressive combat AI for every breathing trainer.
 *
 * <p>Drives the trainer like the demon AI: chains attacks into combos, uses the trainer's
 * FULL moveset (not a hardcoded subset), reactively blocks, paces dashes/jumps on cooldowns,
 * freezes while stunned, and — crucially — uses a mobility move (any move whose config has a
 * dash speed or teleport distance) to scale up to targets that are perched above it (e.g. on a
 * tree). Falls back to a double jump when no mobility move is ready.</p>
 *
 * <p>Entirely generic: it reads each move's range/cooldown/mobility from the moveset config, so
 * it works for any trainer without per-style hardcoding and automatically respects blacklisted
 * moves via {@link BaseBreathingTrainerEntity#canUseMove(int)}.</p>
 */
public class SmartTrainerAttackGoal extends MeleeAttackGoal {

    private static final int   BACKSTEP_RELEASE_DELAY = 8;
    // Dash/mobility pacing is read from config: combat.npcDashCooldownTicks / npcMobilityCooldownTicks.
    private static final int   MAX_COMBO_LENGTH       = 6;
    private static final int   COMBO_WINDOW_TICKS     = 25;
    private static final int   COMBO_GLOBAL_CD        = 4;
    private static final double ELEVATION_THRESHOLD   = 2.5; // blocks above us before we try to climb

    private final BaseBreathingTrainerEntity trainer;

    private int globalCooldown     = 0;
    private int dashCooldown       = 0;
    private int backstepCooldown   = 0;
    private int doubleJumpCooldown = 0;
    private int mobilityCooldown   = 0;
    private int thinkPauseTicks    = 0;
    private int guardHeldTicks     = 0;

    private int pendingMoveIndex   = -1;
    private int pendingMoveDelay   = 0;

    private int    stuckCheckTimer = 0;
    private double lastDistSq      = 0;
    private int    timesStuck      = 0;

    private int comboHits   = 0;
    private int comboWindow = 0;
    private int lastComboMove = Integer.MIN_VALUE;

    public SmartTrainerAttackGoal(BaseBreathingTrainerEntity trainer, double speedModifier, boolean followEvenIfNotSeen) {
        super(trainer, speedModifier, followEvenIfNotSeen);
        this.trainer = trainer;
        setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static float aiNorm() {
        return NichirinModConfig.get().combat.npcAiLevel / 25f;
    }

    @Override
    public boolean canUse() {
        TrainerMode mode = trainer.getMode();
        return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE)
                && trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) { }

    @Override
    public void tick() {
        super.tick();

        if (globalCooldown     > 0) globalCooldown--;
        if (dashCooldown       > 0) dashCooldown--;
        if (backstepCooldown   > 0) backstepCooldown--;
        if (doubleJumpCooldown > 0) doubleJumpCooldown--;
        if (mobilityCooldown   > 0) mobilityCooldown--;
        if (pendingMoveDelay   > 0) pendingMoveDelay--;
        if (thinkPauseTicks    > 0) thinkPauseTicks--;
        if (comboWindow > 0 && --comboWindow == 0) {
            comboHits = 0;
            lastComboMove = Integer.MIN_VALUE;
        }

        LivingEntity target = trainer.getTarget();
        if (target == null || !target.isAlive()) return;

        // Frozen while stunned — no acting, dashing, or guarding.
        if (trainer.hasEffect(NichirinEffectRegistry.stunned())) {
            trainer.getNavigation().stop();
            return;
        }

        trainer.getLookControl().setLookAt(target, 360f, 360f);
        double distSq = trainer.distanceToSqr(target);
        float ai = aiNorm();

        // Reactive blocking
        if (!trainer.isGuardUp() && distSq < 6.0 * 6.0) {
            boolean targetAttacking = target.swinging || target.attackAnim > 0
                    || (target.hurtTime > 0 && target.hurtTime < 5)
                    || MoveExecutor.hasActiveAttacks(target);
            if (!targetAttacking && globalCooldown > 0 && distSq < 3.5 * 3.5) {
                targetAttacking = trainer.getRandom().nextFloat() < 0.08f * ai;
            }
            if (targetAttacking && trainer.getRandom().nextFloat() < 0.1f + 0.5f * ai) {
                trainer.raiseGuard();
                guardHeldTicks = 0;
                return;
            }
        }
        if (trainer.isGuardUp()) {
            guardHeldTicks++;
            if (guardHeldTicks > 18 + trainer.getRandom().nextInt(22)) {
                trainer.dropGuard();
                guardHeldTicks = 0;
            }
            return;
        }

        // Stuck detection
        stuckCheckTimer++;
        if (stuckCheckTimer >= 40) {
            stuckCheckTimer = 0;
            if (Math.abs(distSq - lastDistSq) < 1.0) {
                if (++timesStuck > 2) {
                    trainer.getNavigation().stop();
                    trainer.getNavigation().moveTo(target, 1.5);
                    timesStuck = 0;
                }
            } else {
                timesStuck = 0;
            }
            lastDistSq = distSq;
        }

        // Release a queued (post-backstep) move
        if (pendingMoveIndex >= 0 && pendingMoveDelay == 0) {
            int idx = pendingMoveIndex;
            pendingMoveIndex = -1;
            if (trainer.getMoveset() != null && trainer.canUseMove(idx)) {
                snapToFaceTarget();
                trainer.performMovesetMove(idx);
                registerMoveUse(idx);
            }
            return;
        }
        if (thinkPauseTicks > 0 || pendingMoveIndex >= 0) return;

        // Duel intro: wait for the player's first strike
        if (trainer.isWaitingForFirstBlow()) {
            if (distSq > 3.5 * 3.5) trainer.getNavigation().moveTo(target, 1.0);
            else trainer.getNavigation().stop();
            return;
        }

        if (trainer.getMoveset() == null) return;

        // Elevation handling: target perched above us → climb with a mobility move or a jump.
        if (handleElevation(target)) return;

        if (globalCooldown > 0) {
            if (distSq > 3.5 * 3.5) trainer.getNavigation().moveTo(target, 1.6);
            return;
        }

        // Air control while airborne
        if (!trainer.onGround() && doubleJumpCooldown == 0) {
            EntityMovement.applyAirDodge(trainer, target.position().subtract(trainer.position()).normalize());
            doubleJumpCooldown = 40;
        }

        // Lower AI levels hesitate
        if (ai < 1.0f && trainer.getRandom().nextFloat() > ai) {
            thinkPauseTicks = 8 + trainer.getRandom().nextInt(20);
            return;
        }

        decideAction(target, distSq);
    }

    /**
     * If the target is meaningfully above us, try to gain height: prefer a ready mobility move
     * (dash/teleport — these scale walls and trees), otherwise double jump toward them.
     * @return true if a climbing action was taken this tick.
     */
    private boolean handleElevation(LivingEntity target) {
        double heightDiff = target.getY() - trainer.getY();
        if (heightDiff <= ELEVATION_THRESHOLD) return false;
        double horizSq = trainer.distanceToSqr(target.getX(), trainer.getY(), target.getZ());
        if (horizSq > 14.0 * 14.0) return false; // too far horizontally; just path closer

        if (globalCooldown <= 0 && mobilityCooldown <= 0) {
            int mob = findMobilityMove();
            if (mob >= 0) {
                snapToFaceTarget();
                // Pitch up so dash/teleport moves carry the trainer upward toward the target.
                trainer.setXRot(-40f);
                trainer.performMovesetMove(mob);
                registerMoveUse(mob);
                mobilityCooldown = NichirinModConfig.get().combat.npcMobilityCooldownTicks;
                return true;
            }
        }
        if (trainer.onGround() && trainer.canDoubleJump()) {
            trainer.markDoubleJumped();
            EntityMovement.applyDoubleJump(trainer, target.position().subtract(trainer.position()));
            return true;
        }
        if (!trainer.onGround() && doubleJumpCooldown == 0 && trainer.canDoubleJump()) {
            trainer.markDoubleJumped();
            EntityMovement.applyDoubleJump(trainer, target.position().subtract(trainer.position()));
            doubleJumpCooldown = 30;
            return true;
        }
        return false;
    }

    /** Finds a ready move whose config grants mobility (dash speed or teleport distance). */
    private int findMobilityMove() {
        AbstractMoveset ms = trainer.getMoveset();
        if (ms == null) return -1;
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < ms.getMoveCount(); i++) {
            MoveConfiguration cfg = ms.getMove(i);
            if (cfg == null) continue;
            if ((cfg.hasDashSpeed() || cfg.hasTeleportDistance()) && trainer.canUseMove(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return -1;
        return candidates.get(trainer.getRandom().nextInt(candidates.size()));
    }

    private void decideAction(LivingEntity target, double distSq) {
        // Continue a combo with a different move while the window is open and we're close.
        if (comboWindow > 0 && comboHits < MAX_COMBO_LENGTH && distSq < 6.0 * 6.0) {
            if (tryRightClick()) return;
            if (tryAnyReadyMove(target)) return;
            if (fireLeftClick()) return;
        }

        if (distSq < 3.5 * 3.5) {
            // Very close: mix light attacks, moves, and the occasional backstep-into-move.
            if (fireLeftClick()) return;
            if (tryRightClick()) return;
            if (backstepCooldown == 0 && trainer.getRandom().nextFloat() < 0.25f && queueBackstepMove(target)) return;
            if (tryAnyReadyMove(target)) return;
            applyDash(target);
        } else if (distSq < 7.0 * 7.0) {
            if (tryRightClick()) return;
            if (tryAnyReadyMove(target)) return;
            if (fireLeftClick()) return;
            applyDash(target);
        } else if (distSq < 16.0 * 16.0) {
            // Mid range: favour a ranged/dash move to gap-close, then dash.
            if (tryAnyReadyMove(target)) return;
            if (tryRightClick()) return;
            applyDash(target);
        } else {
            if (tryAnyReadyMove(target)) return;
            applyDash(target);
        }
    }

    /** Tries every move (random order), skipping the last one used, and fires the first that's ready. */
    private boolean tryAnyReadyMove(LivingEntity target) {
        AbstractMoveset ms = trainer.getMoveset();
        if (ms == null) return false;
        int count = ms.getMoveCount();
        if (count == 0) return false;

        List<Integer> order = new ArrayList<>(count);
        for (int i = 0; i < count; i++) order.add(i);
        java.util.Collections.shuffle(order, new java.util.Random(trainer.getRandom().nextLong()));

        // First pass: skip the move we just used so combos vary.
        for (int idx : order) {
            if (idx == lastComboMove) continue;
            if (trainer.canUseMove(idx) && inRangeFor(idx, target)) {
                useMove(idx);
                return true;
            }
        }
        for (int idx : order) {
            if (trainer.canUseMove(idx) && inRangeFor(idx, target)) {
                useMove(idx);
                return true;
            }
        }
        return false;
    }

    /** Rough range gate so the trainer doesn't fire a point-blank move from across the arena. */
    private boolean inRangeFor(int idx, LivingEntity target) {
        MoveConfiguration cfg = trainer.getMoveset().getMove(idx);
        if (cfg == null) return false;
        // Mobility moves are always allowed (they close distance themselves).
        if (cfg.hasDashSpeed() || cfg.hasTeleportDistance()) return true;
        float range = cfg.getRangeOrDefault(4.0f);
        double allowed = (range + 2.0) * (range + 2.0);
        return trainer.distanceToSqr(target) <= allowed;
    }

    private boolean tryRightClick() {
        if (!trainer.canUseRightClickMove(false)) return false;
        snapToFaceTarget();
        trainer.performRightClickMove(false);
        startComboBeat();
        globalCooldown = comboWindow > 0 ? COMBO_GLOBAL_CD : 5;
        return true;
    }

    private boolean fireLeftClick() {
        if (trainer.getMoveset() == null) return false;
        snapToFaceTarget();
        trainer.getMoveset().handleLeftClick(trainer);
        startComboBeat();
        globalCooldown = comboWindow > 0 ? COMBO_GLOBAL_CD : 3;
        return true;
    }

    private void useMove(int idx) {
        snapToFaceTarget();
        trainer.performMovesetMove(idx);
        registerMoveUse(idx);
    }

    private void registerMoveUse(int idx) {
        lastComboMove = idx;
        startComboBeat();
        globalCooldown = comboWindow > 0 ? COMBO_GLOBAL_CD : cooldownAfterMove(idx);
    }

    private void startComboBeat() {
        comboHits++;
        comboWindow = COMBO_WINDOW_TICKS;
    }

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

    private boolean queueBackstepMove(LivingEntity target) {
        int idx = -1;
        AbstractMoveset ms = trainer.getMoveset();
        for (int i = 0; i < ms.getMoveCount(); i++) {
            if (trainer.canUseMove(i)) { idx = i; break; }
        }
        if (idx < 0) return false;
        EntityMovement.applyBackstep(trainer);
        backstepCooldown = 50;
        pendingMoveIndex = idx;
        pendingMoveDelay = BACKSTEP_RELEASE_DELAY;
        return true;
    }

    private void applyDash(LivingEntity target) {
        // Movement cooldown: don't dash every tick. Walk to close the gap if dash isn't ready.
        if (dashCooldown > 0) {
            if (trainer.distanceToSqr(target) > 3.5 * 3.5) trainer.getNavigation().moveTo(target, 1.6);
            globalCooldown = 2;
            return;
        }
        EntityMovement.applyDash(trainer, target.position().subtract(trainer.position()).normalize());
        dashCooldown = NichirinModConfig.get().combat.npcDashCooldownTicks;
        globalCooldown = 3;
    }

    private int cooldownAfterMove(int idx) {
        MoveConfiguration cfg = trainer.getMoveset() != null ? trainer.getMoveset().getMove(idx) : null;
        if (cfg == null) return 12;
        return cfg.getWindupOrDefault(8) + cfg.getDurationOrDefault(15);
    }
}