package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.system.NPCResourceManager;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final EntityDataAccessor<Integer> BLOOD_POINTS      =
            SynchedEntityData.defineId(DemonNPCEntity.class, EntityDataSerializers.INT);

    /** The currently active moveset (the one that drives inputs/AI). */
    protected AbstractMoveset moveset;
    /** All movesets this NPC can switch between, keyed by moveset id. Insertion-ordered. */
    protected final Map<String, AbstractMoveset> movesets = new LinkedHashMap<>();

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

    // Keyed by "<activeMovesetId>:<moveIndex>" so two switchable movesets never share a cooldown slot.
    private static final Map<UUID, Map<String, Long>> npcCooldowns = new HashMap<>();
    private float lastBloodSyncedHealth = -1.0F;

    public DemonNPCEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURRENT_ANIMATION, "");
        builder.define(ANIMATION_SPEED, 1.0f);
        builder.define(ANIMATION_RESET, false);
        builder.define(DEMON_TYPE, getDefaultDemonType());
        builder.define(RENDER_SCALE, 1.0f);
        builder.define(BLOOD_POINTS, maxBloodPoints);
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

    /**
     * Sets the active moveset and registers it as available for switching. Kept as the primary
     * single-moveset entry point (backward compatible): NPCs that only ever use one moveset can
     * keep calling this.
     */
    @Override public void setMoveset(AbstractMoveset m) {
        moveset = m;
        if (m != null) movesets.put(m.getMovesetId(), m);
    }

    /**
     * Registers an additional moveset this NPC can {@link #switchMoveset(String) switch} to. The
     * first moveset added also becomes the active one if none is set yet.
     */
    public void addMoveset(AbstractMoveset m) {
        if (m == null) return;
        movesets.put(m.getMovesetId(), m);
        if (moveset == null) moveset = m;
    }

    /**
     * Switches the active moveset to the one registered under {@code movesetId}. Moveset attribute
     * modifiers (e.g. speed) are swapped so they never stack across a switch. Returns {@code true}
     * if the switch happened (id known and not already active).
     */
    public boolean switchMoveset(String movesetId) {
        AbstractMoveset target = movesets.get(movesetId);
        if (target == null || target == moveset) return false;
        if (moveset != null) moveset.removeAllModifiers(this);
        moveset = target;
        if (!level().isClientSide) moveset.applyAllModifiers(this);
        return true;
    }

    public String getActiveMovesetId()                           { return moveset != null ? moveset.getMovesetId() : null; }
    public boolean hasMovesetLoaded(String movesetId)            { return movesets.containsKey(movesetId); }
    public Collection<AbstractMoveset> getLoadedMovesets()       { return movesets.values(); }
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

    public boolean canUseRightClickMove(boolean crouching) {
        if (moveset == null) return false;
        AbstractMoveset.MoveConfiguration cfg = crouching
                ? moveset.getCrouchRightClickConfiguration()
                : moveset.getRightClickConfiguration();
        if (cfg == null) return false;
        int cooldownKey = crouching ? -2 : -1;
        if (isOnCooldown(cooldownKey)) return false;
        if (cfg.hasBreathCost() && getBreathGauge() < cfg.getBreathCostOrDefault(0f)) return false;
        return true;
    }

    public void performRightClickMove(boolean crouching) {
        if (!canUseRightClickMove(crouching)) return;
        AbstractMoveset.MoveConfiguration cfg = crouching
                ? moveset.getCrouchRightClickConfiguration()
                : moveset.getRightClickConfiguration();
        if (cfg == null) return;
        if (cfg.hasBreathCost()) NPCResourceManager.consumeBreath(this, cfg.getBreathCostOrDefault(0f));
        int cooldown = cfg.getCooldownOrDefault(0);
        if (cooldown <= 0) {
            cooldown = cfg.getWindupOrDefault(5) + cfg.getDurationOrDefault(10) + cfg.getRecoveryOrDefault(10);
        }
        setCooldown(crouching ? -2 : -1, cooldown);
        moveset.handleRightClick(this, crouching);
    }

    @Override
    public void triggerMovesetAnimation(String animationName) {
        setAnimation(animationName, 1.0f);
    }


    @Override public int   getBloodPoints()                       { return entityData.get(BLOOD_POINTS); }
    @Override public void  setBloodPoints(int v)                  {
        int clamped = Math.max(0, Math.min(v, maxBloodPoints));
        entityData.set(BLOOD_POINTS, clamped);
        if (!level().isClientSide) {
            NPCResourceManager.setBloodPoints(getUUID(), clamped, maxBloodPoints);
        }
    }
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


    private String cooldownKey(int moveIndex) {
        return getActiveMovesetId() + ":" + moveIndex;
    }

    private boolean isOnCooldown(int moveIndex) {
        Map<String, Long> cooldowns = npcCooldowns.get(getUUID());
        if (cooldowns == null) return false;
        Long end = cooldowns.get(cooldownKey(moveIndex));
        return end != null && level().getGameTime() < end;
    }

    private void setCooldown(int moveIndex, int ticks) {
        if (ticks <= 0) return;
        npcCooldowns.computeIfAbsent(getUUID(), k -> new HashMap<>())
                .put(cooldownKey(moveIndex), level().getGameTime() + ticks);
    }


    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        entityData.set(ANIMATION_RESET, false);
        tickMovesetSystems();
        syncBloodToHealth();
        tickDemonSystems();

        if (!getCurrentAnimation().isEmpty()) handleAnimationTick();

        if (onGround()) resetDoubleJump();
    }

    @Override
    public void tickMovesetSystems() {
        if (!level().isClientSide) NPCResourceManager.tickNPC(this);
    }

    protected void tickDemonSystems() {
        tickSunlightWeakness();
    }
    protected boolean isInLethalSunlight() {
        Level level = level();
        return level.isDay()
                && !level.isRaining()
                && !level.isThundering()
                && level.canSeeSky(blockPosition());
    }

    protected void tickSunlightWeakness() {
        if (!isInLethalSunlight()) return;
        setTarget(null);
        seekNearbyShade();
        igniteForSeconds(4);
        invulnerableTime = 0;
        hurt(NichirinDamageSources.sunlight(this), getMaxHealth() / 120.0f);
        syncBloodToHealth();
    }

    private void syncBloodToHealth() {
        if (getMaxHealth() <= 0.0F) return;
        float health = getHealth();
        if (health <= 0.0F) {
            setBloodPoints(0);
            lastBloodSyncedHealth = 0.0F;
            return;
        }
        if (lastBloodSyncedHealth < 0.0F) {
            lastBloodSyncedHealth = health;
            return;
        }
        if (health < lastBloodSyncedHealth - 0.01F) {
            int blood = Math.max(1, (int) Math.ceil((health / getMaxHealth()) * maxBloodPoints));
            setBloodPoints(Math.min(getBloodPoints(), blood));
        }
        lastBloodSyncedHealth = health;
    }

    private void seekNearbyShade() {
        if (tickCount % 10 != 0 || getNavigation().isInProgress()) return;
        Level level = level();
        for (int i = 0; i < 12; i++) {
            int dx = getRandom().nextIntBetweenInclusive(-16, 16);
            int dz = getRandom().nextIntBetweenInclusive(-16, 16);
            BlockPos surface = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockPosition().offset(dx, 0, dz));
            if (!level.canSeeSky(surface)) {
                getNavigation().moveTo(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D, 1.35D);
                return;
            }
        }
    }

    protected void handleAnimationTick() {}
    protected void onAnimationComplete(String animationName) {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide) {
            syncBloodToHealth();
        }
        return damaged;
    }


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
        if (tag.contains("max_blood_points"))         maxBloodPoints = tag.getInt("max_blood_points");
        if (tag.contains("max_breath_gauge"))         maxBreathGauge = tag.getFloat("max_breath_gauge");
        if (tag.contains("max_stamina"))              maxStamina = tag.getFloat("max_stamina");
        if (tag.contains("blood_points"))             setBloodPoints(tag.getInt("blood_points"));
        if (tag.contains("breath_gauge"))             setBreathGauge(tag.getFloat("breath_gauge"));
        if (tag.contains("stamina"))                  setStamina(tag.getFloat("stamina"));
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
