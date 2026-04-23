package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Eighth Form: Explosive Rush - Blinding speed dash, ignores all attacks.
 * Invulnerable during dash; deflects all projectiles.
 * Bound to crouch + right click.
 */
public class BeastExplosiveRushAttack extends BeastBreathingAttackBase {

    private boolean wasInvulnerable = false;
    private boolean dashStarted = false;
    private Vec3 dashDirection;

    @Override
    protected void onStart() {
        dashStarted = false;
        dashDirection = user.getLookAngle().normalize();
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.5f, 2.0f);

        createDashBurst();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Dash on first active tick
        if (!dashStarted) {
            Vec3 velocity = dashDirection.scale(dashSpeed != null ? dashSpeed : 12.0f);
            user.setDeltaMovement(velocity);
            user.hurtMarked = true;
            user.hasImpulse = true;
            dashStarted = true;
        }

        // Maintain velocity during dash
        Vec3 current = user.getDeltaMovement();
        float speed = dashSpeed != null ? dashSpeed : 12.0f;
        if (current.length() < speed * 0.5) {
            user.setDeltaMovement(dashDirection.scale(speed));
            user.hurtMarked = true;
        }

        // Deflect all nearby projectiles
        deflectProjectiles();

        // Hit enemies during dash
        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);
        List<LivingEntity> targets = getTargetsInCustomHitbox(center, 2.5f, HitboxData.HitboxShape.LONG);
        for (LivingEntity target : targets) {
            hitTarget(target);
        }

        createDashTrail();
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

    private void createDashBurst() {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        sl.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 30, 0.8, 0.8, 0.8, 0.3);
        sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 20, 0.6, 0.6, 0.6, 0.4);
    }

    private void createDashTrail() {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 back = dashDirection.scale(-1);
        for (int i = 1; i <= 5; i++) {
            Vec3 trail = pos.add(back.scale(i * 0.4));
            sl.sendParticles(ParticleTypes.CLOUD, trail.x, trail.y, trail.z,
                    2, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(user.getDeltaMovement().scale(0.3));
        user.resetFallDistance();
        dashStarted = false;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.6f);
    }
}
