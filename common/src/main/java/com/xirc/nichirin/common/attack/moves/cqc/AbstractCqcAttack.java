package com.xirc.nichirin.common.attack.moves.cqc;

import com.xirc.nichirin.common.attack.component.AbstractAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.DestructiveDeathCqcHook;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.CqcMoveCatalog;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
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

/**
 * Base lifecycle for close-quarters-combat attacks.
 * Extends AbstractAttack for shared fields and hitbox geometry.
 * Uses an external tick(LivingEntity) rather than the self-ticking system.
 * Timing terminology: windup is startup, duration is active frames; recovery is CQC-only.
 */
public abstract class AbstractCqcAttack extends AbstractAttack {

    public static final float DEMON_CQC_STAT_MULTIPLIER = 1.25f;

    // CQC-only timing field; windup and duration come from the base
    protected int recovery;

    private boolean hitboxSent;

    protected AbstractCqcAttack(String moveId) {
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(moveId);
        if (definition == null) throw new IllegalArgumentException("Unknown CQC move: " + moveId);

        this.windup   = Math.max(1, Math.min(6, definition.durationTicks() / 3));
        this.duration = Math.max(2, definition.durationTicks() - windup);
        this.recovery = 4;
        this.cooldown = definition.cooldown();
        this.damage   = definition.damage();
        this.range    = definition.range();
        this.knockback = definition.knockback();
        this.hitboxSize = 1.05f;
        this.hitStun   = definition.hitStun();
        this.dashSpeed = definition.dashDistance(); // reused as dashDistance for CQC
    }


    public void configure(AbstractMoveset.MoveConfiguration config) {
        if (config == null) return;
        this.windup    = config.getWindupOrDefault(this.windup);
        this.duration  = config.getDurationOrDefault(this.duration);
        this.recovery  = config.getRecoveryOrDefault(this.recovery);
        this.cooldown  = config.getCooldownOrDefault(this.cooldown);
        this.damage    = config.getDamageOrDefault(this.damage);
        this.range     = config.getRangeOrDefault(this.range);
        this.knockback = config.getKnockbackOrDefault(this.knockback);
        this.hitboxSize = config.getHitboxSizeOrDefault(this.hitboxSize);
        this.hitStun   = config.getHitStunOrDefault(this.hitStun);
        this.slam      = config.hasSlam();
        this.hyperArmor = config.hasHyperArmor();
        this.dashSpeed = config.getDashSpeedOrDefault(this.dashSpeed != null ? this.dashSpeed : 0f);
    }


    public void start(LivingEntity user) {
        if (user.level().isClientSide()) return;
        this.user  = user;
        this.world = user.level();
        tickCount = 0;
        hitboxSent = false;
        getHitEntities().clear();
        setHitCount(0);
        isActive = true;
        onStart(user, user.level());
    }

    public void tick(LivingEntity user) {
        if (!isActive || user.level().isClientSide()) return;

        tickCount++;
        // Windup cancel on hit (unless hyper armor)
        if (!hyperArmor && tickCount <= windup && user.hurtTime > 0 && windup > 0) {
            endInternal(user);
            return;
        }

        if (tickCount == windup) {
            applyDash(user);
            onActiveStart(user, user.level());
        }

        if (tickCount >= windup && tickCount <= windup + duration) {
            performHitDetection(user, user.level());
            onActiveTick(user, user.level());
        }

        if (tickCount >= getTotalDuration()) endInternal(user);
    }

    @Override
    public int getTotalDuration() { return windup + duration + recovery; }

    /** CQC is ticked externally and never registers in the self-ticking map. */
    @Override
    protected void registerForTicking() {}

    @Override
    public void stop() {
        isActive = false;
        getHitEntities().clear();
    }


