package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Second Form: Rice Spirit
 * 5 slashes focused on a single target - locks onto closest enemy
 */
public class RiceSpiritAttack extends ThunderBreathingAttackBase {

    private int slashCount = 0;
    private int slashTimer = 0;
    private LivingEntity lockedTarget = null;
    private final Random random = new Random();

    public RiceSpiritAttack() {
    }

    @Override
    protected void onStart() {
        // Reset counters
        slashCount = 0;
        slashTimer = 0;
        lockedTarget = null;

        // Find closest enemy within range (using configured range)
        lockedTarget = findClosestEnemy();

        if (lockedTarget == null) {
            // This prevents breath consumption and cooldown application

            // Stop the attack immediately - this will prevent breath consumption
            // since the attack never becomes fully active
            stop();
            return;
        }


    }

    @Override
    protected void onActiveStart() {
        // Thunder sound on start
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Check if we still have a valid target
        if (lockedTarget == null || !lockedTarget.isAlive() || lockedTarget.isRemoved()) {
            stop();
            return;
        }

        // Execute slashes every 2 ticks (0.1s). Tuned when breathing attacks were accidentally
        // double-ticked (pre MoveExecutor dedup) — halved from 4 to keep the original real-time feel.
        slashTimer++;

        if (slashTimer % 2 == 0 && slashCount < 5) {
            performSlash();
            slashCount++;

        }

        // Stop after all 5 slashes are complete
        if (slashCount >= 5) {
            stop();
        }
    }

    private void performSlash() {
        if (lockedTarget == null) return;

        // Get target's current position
        Vec3 targetPos = lockedTarget.position();

        // Add some variation to slash positions around the target
        float angleOffset = (slashCount * 72f) + random.nextFloat() * 30f; // Distribute around target
        float radian = (float) Math.toRadians(angleOffset);
        float offsetDistance = 0.5f + random.nextFloat() * 0.5f;

        Vec3 slashPos = targetPos.add(
                Math.cos(radian) * offsetDistance,
                1.0 + random.nextFloat() * 0.5f, // Vary height slightly
                Math.sin(radian) * offsetDistance
        );

        Vec3 slashDirection = lockedTarget.position().subtract(user.position());
        playThunderVfxAt(VfxIds.RICE_SPIRIT_SLASH, slashPos,
                slashDirection.lengthSqr() > 1.0E-6 ? slashDirection : user.getLookAngle(), 0.85f);

        // Play slash sound
        world.playSound(null, slashPos.x, slashPos.y, slashPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.8f, 1.5f + random.nextFloat() * 0.2f);

        hitTargetNoImmunity(lockedTarget);

    }

    /**
     * Find the closest enemy within range (using configured range)
     */
    private LivingEntity findClosestEnemy() {
        AABB searchBox = new AABB(
                user.getX() - range, user.getY() - range, user.getZ() - range,
                user.getX() + range, user.getY() + range, user.getZ() + range
        );

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());


        if (entities.isEmpty()) {
            return null;
        } else {
            // Play init ding sound
            world.playSound(null, user,
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                    0.8f,1.5f);
        }

        // Sort by distance and return closest
        LivingEntity closest = entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(user)))
                .orElse(null);

        if (closest != null) {
            double distance = Math.sqrt(closest.distanceToSqr(user));
        }

        return closest;
    }

    @Override
    protected void onStop() {
        lockedTarget = null;
    }
}
