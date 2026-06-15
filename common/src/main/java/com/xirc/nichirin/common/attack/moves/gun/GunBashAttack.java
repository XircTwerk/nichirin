package com.xirc.nichirin.common.attack.moves.gun;

import com.xirc.nichirin.common.attack.moves.AbstractKatanaAttack;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gun Bash — a swift forward dash that ends in a pistol-whip to the head. Low damage, heavy stun
 * ({@code hitStun} ticks of STUNNED on hit). Mirrors the katana Thrust dash + Check stun feel.
 */
public class GunBashAttack extends AbstractKatanaAttack {

    private final double dashSpeed;

    public GunBashAttack(int startup, int active, int recovery, int cooldown,
                         float damage, float range, float knockback,
                         float hitboxSize, Vec3 hitboxOffset, int hitStun,
                         SoundEvent startSound, SoundEvent hitSound, double dashSpeed) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
                hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.dashSpeed = dashSpeed;
    }

    @Override
    protected void onActiveStart(LivingEntity player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        player.setDeltaMovement(flat.x * dashSpeed, player.getDeltaMovement().y, flat.z * dashSpeed);
        player.hurtMarked = true;
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    @Override
    protected AABB buildHitbox(LivingEntity user) {
        Vec3 center = user.position()
                .add(0, user.getBbHeight() * 0.6, 0)
                .add(user.getLookAngle().scale(range));
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.stunned(), hitStun, 0, false, false, false));
        }
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 0.9f);
        }
        if (world instanceof ServerLevel sl) {
            Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
            sl.sendParticles(ParticleTypes.CRIT, tp.x, tp.y, tp.z, 8, 0.2, 0.2, 0.2, 0.05);
        }
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, GunBashAttack> {
        private double dashSpeed = 0.8;

        public Builder() {
            startup = 1; active = 4; recovery = 6;
            cooldown = 60; range = 1.5f;
            knockback = 0.4f; hitboxSize = 1.2f; hitStun = 40;
        }

        public Builder withDashSpeed(double dashSpeed) {
            this.dashSpeed = dashSpeed;
            return this;
        }

        @Override
        public GunBashAttack build() {
            return new GunBashAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound, dashSpeed);
        }
    }

    public static GunBashAttack createDefault() {
        return new Builder()
                .withDamage(6.0f)
                .withSounds(SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }
}
