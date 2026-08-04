package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Forward dual-point lunge that scans the traveled corridor. */
public final class DualKatanaThrustAttack extends DualKatanaAttackBase {
    private static final int HIT_SCAN_TICKS = 8;

    @Override
    protected void onStart() {
        playSwingSound(1.1f);
    }

    @Override
    protected void onActiveStart() {
        Vec3 look = user.getLookAngle();
        double speed = dashSpeed != null ? dashSpeed : 4.2;
        user.setDeltaMovement(look.x * speed, look.y * speed * 0.35, look.z * speed);
        user.hurtMarked = true;
        user.hasImpulse = true;
        if (user instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(user));
        }
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;
        int activeTick = tickCount - windup;
        if (activeTick > HIT_SCAN_TICKS) return;

        Vec3 center = user.position().add(0, user.getBbHeight() * 0.5, 0)
                .add(user.getLookAngle().scale(1.0));
        for (LivingEntity target : getTargetsInCustomHitbox(center, hitboxSize,
                com.xirc.nichirin.common.util.HitboxData.HitboxShape.CUBE)) {
            hitTarget(target);
        }

        if (world instanceof ServerLevel level) {
            Vec3 behind = user.position().subtract(user.getLookAngle().scale(0.5))
                    .add(0, user.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.CLOUD, behind.x, behind.y, behind.z,
                    2, 0.15, 0.15, 0.15, 0.02);
        }
    }

    @Override protected void onStop() {}
}
