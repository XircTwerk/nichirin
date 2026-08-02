package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Two-blade overhead slam with downward force against airborne targets. */
public final class DualKatanaSlamAttack extends DualKatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() { executed = false; }

    @Override
    protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsAtRange()) {
            hitTarget(target);
            if (!target.onGround()) {
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * 0.25, -0.65, motion.z * 0.25);
                target.hurtMarked = true;
                target.hasImpulse = true;
                if (target instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundSetEntityMotionPacket(target));
                }
            }
        }
        playSwingSound(0.7f);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
