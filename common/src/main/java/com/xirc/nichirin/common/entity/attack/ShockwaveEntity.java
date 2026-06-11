package com.xirc.nichirin.common.entity.attack;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.architectury.networking.NetworkManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative shockwave projectile. Carries an aura (visual handled by the aura system,
 * not a custom sprite/animation), travels forward, sweeps an AABB for damage every tick, optionally
 * pierces entities, bounces off walls, and phases through them.
 *
 * <p>Spawned via {@link Builder} — most fields are runtime-configurable per shockwave instance.
 * Default tint is blue (Akaza); Overdrive flips it red at spawn time.</p>
 */
public class ShockwaveEntity extends Entity {

    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(ShockwaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED_TINT =
            SynchedEntityData.defineId(ShockwaveEntity.class, EntityDataSerializers.BOOLEAN);

    // Owner + behaviour config (server-side only — clients don't need it for visuals).
    private UUID ownerId;
    private float damage = 4.0f;
    private float knockback = 0.4f;
    private int hitStun = 10;
    private int maxLifeTicks = 30;
    private float speed = 1.5f; // blocks per tick (30 blocks / second at 20 TPS)
    private float hitboxRadius = 1.2f;
    private int piercesRemaining = 0;       // 0 = single-hit per entity, destroys after 1 target
    private int bouncesRemaining = 0;       // 0 = expires on first wall hit
    private boolean phaseWalls = false;     // true = ignore wall collisions entirely
    private boolean noHorizontalPush = false; // true = knockback applies as pure lift, no sideways
    private boolean red = false;
    private static final float IMPACT_HALF_EXTENT = 1.0f; // 2x2x2 impact hitbox at end of life

    private final Set<UUID> hitEntities = new HashSet<>();
    private UUID auraId; // tracked so we remove the aura when the entity dies

    public ShockwaveEntity(EntityType<? extends ShockwaveEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFE_TICKS, 0);
        builder.define(RED_TINT, false);
    }

    @Override
    public void tick() {
        super.tick();

        // Move on both sides so client interpolation is smooth.
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (level().isClientSide) return;

        int life = entityData.get(LIFE_TICKS) + 1;
        entityData.set(LIFE_TICKS, life);
        if (life >= maxLifeTicks) {
            destroyShockwave();
            return;
        }

        if (!phaseWalls && level().getBlockState(blockPosition()).isCollisionShapeFullBlock(level(), blockPosition())) {
            if (bouncesRemaining > 0) {
                bouncesRemaining--;
                // Reflect: invert velocity. Simple billiard-style; doesn't pick which axis to flip.
                this.setDeltaMovement(this.getDeltaMovement().reverse().scale(0.85));
            } else {
                destroyShockwave();
                return;
            }
        }

        sweepDamage();
    }

    private void sweepDamage() {
        AABB sweep = new AABB(
                getX() - hitboxRadius, getY() - hitboxRadius, getZ() - hitboxRadius,
                getX() + hitboxRadius, getY() + hitboxRadius, getZ() + hitboxRadius);

        LivingEntity ownerEntity = ownerEntity();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != ownerEntity && e.isAlive() && !hitEntities.contains(e.getUUID()));
        if (targets.isEmpty()) return;

