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

public abstract class DemonNPCEntity extends Monster implements MovesetCapableNPC {

    private static final EntityDataAccessor<String>  CURRENT_ANIMATION =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   ANIMATION_SPEED   =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ANIMATION_RESET   =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String>  DEMON_TYPE        =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   RENDER_SCALE      =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.FLOAT);

    protected AbstractMoveset moveset;

    protected int   maxBloodPoints         = 10;
    protected float maxBreathGauge         = 100.0f;
    protected float maxStamina             = 100.0f;
    protected float aggression             = 0.8f;
    protected float damageMultiplier       = 1.0f;
    protected float attackSpeedMultiplier  = 1.0f;
    protected float moveSpeedMultiplier    = 1.0f;
    protected boolean canRegenBlood        = true;
    protected float bloodRegenMultiplier   = 1.5f;
    protected float breathRegenMultiplier  = 2.0f;
    protected float staminaRegenMultiplier = 1.5f;

    protected final Set<Integer> blacklistedMoves = new HashSet<>();

    private static final Map<UUID, Map<Integer, Long>> npcCooldowns = new HashMap<>();

    public DemonNPCEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CURRENT_ANIMATION, "");
        entityData.define(ANIMATION_SPEED, 1.0f);
        entityData.define(ANIMATION_RESET, false);
        entityData.define(DEMON_TYPE, getDefaultDemonType());
        entityData.define(RENDER_SCALE, 1.0f);
    }

    protected abstract String getDefaultDemonType();


    public void setAnimation(String animationID, float animationSpeed) {
        entityData.set(ANIMATION_RESET, entityData.get(CURRENT_ANIMATION).equals(animationID));
        entityData.set(CURRENT_ANIMATION, animationID);
        entityData.set(ANIMATION_SPEED, animationSpeed);
    }

    public void stopAnimation() {
        setAnimation("", 1.0f);
    }

    public String getCurrentAnimation() {
        return entityData.get(CURRENT_ANIMATION);
    }

    public float getAnimationSpeed() {
        return entityData.get(ANIMATION_SPEED);
    }

    public boolean wasAnimationReset() {
        return entityData.get(ANIMATION_RESET);
    }

    public String getDemonType() {
        return entityData.get(DEMON_TYPE);
    }

    public void setDemonType(String demonType) {
        entityData.set(DEMON_TYPE, demonType);
    }

    public float getRenderScale() {
        return entityData.get(RENDER_SCALE);
    }

    public void setRenderScale(float scale) {
        entityData.set(RENDER_SCALE, scale);
    }


    @Override public AbstractMoveset getMoveset()                { return moveset; }
    @Override public void setMoveset(AbstractMoveset m)          { moveset = m; }
    @Override public UUID getEntityUUID()                        { return getUUID(); }
    @Override public Level getEntityLevel()                      { return level(); }
    @Override public LivingEntity asLivingEntity()               { return this; }

    @Override
    public boolean canUseMove(int moveIndex) {
        if (isMoveBlacklisted(moveIndex) || moveset == null) return false;
        AbstractMoveset.MoveConfiguration cfg = moveset.getMove(moveIndex);
        if (cfg == null) return false;
        if (isOnCooldown(moveIndex)) return false;
        if (cfg.hasBreathCost() && getBreathGauge() < cfg.getBreathCostOrDefault(0f)) return false;
        return true;
    }

    @Override
    public void performMovesetMove(int moveIndex) {
        if (!canUseMove(moveIndex)) return;
        AbstractMoveset.MoveConfiguration cfg = moveset.getMove(moveIndex);
        if (cfg == null) return;
        if (cfg.hasBreathCost()) NPCResourceManager.consumeBreath(this, cfg.getBreathCostOrDefault(0f));
        setCooldown(moveIndex, cfg.getCooldownOrDefault(0));
        moveset.performMove(this, moveIndex);
    }

    @Override
    public void triggerMovesetAnimation(String animationName) {
        setAnimation(animationName, 1.0f);
    }


    @Override public int   getBloodPoints()                       { return NPCResourceManager.getBloodPoints(getUUID(), maxBloodPoints); }
    @Override public void  setBloodPoints(int v)                  { NPCResourceManager.setBloodPoints(getUUID(), v, maxBloodPoints); }
    @Override public int   getMaxBloodPoints()                    { return maxBloodPoints; }
    @Override public boolean canRegenBlood()                      { return canRegenBlood; }
    @Override public float getBloodRegenMultiplier()              { return bloodRegenMultiplier; }


    @Override public float getBreathGauge()                       { return NPCResourceManager.getBreathGauge(getUUID(), maxBreathGauge); }
    @Override public void  setBreathGauge(float v)                { NPCResourceManager.setBreathGauge(getUUID(), v, maxBreathGauge); }
    @Override public float getMaxBreathGauge()                    { return maxBreathGauge; }
    @Override public float getBreathRegenMultiplier()             { return breathRegenMultiplier; }


    @Override public float getStamina()                           { return NPCResourceManager.getStamina(getUUID(), maxStamina); }
    @Override public void  setStamina(float v)                    { NPCResourceManager.setStamina(getUUID(), v, maxStamina); }
    @Override public float getMaxStamina()                        { return maxStamina; }
    @Override public float getStaminaRegenMultiplier()            { return staminaRegenMultiplier; }


    @Override public boolean canDoubleJump()                      { return !onGround() && NPCResourceManager.canDoubleJump(getUUID()); }
    @Override public void    markDoubleJumped()                   { NPCResourceManager.markDoubleJumped(getUUID()); }
    @Override public void    resetDoubleJump()                    { NPCResourceManager.resetDoubleJump(getUUID()); }
    // isSprinting() / setSprinting() are satisfied by LivingEntity inheritance


    @Override public float getAggression()                        { return aggression; }
    @Override public float getDamageMultiplier()                  { return damageMultiplier; }
    @Override public float getAttackSpeedMultiplier()             { return attackSpeedMultiplier; }
    @Override public float getMoveSpeedMultiplier()               { return moveSpeedMultiplier; }
    @Override public Set<Integer> getBlacklistedMoves()           { return blacklistedMoves; }


    private boolean isOnCooldown(int moveIndex) {
        Map<Integer, Long> cooldowns = npcCooldowns.get(getUUID());
        if (cooldowns == null) return false;
        Long end = cooldowns.get(moveIndex);
        return end != null && level().getGameTime() < end;
    }

    private void setCooldown(int moveIndex, int ticks) {
        if (ticks <= 0) return;
        npcCooldowns.computeIfAbsent(getUUID(), k -> new HashMap<>())
                .put(moveIndex, level().getGameTime() + ticks);
    }


    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        entityData.set(ANIMATION_RESET, false);
        tickMovesetSystems();
        tickDemonSystems();

        if (!getCurrentAnimation().isEmpty()) handleAnimationTick();

        if (onGround()) resetDoubleJump();
    }

    @Override
    public void tickMovesetSystems() {
        if (!level().isClientSide) NPCResourceManager.tickNPC(this);
    }

    protected void tickDemonSystems() {}
    protected void handleAnimationTick() {}
    protected void onAnimationComplete(String animationName) {}


    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            npcCooldowns.remove(getUUID());
            NPCResourceManager.cleanupNPC(getUUID());
            AbstractMoveset.cleanupEntity(this);
        }
        super.remove(reason);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("current_animation", getCurrentAnimation());
        tag.putFloat("animation_speed", getAnimationSpeed());
        tag.putString("demon_type", getDemonType());
        tag.putFloat("render_scale", getRenderScale());
        if (moveset != null) tag.putString("moveset_id", moveset.getMovesetId());
        tag.putInt("blood_points", getBloodPoints());
        tag.putFloat("breath_gauge", getBreathGauge());
        tag.putFloat("stamina", getStamina());
        tag.putInt("max_blood_points", maxBloodPoints);
        tag.putFloat("max_breath_gauge", maxBreathGauge);
        tag.putFloat("max_stamina", maxStamina);
        tag.putFloat("aggression", aggression);
        tag.putFloat("damage_multiplier", damageMultiplier);
        tag.putFloat("attack_speed_multiplier", attackSpeedMultiplier);
        tag.putFloat("move_speed_multiplier", moveSpeedMultiplier);
        tag.putBoolean("can_regen_blood", canRegenBlood);
        tag.putFloat("blood_regen_multiplier", bloodRegenMultiplier);
        tag.putFloat("breath_regen_multiplier", breathRegenMultiplier);
        tag.putFloat("stamina_regen_multiplier", staminaRegenMultiplier);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("current_animation"))        entityData.set(CURRENT_ANIMATION, tag.getString("current_animation"));
        if (tag.contains("animation_speed"))          entityData.set(ANIMATION_SPEED, tag.getFloat("animation_speed"));
        if (tag.contains("demon_type"))               entityData.set(DEMON_TYPE, tag.getString("demon_type"));
        if (tag.contains("render_scale"))             entityData.set(RENDER_SCALE, tag.getFloat("render_scale"));
        if (tag.contains("blood_points"))             setBloodPoints(tag.getInt("blood_points"));
        if (tag.contains("breath_gauge"))             setBreathGauge(tag.getFloat("breath_gauge"));
        if (tag.contains("stamina"))                  setStamina(tag.getFloat("stamina"));
        if (tag.contains("max_blood_points"))         maxBloodPoints = tag.getInt("max_blood_points");
        if (tag.contains("max_breath_gauge"))         maxBreathGauge = tag.getFloat("max_breath_gauge");
        if (tag.contains("max_stamina"))              maxStamina = tag.getFloat("max_stamina");
        if (tag.contains("aggression"))               aggression = tag.getFloat("aggression");
        if (tag.contains("damage_multiplier"))        damageMultiplier = tag.getFloat("damage_multiplier");
        if (tag.contains("attack_speed_multiplier"))  attackSpeedMultiplier = tag.getFloat("attack_speed_multiplier");
        if (tag.contains("move_speed_multiplier"))    moveSpeedMultiplier = tag.getFloat("move_speed_multiplier");
        if (tag.contains("can_regen_blood"))          canRegenBlood = tag.getBoolean("can_regen_blood");
        if (tag.contains("blood_regen_multiplier"))   bloodRegenMultiplier = tag.getFloat("blood_regen_multiplier");
        if (tag.contains("breath_regen_multiplier"))  breathRegenMultiplier = tag.getFloat("breath_regen_multiplier");
        if (tag.contains("stamina_regen_multiplier")) staminaRegenMultiplier = tag.getFloat("stamina_regen_multiplier");
    }


    public static AttributeSupplier.Builder createDemonAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,    40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE,  6.0)
                .add(Attributes.ARMOR,          2.0)
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }
}
