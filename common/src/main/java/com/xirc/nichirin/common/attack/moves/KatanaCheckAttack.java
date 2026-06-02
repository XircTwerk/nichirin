package com.xirc.nichirin.common.attack.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Quick Check — hits with the back handle (pommel) of the katana.
 * Low damage, decent knockback, lots of stun. Applies STUNNED effect on hit.
 */
public class KatanaCheckAttack extends AbstractKatanaAttack {

    public KatanaCheckAttack(int startup, int active, int recovery, int cooldown,
                              float damage, float range, float knockback,
                              float hitboxSize, Vec3 hitboxOffset, int hitStun,
                              SoundEvent startSound, SoundEvent hitSound) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
    }



    /**
     * Override hitbox to sit right at the player's torso — a tight, close-range box
     * matching the "shoulder/pommel bash" feel: the strike originates from the handle
     * of the katana held at the player's hip, not the blade tip.
     */
    @Override
    protected AABB buildHitbox(LivingEntity user) {
        // Center the box at the player's mid-body, just barely in front of them
        Vec3 origin = user.position().add(0, user.getBbHeight() * 0.45, 0);
        Vec3 center = origin.add(user.getLookAngle().scale(range));
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize
        );
    }


    @Override
    protected void onStart(LivingEntity player) {
        if (startSound != null) {
            // Match gut-punch startup pitch
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitSound != null) {
            // Match gut-punch impact pitch
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.2f, 0.6f);
        }

        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }


    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaCheckAttack> {
        public Builder() {
            startup = 0; active = 1; recovery = 4;
            cooldown = 30; range = 0.9f;
            knockback = 2.2f; hitboxSize = 2.0f; hitStun = 20;
        }

        @Override
        public KatanaCheckAttack build() {
            return new KatanaCheckAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public static KatanaCheckAttack createDefault() {
        return new Builder()
                .withCooldown(30)
                .withDamage(2.0f)
                .withSounds(SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }
}