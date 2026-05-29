package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.TrainerMode;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal for the Thunder Breathing Trainer (Jigoro Kuwajima).
 *
 * Move indices (ThunderBreathingMoveset wheel, index 0-5):
 *  0 - Rice Spirit         windup=120, duration=8,   cooldown=120
 *  1 - Thunder Swarm       windup=140, duration=12,  cooldown=35
 *  2 - Distant Thunder     windup=320, duration=7,   cooldown=120
 *  3 - Heat Lightning      windup=180, duration=10,  cooldown=20
 *  4 - Rumble and Flash    windup=180, duration=9,   cooldown=25
 *  5 - Honoikazuchi no Kami windup=600, duration=120, cooldown=40
 */
public class ThunderBreathingAttackGoal extends MeleeAttackGoal {

    private static final int BACKSTEP_RELEASE_DELAY = 8;

    private final ThunderBreathingTrainerEntity trainer;

    private int globalCooldown     = 0;
    private int backstepCooldown   = 0;
    private int doubleJumpCooldown = 0;
    private int thinkPauseTicks    = 0;

    private int pendingMoveIndex   = -1;
    private int pendingMoveDelay   = 0;

    private int    stuckCheckTimer = 0;
    private double lastDistSq      = 0;
    private int    timesStuck      = 0;
    private int    guardHeldTicks  = 0;

    public ThunderBreathingAttackGoal(ThunderBreathingTrainerEntity trainer, double speedModifier, boolean followEvenIfNotSeen) {
        super(trainer, speedModifier, followEvenIfNotSeen);
        this.trainer = trainer;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        TrainerMode mode = trainer.getMode();
        return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE)
                && trainer.getTarget() != null && trainer.getTarget().isAlive();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distSq) { }

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

        // Reactive blocking
        float ai = aiNorm();
        if (!trainer.isGuardUp() && distSq < 6.0 * 6.0) {
            boolean targetAttacking = target.swinging || target.attackAnim > 0
                    || MoveExecutor.hasActiveAttacks(target);
            if (!targetAttacking && globalCooldown > 0 && distSq < 3.5 * 3.5) {
                targetAttacking = trainer.getRandom().nextFloat() < 0.08f * ai;
            }
            if (targetAttacking) {
                if (trainer.getRandom().nextFloat() < 0.1f + 0.5f * ai) {
                    trainer.raiseGuard();
                    guardHeldTicks = 0;
                    return;
                }
            }
        }
        if (trainer.isGuardUp()) {
            guardHeldTicks++;
            if (guardHeldTicks > 20 + trainer.getRandom().nextInt(25)) {
                trainer.dropGuard();
                guardHeldTicks = 0;
            }
            return;
        }

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

        if (trainer.isWaitingForFirstBlow()) {
            if (distSq > 3.5 * 3.5) {
                trainer.getNavigation().moveTo(target, 1.0);
            } else {
                trainer.getNavigation().stop();
            }
            return;
        }

        if (globalCooldown > 0) {
            if (distSq > 3.5 * 3.5) trainer.getNavigation().moveTo(target, 1.6);
            return;
        }

        if (!trainer.onGround() && doubleJumpCooldown == 0) {
            Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
            EntityMovement.applyAirDodge(trainer, toTarget);
            doubleJumpCooldown = 40;
        }

        if (trainer.onGround() && target.getY() > trainer.getY() + 2.0 && trainer.canDoubleJump()) {
            trainer.markDoubleJumped();
            EntityMovement.applyDoubleJump(trainer, target.position().subtract(trainer.position()));
        }

        if (trainer.getMoveset() == null) return;

        if (ai < 1.0f && trainer.getRandom().nextFloat() > ai) {
            thinkPauseTicks = 8 + trainer.getRandom().nextInt(20);
            return;
        }

        decideAction(target, distSq);
    }

    private void decideAction(LivingEntity target, double distSq) {
        if (distSq < 3.5 * 3.5) {
            if (trainer.canUseRightClickMove(false)) useRightClick(false);
            else if (canDo(3)) useMove(3);
            else if (canDo(0) && backstepCooldown == 0) queueAfterBackstep(0);
            else if (canDo(0)) useMove(0);
            else if (canDo(1)) useMove(1);
            else applyDash(target);

        } else if (distSq < 7.0 * 7.0) {
            if (trainer.canUseRightClickMove(false)) useRightClick(false);
            else if (canDo(1)) useMove(1);
            else if (canDo(4)) useMove(4);
            else if (canDo(0)) useMove(0);
            else applyDash(target);

        } else if (distSq < 15.0 * 15.0) {
            if (trainer.canUseRightClickMove(false)) useRightClick(false);
            else if (canDo(4)) useMove(4);
            else if (canDo(2)) useMove(2);
            else applyDash(target);

        } else {
            if (trainer.canUseRightClickMove(false)) useRightClick(false);
            else if (canDo(5)) useMove(5);
            else if (canDo(4)) useMove(4);
            else applyDash(target);
        }
    }

    private boolean canDo(int idx) {
        return trainer.canUseMove(idx);
    }

    private void useMove(int idx) {
        if (!canDo(idx)) return;
        snapToFaceTarget();
        trainer.performMovesetMove(idx);
        globalCooldown = cooldownAfterMove(idx);
    }

    private void useRightClick(boolean crouching) {
        if (!trainer.canUseRightClickMove(crouching)) return;
        snapToFaceTarget();
        trainer.performRightClickMove(crouching);
        globalCooldown = 5;
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

    private void queueAfterBackstep(int idx) {
        EntityMovement.applyBackstep(trainer);
        backstepCooldown = 50;
        pendingMoveIndex = idx;
        pendingMoveDelay = BACKSTEP_RELEASE_DELAY;
    }

    private void applyDash(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
        EntityMovement.applyDash(trainer, toTarget);
        globalCooldown = 3;
    }

    private int cooldownAfterMove(int idx) {
        MoveConfiguration cfg = trainer.getMoveset() != null ? trainer.getMoveset().getMove(idx) : null;
        if (cfg == null) return 15;

        return cfg.getWindupOrDefault(8) + cfg.getDurationOrDefault(15);
    }
}
