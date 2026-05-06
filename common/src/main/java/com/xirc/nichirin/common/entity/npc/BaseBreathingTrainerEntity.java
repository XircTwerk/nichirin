package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import com.xirc.nichirin.common.system.NPCResourceManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class BaseBreathingTrainerEntity extends PathfinderMob implements MovesetCapableNPC {

    private static final EntityDataAccessor<String>  CURRENT_ANIMATION =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   ANIMATION_SPEED   =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ANIMATION_RESET   =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.BOOLEAN);

    protected static final float DUEL_HP               = 100.0f;
    protected static final float PEACEFUL_HP           = 200.0f;
    protected static final float DUEL_WIN_HP_THRESHOLD = 1.0f;
    protected static final float PLAYER_DUEL_MIN_HP    = 0.5f;
    protected static final int   DUEL_COOLDOWN_TICKS   = 20 * 60 * 3;

    public enum TrainerMode { PEACEFUL, DUELING, SELF_DEFENSE }

    public enum DuelDifficulty { EASY, MEDIUM, HARD }

    @Getter protected final TrainerType trainerType;
    @Getter private TrainerMode mode           = TrainerMode.PEACEFUL;
    private UUID        duelPlayerId   = null;
    @Getter private int duelCooldownTicks = 0;

    @Getter private DuelDifficulty duelDifficulty = DuelDifficulty.EASY;
    @Getter private boolean hasBeatenTrainer = false;
    private ServerBossEvent bossBar = null;

    // Provoked combat — tracks who hit us while peaceful so we can warn then fight
    private UUID provokedWarnedBy = null;

    protected AbstractMoveset moveset;
    protected float maxBreathGauge         = 100.0f;
    protected float maxStamina             = 100.0f;
    protected float breathRegenMultiplier  = 2.0f;
    protected float staminaRegenMultiplier = 1.5f;

    protected final Set<Integer> blacklistedMoves = new HashSet<>();
    private static final Map<UUID, Map<Integer, Long>> trainerCooldowns = new HashMap<>();

    /** Tracks which players are currently in a formal duel so other trainers can block interaction. */
    private static final Map<UUID, UUID> playersInDuel = new HashMap<>(); // playerUUID → trainerUUID

    protected BaseBreathingTrainerEntity(EntityType<? extends BaseBreathingTrainerEntity> type,
                                          Level level, TrainerType trainerType) {
        super(type, level);
        this.trainerType = trainerType;
        this.setMaxUpStep(1.0f);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CURRENT_ANIMATION, "");
        entityData.define(ANIMATION_SPEED, 1.0f);
        entityData.define(ANIMATION_RESET, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,           PEACEFUL_HP)
                .add(Attributes.MOVEMENT_SPEED,       0.28)
                .add(Attributes.ATTACK_DAMAGE,        5.0)
                .add(Attributes.ARMOR,                6.0)
                .add(Attributes.FOLLOW_RANGE,         32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TrainerDuelGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE) && super.canUse();
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnType, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, tag);
        equipArmor();
        return result;
    }

    protected void equipArmor() {}


    public void setAnimation(String animationID, float speed) {
        entityData.set(ANIMATION_RESET, entityData.get(CURRENT_ANIMATION).equals(animationID));
        entityData.set(CURRENT_ANIMATION, animationID);
        entityData.set(ANIMATION_SPEED, speed);
    }

    public void stopAnimation() { setAnimation("", 1.0f); }

    public String getCurrentAnimation()  { return entityData.get(CURRENT_ANIMATION); }
    public float  getAnimationSpeed()    { return entityData.get(ANIMATION_SPEED); }
    public boolean wasAnimationReset()   { return entityData.get(ANIMATION_RESET); }


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
        Map<Integer, Long> cooldowns = trainerCooldowns.get(getUUID());
        if (cooldowns != null) {
            Long end = cooldowns.get(moveIndex);
            if (end != null && level().getGameTime() < end) return false;
        }
        if (cfg.hasBreathCost() && getBreathGauge() < cfg.getBreathCostOrDefault(0f)) return false;
        return true;
    }

    @Override
    public void performMovesetMove(int moveIndex) {
        if (!canUseMove(moveIndex)) return;
        AbstractMoveset.MoveConfiguration cfg = moveset.getMove(moveIndex);
        if (cfg == null) return;
        if (cfg.hasBreathCost()) NPCResourceManager.consumeBreath(this, cfg.getBreathCostOrDefault(0f));
        int cd = cfg.getCooldownOrDefault(0);
        if (cd > 0) {
            // Scale cooldown by difficulty — Hard attacks much more frequently
            int scaledCd = Math.max(1, (int)(cd * getDifficultyCooldownScale()));
            trainerCooldowns.computeIfAbsent(getUUID(), k -> new HashMap<>())
                    .put(moveIndex, level().getGameTime() + scaledCd);
        }
        moveset.performMove(this, moveIndex);
    }

    /** Multiplier applied to per-move cooldowns. Hard = 0.35x (very frequent), Easy = 1.3x (slow). */
    protected float getDifficultyCooldownScale() {
        return switch (duelDifficulty) {
            case EASY   -> 1.3f;
            case MEDIUM -> 0.75f;
            case HARD   -> 0.35f;
        };
    }

    @Override
    public void triggerMovesetAnimation(String animationName) { setAnimation(animationName, 1.0f); }

    @Override public int   getBloodPoints()                    { return 0; }
    @Override public void  setBloodPoints(int v)               {}
    @Override public int   getMaxBloodPoints()                 { return 0; }
    @Override public boolean canRegenBlood()                   { return false; }
    @Override public float getBloodRegenMultiplier()           { return 0f; }

    @Override public float getBreathGauge()                    { return NPCResourceManager.getBreathGauge(getUUID(), maxBreathGauge); }
    @Override public void  setBreathGauge(float v)             { NPCResourceManager.setBreathGauge(getUUID(), v, maxBreathGauge); }
    @Override public float getMaxBreathGauge()                 { return maxBreathGauge; }
    @Override public float getBreathRegenMultiplier()          { return breathRegenMultiplier; }

    @Override public float getStamina()                        { return NPCResourceManager.getStamina(getUUID(), maxStamina); }
    @Override public void  setStamina(float v)                 { NPCResourceManager.setStamina(getUUID(), v, maxStamina); }
    @Override public float getMaxStamina()                     { return maxStamina; }
    @Override public float getStaminaRegenMultiplier()         { return staminaRegenMultiplier; }

    @Override public boolean canDoubleJump()                   { return !onGround() && NPCResourceManager.canDoubleJump(getUUID()); }
    @Override public void    markDoubleJumped()                { NPCResourceManager.markDoubleJumped(getUUID()); }
    @Override public void    resetDoubleJump()                 { NPCResourceManager.resetDoubleJump(getUUID()); }
    // isSprinting() / setSprinting() inherited from LivingEntity

    @Override public float getAggression()                     { return 0.6f; }
    @Override public float getDamageMultiplier()               { return 1.0f; }
    @Override public float getAttackSpeedMultiplier()          { return 1.0f; }
    @Override public float getMoveSpeedMultiplier()            { return 1.0f; }
    @Override public Set<Integer> getBlacklistedMoves()        { return blacklistedMoves; }

    @Override
    public void tickMovesetSystems() {
        if (!level().isClientSide) NPCResourceManager.tickNPC(this);
    }


    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.sidedSuccess(true);

        if (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE) {
            sp.displayClientMessage(
                    Component.literal("The spar is not over!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return InteractionResult.CONSUME;
        }

        // Block interaction if this player is sparring with a different trainer
        UUID activeDuelTrainer = playersInDuel.get(sp.getUUID());
        if (activeDuelTrainer != null && !activeDuelTrainer.equals(getUUID())) {
            sp.displayClientMessage(
                    Component.literal("Finish your current spar first.")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return InteractionResult.CONSUME;
        }

        sendDialoguePacket(sp);
        return InteractionResult.SUCCESS;
    }

    private void sendDialoguePacket(ServerPlayer player) {
        OpenTrainerDialoguePacket.DialogueState state;
        if (duelCooldownTicks > 0) {
            state = OpenTrainerDialoguePacket.DialogueState.DUEL_COOLDOWN;
        } else if (ProgressionHelper.isMovesetUnlocked(player, trainerType.movesetId)) {
            state = OpenTrainerDialoguePacket.DialogueState.STUDENT;
        } else if (playerHasPrerequisite(player)) {
            state = OpenTrainerDialoguePacket.DialogueState.PREREQ_MET;
        } else {
            state = OpenTrainerDialoguePacket.DialogueState.STRANGER;
        }

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new OpenTrainerDialoguePacket(this.getUUID(), trainerType, state, hasBeatenTrainer).toBytes(buf);
            NetworkManager.sendToPlayer(player, NichirinPacketRegistry.OPEN_TRAINER_DIALOGUE_ID, buf);
        } catch (Exception ignored) {}
    }

    private boolean playerHasPrerequisite(ServerPlayer player) {
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == trainerType.prerequisiteItem) have += stack.getCount();
        }
        return have >= trainerType.prerequisiteCount;
    }


    /** Backward-compat bridge — defaults to EASY difficulty. */
    public void startDuel(ServerPlayer challenger) {
        startDuel(challenger, DuelDifficulty.EASY);
    }

    public void startDuel(ServerPlayer challenger, DuelDifficulty difficulty) {
        if (mode == TrainerMode.DUELING) return;
        if (duelCooldownTicks > 0) {
            challenger.displayClientMessage(
                    Component.literal("I need time to recover. Come back later.")
                            .withStyle(s -> s.withColor(0xFFAA00)), false);
            return;
        }

        mode           = TrainerMode.DUELING;
        duelPlayerId   = challenger.getUUID();
        duelDifficulty = difficulty;
        playersInDuel.put(challenger.getUUID(), getUUID());

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(DUEL_HP);
        setHealth(DUEL_HP);
        setTarget(challenger);

        // Create boss bar
        bossBar = new ServerBossEvent(
                Component.literal(trainerType.npcName),
                getBossBarColor(),
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.addPlayer(challenger);
        bossBar.setProgress(1.0f);

        challenger.displayClientMessage(
                Component.literal(trainerType.duelStartMsg)
                        .withStyle(s -> s.withColor(trainerType.styleColor)), false);
    }

    protected BossEvent.BossBarColor getBossBarColor() {
        return BossEvent.BossBarColor.WHITE;
    }

    protected void endDuel(boolean playerWon) {
        mode = TrainerMode.PEACEFUL;
        duelCooldownTicks = DUEL_COOLDOWN_TICKS;
        provokedWarnedBy = null;
        if (duelPlayerId != null) playersInDuel.remove(duelPlayerId);
        setTarget(null);

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(PEACEFUL_HP);
        setHealth(PEACEFUL_HP);

        if (playerWon) hasBeatenTrainer = true;

        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }

        if (playerWon && duelPlayerId != null) {
            Player winner = level().getPlayerByUUID(duelPlayerId);
            if (winner instanceof ServerPlayer sp) {
                ProgressionHelper.unlockMoveset(sp, trainerType.movesetId);
                sp.displayClientMessage(
                        Component.literal(trainerType.duelWinMsg)
                                .withStyle(s -> s.withColor(trainerType.styleColor)), false);
            }
        }

        duelPlayerId = null;
    }

    public float getDifficultyDamageMultiplier() {
        return switch (duelDifficulty) {
            case EASY   -> 1f / 3f;
            case MEDIUM -> 2f / 3f;
            case HARD   -> 1f;
        };
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.@NotNull Entity target) {
        if (mode == TrainerMode.DUELING && target instanceof Player p
                && p.getUUID().equals(duelPlayerId)) {
            // If trainer is already near-dead the player is about to win — don't claim trainer wins
            if (getHealth() <= DUEL_WIN_HP_THRESHOLD + 1.0f) return false;
            float atk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (p.getHealth() - atk <= PLAYER_DUEL_MIN_HP) {
                p.setHealth(PLAYER_DUEL_MIN_HP);
                endDuel(false);
                p.displayClientMessage(
                        Component.literal(trainerType.duelLoseMsg)
                                .withStyle(s -> s.withColor(trainerType.styleColor)), false);
                return false;
            }
        }
        return super.doHurtTarget(target);
    }


    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (mode == TrainerMode.DUELING) {
            // Clamp so trainer can't be killed — duel ends at DUEL_WIN_HP_THRESHOLD
            float safe = Math.min(amount, Math.max(0, getHealth() - DUEL_WIN_HP_THRESHOLD));
            boolean hit = safe > 0 && super.hurt(source, safe);
            // End duel whenever health is at threshold, even if this specific hit was
            // blocked by hurtTime immunity (safe=0 or super.hurt returned false).
            if (getHealth() <= DUEL_WIN_HP_THRESHOLD && mode == TrainerMode.DUELING) {
                boolean won = source.getEntity() instanceof Player atk
                        && atk.getUUID().equals(duelPlayerId);
                endDuel(won);
            }
            return hit;
        }

        if (mode == TrainerMode.SELF_DEFENSE) {
            // No restrictions — real fight, trainer can die
            return super.hurt(source, amount);
        }

        // PEACEFUL — provoked combat warning system
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player attacker) {
            if (provokedWarnedBy == null || !provokedWarnedBy.equals(attacker.getUUID())) {
                // First hit: warn
                provokedWarnedBy = attacker.getUUID();
                if (attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(
                            Component.literal(trainerType.npcName + ": Are you sure you want to do this?")
                                    .withStyle(s -> s.withColor(0xFFAA00)));
                }
            } else {
                // Second hit from same player: enter self-defense
                enterSelfDefense(attacker);
            }
        }
        return result;
    }

    protected void enterSelfDefense(Player aggressor) {
        mode = TrainerMode.SELF_DEFENSE;
        duelDifficulty = DuelDifficulty.HARD;
        provokedWarnedBy = null;
        setTarget(aggressor);
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(DUEL_HP);
    }


    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            entityData.set(ANIMATION_RESET, false);
            tickMovesetSystems();
        }

        if (level().isClientSide) return;

        if (duelCooldownTicks > 0) duelCooldownTicks--;

        if (mode == TrainerMode.DUELING && duelPlayerId != null) {
            Player duelist = level().getPlayerByUUID(duelPlayerId);
            if (duelist == null || !duelist.isAlive()) endDuel(false);
        }

        // Self-defense ends when target is gone or trainer is killed (natural death handles the latter)
        if (mode == TrainerMode.SELF_DEFENSE) {
            LivingEntity selfDefenseTarget = getTarget();
            if (selfDefenseTarget == null || !selfDefenseTarget.isAlive()) {
                mode = TrainerMode.PEACEFUL;
                provokedWarnedBy = null;
                setTarget(null);
            }
        }

        if (bossBar != null && mode == TrainerMode.DUELING) {
            bossBar.setProgress(Math.max(0f, Math.min(1f, getHealth() / DUEL_HP)));
        }

        if (onGround()) resetDoubleJump();

        if ((tickCount % 200) == 0) ensureEquipment();
    }

    private void ensureEquipment() {
        if (getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()) {
            equipArmor();
        }
    }


    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            trainerCooldowns.remove(getUUID());
            NPCResourceManager.cleanupNPC(getUUID());
            AbstractMoveset.cleanupEntity(this);
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }
        }
        super.remove(reason);
    }


    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TrainerMode", mode.name());
        tag.putInt("DuelCooldown", duelCooldownTicks);
        if (duelPlayerId != null) tag.putUUID("DuelPlayer", duelPlayerId);
        tag.putFloat("breath_gauge", getBreathGauge());
        tag.putFloat("stamina", getStamina());
        if (moveset != null) tag.putString("moveset_id", moveset.getMovesetId());
        tag.putBoolean("HasBeatenTrainer", hasBeatenTrainer);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try { mode = TrainerMode.valueOf(tag.getString("TrainerMode")); }
        catch (Exception e) { mode = TrainerMode.PEACEFUL; }
        duelCooldownTicks = tag.getInt("DuelCooldown");
        duelPlayerId = tag.hasUUID("DuelPlayer") ? tag.getUUID("DuelPlayer") : null;
        if (mode == TrainerMode.DUELING) { mode = TrainerMode.PEACEFUL; duelPlayerId = null; }
        if (tag.contains("breath_gauge")) setBreathGauge(tag.getFloat("breath_gauge"));
        if (tag.contains("stamina"))      setStamina(tag.getFloat("stamina"));
        hasBeatenTrainer = tag.getBoolean("HasBeatenTrainer");
    }


    @Override public boolean canBeLeashed(@NotNull Player p)    { return false; }
    @Override protected boolean shouldDespawnInPeaceful()       { return false; }
    @Override public boolean removeWhenFarAway(double dist)     { return false; }

    private static class TrainerDuelGoal extends MeleeAttackGoal {
        private final BaseBreathingTrainerEntity trainer;
        TrainerDuelGoal(BaseBreathingTrainerEntity trainer) {
            super(trainer, 1.1, true);
            this.trainer = trainer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        @Override public boolean canUse()           { return trainer.mode == TrainerMode.DUELING && super.canUse(); }
        @Override public boolean canContinueToUse() { return trainer.mode == TrainerMode.DUELING && super.canContinueToUse(); }
        @Override protected double getAttackReachSqr(net.minecraft.world.entity.@NotNull LivingEntity t) { return 9.0; }
        @Override protected int getAttackInterval() { return 25; }
    }
}
