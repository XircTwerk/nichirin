package com.xirc.nichirin.common.entity.ai;

import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class WaterBreathingAttackGoal extends MeleeAttackGoal {

    private static final int GLOBAL_COOLDOWN_MIN = 12;
    private static final int GLOBAL_COOLDOWN_MAX = 22;

    private final WaterBreathingTrainerEntity trainer;

    private int globalCooldown = 0;
    private int cooldownMove0  = 0; // Water Surface Slash
    private int cooldownMove1  = 0; // Water Wheel
    private int cooldownMove2  = 0; // Flowing Dance
    private int cooldownMove3  = 0; // Striking Tide
    private int cooldownMove4  = 0; // Drop Ripple Thrust
    private int cooldownMove5  = 0; // Whirlpool (if present)
    private int backstepCooldown = 0;
    private int doubleJumpCooldown = 0;

    private int stuckCheckTimer = 0;
    private double lastDistSq   = 0;
    private int timesStuck      = 0;

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

        if (globalCooldown   > 0) globalCooldown--;
        if (cooldownMove0    > 0) cooldownMove0--;
        if (cooldownMove1    > 0) cooldownMove1--;
        if (cooldownMove2    > 0) cooldownMove2--;
        if (cooldownMove3    > 0) cooldownMove3--;
        if (cooldownMove4    > 0) cooldownMove4--;
        if (cooldownMove5    > 0) cooldownMove5--;
        if (backstepCooldown > 0) backstepCooldown--;
        if (doubleJumpCooldown > 0) doubleJumpCooldown--;

        LivingEntity target = trainer.getTarget();
        if (target == null || !target.isAlive()) return;

        trainer.getLookControl().setLookAt(target, 30f, 30f);

        double distSq = trainer.distanceToSqr(target);

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

        if (globalCooldown > 0) return;

        // Air dodge toward target while airborne
        if (!trainer.onGround() && doubleJumpCooldown == 0) {
            Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
            EntityMovement.applyAirDodge(trainer, toTarget);
            doubleJumpCooldown = 40;
        }

        // Double jump if target is above and we're on ground
        if (trainer.onGround() && target.getY() > trainer.getY() + 2.0 && trainer.canDoubleJump()) {
            trainer.markDoubleJumped();
            EntityMovement.applyDoubleJump(trainer, target.position().subtract(trainer.position()));
        }

        // Backstep if target is very close — create distance
        if (distSq < 3.0 * 3.0 && backstepCooldown == 0 && trainer.getRandom().nextInt(4) == 0) {
            EntityMovement.applyBackstep(trainer);
            backstepCooldown = 60;
            globalCooldown = 20;
            return;
        }

        if (trainer.getMoveset() == null) return;

        // Pick a move based on distance and availability
        if (distSq < 6.0 * 6.0) {
            // Close range — prefer fast forms
            if (cooldownMove0 == 0 && trainer.canUseMove(0)) {
                trainer.performMovesetMove(0);
                cooldownMove0  = 40;
                globalCooldown = randomGlobalCooldown();
            } else if (cooldownMove2 == 0 && trainer.canUseMove(2)) {
                trainer.performMovesetMove(2);
                cooldownMove2  = 60;
                globalCooldown = randomGlobalCooldown();
            } else if (cooldownMove3 == 0 && trainer.canUseMove(3)) {
                trainer.performMovesetMove(3);
                cooldownMove3  = 50;
                globalCooldown = randomGlobalCooldown();
            }
        } else if (distSq < 15.0 * 15.0) {
            // Mid range — water wheel or thrusts
            if (cooldownMove1 == 0 && trainer.canUseMove(1)) {
                trainer.performMovesetMove(1);
                cooldownMove1  = 80;
                globalCooldown = randomGlobalCooldown();
            } else if (cooldownMove4 == 0 && trainer.canUseMove(4)) {
                trainer.performMovesetMove(4);
                cooldownMove4  = 70;
                globalCooldown = randomGlobalCooldown();
            }
        } else {
            // Long range — dash in
            if (cooldownMove5 == 0 && trainer.canUseMove(5)) {
                trainer.performMovesetMove(5);
                cooldownMove5  = 120;
                globalCooldown = randomGlobalCooldown();
            } else {
                EntityMovement.applyDash(trainer, target.position().subtract(trainer.position()));
                globalCooldown = 15;
            }
        }
    }

    private int randomGlobalCooldown() {
        return GLOBAL_COOLDOWN_MIN + trainer.getRandom().nextInt(GLOBAL_COOLDOWN_MAX - GLOBAL_COOLDOWN_MIN);
    }
}
