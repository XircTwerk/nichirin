package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared base for the three default katana wheel attacks: Check, Overhead, and Thrust.
 *
 * <p>Owns the full startup → active → recovery lifecycle, hit detection loop, hitbox
 * visualisation, and common combat math (damage, knockback, hit-stun).  Subclasses
 * customise behaviour through a small set of protected hook methods and, where needed,
 * overrides of {@link #buildHitbox} or {@link #applyKnockback}.</p>
 *
 * <h3>Hook methods (all optional except {@link #onHitTarget})</h3>
 * <ul>
 *   <li>{@link #onStart}        – runs after state reset; play sounds / spawn start particles here.</li>
 *   <li>{@link #onActiveStart}  – runs on the first active tick; apply dash velocity here (Thrust).</li>
 *   <li>{@link #onActiveTick}   – runs every active tick; emit trail particles here (Thrust).</li>
 *   <li>{@link #onHitTarget}    – <b>abstract</b>; apply unique hit effects, particles, sounds here.</li>
 *   <li>{@link #onEnd}          – runs when the attack expires.</li>
 * </ul>
 */
public abstract class AbstractKatanaAttack {

    // Stat fields are non-final so they can be overridden by a moveset MoveConfiguration
    // via {@link #configure}. Defaults come from the subclass Builder; the moveset is authoritative.
    protected int startup;
    protected int active;
    protected int recovery;
    protected int cooldown;
    protected float damage;
    protected float range;
    protected float knockback;
    protected float hitboxSize;
    protected final Vec3 hitboxOffset;
    protected int hitStun;
    protected final SoundEvent startSound;
    protected final SoundEvent hitSound;

    /**
     * When {@code true} (default), hit-detection stops after the first successful hit
     * per activation.  Set to {@code false} in Thrust so every entity in the dash path
     * is struck each active tick (the {@code hitEntities} set still prevents double-hits).
     */
    protected boolean stopAfterFirstHit = true;

    protected int tickCount  = 0;
    protected boolean isActive = false;
    protected boolean hasHit   = false;
    protected boolean hitboxSent = false;
    protected final Set<LivingEntity> hitEntities = new HashSet<>();

    protected AbstractKatanaAttack(int startup, int active, int recovery, int cooldown,
                                    float damage, float range, float knockback,
                                    float hitboxSize, Vec3 hitboxOffset, int hitStun,
                                    SoundEvent startSound, SoundEvent hitSound) {
        this.startup      = startup;
        this.active       = active;
        this.recovery     = recovery;
        this.cooldown     = cooldown;
        this.damage       = damage;
        this.range        = range;
        this.knockback    = knockback;
        this.hitboxSize   = hitboxSize;
        this.hitboxOffset = hitboxOffset;
        this.hitStun      = hitStun;
        this.startSound   = startSound;
        this.hitSound     = hitSound;
    }


    /**
     * Overrides this attack's stats from a moveset {@link AbstractMoveset.MoveConfiguration}.
     * Any value the config doesn't specify keeps the subclass Builder default.
     * Timing maps: windup -> startup, duration -> active, recovery -> recovery.
     */
    public void configure(AbstractMoveset.MoveConfiguration config) {
        if (config == null) return;
        this.startup    = config.getWindupOrDefault(this.startup);
        this.active     = config.getDurationOrDefault(this.active);
        this.recovery   = config.getRecoveryOrDefault(this.recovery);
        this.cooldown   = config.getCooldownOrDefault(this.cooldown);
        this.damage     = config.getDamageOrDefault(this.damage);
        this.range      = config.getRangeOrDefault(this.range);
        this.knockback  = config.getKnockbackOrDefault(this.knockback);
        this.hitboxSize = config.getHitboxSizeOrDefault(this.hitboxSize);
        this.hitStun    = config.getHitStunOrDefault(this.hitStun);
    }

    public final void start(LivingEntity player) {
        if (player.level().isClientSide()) return;

        tickCount  = 0;
        hasHit     = false;
        hitboxSent = false;
        hitEntities.clear();
        isActive   = true;

        onStart(player);
    }

    public final void tick(LivingEntity player) {
        if (!isActive) return;
        if (player.level().isClientSide()) return;

        tickCount++;

        if (tickCount <= startup && player.hurtTime > 0 && startup > 0) {
            end(player);
            return;
        }

        if (tickCount == startup) {
            onActiveStart(player);
        }

        if (tickCount >= startup && tickCount <= startup + active) {
            if (!stopAfterFirstHit || !hasHit) {
                performHitDetection(player, player.level());
            }
            onActiveTick(player);
        }

        if (tickCount >= getTotalDuration()) {
            end(player);
        }
    }


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
            if (hitEntities.contains(target)) continue;

            // Damage first — vanilla hurt() resets invulnerableTime internally,
            // so we must override it AFTER the call, not before.
            boolean damaged = NichirinArmorDamage.hurt(target, damageSource, damage);
            hitEntities.add(target);

            if (damaged) {
                applyKnockback(user, target);
                if (hitStun > 0) target.invulnerableTime = hitStun;

                if (user instanceof Player p) {
                    ComboIntegration.handleSuccessfulHit(p, target, hitStun, damage);
                }
            }

            onHitTarget(user, target, world);
        }
    }

    /**
     * Builds the AABB for this tick's hit scan.
     *
     * <p>Default: a cube of radius {@code hitboxSize} centred {@code range} blocks ahead
     * of the player (plus {@code hitboxOffset}).  Override in Thrust to use a fixed
     * 1-block-ahead box that tracks the dashing player's position each tick.</p>
     */
    protected AABB buildHitbox(LivingEntity user) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 center  = userPos.add(lookDir.scale(range)).add(hitboxOffset);
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize, center.z + hitboxSize
        );
    }

    /**
     * Applies knockback to a struck target.
     *
     * <p>Default: pushes the target directly away from the player.
     * Override in Overhead to slam airborne targets straight down.</p>
     */
    protected void applyKnockback(LivingEntity user, LivingEntity target) {
        if (knockback > 0) {
            Vec3 knockVec = target.position().subtract(user.position()).normalize();
            target.knockback(knockback, -knockVec.x, -knockVec.z);
        }
    }

    private void end(LivingEntity player) {
        isActive = false;
        hitEntities.clear();
        onEnd(player);
    }

    public void stop() {
        isActive = false;
        hitEntities.clear();
    }


    /** Called at the start of the attack (after state reset). Play sounds / start particles here. */
    protected void onStart(LivingEntity player) {}

    /** Called on the first active tick ({@code tickCount == startup}). Apply dash velocity here. */
    protected void onActiveStart(LivingEntity player) {}

    /** Called every tick while in active frames. Emit trail particles here. */
    protected void onActiveTick(LivingEntity player) {}

    /**
     * Called for every newly-struck target.
     * Apply unique hit effects (e.g. STUNNED), extra particles, and hit sounds here.
     */
    protected abstract void onHitTarget(LivingEntity user, LivingEntity target, Level world);

    /** Called when the attack expires. */
    protected void onEnd(LivingEntity player) {}


    public boolean isActive()     { return isActive; }
    public int getTotalDuration() { return startup + active + recovery; }
    public int getCooldown()      { return cooldown; }

    public void applyDamageAndStunMultiplier(float multiplier) {
        if (multiplier == 1.0f) return;
        this.damage *= multiplier;
        if (this.hitStun > 0) {
            this.hitStun = Math.max(1, Math.round(this.hitStun * multiplier));
        }
    }


    /**
     * Generic builder base.  Each concrete attack provides a thin subclass that
     * sets sensible defaults in its no-arg constructor and implements {@link #build()}.
     *
     * @param <B> the concrete Builder type (self-type for fluent chaining)
     * @param <T> the concrete attack type produced by {@link #build()}
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Builder<B, T>, T extends AbstractKatanaAttack> {

        protected int startup     = 4;
        protected int active      = 6;
        protected int recovery    = 8;
        protected int cooldown    = 40;
        protected float damage    = 0.0f;
        protected float range     = 2.5f;
        protected float knockback = 1.0f;
        protected float hitboxSize   = 2.0f;
        protected Vec3  hitboxOffset = Vec3.ZERO;
        protected int   hitStun      = 10;
        protected SoundEvent startSound = null;
        protected SoundEvent hitSound   = null;

        public B withTiming(int startup, int active, int recovery) {
            this.startup = startup; this.active = active; this.recovery = recovery;
            return (B) this;
        }
        public B withCooldown(int cooldown)              { this.cooldown = cooldown;   return (B) this; }
        public B withDamage(float damage)                { this.damage = damage;       return (B) this; }
        public B withRange(float range)                  { this.range = range;         return (B) this; }
        public B withKnockback(float knockback)          { this.knockback = knockback; return (B) this; }
        public B withHitbox(float size, Vec3 offset)     { this.hitboxSize = size; this.hitboxOffset = offset; return (B) this; }
        public B withHitStun(int hitStun)                { this.hitStun = hitStun;     return (B) this; }
        public B withSounds(SoundEvent s, SoundEvent h)  { this.startSound = s; this.hitSound = h; return (B) this; }

        public abstract T build();
    }
}
