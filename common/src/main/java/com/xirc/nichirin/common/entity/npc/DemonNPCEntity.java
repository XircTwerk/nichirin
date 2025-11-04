package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.system.NPCResourceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for demon NPCs with moveset support
 * Implements MovesetCapableNPC for full moveset integration
 */
public abstract class DemonNPCEntity extends Monster implements MovesetCapableNPC {

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

    // Moveset system
    protected AbstractMoveset moveset;

    // Configurable NPC properties
    protected int maxBloodPoints = 10;
    protected float maxBreathGauge = 100.0f;
    protected float aggression = 0.8f; // 80% aggressive by default
    protected float damageMultiplier = 1.0f;
    protected float attackSpeedMultiplier = 1.0f;
    protected float moveSpeedMultiplier = 1.0f;
    protected boolean canRegenBlood = true;
    protected float bloodRegenMultiplier = 1.5f; // Faster regen than players
    protected float breathRegenMultiplier = 2.0f; // Much faster breath regen

    // Blacklisted moves (by index)
    protected final Set<Integer> blacklistedMoves = new HashSet<>();

    // Cooldown tracking per NPC
    private static final Map<UUID, Map<Integer, Long>> npcCooldowns = new HashMap<>();

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

    protected abstract String getDefaultDemonType();

    // ========== ANIMATION METHODS ==========

    public void setAnimation(String animationID, float animationSpeed) {
        this.entityData.set(ANIMATION_RESET, this.entityData.get(CURRENT_ANIMATION).equals(animationID));
        this.entityData.set(CURRENT_ANIMATION, animationID);
        this.entityData.set(ANIMATION_SPEED, animationSpeed);
    }

    public void stopAnimation() {
        setAnimation("", 1.0f);
    }

    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    public float getAnimationSpeed() {
        return this.entityData.get(ANIMATION_SPEED);
    }

    public boolean wasAnimationReset() {
        return this.entityData.get(ANIMATION_RESET);
    }

    public String getDemonType() {
        return this.entityData.get(DEMON_TYPE);
    }

    public void setDemonType(String demonType) {
        this.entityData.set(DEMON_TYPE, demonType);
    }

    public float getRenderScale() {
        return this.entityData.get(RENDER_SCALE);
    }

    public void setRenderScale(float scale) {
        this.entityData.set(RENDER_SCALE, scale);
    }

    // ========== MOVESET CAPABLE NPC IMPLEMENTATION ==========

    @Override
    public AbstractMoveset getMoveset() {
        return moveset;
    }

    @Override
    public void setMoveset(AbstractMoveset moveset) {
        this.moveset = moveset;
    }

    @Override
    public UUID getEntityUUID() {
        return this.getUUID();
    }

    @Override
    public Level getEntityLevel() {
        return this.level();
    }

    @Override
    public LivingEntity asLivingEntity() {
        return this;
    }

    @Override
    public boolean canUseMove(int moveIndex) {
        // Check if move is blacklisted
        if (isMoveBlacklisted(moveIndex)) {
            return false;
        }

        // Check if moveset exists
        if (moveset == null) {
            return false;
        }

        // Check if move exists
        AbstractMoveset.MoveConfiguration config = moveset.getMove(moveIndex);
        if (config == null) {
            return false;
        }

        // Check cooldown
        if (isOnCooldown(moveIndex)) {
            return false;
        }

        // Check breath cost
        if (config.hasBreathCost()) {
            float breathCost = config.getBreathCostOrDefault(0f);
            if (getBreathGauge() < breathCost) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void performMovesetMove(int moveIndex) {
        if (!canUseMove(moveIndex)) {
            return;
        }

        AbstractMoveset.MoveConfiguration config = moveset.getMove(moveIndex);
        if (config == null) {
            return;
        }

        // Consume breath
        if (config.hasBreathCost()) {
            float breathCost = config.getBreathCostOrDefault(0f);
            NPCResourceManager.consumeBreath(this, breathCost);
        }

        // Set cooldown
        setCooldown(moveIndex, config.getCooldownOrDefault(0));

        // Execute the move
        moveset.performMove(this, moveIndex);
    }

    @Override
    public void triggerMovesetAnimation(String animationName) {
        setAnimation(animationName, 1.0f);
    }

    @Override
    public int getBloodPoints() {
        return NPCResourceManager.getBloodPoints(getEntityUUID(), maxBloodPoints);
    }

    @Override
    public void setBloodPoints(int bloodPoints) {
        NPCResourceManager.setBloodPoints(getEntityUUID(), bloodPoints, maxBloodPoints);
    }

    @Override
    public int getMaxBloodPoints() {
        return maxBloodPoints;
    }

    @Override
    public float getBreathGauge() {
        return NPCResourceManager.getBreathGauge(getEntityUUID(), maxBreathGauge);
    }

    @Override
    public void setBreathGauge(float breath) {
        NPCResourceManager.setBreathGauge(getEntityUUID(), breath, maxBreathGauge);
    }

    @Override
    public float getMaxBreathGauge() {
        return maxBreathGauge;
    }

    @Override
    public float getAggression() {
        return aggression;
    }

    @Override
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    @Override
    public float getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }

    @Override
    public float getMoveSpeedMultiplier() {
        return moveSpeedMultiplier;
    }

    @Override
    public boolean canRegenBlood() {
        return canRegenBlood;
    }

    @Override
    public float getBloodRegenMultiplier() {
        return bloodRegenMultiplier;
    }

    @Override
    public float getBreathRegenMultiplier() {
        return breathRegenMultiplier;
    }

    @Override
    public Set<Integer> getBlacklistedMoves() {
        return blacklistedMoves;
    }

    @Override
    public void tickMovesetSystems() {
        if (!this.level().isClientSide) {
            NPCResourceManager.tickNPC(this);
        }
    }

    // ========== COOLDOWN SYSTEM ==========

    private boolean isOnCooldown(int moveIndex) {
        Map<Integer, Long> cooldowns = npcCooldowns.get(this.getUUID());
        if (cooldowns == null) return false;

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) return false;

        return this.level().getGameTime() < cooldownEnd;
    }