    protected void performHitDetection(LivingEntity user, Level world) {
        AABB hitbox = buildHitbox(user);
        if (!hitboxSent) {
            NichirinPacketRegistry.sendHitboxToTracking(user, hitbox, Math.max(duration * 50L, 1200L));
            hitboxSent = true;
        }

        DamageSource source = user instanceof TempleDemonEntity
                ? NichirinDamageSources.templeDemon(user)
                : NichirinDamageSources.cqc(user);

        world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive()
                        && !getHitEntities().contains(entity.getUUID())
                        && acceptTarget(user, entity)
        ).forEach(target -> {
            getHitEntities().add(target.getUUID());
            float scaledDamage = scaleCqcStat(user, damage);
            boolean damaged = NichirinArmorDamage.hurt(target, source, scaledDamage);
            if (damaged) {
                applyKnockbackCqc(user, target);
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                    if (slam) target.addEffect(new MobEffectInstance(
                            NichirinEffectRegistry.slammed(), hitStun, 0, false, false, true));
                }
                if (user instanceof Player player)
                    ComboIntegration.handleSuccessfulHit(player, target, hitStun, scaledDamage);
            }
            onHitTarget(user, target, world);
            DestructiveDeathCqcHook.onCqcHit(user, target, world);
        });
    }

    protected AABB buildHitbox(LivingEntity user) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        float r = scaleCqcStat(user, range);
        float s = scaleCqcStat(user, hitboxSize);
        Vec3 center = pos.add(user.getLookAngle().scale(r));
        return new AABB(center.x - s, center.y - s, center.z - s,
                        center.x + s, center.y + s, center.z + s);
    }

    private void applyKnockbackCqc(LivingEntity user, LivingEntity target) {
        if (knockback <= 0) return;
        Vec3 kv = target.position().subtract(user.position()).normalize();
        target.knockback(scaleCqcStat(user, knockback), -kv.x, -kv.z);
    }

    protected void applyDash(LivingEntity user) {
        if (dashSpeed == null || dashSpeed <= 0) return;
        Vec3 look = user.getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        if (h.lengthSqr() <= 0.0001) return;
        Vec3 dash = h.normalize().scale(scaleCqcStat(user, dashSpeed) / Math.max(1, duration));
        user.setDeltaMovement(dash.x, Math.max(user.getDeltaMovement().y, 0.05), dash.z);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    public void applyDamageMultiplier(float multiplier) {
        if (multiplier == 1.0f) return;
        damage *= multiplier;
    }

    private void endInternal(LivingEntity user) {
        isActive = false;
        getHitEntities().clear();
        onEnd(user, user.level());
    }


    @Override
    protected void onStart() {
        if (user != null) onStart(user, user.level());
    }

    protected void onStart(LivingEntity user, Level world) {}

    protected boolean acceptTarget(LivingEntity user, LivingEntity candidate) { return true; }

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


    protected void launchTarget(LivingEntity user, LivingEntity target, double upward, double away) {
        if (target.onGround()) target.setPos(target.getX(), target.getY() + 0.08, target.getZ());
        Vec3 dir = target.position().subtract(user.position());
        Vec3 h = new Vec3(dir.x, 0, dir.z);
        if (h.lengthSqr() > 0.0001) h = h.normalize().scale(scaleCqcStat(user, away));
        setTargetVelocity(target, h.x, scaleCqcStat(user, upward), h.z);
    }

    protected void slamTarget(LivingEntity target, double downward) {
        setTargetVelocity(target,
                target.getDeltaMovement().x * 0.25,
                -Math.abs(downward),
                target.getDeltaMovement().z * 0.25);
    }

    protected void shoveTarget(LivingEntity user, LivingEntity target, double strength, double lift) {
        Vec3 dir = target.position().subtract(user.position());
        Vec3 h = new Vec3(dir.x, 0, dir.z);
        if (h.lengthSqr() <= 0.0001) h = user.getLookAngle();
        h = new Vec3(h.x, 0, h.z).normalize().scale(scaleCqcStat(user, strength));
        setTargetVelocity(target, h.x, scaleCqcStat(user, lift), h.z);
    }

    protected void sidestepTarget(LivingEntity user, LivingEntity target, double strength) {
        Vec3 look = user.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        if (side.lengthSqr() <= 0.0001) return;
        side = side.normalize().scale(scaleCqcStat(user, strength));
        setTargetVelocity(target, side.x, Math.max(target.getDeltaMovement().y, 0.05), side.z);
    }

    protected void pullTarget(LivingEntity user, LivingEntity target, double strength) {
        Vec3 toward = user.position().subtract(target.position());
        Vec3 h = new Vec3(toward.x, 0, toward.z);
        if (h.lengthSqr() <= 0.0001) return;
        h = h.normalize().scale(scaleCqcStat(user, strength));
        setTargetVelocity(target, h.x, Math.max(target.getDeltaMovement().y, 0.03), h.z);
    }

    protected void slowTarget(LivingEntity target, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                durationTicks, amplifier, false, true, true));
    }

    protected void blindTarget(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                durationTicks, 0, false, true, true));
    }

    protected void weakenTarget(LivingEntity target, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                durationTicks, amplifier, false, true, true));
    }

    private void setTargetVelocity(LivingEntity target, double x, double y, double z) {
        target.setDeltaMovement(x, y, z);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (target instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(target));
    }


    protected void playUserSound(Level world, LivingEntity user, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    protected void playTargetSound(Level world, LivingEntity target, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    protected void forwardBurst(Level world, LivingEntity user, ParticleOptions particle,
                                int count, double spread, double speed) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() * 0.45, 0)
                .add(user.getLookAngle().scale(Math.max(0.4f, scaleCqcStat(user, range) * 0.55f)));
        sl.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread * 0.35, spread, speed);
    }

    protected void hitBurst(Level world, LivingEntity target, ParticleOptions particle,
                            int count, double xSpread, double ySpread, double zSpread, double speed) {
        if (!(world instanceof ServerLevel sl)) return;
        sl.sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                count, xSpread, ySpread, zSpread, speed);
    }


    protected static float scaleCqcStat(LivingEntity user, float value) {
        return isDemonUser(user) ? value * DEMON_CQC_STAT_MULTIPLIER : value;
    }

    protected static double scaleCqcStat(LivingEntity user, double value) {
        return isDemonUser(user) ? value * DEMON_CQC_STAT_MULTIPLIER : value;
    }

    public static boolean isDemonUser(LivingEntity user) {
        if (user instanceof Player player) return DemonManager.isDemon(player);
        return user instanceof DemonNPCEntity;
    }
}
