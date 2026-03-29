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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Thrust — player dashes forward, hitting everything in their path.
 * Good damage, great knockback.  The dash itself is the hitbox scan.
 *
 * <p>Key differences from the other wheel attacks:
 * <ul>
 *   <li>{@link #stopAfterFirstHit} is {@code false} — every enemy in the dash corridor is hit.</li>
 *   <li>{@link #buildHitbox} uses a fixed 1-block-ahead box (not the full {@code range} field)
 *       so the scan tracks the player's real position each active tick.</li>
 *   <li>{@link #onActiveStart} fires the dash velocity on the first active tick.</li>
 *   <li>{@link #onActiveTick} emits a cloud trail behind the player each active tick.</li>
 * </ul>
 * </p>
 */
public class KatanaThrustAttack extends AbstractKatanaAttack {

    public KatanaThrustAttack(int startup, int active, int recovery, int cooldown,
                               float damage, float range, float knockback,
                               float hitboxSize, Vec3 hitboxOffset, int hitStun,
                               SoundEvent startSound, SoundEvent hitSound) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        // Every enemy in the dash path should be struck, not just the first
        this.stopAfterFirstHit = false;
    }

    // ── Hooks ─────────────────────────────────────────────────────────────────

    @Override
    protected void onStart(Player player) {
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 1.1f);
        }
    }

    /** First active tick: apply the forward dash velocity and sync to the client. */
    @Override
    protected void onActiveStart(Player player) {
        Vec3 lookDir = player.getLookAngle();
        player.setDeltaMovement(new Vec3(lookDir.x * 3.0, player.getDeltaMovement().y, lookDir.z * 3.0));
        player.hurtMarked = true;
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    /** Every active tick: emit a cloud trail behind the player. */
    @Override
    protected void onActiveTick(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Vec3 behind = player.position().add(player.getLookAngle().scale(-0.5)).add(0, player.getBbHeight() * 0.5, 0);
        sl.sendParticles(ParticleTypes.CLOUD,
                behind.x, behind.y, behind.z, 2, 0.15, 0.15, 0.15, 0.02);
        sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                behind.x, behind.y, behind.z, 1, 0.1, 0.1, 0.1, 0.0);
    }

    /**
     * Thrust scan: 1-block-ahead cube tracking the player's live position each tick,
     * rather than a static box at full range.
     */
    @Override
    protected AABB buildHitbox(Player user) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 center  = userPos.add(user.getLookAngle().scale(1.0));
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize
        );
    }

    @Override
    protected void onHitTarget(Player user, LivingEntity target, Level world) {
        // Hit sound
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Piercing impact sparks
        if (world instanceof ServerLevel sl) {
            Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    tp.x, tp.y, tp.z, 8, 0.15, 0.15, 0.15, 0.0);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    tp.x, tp.y, tp.z, 3, 0.2, 0.2, 0.2, 0.0);
        }

        // Sync motion for remote players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaThrustAttack> {
        public Builder() {
            startup = 3; active = 10; recovery = 8;
            cooldown = 50; damage = 8.0f; range = 7.0f;
            knockback = 1.2f; hitboxSize = 1.2f; hitStun = 15;
        }

        @Override
        public KatanaThrustAttack build() {
            return new KatanaThrustAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public static KatanaThrustAttack createDefault() {
        return new Builder()
                .withCooldown(50)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }
}
