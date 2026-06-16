package com.xirc.nichirin.common.attack.moves.gun;

import com.xirc.nichirin.common.attack.moves.AbstractKatanaAttack;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hitscan gun shot. Fires {@code barrels} rays from the shooter's eyes; damage scales by distance
 * ({@link #CLOSE_DAMAGE_MULT}x point-blank, 1.0x at {@link #BASELINE_DISTANCE}, 0 at
 * {@link #ZERO_DAMAGE_DISTANCE}). {@code range} is the bullet travel distance; {@code damage},
 * {@code knockback} and timing come from the move config via {@link #configure}.
 */
public class GunShotAttack extends AbstractKatanaAttack {

    public static final float CLOSE_DAMAGE_MULT     = 3.0f;
    public static final double BASELINE_DISTANCE    = 8.0;
    public static final double ZERO_DAMAGE_DISTANCE = 20.0;
    // Random angular spread applied to every pellet (max deviation per axis before re-normalizing).
    private static final double SPREAD = 0.3;
    private static final int PELLETS_PER_BARREL = 8;
    private static final String IMPACT_SHAKE_EFFECT = "com.xirc.nichirin.client.shader.ImpactShakeShaderEffect";

    private final int barrels;

    public GunShotAttack(int startup, int active, int recovery, int cooldown,
                         float damage, float range, float knockback,
                         float hitboxSize, Vec3 hitboxOffset, int hitStun,
                         SoundEvent startSound, SoundEvent hitSound, int barrels) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
                hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.barrels = barrels;
    }

    /** Distance-based damage multiplier shared with point-blank grab shots. */
    public static float damageMultiplier(double distance) {
        if (distance <= BASELINE_DISTANCE) {
            double t = distance / BASELINE_DISTANCE;
            return (float) (CLOSE_DAMAGE_MULT + (1.0 - CLOSE_DAMAGE_MULT) * t);
        }
        double t = Math.min(1.0, (distance - BASELINE_DISTANCE) / (ZERO_DAMAGE_DISTANCE - BASELINE_DISTANCE));
        return (float) (1.0 - t);
    }

    /**
     * The shot is fully resolved here (sound, shake, hitscan, tracer) on the same tick the attack
     * starts — i.e. the tick the input is processed — so nothing lags behind the recoil shake.
     */
    @Override
    protected void onStart(LivingEntity player) {
        Level world = player.level();

        SoundEvent sound = barrels >= 2
                ? NicirinSoundRegistry.GENYA_DOUBLESHOT.get()
                : NicirinSoundRegistry.GENYA_SINGLESHOT.get();
        RandomSource random = world.getRandom();
        float pitch = 1.0f + (random.nextFloat() - 0.5f) * 0.16f;
        float volume = 0.92f + random.nextFloat() * 0.08f;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);

        sendShootShake(player, barrels >= 2 ? 0.45f : 0.25f);

        float pelletDamage = damage / PELLETS_PER_BARREL;
        int totalPellets = barrels * PELLETS_PER_BARREL;
        for (int i = 0; i < totalPellets; i++) {
            fireRay(player, world, pelletDamage);
        }
    }

    /** Slight recoil shake on the shooter's own screen. */
    public static void sendShootShake(LivingEntity user, float magnitude) {
        if (user instanceof ServerPlayer sp) {
            NichirinPacketRegistry.sendToPlayer(
                    new TriggerShaderPacket(IMPACT_SHAKE_EFFECT, true, magnitude), sp);
        }
    }

    /** Hitscan fires instantly in {@link #onStart}; the active window is just recovery padding. */
    @Override
    protected void performHitDetection(LivingEntity user, Level world) {
    }

    private void fireRay(LivingEntity user, Level world, float pelletDamage) {
        Vec3 eye = user.getEyePosition();
        RandomSource random = user.getRandom();
        Vec3 look = user.getViewVector(1.0f).add(
                (random.nextDouble() - 0.5) * SPREAD,
                (random.nextDouble() - 0.5) * SPREAD,
                (random.nextDouble() - 0.5) * SPREAD
        ).normalize();
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = world.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchBox = new AABB(eye, end).inflate(0.3);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                world, user, eye, end, searchBox,
                e -> e instanceof LivingEntity && e != user && e.isAlive());

        Vec3 impact = end;
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            impact = entityHit.getLocation();
            float dmg = pelletDamage * damageMultiplier(eye.distanceTo(impact));
            boolean damaged = dmg > 0 && NichirinArmorDamage.hurt(target, NichirinDamageSources.blade(user), dmg);
            target.invulnerableTime = 0;
            if (damaged && knockback > 0) {
                Vec3 kb = target.position().subtract(user.position()).normalize();
                target.knockback(knockback, -kb.x, -kb.z);
            }
        }

        spawnTracer(world, eye, impact);
    }

    private void spawnTracer(Level world, Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.01) return;
        Vec3 step = dir.scale(1.0 / length);
        for (double d = 1.0; d < length; d += 1.0) {
            Vec3 p = start.add(step.scale(d));
            sl.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        sl.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.05, 0.05, 0.05, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        // Hits are applied directly in fireRay (hitscan); nothing to do here.
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, GunShotAttack> {
        private int barrels = 1;

        public Builder() {
            startup = 0; active = 1; recovery = 9;
            cooldown = 0; range = 30.0f;
            knockback = 0.6f; hitboxSize = 0.0f; hitStun = 0;
        }

        public Builder withBarrels(int barrels) {
            this.barrels = barrels;
            return this;
        }

        @Override
        public GunShotAttack build() {
            return new GunShotAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound, barrels);
        }
    }

    public static GunShotAttack create(int barrels) {
        return new Builder().withBarrels(barrels).build();
    }
}
