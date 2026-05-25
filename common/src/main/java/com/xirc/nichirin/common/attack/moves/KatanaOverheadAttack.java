package com.xirc.nichirin.common.attack.moves;


import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
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
        if (player.level() instanceof ServerLevel sl) {
            Vec3 front = player.position().add(player.getLookAngle().scale(range * 0.6));
            // Vertical arc from high to low for the downward slash
            for (int i = 0; i <= 4; i++) {
                double t = i / 4.0;
                double y = player.getY() + player.getBbHeight() * (1.2 - t * 1.4);
                sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                        front.x, y, front.z, 1, 0.15, 0.05, 0.15, 0.0);
            }
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

        // Heavy downward impact particles
        if (world instanceof ServerLevel sl) {
            Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    tp.x, tp.y, tp.z, 8, 0.2, 0.2, 0.2, 0.0);
            sl.sendParticles(ParticleTypes.CRIT,
                    tp.x, tp.y, tp.z, 10, 0.3, 0.3, 0.3, 0.1);
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
