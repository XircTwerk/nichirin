package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

    // ── Configuration (immutable after construction) ──────────────────────────
    protected final int startup;
    protected final int active;
    protected final int recovery;
    protected final int cooldown;
    protected final float damage;
    protected final float range;
    protected final float knockback;
    protected final float hitboxSize;
    protected final Vec3 hitboxOffset;
    protected final int hitStun;
    protected final SoundEvent startSound;
    protected final SoundEvent hitSound;

    /**
     * When {@code true} (default), hit-detection stops after the first successful hit
     * per activation.  Set to {@code false} in Thrust so every entity in the dash path
     * is struck each active tick (the {@code hitEntities} set still prevents double-hits).
     */
    protected boolean stopAfterFirstHit = true;

    // ── State ─────────────────────────────────────────────────────────────────
    protected int tickCount  = 0;
    protected boolean isActive = false;
    protected boolean hasHit   = false;
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public final void start(Player player) {
        if (player.level().isClientSide()) return;

        tickCount = 0;
        hasHit    = false;
        hitEntities.clear();
        isActive  = true;

        onStart(player);
    }

    public final void tick(Player player) {
        if (!isActive) return;
        if (player.level().isClientSide()) return;

        tickCount++;

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

    // ── Hit detection ─────────────────────────────────────────────────────────

    protected final void performHitDetection(Player user, Level world) {
        AABB hitbox = buildHitbox(user);

        NichirinPacketRegistry.sendHitboxToTracking(user, hitbox, Math.max(active * 50L, 1500L));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive() && !hitEntities.contains(entity));

        if (targets.isEmpty()) return;

        hasHit = true;
        DamageSource damageSource = user.damageSources().playerAttack(user);

        for (LivingEntity target : targets) {
            if (hitEntities.contains(target)) continue;

            // Damage first — vanilla hurt() resets invulnerableTime internally,
            // so we must override it AFTER the call, not before.
            boolean damaged = target.hurt(damageSource, damage);
            hitEntities.add(target);

            // Only apply post-hit effects if damage actually landed
            if (damaged) {
                applyKnockback(user, target);
                if (hitStun > 0) target.invulnerableTime = hitStun;
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
    protected AABB buildHitbox(Player user) {
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
    protected void applyKnockback(Player user, LivingEntity target) {
        if (knockback > 0) {
            Vec3 knockVec = target.position().subtract(user.position()).normalize();
            target.knockback(knockback, -knockVec.x, -knockVec.z);
        }
    }

    private void end(Player player) {
        isActive = false;
        hitEntities.clear();
        onEnd(player);
    }

    // ── Hook methods ──────────────────────────────────────────────────────────

    /** Called at the start of the attack (after state reset). Play sounds / start particles here. */
    protected void onStart(Player player) {}

    /** Called on the first active tick ({@code tickCount == startup}). Apply dash velocity here. */
    protected void onActiveStart(Player player) {}

    /** Called every tick while in active frames. Emit trail particles here. */
    protected void onActiveTick(Player player) {}

    /**
     * Called for every newly-struck target.
     * Apply unique hit effects (e.g. STUNNED), extra particles, and hit sounds here.
     */
    protected abstract void onHitTarget(Player user, LivingEntity target, Level world);

    /** Called when the attack expires. */
    protected void onEnd(Player player) {}

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isActive()     { return isActive; }
    public int getTotalDuration() { return startup + active + recovery; }
    public int getCooldown()      { return cooldown; }

    // ── Shared builder ────────────────────────────────────────────────────────

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
        protected float damage    = 5.0f;
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
