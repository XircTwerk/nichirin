package com.xirc.nichirin.common.entity.effect;

import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity;
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
 * A short-lived cosmetic clone of a player that swoops through a target from mist,
 * swings its weapon at the center, retreats, then repeats before disappearing.
 */
public class PlayerCloneEntity extends Monster {

    private static final EntityDataAccessor<Optional<UUID>> MASTER_UUID =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> MASTER_NAME =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> PART_MASK =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.BYTE);

    /** Speed in blocks per tick (16 blocks / 20 ticks = 0.8 blocks/tick → center in ~10 ticks). */
    private static final float DASH_SPEED  = 0.8f;
    /** Distance from center where the clone spawns and vanishes. */
    private static final float SPAWN_DIST  = 8f;
    /** How many times to dash through the center before despawning. */
    private static final int   MAX_PASSES  = 3;

    private GameProfile gameProfile;

    private double orbitCenterX, orbitCenterY, orbitCenterZ;
    private int    lifetimeTicks;

    // Swoop state
    private float   spawnAngle;     // radians: angle from center to spawn point
    private int     spawnDelay;     // ticks to wait before the first dash
    private float   dashPosition;   // 0 = spawn edge, 8 = center, 16 = far edge
    private boolean dashingIn;      // true while moving toward far side
    private int     passCount;      // how many times we've reached the far edge
    private boolean swungThisPass;  // gate so we only swing once per pass

    private static final int MAX_LIFETIME = 600; // 30 seconds absolute safety cap

    public PlayerCloneEntity(EntityType<? extends PlayerCloneEntity> type, Level level) {
        super(type, level);
        this.noCulling  = true;
        this.noPhysics  = true;
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    @Override
    public boolean shouldBeSaved() { return false; }

    /**
     * Factory. The clone spawns {@code SPAWN_DIST} blocks from {@code center} at {@code spawnAngle},
     * waits {@code spawnDelay} ticks, then dashes through the center {@code MAX_PASSES} times.
     */
    public static PlayerCloneEntity create(EntityType<PlayerCloneEntity> type, Level level,
                                           LivingEntity source, Vec3 center,
                                           float spawnAngle, int spawnDelay, int lifetime) {
        PlayerCloneEntity clone = new PlayerCloneEntity(type, level);
        clone.orbitCenterX  = center.x;
        clone.orbitCenterY  = center.y;
        clone.orbitCenterZ  = center.z;
        clone.lifetimeTicks = lifetime;

        clone.spawnAngle    = spawnAngle;
        clone.spawnDelay    = spawnDelay;
        clone.dashPosition  = 0f;
        clone.dashingIn     = true;
        clone.passCount     = 0;
        clone.swungThisPass = false;

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

        // Place at spawn position (SPAWN_DIST blocks from center along spawnAngle)
        double startX = center.x + Math.cos(spawnAngle) * SPAWN_DIST;
        double startZ = center.z + Math.sin(spawnAngle) * SPAWN_DIST;
        clone.setPos(startX, center.y, startZ);
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
        if (level().isClientSide) return;

        // Safety cap: discard if alive way too long (e.g. attack reference lost)
        if (tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        lifetimeTicks--;
        if (lifetimeTicks <= 0) {
            spawnMistBurst();
            discard();
            return;
        }

        // Wait for stagger delay before first dash
        if (spawnDelay > 0) {
            spawnDelay--;
            return;
        }

        // Advance dash position
        if (dashingIn) {
            dashPosition += DASH_SPEED;
            if (dashPosition >= 16f) {
                dashPosition = 16f;
                dashingIn    = false;
                passCount++;
                swungThisPass = false;
                if (passCount >= MAX_PASSES) {
                    spawnMistBurst();
                    discard();
                    return;
                }
            }
        } else {
            dashPosition -= DASH_SPEED;
            if (dashPosition <= 0f) {
                dashPosition  = 0f;
                dashingIn     = true;
                swungThisPass = false;
            }
        }

        // pos = center + dir * (SPAWN_DIST - dashPosition)
        // dashPosition 0  → spawn edge (center + dir*8)
        // dashPosition 8  → center     (center + dir*0)
        // dashPosition 16 → far edge   (center - dir*8)
        double dirX = Math.cos(spawnAngle);
        double dirZ = Math.sin(spawnAngle);
        double offset = SPAWN_DIST - dashPosition;
        double newX = orbitCenterX + dirX * offset;
        double newZ = orbitCenterZ + dirZ * offset;
        setPos(newX, orbitCenterY, newZ);

        // Face the direction of movement
        if (dashingIn) {
            // Moving toward far side — face away from spawn (toward -dir)
            setYRot(facingYaw(newX, newZ, orbitCenterX - dirX * SPAWN_DIST, orbitCenterZ - dirZ * SPAWN_DIST));
        } else {
            // Retreating — face back toward spawn
            setYRot(facingYaw(newX, newZ, orbitCenterX + dirX * SPAWN_DIST, orbitCenterZ + dirZ * SPAWN_DIST));
        }
        yBodyRot = getYRot();
        yHeadRot = getYRot();

        // Swing weapon as the clone passes through the center
        if (!swungThisPass && dashPosition >= 8f - DASH_SPEED && dashPosition <= 8f + DASH_SPEED) {
            swing(InteractionHand.MAIN_HAND, true);
            swungThisPass = true;
        }

        // Cloud mist trail during swoop
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    newX, orbitCenterY + 1.0, newZ,
                    3, 0.15, 0.2, 0.15, 0.01);
        }
    }

    private void spawnMistBurst() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 1.0, getZ(),
                    12, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private static float facingYaw(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MASTER_UUID, Optional.empty());
        builder.define(MASTER_NAME, "");
        builder.define(PART_MASK, (byte) 0);
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
        lifetimeTicks = tag.getInt("life");
        spawnAngle    = tag.getFloat("sAng");
        spawnDelay    = tag.getInt("sDel");
        dashPosition  = tag.getFloat("dPos");
        dashingIn     = tag.getBoolean("dIn");
        passCount     = tag.getInt("pCnt");
        swungThisPass = tag.getBoolean("swung");
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
        tag.putInt("life", lifetimeTicks);
        tag.putFloat("sAng", spawnAngle);
        tag.putInt("sDel", spawnDelay);
        tag.putFloat("dPos", dashPosition);
        tag.putBoolean("dIn", dashingIn);
        tag.putInt("pCnt", passCount);
        tag.putBoolean("swung", swungThisPass);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return NetworkManager.createAddEntityPacket(this, entity);
    }

    public UUID getMasterUUID()   { return entityData.get(MASTER_UUID).orElse(null); }
    public String getMasterName() { return entityData.get(MASTER_NAME); }
    public byte getPartMask()     { return entityData.get(PART_MASK); }

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
    @Override protected boolean isAffectedByFluids()   { return false; }
    @Override public void push(Entity entity)          {}
    @Override public boolean isPushable()              { return false; }
}