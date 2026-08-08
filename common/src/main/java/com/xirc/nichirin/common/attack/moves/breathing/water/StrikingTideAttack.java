package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fourth Form: Striking Tide
 * The user performs big omnidirectional slashes after a short windup
 * 360° omnidirectional multiple slashes.
 */
public class StrikingTideAttack extends WaterBreathingAttackBase {

    private boolean tideStarted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int tideTicks = 0;
    private int slashCount = 0;
    private static final int TOTAL_SLASHES = 4; // 4 big omnidirectional slashes

    public StrikingTideAttack() {
    }

    @Override
    protected void onStart() {
        tideStarted = false;
        hitEntities.clear();
        tideTicks = 0;
        slashCount = 0;
    }

    @Override
    protected void onActiveStart() {
        // Striking tide startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start tide slashes after windup
        if (!tideStarted && tickCount == windup + 1) {
            startTide();
            tideStarted = true;
        }

        // Continue omnidirectional slashes during duration
        if (tideStarted && tickCount > windup && tickCount < windup + duration) {
            tideTicks++;
            performOmnidirectionalSlashes();
        }
    }


    private void startTide() {
        playWaterVfxAt(VfxIds.STRIKING_TIDE, user.position(), user.getLookAngle(), 1.0f);
        // Tide start sound - powerful water rush
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.6f);

    }

    private void performOmnidirectionalSlashes() {
        // Perform slashes every 5 ticks (halved from 10 after the double-tick dedup)
        if (tideTicks % 5 == 0 && slashCount < TOTAL_SLASHES) {
            executeOmnidirectionalSlash();
            slashCount++;
        }

        // Hit all enemies in 360° range continuously but with spacing (halved from 6 after the
        // double-tick dedup)
        if (tideTicks % 3 == 0) {
            List<LivingEntity> targets = getTargetsInCustomHitbox(
                    user.position().add(0, user.getBbHeight() / 2, 0),
                    range * 2, // Full diameter
                    user.getBbHeight() + 2, // Height
                    range * 2  // Full diameter
            );

            for (LivingEntity target : targets) {
                // Allow re-hitting but with spacing
                if (!hasRecentlyHit(target)) {
                    hitTargetNoImmunity(target);
                    trackRecentHit(target);

                    // Outward knockback from center
                    Vec3 outwardKnockback = target.position().subtract(user.position()).normalize();
                    target.push(outwardKnockback.x * knockback * 0.6, 0.1, outwardKnockback.z * knockback * 0.6);
                }
            }
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 12 ticks (0.6 seconds)
        return hitEntities.contains(target) && tideTicks % 12 > 6;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically
        if (tideTicks % 24 == 0) {
            hitEntities.clear();
        }
    }

    private void executeOmnidirectionalSlash() {
        // Slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f + slashCount * 0.1f);
    }


    @Override
    public boolean isOmnidirectional() {
        return true; // This attack hits in all directions
    }

    @Override
    protected void onStop() {
        // Clear state
        hitEntities.clear();
        tideTicks = 0;
        tideStarted = false;
        slashCount = 0;

        // Final tide sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.5f);
    }
}
