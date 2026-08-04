package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Eighth Form: Explosive Rush. Invulnerable blinding dash that deflects all projectiles.
public class BeastExplosiveRushAttack extends BeastBreathingAttackBase {

    private boolean wasInvulnerable = false;
    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 dashStartPos;
    private int dashTick = 0;
    private boolean hitConnected = false;

    @Override
    protected void onStart() {
        dashStarted = false;
        dashStartPos = null;
        dashTick = 0;
        hitConnected = false;
        dashDirection = angledDashDirection();
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.5f, 2.0f);

        playBeastVfx(VfxIds.BEAST_EXPLOSIVE_RUSH,
                user.position().add(0, user.getBbHeight() * 0.4, 0), dashDirection, 1.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Velocity-based movement — smooth for client, no snapping
        if (!dashStarted) {
            dashStartPos = user.position();
            dashStarted = true;
            float speed = dashSpeed != null ? dashSpeed : 14.0f;
            // Launch with full velocity; divide by duration for per-tick equivalent
            float perTickSpeed = speed / Math.max(duration, 1);
            user.setDeltaMovement(dashDirection.scale(perTickSpeed));
            user.hurtMarked = true;
            user.hasImpulse = true;
        } else if (!hitConnected) {
            // Maintain dash velocity each tick until a hit connects
            float speed = dashSpeed != null ? dashSpeed : 14.0f;
            float perTickSpeed = speed / Math.max(duration, 1);
            Vec3 current = user.getDeltaMovement();
            // Blend towards dash direction to keep heading straight
            user.setDeltaMovement(
                    dashDirection.x * perTickSpeed,
                    current.y,
                    dashDirection.z * perTickSpeed
            );
            user.hurtMarked = true;
        }
        dashTick++;

        deflectProjectiles();

        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);
        List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize, HitboxData.HitboxShape.CUBE);
        for (LivingEntity target : targets) {
            hitTarget(target);
            // Cancel user movement on hit
            if (!hitConnected) {
                hitConnected = true;
                user.setDeltaMovement(Vec3.ZERO);
            }
        }

    }

    private void deflectProjectiles() {
        AABB searchBox = new AABB(
                user.getX() - 4, user.getY() - 4, user.getZ() - 4,
                user.getX() + 4, user.getY() + 4, user.getZ() + 4
        );

        List<AbstractArrow> arrows = world.getEntitiesOfClass(AbstractArrow.class, searchBox,
                a -> a.getOwner() != user);
        for (AbstractArrow arrow : arrows) {
            Vec3 deflect = arrow.getDeltaMovement().scale(-1.5).add(0, 0.5, 0);
            arrow.setDeltaMovement(deflect);
        }

        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class, searchBox,
                p -> p.getOwner() != user);
        for (Projectile proj : projectiles) {
            Vec3 deflect = proj.getDeltaMovement().scale(-1.5).add(0, 0.5, 0);
            proj.setDeltaMovement(deflect);
        }
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);
        user.resetFallDistance();
        dashStarted = false;
        dashStartPos = null;
        dashTick = 0;
        hitConnected = false;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    private Vec3 angledDashDirection() {
        Vec3 look = user.getLookAngle();
        return new Vec3(look.x, Math.max(-0.25, Math.min(0.25, look.y)), look.z).normalize();
    }
}
