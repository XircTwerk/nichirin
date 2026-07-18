package com.xirc.nichirin.common.entity.projectile;

import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinDamageHandler;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thrown katana projectile for Beast Breathing's Eleventh Fang.
 * Spins forward, pierces entities, sticks to blocks, expires after 10 seconds.
 */
public class ThrownKatanaEntity extends Entity {

    private static final int MAX_LIFE_TICKS = 200; // 10 seconds
    private static final double HIT_RADIUS = 0.4;
    /** Half the visual blade length in blocks (model is 31/16 blocks long). */
    private static final float KATANA_HALF_LEN = 31.0f / 32.0f;
    /** Blade cross-section thickness for the hitbox. */
    private static final float KATANA_THICKNESS = 0.08f;

    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(ThrownKatanaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> THROWN_ITEM =
            SynchedEntityData.defineId(ThrownKatanaEntity.class, EntityDataSerializers.ITEM_STACK);

    private int lifeTicks = 0;
    private boolean stuck = false;
    private float damage = 20.0f;
    private int hitStun = 20;
    private Entity owner;
    private final Set<UUID> hitEntities = new HashSet<>();
    /** Last known normalised travel direction — kept so stuck katanas preserve their orientation. */
    private Vec3 travelDir = new Vec3(0, 0, 1);

    public ThrownKatanaEntity(EntityType<? extends ThrownKatanaEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownKatanaEntity(EntityType<? extends ThrownKatanaEntity> entityType, Level level,
                               LivingEntity owner, float damage, int hitStun) {
        this(entityType, level);
        this.owner = owner;
        this.damage = damage;
        this.hitStun = hitStun;
    }

    /**
     * Override EntityDimensions so Minecraft uses a near-zero base size.
     * The actual collision AABB is computed dynamically in makeBoundingBox().
     */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(KATANA_THICKNESS * 2, KATANA_THICKNESS * 2);
    }

    /**
     * Builds an oriented AABB that follows the katana's travel direction.
     * The box extends KATANA_HALF_LEN blocks along the flight axis and
     * KATANA_THICKNESS blocks on the cross axes, so it tightly wraps the model.
     */
    @Override
    public AABB makeBoundingBox() {
        Vec3 pos = position();
        Vec3 dir = (travelDir != null ? travelDir : new Vec3(0, 0, 1)).scale(KATANA_HALF_LEN);
        return new AABB(
                pos.x - Math.abs(dir.x) - KATANA_THICKNESS,
                pos.y - Math.abs(dir.y) - KATANA_THICKNESS,
                pos.z - Math.abs(dir.z) - KATANA_THICKNESS,
                pos.x + Math.abs(dir.x) + KATANA_THICKNESS,
                pos.y + Math.abs(dir.y) + KATANA_THICKNESS,
                pos.z + Math.abs(dir.z) + KATANA_THICKNESS
        );
    }

    public boolean isStuck() {
        return entityData.get(STUCK);
    }

    public ItemStack getThrownItem() {
        return entityData.get(THROWN_ITEM);
    }

    public void setThrownItem(ItemStack stack) {
        entityData.set(THROWN_ITEM, stack.copy());
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STUCK, false);
        builder.define(THROWN_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();

        lifeTicks++;
        if (lifeTicks >= MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        if (stuck) {
            return;
        }

        Vec3 motion = getDeltaMovement().add(0.0, -0.05, 0.0);
        setDeltaMovement(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);

        // Keep travelDir current so makeBoundingBox() stays accurate in flight.
        if (motion.lengthSqr() > 0.001) {
            travelDir = motion.normalize();
        }

        // Point toward travel direction
        double horizDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Mth.atan2(-motion.y, horizDist) * (180.0 / Math.PI)));

        // Only stick after a real block hit along this tick's path. The old broad
        // collision check could report a nearby block and freeze the katana in air.
        BlockHitResult hit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 dir = motion.normalize();
            Vec3 surface = hit.getLocation();
            // Back off so the blade tip embeds about 0.3 blocks into the surface.
            double backoff = KATANA_HALF_LEN - 0.3;
            setPos(surface.x - dir.x * backoff,
                   surface.y - dir.y * backoff,
                   surface.z - dir.z * backoff);
            stuck = true;
            entityData.set(STUCK, true);
            setDeltaMovement(Vec3.ZERO);
            // Refresh hitbox at stick position with final orientation.
            this.setBoundingBox(makeBoundingBox());
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ARROW_HIT, SoundSource.NEUTRAL, 1.0f, 1.2f);
            return;
        }

        setPos(end);
        // Refresh AABB after moving so the oriented hitbox tracks the new position.
        this.setBoundingBox(makeBoundingBox());

        // Pierce entities
        if (!this.level().isClientSide) {
            hitNearbyEntities();
        }

        if (level() instanceof ServerLevel sl && lifeTicks % 3 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void hitNearbyEntities() {
        AABB hitBox = this.getBoundingBox().inflate(HIT_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != owner && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : targets) {
            DamageSource source = NichirinDamageSources.thrownKatana(target, this, owner);

            boolean hurt = NichirinDamageHandler.hurt(target, source, damage);
            if (hurt) {
                if (hitStun > 0) {
                    target.invulnerableTime = hitStun;
                    target.addEffect(new MobEffectInstance(
                            NichirinEffectRegistry.stunned(), hitStun, 1, false, false, true));
                }
                if (owner instanceof Player player) {
                    float actualDamage = NichirinDamageHandler.actualDamageOr(target, damage);
                    ComboIntegration.handleSuccessfulHit(player, target, hitStun, actualDamage);
                }
                Vec3 direction = getDeltaMovement().normalize();
                target.push(direction.x * 0.3, 0.1, direction.z * 0.3);

                hitEntities.add(target.getUUID()); // Pierce: track but don't stop
                level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("LifeTicks");
        stuck = tag.getBoolean("Stuck");
        entityData.set(STUCK, stuck);
        damage = tag.getFloat("Damage");
        hitStun = tag.getInt("HitStun");
        if (tag.contains("ThrownItem")) {
            setThrownItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("ThrownItem")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTicks", lifeTicks);
        tag.putBoolean("Stuck", stuck);
        tag.putFloat("Damage", damage);
        tag.putInt("HitStun", hitStun);
        ItemStack item = getThrownItem();
        if (!item.isEmpty()) {
            tag.put("ThrownItem", item.save(registryAccess(), new CompoundTag()));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }
}
