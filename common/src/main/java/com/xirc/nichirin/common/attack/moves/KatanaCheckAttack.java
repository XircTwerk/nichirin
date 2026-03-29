package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    // ── Hooks ─────────────────────────────────────────────────────────────────

    @Override
    protected void onStart(Player player) {
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    @Override
    protected void onHitTarget(Player user, LivingEntity target, Level world) {
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaCheckAttack> {
        public Builder() {
            startup = 0; active = 1; recovery = 4;
            cooldown = 30; damage = 2.0f; range = 2.0f;
            knockback = 2.2f; hitboxSize = 1.2f; hitStun = 20;
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
                .withSounds(NicirinSoundRegistry.SLASH_WHOOSH_1.get(), SoundEvents.PLAYER_ATTACK_KNOCKBACK)
                .build();
    }
}
