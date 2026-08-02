package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinDamageHandler;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Double slash — X-pattern with two hits per target. Knockback only on second hit.
 */
public class KatanaDoubleSlashAttack extends AbstractKatanaAttack {

    private final int slashDelay;
    private final Map<LivingEntity, Integer> hitCooldowns = new HashMap<>();
    private final Map<LivingEntity, Integer> perTargetHitCount = new HashMap<>();
    private boolean firstSlashVisual = false;
    private boolean secondSlashVisual = false;

    public KatanaDoubleSlashAttack(int startup, int active, int recovery, int cooldown,
                                   float damage, float range, float knockback,
                                   float hitboxSize, Vec3 hitboxOffset, int hitStun,
                                   SoundEvent startSound, SoundEvent hitSound, int slashDelay) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.slashDelay = slashDelay;
        this.stopAfterFirstHit = false;
    }

    @Override
    protected void onStart(LivingEntity player) {
        hitCooldowns.clear();
        perTargetHitCount.clear();
        firstSlashVisual = false;
        secondSlashVisual = false;

        if (startup == 0) {
            createDiagonalSlashParticles(player, true);
            firstSlashVisual = true;
        }

        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void onActiveTick(LivingEntity player) {
        // Tick down hit cooldowns
        hitCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

        // First slash visual
        if (tickCount == startup && !firstSlashVisual) {
            createDiagonalSlashParticles(player, true);
            firstSlashVisual = true;
        }

        // Second slash visual
        if (tickCount == startup + slashDelay && !secondSlashVisual) {
            createDiagonalSlashParticles(player, false);
            secondSlashVisual = true;
            if (startSound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        startSound, SoundSource.PLAYERS, 1.0f, 1.1f);
            }
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
                entity -> entity != user && entity.isAlive()
                        && !hitCooldowns.containsKey(entity)
                        && perTargetHitCount.getOrDefault(entity, 0) < 2);

        if (targets.isEmpty()) return;

        hasHit = true;
        DamageSource damageSource = NichirinDamageSources.blade(user);

        for (LivingEntity target : targets) {
            NichirinDamageHandler.hurt(target, damageSource, damage);

            int currentHitCount = perTargetHitCount.getOrDefault(target, 0) + 1;
            perTargetHitCount.put(target, currentHitCount);

            if (knockback > 0 && currentHitCount >= 2) {
                Vec3 knockVec = target.position().subtract(user.position()).normalize();
                target.knockback(knockback, -knockVec.x, -knockVec.z);
            }

            if (hitStun > 0) target.invulnerableTime = hitStun;

            if (user instanceof Player p) {
                ComboIntegration.handleSuccessfulHit(p, target, hitStun, damage, comboSequence);
            }

            hitCooldowns.put(target, 6);

            onHitTarget(user, target, world);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void onEnd(LivingEntity player) {
        hitCooldowns.clear();
        perTargetHitCount.clear();
    }

    private void createDiagonalSlashParticles(LivingEntity user, boolean isFirstDiagonal) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        Vec3 centre = user.position().add(0, user.getBbHeight() * 0.5, 0)
                .add(user.getLookAngle().scale(range * 0.6));
        Vec3 right = new Vec3(-user.getLookAngle().z, 0, user.getLookAngle().x).normalize();
        double sign = isFirstDiagonal ? 1.0 : -1.0;
        Vec3 start = centre.add(right.scale(sign * 1.2)).add(0, 0.5, 0);
        Vec3 end = centre.add(right.scale(sign * -1.2)).add(0, -0.5, 0);
        for (double t = 0; t <= 1.0; t += 0.25) {
            Vec3 pos = start.add(end.subtract(start).scale(t));
            sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                centre.x, centre.y, centre.z, 2, 0.2, 0.2, 0.2, 0.0);
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaDoubleSlashAttack> {
        private int slashDelay = 2;

        public Builder() {
            startup = 0; active = 16; recovery = 2;
            cooldown = 20; range = 2.8f;
            knockback = 0.4f; hitboxSize = 2.0f; hitStun = 7;
        }

        public Builder withSlashDelay(int delay) {
            this.slashDelay = delay;
            return this;
        }

        @Override
        public KatanaDoubleSlashAttack build() {
            return new KatanaDoubleSlashAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound, slashDelay);
        }
    }

    public static KatanaDoubleSlashAttack createDefault() {
        return new Builder()
                .withDamage(3.5f)
                .withSlashDelay(2)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }
}
