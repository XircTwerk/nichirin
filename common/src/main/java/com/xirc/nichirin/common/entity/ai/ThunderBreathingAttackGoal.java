package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.DuelDifficulty;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity.TrainerMode;
import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal for the Thunder Breathing Trainer (Jigoro Kuwajima).
 *
 * Thunder style is high-aggression, speed-focused. Hard difficulty chains
 * thunderclap-flash-style bursts with almost no breathing room.
 *
 * Move indices (ThunderBreathingMoveset wheel, index 0–5):
 *  0 – Rice Spirit         windup=120, duration=8,   cooldown=120
 *  1 – Thunder Swarm       windup=140, duration=12,  cooldown=35
 *  2 – Distant Thunder     windup=320, duration=7,   cooldown=120
 *  3 – Heat Lightning      windup=180, duration=10,  cooldown=20
 *  4 – Rumble and Flash    windup=180, duration=9,   cooldown=25
 *  5 – Honoikazuchi no Kami windup=600, duration=120, cooldown=40
 */
public class ThunderBreathingAttackGoal extends MeleeAttackGoal {

    private static final int PADDING_EASY_MIN  = 20;
    private static final int PADDING_EASY_MAX  = 50;
    private static final int PADDING_MED_MIN   = 10;
    private static final int PADDING_MED_MAX   = 25;
    private static final int PADDING_HARD_MIN  =  0;
    private static final int PADDING_HARD_MAX  =  2;

    private static final int BACKSTEP_RELEASE_DELAY = 8;

    private static final float THINK_CHANCE_EASY = 0.04f;
    private static final float THINK_CHANCE_MED  = 0.05f;

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

    public ThunderBreathingAttackGoal(ThunderBreathingTrainerEntity trainer, double speedModifier, boolean followEvenIfNotSeen) {
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
            double approachSpeed = trainer.getDuelDifficulty() == DuelDifficulty.HARD ? 1.6 : 1.2;
            if (distSq > 3.5 * 3.5) trainer.getNavigation().moveTo(target, approachSpeed);
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

        float thinkChance = thinkChance();
        if (thinkChance > 0 && trainer.getRandom().nextFloat() < thinkChance) {
            thinkPauseTicks = 6 + trainer.getRandom().nextInt(12);
            if (trainer.getRandom().nextBoolean() && distSq > 3.0 * 3.0 && backstepCooldown == 0) {
                EntityMovement.applyBackstep(trainer);
                backstepCooldown = 25;
            }
            return;
        }

        decideAction(target, distSq);
    }

    private void decideAction(LivingEntity target, double distSq) {
        if (distSq < 3.5 * 3.5) {
            // Very close — aggressive combo, M2 thunderclap to reposition
            if (trainer.canUseRightClickMove(false) && trainer.getRandom().nextInt(3) == 0) {
                useRightClick(false); // Thunderclap Flash — instant gap reopen
            } else if (canDo(3) && trainer.getRandom().nextInt(3) == 0) {
                useMove(3); // Heat Lightning upward launch
            } else if (canDo(0) && backstepCooldown == 0) {
                queueAfterBackstep(0); // Rice Spirit burst after backstep
            } else if (canDo(0)) {
                useMove(0);
            } else if (canDo(1)) {
                useMove(1);
            } else {
                applyDash(target);
            }

        } else if (distSq < 7.0 * 7.0) {
            // Close-mid — dash in with thunderclap or swarm
            if (trainer.canUseRightClickMove(false) && trainer.getRandom().nextBoolean()) {
                useRightClick(false);
            } else if (canDo(1) && trainer.getRandom().nextBoolean()) {
                useMove(1); // Thunder Swarm
            } else if (canDo(4)) {
                useMove(4); // Rumble and Flash
            } else if (canDo(0)) {
                useMove(0);
            } else {
                applyDash(target);
            }

        } else if (distSq < 15.0 * 15.0) {
            // Mid — thunderclap to close instantly, or rumble flash
            if (trainer.canUseRightClickMove(false) && trainer.getRandom().nextInt(3) == 0) {
                useRightClick(false);
            } else if (canDo(4) && trainer.getRandom().nextBoolean()) {
                useMove(4);
            } else if (canDo(2)) {
                useMove(2); // Distant Thunder area denial
            } else {
                applyDash(target);
            }

        } else {
            // Long — thunderclap to close gap, or ultimate
            if (trainer.canUseRightClickMove(false)) {
                useRightClick(false);
            } else if (canDo(5) && trainer.getRandom().nextInt(6) == 0) {
                useMove(5); // Honoikazuchi rarely
            } else if (canDo(4)) {
                useMove(4);
            } else {
                applyDash(target);
            }
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
        // Thunderclap flash is nearly instant — very short global cooldown
        globalCooldown = switch (trainer.getDuelDifficulty()) {
            case EASY   -> 35;
            case MEDIUM -> 15;
            case HARD   -> 5;
        };
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
        globalCooldown = 10;
    }

    private int cooldownAfterMove(int idx) {
        MoveConfiguration cfg = trainer.getMoveset() != null ? trainer.getMoveset().getMove(idx) : null;
        if (cfg == null) return 30;

        int animBase  = cfg.getWindupOrDefault(8) + cfg.getDurationOrDefault(15);
        int movesetCd = cfg.getCooldownOrDefault(80);

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
            case HARD   -> 0f;
        };
    }
}
