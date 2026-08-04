package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.entity.attack.ThunderBallEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

/**
 * Fourth Form: Distant Thunder
 * Spawns a thunder ball that travels forward for 6 seconds
 * The ball continuously damages nearby entities and strikes with lightning
 */
public class DistantThunderAttack extends ThunderBreathingAttackBase {

    public DistantThunderAttack() {
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onActiveStart() {
        // Ominous thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.5f);

        playThunderVfx(VfxIds.DISTANT_THUNDER_CHARGE, user.position(), user.getLookAngle(), 1.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Only execute once (on first perform tick after windup)
        if (tickCount == windup + 1) {
            spawnThunderBall();
        }
    }

    private void spawnThunderBall() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Check entity type registration
        EntityType<ThunderBallEntity> entityType = NichirinEntityRegistry.THUNDER_BALL.get();
        if (entityType == null) return;

        // Create entity
        ThunderBallEntity thunderBall = new ThunderBallEntity(entityType, world, user, damage, hitStun);

        // Set position and movement
        Vec3 userChestPos = new Vec3(user.getX(), user.getY() + 1.0, user.getZ());
        Vec3 lookDirection = user.getLookAngle();
        Vec3 spawnPos = userChestPos.add(lookDirection.scale(1.5));

        thunderBall.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        thunderBall.setDeltaMovement(lookDirection.scale(0.2));

        // Add to world
        serverLevel.addFreshEntity(thunderBall);

        // Play effects
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.5f);

        playThunderVfx(VfxIds.THUNDERCLAP_FLASH, userChestPos, lookDirection, 0.72f);
    }

    @Override
    protected void onStop() {
        // Nothing to clean up - the thunder ball entity manages itself
    }
}
