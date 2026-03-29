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
import net.minecraft.world.entity.player.Player;
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

    // ── Hooks ─────────────────────────────────────────────────────────────────

    @Override
    protected void onStart(Player player) {
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 0.7f);
        }

    }

    /**
     * Aerial slam: drives airborne targets downward; grounded targets get normal knockback.
     */
    @Override
    protected void applyKnockback(Player user, LivingEntity target) {
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
    protected void onHitTarget(Player user, LivingEntity target, Level world) {
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

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaOverheadAttack> {
        public Builder() {
            startup = 4; active = 8; recovery = 10;
            cooldown = 40; damage = 10.0f; range = 2.8f;
            knockback = 1.0f; hitboxSize = 1.5f; hitStun = 10;
        }

        @Override
        public KatanaOverheadAttack build() {
            return new KatanaOverheadAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public static KatanaOverheadAttack createDefault() {
        return new Builder()
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }
}
