package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Committed rising dual-blade heavy that launches struck targets. */
public final class DualKatanaCrouchHeavyAttack extends DualKatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() { executed = false; }

    @Override
    protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsAtRange()) {
            hitTarget(target);
            Vec3 away = target.position().subtract(user.position()).normalize();
            target.setDeltaMovement(away.x * knockback, 0.8, away.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
            if (target instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
        }
        playSwingSound(0.82f);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
