package com.xirc.nichirin.common.entity.effect;

import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A short-lived cosmetic clone of a player that orbits a center point,
 * looks inward at the center, and periodically swings its weapon.
 */
public class PlayerCloneEntity extends Monster {

    private static final EntityDataAccessor<Optional<UUID>> MASTER_UUID =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> MASTER_NAME =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> PART_MASK =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.BYTE);

    private static final float ORBIT_SPEED = 0.05f;
    private static final int   SWING_INTERVAL = 12; // ticks between attack swings

    private GameProfile gameProfile;

    private double orbitCenterX, orbitCenterY, orbitCenterZ;
    private float  orbitRadius;
    private float  orbitAngle;
    private int    lifetimeTicks;

    public PlayerCloneEntity(EntityType<? extends PlayerCloneEntity> type, Level level) {
        super(type, level);
        this.noCulling  = true;
        this.noPhysics  = true;
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static PlayerCloneEntity create(EntityType<PlayerCloneEntity> type, Level level,
                                            LivingEntity source, Vec3 center, float radius,
                                            float initialAngle, int lifetime) {
        PlayerCloneEntity clone = new PlayerCloneEntity(type, level);
        clone.orbitCenterX  = center.x;
        clone.orbitCenterY  = center.y;
        clone.orbitCenterZ  = center.z;
        clone.orbitRadius   = radius;
        clone.orbitAngle    = initialAngle;
        clone.lifetimeTicks = lifetime;

        clone.entityData.set(MASTER_UUID, Optional.of(source.getUUID()));
        clone.entityData.set(MASTER_NAME, source.getScoreboardName());

        if (source instanceof ServerPlayer sp) {
            byte partMask = 0;
            for (PlayerModelPart part : PlayerModelPart.values()) {
                if (sp.isModelPartShown(part)) partMask |= (byte) part.getMask();
            }
            clone.entityData.set(PART_MASK, partMask);
            clone.setLeftHanded(sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        }

        double startX = center.x + Math.cos(initialAngle) * radius;
        double startZ = center.z + Math.sin(initialAngle) * radius;
        clone.setPos(startX, center.y, startZ);

        // Face inward at spawn
        clone.setYRot(facingYaw(startX, startZ, center.x, center.z));

        return clone;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    /** Call after addFreshEntity so the tracking system sends the equipment packet to clients. */
    public void copyEquipmentFrom(LivingEntity source) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = source.getItemBySlot(slot);
            setItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            lifetimeTicks--;
            if (lifetimeTicks <= 0) {
                discard();
                return;
            }

            orbitAngle += ORBIT_SPEED;
            double newX = orbitCenterX + Math.cos(orbitAngle) * orbitRadius;
            double newZ = orbitCenterZ + Math.sin(orbitAngle) * orbitRadius;
            setPos(newX, orbitCenterY, newZ);

            // Always face inward toward orbit center
            float yaw = facingYaw(newX, newZ, orbitCenterX, orbitCenterZ);
            setYRot(yaw);
            yBodyRot  = yaw;
            yHeadRot  = yaw;

            // Periodic attack swing animation
            if (tickCount % SWING_INTERVAL == 0) {
                swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    private static float facingYaw(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MASTER_UUID, Optional.empty());
        entityData.define(MASTER_NAME, "");
        entityData.define(PART_MASK, (byte) 0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("MasterUUID")) {
            entityData.set(MASTER_UUID, Optional.of(tag.getUUID("MasterUUID")));
            entityData.set(MASTER_NAME, tag.getString("MasterName"));
            entityData.set(PART_MASK, tag.getByte("PartMask"));
        }
        orbitCenterX  = tag.getDouble("ocx");
        orbitCenterY  = tag.getDouble("ocy");
        orbitCenterZ  = tag.getDouble("ocz");
        orbitRadius   = tag.getFloat("orad");
        orbitAngle    = tag.getFloat("oang");
        lifetimeTicks = tag.getInt("life");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID masterId = getMasterUUID();
        if (masterId != null) {
            tag.putUUID("MasterUUID", masterId);
            tag.putString("MasterName", getMasterName());
            tag.putByte("PartMask", getPartMask());
        }
        tag.putDouble("ocx", orbitCenterX);
        tag.putDouble("ocy", orbitCenterY);
        tag.putDouble("ocz", orbitCenterZ);
        tag.putFloat("orad", orbitRadius);
        tag.putFloat("oang", orbitAngle);
        tag.putInt("life", lifetimeTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    public UUID getMasterUUID()  { return entityData.get(MASTER_UUID).orElse(null); }
    public String getMasterName() { return entityData.get(MASTER_NAME); }
    public byte getPartMask()    { return entityData.get(PART_MASK); }

    public GameProfile getGameProfile() {
        UUID id   = getMasterUUID();
        String name = getMasterName();
        if (id == null || name == null || name.isEmpty()) return null;
        if (gameProfile == null || !id.equals(gameProfile.getId()) || !name.equals(gameProfile.getName())) {
            gameProfile = new GameProfile(id, name);
        }
        return gameProfile;
    }

    @Override public boolean shouldDespawnInPeaceful() { return false; }
    @Override protected boolean isAffectedByFluids() { return false; }
    @Override public void push(Entity entity) {}
    @Override public boolean isPushable() { return false; }
}
