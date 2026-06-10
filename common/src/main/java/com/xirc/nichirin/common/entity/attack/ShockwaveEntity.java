package com.xirc.nichirin.common.entity.attack;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
    private int maxLifeTicks = 30;
    private float speed = 0.6f; // blocks per tick (12 blocks / second at 20 TPS)
    private float hitboxRadius = 1.2f;
    private int piercesRemaining = 0;       // 0 = single-hit per entity, destroys after 1 target
    private int bouncesRemaining = 0;       // 0 = expires on first wall hit
    private boolean phaseWalls = false;     // true = ignore wall collisions entirely
    private boolean red = false;

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
            DamageSource source = ownerEntity != null
                    ? NichirinDamageSources.demon(ownerEntity)
                    : level().damageSources().generic();
            target.hurt(source, damage);

            if (knockback > 0) {
                Vec3 push = this.getDeltaMovement().normalize().scale(knockback);
                target.push(push.x, 0.15, push.z);
                target.hurtMarked = true;
            }

            hitEntities.add(target.getUUID());

            if (piercesRemaining <= 0) {
                destroyShockwave();
                return;
            }
            piercesRemaining--;
        }
    }

    private void destroyShockwave() {
        if (auraId != null) {
            AuraManager.removeAura(this, auraId);
            auraId = null;
        }
        this.discard();
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
        AuraInstance aura = AuraInstance.builder()
                .color(red ? 1.0f : 0.35f, red ? 0.2f : 0.55f, red ? 0.25f : 1.0f, 0.85f)
                .radius(1.6f)
                .jitter(4.0f)
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

    // ===== Builder =====
    public static final class Builder {
        private LivingEntity owner;
        private Vec3 origin;
        private Vec3 direction = new Vec3(1, 0, 0);
        private float damage = 4.0f;
        private float knockback = 0.4f;
        private int lifeTicks = 30;
        private float speed = 0.6f;
        private float hitboxRadius = 1.2f;
        private int pierces = 0;
        private int bounces = 0;
        private boolean phaseWalls = false;
        private boolean red = false;

        public Builder owner(LivingEntity o) { this.owner = o; return this; }
        public Builder origin(Vec3 v) { this.origin = v; return this; }
        public Builder direction(Vec3 v) { this.direction = v.normalize(); return this; }
        public Builder damage(float v) { this.damage = v; return this; }
        public Builder knockback(float v) { this.knockback = v; return this; }
        public Builder lifeTicks(int v) { this.lifeTicks = v; return this; }
        public Builder speed(float v) { this.speed = v; return this; }
        public Builder hitboxRadius(float v) { this.hitboxRadius = v; return this; }
        public Builder pierces(int v) { this.pierces = v; return this; }
        public Builder bounces(int v) { this.bounces = v; return this; }
        public Builder phaseWalls(boolean v) { this.phaseWalls = v; return this; }
        public Builder red(boolean v) { this.red = v; return this; }

        public ShockwaveEntity spawn(EntityType<ShockwaveEntity> type, Level level) {
            if (owner == null || origin == null) {
                throw new IllegalStateException("ShockwaveEntity.Builder requires owner + origin");
            }
            ShockwaveEntity sw = new ShockwaveEntity(type, level);
            sw.ownerId = owner.getUUID();
            sw.damage = damage;
            sw.knockback = knockback;
            sw.maxLifeTicks = lifeTicks;
            sw.speed = speed;
            sw.hitboxRadius = hitboxRadius;
            sw.piercesRemaining = pierces;
            sw.bouncesRemaining = bounces;
            sw.phaseWalls = phaseWalls;
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
