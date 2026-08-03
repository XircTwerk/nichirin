package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Delayed rushing thrust whose live hitbox follows the dash path. */
public final class KatanaThrustAttack extends KatanaAttackBase {

    @Override protected void onStart() { playSwing(1.1f); }

    @Override protected void onActiveStart() {
        Vec3 look = user.getLookAngle();
        double speed = dashSpeed != null ? dashSpeed : 3.0;
        user.setDeltaMovement(look.x * speed, user.getDeltaMovement().y, look.z * speed);
        user.hurtMarked = true;
        user.hasImpulse = true;
        if (user instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(user));
        }
    }

    @Override protected void perform() {
        if (world.isClientSide) return;
        Vec3 center = user.position().add(0.0, user.getBbHeight() * 0.5, 0.0)
                .add(user.getLookAngle().scale(1.0));
        for (LivingEntity target : getTargetsInHitbox(center)) hitTarget(target);
    }
}
