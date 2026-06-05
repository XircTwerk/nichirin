package com.xirc.nichirin.common.attack.moves.cqc;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.CqcMoveCatalog;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base lifecycle for close-quarters-combat attacks.
 *
 * <p>CQC attacks intentionally do not inherit katana behavior. Concrete subclasses exist
 * per move so unique effects can be added later by overriding the hook methods.</p>
 */
public abstract class AbstractCqcAttack {

    protected int startup;
    protected int active;
    protected int recovery;
    protected int cooldown;
    protected float damage;
    protected float range;
    protected float knockback;
    protected float hitboxSize;
    protected int hitStun;
    protected boolean slam;
    protected float dashDistance;

    private int tickCount;
    private boolean activeState;
    private boolean hitboxSent;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    protected AbstractCqcAttack(String moveId) {
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(moveId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown CQC move: " + moveId);
        }

        this.startup = Math.max(1, Math.min(6, definition.durationTicks() / 3));
        this.active = Math.max(2, definition.durationTicks() - startup);
        this.recovery = 4;
        this.cooldown = definition.cooldown();
        this.damage = definition.damage();
        this.range = definition.range();
        this.knockback = definition.knockback();
        this.hitboxSize = 1.05f;
        this.hitStun = definition.hitStun();
        this.dashDistance = definition.dashDistance();
    }

    public void configure(AbstractMoveset.MoveConfiguration config) {
        if (config == null) return;
        this.startup = config.getWindupOrDefault(this.startup);
        this.active = config.getDurationOrDefault(this.active);
        this.recovery = config.getRecoveryOrDefault(this.recovery);
        this.cooldown = config.getCooldownOrDefault(this.cooldown);
        this.damage = config.getDamageOrDefault(this.damage);
        this.range = config.getRangeOrDefault(this.range);
        this.knockback = config.getKnockbackOrDefault(this.knockback);
        this.hitboxSize = config.getHitboxSizeOrDefault(this.hitboxSize);
        this.hitStun = config.getHitStunOrDefault(this.hitStun);
        this.slam = config.hasSlam();
        this.dashDistance = config.getDashSpeedOrDefault(this.dashDistance);
    }

    public void start(LivingEntity user) {
        if (user.level().isClientSide()) return;
        tickCount = 0;
        hitboxSent = false;
        hitEntities.clear();
        activeState = true;
        onStart(user, user.level());
    }

    public void tick(LivingEntity user) {
        if (!activeState || user.level().isClientSide()) return;

        tickCount++;
        if (tickCount <= startup && user.hurtTime > 0 && startup > 0) {
            end(user);
            return;
        }

        if (tickCount == startup) {
            applyDash(user);
            onActiveStart(user, user.level());
        }

        if (tickCount >= startup && tickCount <= startup + active) {
            performHitDetection(user, user.level());
            onActiveTick(user, user.level());
        }

        if (tickCount >= getTotalDuration()) {
            end(user);
        }
    }

