package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Form 3: 360° circular slash. Deflects projectiles, brief invuln on active frames.
public class ScatteringMistSplashAttack extends MistBreathingAttackBase {

    private final Set<LivingEntity> hitEnemies = new HashSet<>();
    private boolean invulnerabilityApplied = false;
    private int spinTicks = 0;

    @Override
    protected void onStart() {
        hitEnemies.clear();
        invulnerabilityApplied = false;
        spinTicks = 0;
    }

    @Override
    protected void onActiveStart() {
        playMistVfx(VfxIds.SCATTERING_MIST_SPLASH,
                user.position().add(0, user.getBbHeight() * 0.45, 0), user.getLookAngle(), range / 4.0f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!invulnerabilityApplied) {
            user.setInvulnerable(true);
            invulnerabilityApplied = true;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        spinTicks++;

        performCircularSlash();
        deflectProjectiles();

        if (spinTicks % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
    }

    private void performCircularSlash() {
        Vec3 userPos = user.position();

        List<LivingEntity> targets = getTargetsInCircle(range, 12);

        for (LivingEntity target : targets) {
            if (!hitEnemies.contains(target)) {
                hitTarget(target);
                hitEnemies.add(target);

                Vec3 pushDir = target.position().subtract(userPos).normalize();
                target.push(pushDir.x * knockback, 0.15, pushDir.z * knockback);
            }
        }
    }

    private void deflectProjectiles() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class,
                new AABB(userPos.subtract(range, 2, range), userPos.add(range, 2, range)),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            if (projectile instanceof AbstractArrow) {
                projectile.discard();
            } else {
                Vec3 reflectDir = projectile.position().subtract(userPos).normalize();
                projectile.setDeltaMovement(reflectDir.scale(1.5));
                projectile.hurtMarked = true;
            }

            world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(false);
        hitEnemies.clear();
        spinTicks = 0;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}