        for (LivingEntity target : targets) {
            applyHit(ownerEntity, target);
            hitEntities.add(target.getUUID());

            if (piercesRemaining <= 0) {
                destroyShockwave();
                return;
            }
            piercesRemaining--;
        }
    }

    /** Damage + stun + combo tracking — shared by per-tick sweep and the 2×2×2 impact AoE. */
    private void applyHit(LivingEntity ownerEntity, LivingEntity target) {
        DamageSource source = ownerEntity != null
                ? NichirinDamageSources.demon(ownerEntity)
                : level().damageSources().generic();
        target.hurt(source, damage);

        if (knockback > 0) {
            if (noHorizontalPush) {
                // Pure-lift mode: zero out current horizontal motion + push straight up. Used by
                // launcher moves (Flying Planet) so the target stays under you for follow-ups.
                Vec3 v = target.getDeltaMovement();
                target.setDeltaMovement(0, knockback, 0);
                target.hurtMarked = true;
            } else {
                Vec3 push = this.getDeltaMovement().normalize().scale(knockback);
                target.push(push.x, 0.15, push.z);
                target.hurtMarked = true;
            }
        }

        if (hitStun > 0) {
            target.invulnerableTime = 0; // don't lock out the stun-application from i-frames
            target.addEffect(new MobEffectInstance(NichirinEffectRegistry.stunned(),
                    hitStun, 0, false, false, true));
        }

        if (ownerEntity instanceof Player ownerPlayer) {
            ComboIntegration.handleSuccessfulHit(ownerPlayer, target, hitStun, damage);
        }
    }

    /** 2×2×2 AABB centred on the current position; applied once at termination. */
    private void impactBurst() {
        if (level().isClientSide) return;
        AABB box = new AABB(
                getX() - IMPACT_HALF_EXTENT, getY() - IMPACT_HALF_EXTENT, getZ() - IMPACT_HALF_EXTENT,
                getX() + IMPACT_HALF_EXTENT, getY() + IMPACT_HALF_EXTENT, getZ() + IMPACT_HALF_EXTENT);
        LivingEntity ownerEntity = ownerEntity();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != ownerEntity && e.isAlive() && !hitEntities.contains(e.getUUID()));
        for (LivingEntity target : targets) {
            applyHit(ownerEntity, target);
            hitEntities.add(target.getUUID());
        }
    }

    private void destroyShockwave() {
        impactBurst();
        spawnImpactEffect();
        if (auraId != null) {
            AuraManager.removeAura(this, auraId);
            auraId = null;
        }
        this.discard();
    }

    private void spawnImpactEffect() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        double x = getX(), y = getY() + 0.5, z = getZ();
        var spark = NichirinParticleRegistry.SLASH_IMPACT_SPARK.get();
        var flash = red ? NichirinParticleRegistry.FLASH1.get() : NichirinParticleRegistry.BLUE_FLASH1.get();
        var ring  = red ? NichirinParticleRegistry.SHOCKWAVE.get() : NichirinParticleRegistry.BLUE_SHOCKWAVE.get();
        serverLevel.sendParticles(spark, x, y, z, 14, 0.6, 0.6, 0.6, 0.2);
        serverLevel.sendParticles(flash, x, y, z, 3, 0.3, 0.3, 0.3, 0.0);
        serverLevel.sendParticles(ring,  x, y, z, 5, 0.5, 0.1, 0.5, 0.05);
        serverLevel.playSound(null, x, y, z,
                NicirinSoundRegistry.PARRY_CLASH.get(), SoundSource.PLAYERS, 0.9f, 0.5f);
    }

    private LivingEntity ownerEntity() {
        if (ownerId == null || level().isClientSide) return null;
        for (Entity e : ((net.minecraft.server.level.ServerLevel) level()).getAllEntities()) {
            if (ownerId.equals(e.getUUID()) && e instanceof LivingEntity le) return le;
        }
        return null;
    }

    /** Attaches the visual aura. Called by the spawner once the entity is in the world. */
    public void attachAura() {
        // Pure blue / pure red. The aura system applies brightness ×1.15 and a whiten pass on the
        // inner pixels, both of which lift the green channel toward white. Keep green near zero so
        // the rim reads as clearly blue (not cyan/teal/green) at every distance.
        AuraInstance aura = AuraInstance.builder()
                .color(red ? 1.0f : 0.05f, red ? 0.05f : 0.10f, red ? 0.10f : 1.0f, 0.85f)
                .radius(1.4f)
                .jitter(2.2f)
                .build();
        auraId = aura.id();
        AuraManager.addAura(this, aura, AuraAudience.ALL);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // Shockwaves are short-lived; nothing persisted.
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return NetworkManager.createAddEntityPacket(this, serverEntity);
    }

    public static final class Builder {
        private LivingEntity owner;
        private Vec3 origin;
        private Vec3 direction = new Vec3(1, 0, 0);
        private float damage = 4.0f;
        private float knockback = 0.4f;
        private int hitStun = 10;
        private int lifeTicks = 30;
        private float speed = 1.5f;
        private float hitboxRadius = 1.2f;
        private int pierces = 0;
        private int bounces = 0;
        private boolean phaseWalls = false;
        private boolean noHorizontalPush = false;
        private boolean red = false;

        public Builder owner(LivingEntity o) { this.owner = o; return this; }
        public Builder origin(Vec3 v) { this.origin = v; return this; }
        public Builder direction(Vec3 v) { this.direction = v.normalize(); return this; }
        public Builder damage(float v) { this.damage = v; return this; }
        public Builder knockback(float v) { this.knockback = v; return this; }
        public Builder hitStun(int v) { this.hitStun = v; return this; }
        public Builder lifeTicks(int v) { this.lifeTicks = v; return this; }
        public Builder speed(float v) { this.speed = v; return this; }
        public Builder hitboxRadius(float v) { this.hitboxRadius = v; return this; }
        public Builder pierces(int v) { this.pierces = v; return this; }
        public Builder bounces(int v) { this.bounces = v; return this; }
        public Builder phaseWalls(boolean v) { this.phaseWalls = v; return this; }
        public Builder noHorizontalPush(boolean v) { this.noHorizontalPush = v; return this; }
        public Builder red(boolean v) { this.red = v; return this; }

        public ShockwaveEntity spawn(EntityType<ShockwaveEntity> type, Level level) {
            if (owner == null || origin == null) {
                throw new IllegalStateException("ShockwaveEntity.Builder requires owner + origin");
            }
            ShockwaveEntity sw = new ShockwaveEntity(type, level);
            sw.ownerId = owner.getUUID();
            sw.damage = damage;
            sw.knockback = knockback;
            sw.hitStun = hitStun;
            sw.maxLifeTicks = lifeTicks;
            sw.speed = speed;
            sw.hitboxRadius = hitboxRadius;
            sw.piercesRemaining = pierces;
            sw.bouncesRemaining = bounces;
            sw.phaseWalls = phaseWalls;
            sw.noHorizontalPush = noHorizontalPush;
            sw.red = red;
            sw.setPos(origin.x, origin.y, origin.z);
            sw.setDeltaMovement(direction.scale(speed));
            sw.entityData.set(RED_TINT, red);
            level.addFreshEntity(sw);
            sw.attachAura();
            return sw;
        }
    }
}
