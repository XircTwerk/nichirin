package com.xirc.nichirin.common.attack.moves;


import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Overhead — heavy downward slash.
 * Good damage, good knockback, and slams airborne targets straight down.
 */
public class KatanaOverheadAttack extends AbstractKatanaAttack {

    public KatanaOverheadAttack(int startup, int active, int recovery, int cooldown,
                                 float damage, float range, float knockback,
                                 float hitboxSize, Vec3 hitboxOffset, int hitStun,
                                 SoundEvent startSound, SoundEvent hitSound) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
    }


    @Override
    protected void onStart(LivingEntity player) {
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
    }

    /**
     * Aerial slam: drives airborne targets downward; grounded targets get normal knockback.
     */
    @Override
    protected void applyKnockback(LivingEntity user, LivingEntity target) {
        if (!target.onGround()) {
            Vec3 current = target.getDeltaMovement();
            target.setDeltaMovement(new Vec3(current.x * knockback, current.y - 0.5, current.z * knockback));
            target.hurtMarked = true;
            target.hasImpulse = true;
        } else {
            super.applyKnockback(user, target);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        // Hit sound (low pitch = heavy impact)
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        // Sync motion for remote players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }


    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaOverheadAttack> {
        public Builder() {
            startup = 4; active = 8; recovery = 10;
            cooldown = 40; range = 2.8f;
            knockback = 1.0f; hitboxSize = 2.0f; hitStun = 10;
        }

        @Override
        public KatanaOverheadAttack build() {
            return new KatanaOverheadAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public static KatanaOverheadAttack createDefault() {
        return new Builder()
                .withDamage(10.0f)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }
}