    public boolean isActive() {
        return activeState;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void stop() {
        activeState = false;
        hitEntities.clear();
    }

    protected int getTotalDuration() {
        return startup + active + recovery;
    }

    protected void performHitDetection(LivingEntity user, Level world) {
        AABB hitbox = buildHitbox(user);
        if (!hitboxSent) {
            NichirinPacketRegistry.sendHitboxToTracking(user, hitbox, Math.max(active * 50L, 1200L));
            hitboxSent = true;
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));
        if (targets.isEmpty()) return;

        DamageSource source = user instanceof Player player
                ? user.damageSources().playerAttack(player)
                : user.damageSources().mobAttack(user);

        for (LivingEntity target : targets) {
            hitEntities.add(target);
            boolean damaged = NichirinArmorDamage.hurt(target, source, damage);
            if (damaged) {
                applyKnockback(user, target);
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                    // Slam moves apply the Slammed effect for hitStun ticks (slam ticks == hit stun).
                    if (slam) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                com.xirc.nichirin.registry.NichirinEffectRegistry.slammed(),
                                hitStun, 0, false, false, true));
                    }
                }
                if (user instanceof Player player) {
                    ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
                }
            }
            onHitTarget(user, target, world);
        }
    }

    protected AABB buildHitbox(LivingEntity user) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 center = userPos.add(user.getLookAngle().scale(range));
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize
        );
    }

    protected void applyKnockback(LivingEntity user, LivingEntity target) {
        if (knockback <= 0) return;
        Vec3 knockVec = target.position().subtract(user.position()).normalize();
        target.knockback(knockback, -knockVec.x, -knockVec.z);
    }

    protected void applyDash(LivingEntity user) {
        if (dashDistance <= 0) return;

        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() <= 0.0001) return;

        Vec3 dash = horizontal.normalize().scale(dashDistance / Math.max(1, active));
        user.setDeltaMovement(dash.x, Math.max(user.getDeltaMovement().y, 0.05), dash.z);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private void end(LivingEntity user) {
        activeState = false;
        hitEntities.clear();
        onEnd(user, user.level());
    }

    protected void onStart(LivingEntity user, Level world) {}

    protected void onActiveStart(LivingEntity user, Level world) {}

    protected void onActiveTick(LivingEntity user, Level world) {
        if (tickCount % 2 == 0) {
            forwardBurst(world, user, ParticleTypes.CRIT, 2, 0.08, 0.03);
            forwardBurst(world, user, ParticleTypes.CLOUD, 1, 0.12, 0.02);
        }
    }

    protected void onEnd(LivingEntity user, Level world) {}

    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.25f, 0.85f);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.95f, 1.15f);
        hitBurst(world, target, ParticleTypes.CRIT, 14, 0.25, 0.32, 0.25, 0.14);
        hitBurst(world, target, ParticleTypes.POOF, 8, 0.22, 0.18, 0.22, 0.08);
        hitBurst(world, target, ParticleTypes.CLOUD, 6, 0.28, 0.12, 0.28, 0.04);
    }

    protected void playUserSound(Level world, LivingEntity user, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    protected void playTargetSound(Level world, LivingEntity target, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    protected void forwardBurst(Level world, LivingEntity user, ParticleOptions particle, int count,
                                double spread, double speed) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() * 0.45, 0)
                .add(user.getLookAngle().scale(Math.max(0.4f, range * 0.55f)));
        serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread * 0.35, spread, speed);
    }

    protected void hitBurst(Level world, LivingEntity target, ParticleOptions particle, int count,
                            double xSpread, double ySpread, double zSpread, double speed) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                count, xSpread, ySpread, zSpread, speed);
    }

    protected void launchTarget(LivingEntity user, LivingEntity target, double upward, double away) {
        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.08, target.getZ());
        }
        Vec3 direction = target.position().subtract(user.position());
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() > 0.0001) {
            horizontal = horizontal.normalize().scale(away);
        }
        setTargetVelocity(target, horizontal.x, upward, horizontal.z);
    }

    protected void slamTarget(LivingEntity target, double downward) {
        setTargetVelocity(target, target.getDeltaMovement().x * 0.25, -Math.abs(downward), target.getDeltaMovement().z * 0.25);
    }

    protected void shoveTarget(LivingEntity user, LivingEntity target, double strength, double lift) {
        Vec3 direction = target.position().subtract(user.position());
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() <= 0.0001) {
            horizontal = user.getLookAngle();
        }
        horizontal = new Vec3(horizontal.x, 0, horizontal.z).normalize().scale(strength);
        setTargetVelocity(target, horizontal.x, lift, horizontal.z);
    }

    protected void sidestepTarget(LivingEntity user, LivingEntity target, double strength) {
        Vec3 look = user.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        if (side.lengthSqr() <= 0.0001) return;
        side = side.normalize().scale(strength);
        setTargetVelocity(target, side.x, Math.max(target.getDeltaMovement().y, 0.05), side.z);
    }

    protected void pullTarget(LivingEntity user, LivingEntity target, double strength) {
        Vec3 towardUser = user.position().subtract(target.position());
        Vec3 horizontal = new Vec3(towardUser.x, 0, towardUser.z);
        if (horizontal.lengthSqr() <= 0.0001) return;
        horizontal = horizontal.normalize().scale(strength);
        setTargetVelocity(target, horizontal.x, Math.max(target.getDeltaMovement().y, 0.03), horizontal.z);
    }

    protected void slowTarget(LivingEntity target, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, amplifier, false, true, true));
    }

    protected void blindTarget(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, true, true));
    }

    protected void weakenTarget(LivingEntity target, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, amplifier, false, true, true));
    }

    private void setTargetVelocity(LivingEntity target, double x, double y, double z) {
        target.setDeltaMovement(x, y, z);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }
}
