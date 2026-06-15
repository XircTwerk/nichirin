package com.xirc.nichirin.common.attack.moves.gun;

import com.xirc.nichirin.common.attack.moves.AbstractKatanaAttack;
import com.xirc.nichirin.common.system.GrabManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Grab — reaches forward and, if it catches a target, hands it to {@link GrabManager} which pins,
 * stuns and (after the window) launches it. Point-blank shots while held are handled by the moveset.
 */
public class GunGrabAttack extends AbstractKatanaAttack {

    private final int grabWindow;

    public GunGrabAttack(int startup, int active, int recovery, int cooldown,
                         float damage, float range, float knockback,
                         float hitboxSize, Vec3 hitboxOffset, int hitStun,
                         SoundEvent startSound, SoundEvent hitSound, int grabWindow) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
                hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.grabWindow = grabWindow;
    }

    /** No damage — instead, latch onto the first target in front. */
    @Override
    protected void performHitDetection(LivingEntity user, Level world) {
        if (hasHit || GrabManager.isGrabbing(user)) return;
        hasHit = true;

        AABB box = buildHitbox(user);
        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != user && e.isAlive());
        if (targets.isEmpty()) {
            if (startSound != null) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        startSound, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            return;
        }

        LivingEntity target = targets.get(0);
        GrabManager.startGrab(user, target, grabWindow);
        if (hitSound != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    hitSound, SoundSource.PLAYERS, 0.9f, 0.8f);
        }
        if (world instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    protected AABB buildHitbox(LivingEntity user) {
        Vec3 center = user.position()
                .add(user.getLookAngle().scale(range))
                .add(0, user.getBbHeight() * 0.4, 0);
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        // Grab handling lives in performHitDetection.
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, GunGrabAttack> {
        private int grabWindow = 30;

        public Builder() {
            startup = 1; active = 1; recovery = 6;
            cooldown = 80; range = 2.0f;
            knockback = 0.0f; hitboxSize = 1.5f; hitStun = 0;
        }

        public Builder withGrabWindow(int grabWindow) {
            this.grabWindow = grabWindow;
            return this;
        }

        @Override
        public GunGrabAttack build() {
            return new GunGrabAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound, grabWindow);
        }
    }

    public static GunGrabAttack createDefault() {
        return new Builder()
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }
}
