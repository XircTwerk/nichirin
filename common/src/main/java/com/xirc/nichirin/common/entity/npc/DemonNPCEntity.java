package com.xirc.nichirin.common.entity.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.common.network.s2c.NPCAnimationPacket;

/**
 * Abstract base class for demon NPCs that can use PlayerAnimator animations.
 * Simplified to use standard AI goals like SpecUserMob.
 */
public abstract class DemonNPCEntity extends Monster {

    private static final EntityDataAccessor<String> CURRENT_ANIMATION =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> ANIMATION_RESET =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> DEMON_TYPE =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Float> RENDER_SCALE =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.FLOAT);

    public DemonNPCEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_ANIMATION, "");
        this.entityData.define(ANIMATION_SPEED, 1.0f);
        this.entityData.define(ANIMATION_RESET, false);
        this.entityData.define(DEMON_TYPE, getDefaultDemonType());
        this.entityData.define(RENDER_SCALE, 1.0f);
    }

    /**
     * Override this in subclasses to set the default demon type
     */
    protected abstract String getDefaultDemonType();

    /**
     * Set an animation to play. Similar to SpecUserMob's setAnimation method.
     */
    public void setAnimation(String animationID, float animationSpeed) {
        this.entityData.set(ANIMATION_RESET, this.entityData.get(CURRENT_ANIMATION).equals(animationID));
        this.entityData.set(CURRENT_ANIMATION, animationID);
        this.entityData.set(ANIMATION_SPEED, animationSpeed);

        // Send packet to all nearby players if on server
        if (!this.level().isClientSide && !animationID.isEmpty()) {
            sendAnimationPacketToNearbyPlayers(animationID, false);
        }
    }

    /**
     * Stop current animation
     */
    public void stopAnimation() {
        setAnimation("", 1.0f);

        // Send stop packet to all nearby players if on server
        if (!this.level().isClientSide) {
            sendAnimationPacketToNearbyPlayers("", true);
        }
    }

    /**
     * Get current animation name
     */
    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    /**
     * Get animation speed
     */
    public float getAnimationSpeed() {
        return this.entityData.get(ANIMATION_SPEED);
    }

    /**
     * Check if animation was reset (for client-side animation controllers)
     */
    public boolean wasAnimationReset() {
        return this.entityData.get(ANIMATION_RESET);
    }

    /**
     * Get demon type for texture/model variations
     */
    public String getDemonType() {
        return this.entityData.get(DEMON_TYPE);
    }

    /**
     * Set demon type
     */
    public void setDemonType(String demonType) {
        this.entityData.set(DEMON_TYPE, demonType);
    }

    /**
     * Get render scale
     */
    public float getRenderScale() {
        return this.entityData.get(RENDER_SCALE);
    }

    /**
     * Set render scale
     */
    public void setRenderScale(float scale) {
        this.entityData.set(RENDER_SCALE, scale);
    }

    /**
     * Send animation packet to nearby players
     */
    private void sendAnimationPacketToNearbyPlayers(String animationName, boolean stop) {
        if (this.level().isClientSide) return;

        // Send to all players within tracking range
        this.level().players().stream()
                .filter(player -> player.distanceToSqr(this) < 64 * 64)
                .forEach(player -> {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        NPCAnimationPacket packet = stop ?
                                NPCAnimationPacket.stopAnimation(this.getId()) :
                                NPCAnimationPacket.playAnimation(this.getId(), animationName);
                        NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
                    }
                });
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("current_animation", getCurrentAnimation());
        tag.putFloat("animation_speed", getAnimationSpeed());
        tag.putString("demon_type", getDemonType());
        tag.putFloat("render_scale", getRenderScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("current_animation")) {
            this.entityData.set(CURRENT_ANIMATION, tag.getString("current_animation"));
        }
        if (tag.contains("animation_speed")) {
            this.entityData.set(ANIMATION_SPEED, tag.getFloat("animation_speed"));
        }
        if (tag.contains("demon_type")) {
            this.entityData.set(DEMON_TYPE, tag.getString("demon_type"));
        }
        if (tag.contains("render_scale")) {
            this.entityData.set(RENDER_SCALE, tag.getFloat("render_scale"));
        }
    }

    /**
     * Create base attributes for demon NPCs
     */
    public static AttributeSupplier.Builder createDemonAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        // Reset animation reset flag after one tick
        entityData.set(ANIMATION_RESET, false);

        // Tick demon-specific systems (override in subclasses)
        tickDemonSystems();

        // Auto-stop animations when appropriate (override in subclasses if needed)
        if (!getCurrentAnimation().isEmpty()) {
            handleAnimationTick();
        }
    }

    /**
     * Override this to tick demon-specific systems (movesets, attacks, etc.)
     */
    protected void tickDemonSystems() {
        // Override in subclasses
    }

    /**
     * Handle animation-related tick logic
     */
    protected void handleAnimationTick() {
        // Override in subclasses for custom animation handling
    }

    /**
     * Called when animation finishes (override in subclasses)
     */
    protected void onAnimationComplete(String animationName) {
        // Override in subclasses for custom behavior
    }
}