    private void setCooldown(int moveIndex, int ticks) {
        if (ticks <= 0) return;

        long cooldownEnd = this.level().getGameTime() + ticks;
        npcCooldowns.computeIfAbsent(this.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    // ========== TICK AND LIFECYCLE ==========

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        // Reset animation flag
        entityData.set(ANIMATION_RESET, false);

        // Tick moveset systems (blood regen, breath regen)
        tickMovesetSystems();

        // Custom demon systems
        tickDemonSystems();

        // Handle animation tick
        if (!getCurrentAnimation().isEmpty()) {
            handleAnimationTick();
        }
    }

    protected void tickDemonSystems() {
        // Override in subclasses for custom behavior
    }

    protected void handleAnimationTick() {
        // Override in subclasses for animation handling
    }

    protected void onAnimationComplete(String animationName) {
        // Override in subclasses
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            npcCooldowns.remove(this.getUUID());
            NPCResourceManager.cleanupNPC(this.getUUID());
            AbstractMoveset.cleanupEntity(this);
        }

        super.remove(reason);
    }

    // ========== NBT SAVE/LOAD ==========

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("current_animation", getCurrentAnimation());
        tag.putFloat("animation_speed", getAnimationSpeed());
        tag.putString("demon_type", getDemonType());
        tag.putFloat("render_scale", getRenderScale());

        // Save moveset data
        if (moveset != null) {
            tag.putString("moveset_id", moveset.getMovesetId());
        }

        // Save blood and breath
        tag.putInt("blood_points", getBloodPoints());
        tag.putFloat("breath_gauge", getBreathGauge());

        // Save configuration
        tag.putInt("max_blood_points", maxBloodPoints);
        tag.putFloat("max_breath_gauge", maxBreathGauge);
        tag.putFloat("aggression", aggression);
        tag.putFloat("damage_multiplier", damageMultiplier);
        tag.putFloat("attack_speed_multiplier", attackSpeedMultiplier);
        tag.putFloat("move_speed_multiplier", moveSpeedMultiplier);
        tag.putBoolean("can_regen_blood", canRegenBlood);
        tag.putFloat("blood_regen_multiplier", bloodRegenMultiplier);
        tag.putFloat("breath_regen_multiplier", breathRegenMultiplier);
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

        // Load blood and breath
        if (tag.contains("blood_points")) {
            setBloodPoints(tag.getInt("blood_points"));
        }
        if (tag.contains("breath_gauge")) {
            setBreathGauge(tag.getFloat("breath_gauge"));
        }

        // Load configuration
        if (tag.contains("max_blood_points")) {
            maxBloodPoints = tag.getInt("max_blood_points");
        }
        if (tag.contains("max_breath_gauge")) {
            maxBreathGauge = tag.getFloat("max_breath_gauge");
        }
        if (tag.contains("aggression")) {
            aggression = tag.getFloat("aggression");
        }
        if (tag.contains("damage_multiplier")) {
            damageMultiplier = tag.getFloat("damage_multiplier");
        }
        if (tag.contains("attack_speed_multiplier")) {
            attackSpeedMultiplier = tag.getFloat("attack_speed_multiplier");
        }
        if (tag.contains("move_speed_multiplier")) {
            moveSpeedMultiplier = tag.getFloat("move_speed_multiplier");
        }
        if (tag.contains("can_regen_blood")) {
            canRegenBlood = tag.getBoolean("can_regen_blood");
        }
        if (tag.contains("blood_regen_multiplier")) {
            bloodRegenMultiplier = tag.getFloat("blood_regen_multiplier");
        }
        if (tag.contains("breath_regen_multiplier")) {
            breathRegenMultiplier = tag.getFloat("breath_regen_multiplier");
        }
    }

    // ========== ATTRIBUTES ==========

    public static AttributeSupplier.Builder createDemonAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }
}