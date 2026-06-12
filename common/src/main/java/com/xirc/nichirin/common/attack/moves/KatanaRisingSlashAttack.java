package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Rising slash — launches targets upward.
 */
public class KatanaRisingSlashAttack extends AbstractKatanaAttack {

    private final float launchPower;

    public KatanaRisingSlashAttack(int startup, int active, int recovery, int cooldown,
                                   float damage, float range, float knockback,
                                   float hitboxSize, Vec3 hitboxOffset, int hitStun,
                                   SoundEvent startSound, SoundEvent hitSound, float launchPower) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.launchPower = launchPower;
    }

    @Override
    protected void onStart(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        Vec3 base = player.position().add(0, player.getBbHeight() * 0.3, 0)
                .add(player.getLookAngle().scale(range * 0.5));
        for (int i = 0; i < 5; i++) {
            Vec3 pos = base.add(0, i * 0.35, 0);
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    pos.x, pos.y, pos.z, 2, 0.1, 0.05, 0.1, 0.0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                base.x, base.y + 0.8, base.z, 3, 0.2, 0.2, 0.2, 0.0);

        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    @Override
    protected void performHitDetection(LivingEntity user, Level world) {
        AABB hitbox = buildHitbox(user);
        if (!hitboxSent) {
            NichirinPacketRegistry.sendHitboxToTracking(user, hitbox, Math.max(active * 50L, 1500L));
            hitboxSent = true;
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        if (targets.isEmpty()) return;

        hasHit = true;
        DamageSource damageSource = NichirinDamageSources.blade(user);

        for (LivingEntity target : targets) {
            // Damage first
            NichirinArmorDamage.hurt(target, damageSource, damage);
            hitEntities.add(target);

            // Launch
            if (target.onGround()) {
                target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
            }
            Vec3 launchVelocity = new Vec3(0, launchPower, 0);
            if (knockback > 0) {
                Vec3 knockDir = target.position().subtract(user.position()).normalize();
                launchVelocity = launchVelocity.add(knockDir.x * knockback, 0, knockDir.z * knockback);
            }
            target.setDeltaMovement(launchVelocity);
            target.hurtMarked = true;
            target.hasImpulse = true;

            if (hitStun > 0) target.invulnerableTime = hitStun;

            if (user instanceof Player p) {
                ComboIntegration.handleSuccessfulHit(p, target, hitStun, damage);
            }

            if (target instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(target));
            }

            onHitTarget(user, target, world);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaRisingSlashAttack> {
        private float launchPower = 0.8f;

        public Builder() {
            startup = 0; active = 10; recovery = 4;
            cooldown = 25; range = 2.5f;
            knockback = 0.2f; hitboxSize = 2.0f;
            hitboxOffset = new Vec3(0, 0.5, 0);
            hitStun = 10;
        }

        public Builder withLaunchPower(float power) {
            this.launchPower = power;
            return this;
        }

        @Override
        public KatanaRisingSlashAttack build() {
            return new KatanaRisingSlashAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound, launchPower);
        }
    }

    public static KatanaRisingSlashAttack createDefault() {
        return new Builder()
                .withDamage(4.0f)
                .withLaunchPower(0.8f)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }
}
