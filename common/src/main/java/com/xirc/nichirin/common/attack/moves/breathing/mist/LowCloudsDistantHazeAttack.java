package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Form 1: Low Clouds, Distant Haze — velocity-based lunge that pierces enemies along the path.
public class LowCloudsDistantHazeAttack extends MistBreathingAttackBase {

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 dashStartPos;
    private int dashTick = 0;
    private int dashDuration = 0;
    private Vec3 lastHitboxPos;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    @Override
    protected void onStart() {
        dashStarted = false;
        dashTick = 0;
        dashDuration = 0;
        hitEntities.clear();
        dashDirection = null;
        dashStartPos = null;
        lastHitboxPos = null;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!dashStarted && tickCount == windup + 1) {
            dashDirection = angledDashDirection();
            dashStartPos = user.position();
            dashDuration = Math.max(1, duration);
            dashStarted = true;
            playMistVfx(VfxIds.LOW_CLOUDS_DISTANT_HAZE,
                    user.position().add(0, user.getBbHeight() * 0.45, 0), dashDirection, 1.0f);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
            playMistSound();
        }

        if (!dashStarted || dashDirection == null) return;

        if (dashTick < dashDuration) {
            dashTick++;
            // Recompute the dash direction each tick so the player can STEER mid-dash by
            // turning the camera — the dash isn't locked to its starting heading anymore.
            dashDirection = angledDashDirection();
            // Velocity-based dash (smooth, client-predicted) instead of per-tick teleport.
            // Covers range*10 blocks over dashDuration ticks.
            float speedPerTick = (range * 10f) / dashDuration;
            user.setDeltaMovement(
                    dashDirection.x * speedPerTick,
                    dashDirection.y * speedPerTick,
                    dashDirection.z * speedPerTick);
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);
        boolean isFinalTick = dashTick >= dashDuration;
        float boxSize = isFinalTick ? hitboxSize * 1.5f : hitboxSize;
        HitboxData hb = new HitboxData(boxSize);
        Set<LivingEntity> targets = new HashSet<>();
        Vec3 midpoint = lastHitboxPos != null ? lastHitboxPos.lerp(center, 0.5) : center;
        for (Vec3 sample : List.of(midpoint, center)) {
            AABB hitbox = hb.createAABB(sample, dashDirection, (float) Math.toRadians(user.getYRot()));
            if (user instanceof ServerPlayer sp) {
                NichirinPacketRegistry.sendHitboxToClient(sp, hitbox, 200L);
            }
            targets.addAll(world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    e -> e != user && e.isAlive()));
        }
        lastHitboxPos = center;
        for (LivingEntity target : targets) {
            if (!hitEntities.contains(target)) {
                hitTarget(target);
                hitEntities.add(target);
                if (!isFinalTick) {
                    dashTick = dashDuration;
                    user.setDeltaMovement(Vec3.ZERO);
                    user.hurtMarked = true;
                }
            }
        }
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);
        user.hurtMarked = true;

        dashStarted = false;
        dashTick = 0;
        dashDuration = 0;
        dashStartPos = null;
        lastHitboxPos = null;
        hitEntities.clear();
    }

    private Vec3 angledDashDirection() {
        Vec3 look = user.getLookAngle();
        return new Vec3(look.x, Math.max(0, Math.min(0.15, look.y)), look.z).normalize();
    }
}
